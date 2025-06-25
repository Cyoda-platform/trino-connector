/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cyoda.connector.auth;

import com.cyoda.connector.CyodaConnectorFactory;
import com.google.common.collect.ImmutableSet;
import io.trino.spi.QueryId;
import io.trino.spi.connector.CatalogSchemaName;
import io.trino.spi.connector.CatalogSchemaRoutineName;
import io.trino.spi.connector.CatalogSchemaTableName;
import io.trino.spi.connector.EntityKindAndName;
import io.trino.spi.connector.EntityPrivilege;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.eventlistener.EventListener;
import io.trino.spi.function.SchemaFunctionName;
import io.trino.spi.security.AccessDeniedException;
import io.trino.spi.security.Identity;
import io.trino.spi.security.Privilege;
import io.trino.spi.security.SystemAccessControl;
import io.trino.spi.security.SystemAccessControlFactory;
import io.trino.spi.security.SystemSecurityContext;
import io.trino.spi.security.TrinoPrincipal;

import java.security.Principal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.trino.spi.security.AccessDeniedException.denyCreateCatalog;
import static io.trino.spi.security.AccessDeniedException.denyCreateRole;
import static io.trino.spi.security.AccessDeniedException.denyDenyEntityPrivilege;
import static io.trino.spi.security.AccessDeniedException.denyDenySchemaPrivilege;
import static io.trino.spi.security.AccessDeniedException.denyDenyTablePrivilege;
import static io.trino.spi.security.AccessDeniedException.denyDropCatalog;
import static io.trino.spi.security.AccessDeniedException.denyDropRole;
import static io.trino.spi.security.AccessDeniedException.denyGrantEntityPrivilege;
import static io.trino.spi.security.AccessDeniedException.denyGrantRoles;
import static io.trino.spi.security.AccessDeniedException.denyGrantSchemaPrivilege;
import static io.trino.spi.security.AccessDeniedException.denyGrantTablePrivilege;
import static io.trino.spi.security.AccessDeniedException.denyKillQuery;
import static io.trino.spi.security.AccessDeniedException.denyReadSystemInformationAccess;
import static io.trino.spi.security.AccessDeniedException.denyRevokeEntityPrivilege;
import static io.trino.spi.security.AccessDeniedException.denyRevokeRoles;
import static io.trino.spi.security.AccessDeniedException.denyRevokeSchemaPrivilege;
import static io.trino.spi.security.AccessDeniedException.denyRevokeTablePrivilege;
import static io.trino.spi.security.AccessDeniedException.denySetCatalogSessionProperty;
import static io.trino.spi.security.AccessDeniedException.denySetSchemaAuthorization;
import static io.trino.spi.security.AccessDeniedException.denySetSystemSessionProperty;
import static io.trino.spi.security.AccessDeniedException.denySetTableAuthorization;
import static io.trino.spi.security.AccessDeniedException.denyShowSchemas;
import static io.trino.spi.security.AccessDeniedException.denyViewQuery;
import static io.trino.spi.security.AccessDeniedException.denyWriteSystemInformationAccess;
import static java.util.Collections.emptySet;

public class CyodaSystemAccessControl
        implements SystemAccessControl
{
    public static final String NAME = "cyoda";
    private static final String SYSTEM_CATALOG_NAME = "system"; // required for jdbc client introspection
    public static final HashSet<String> DEFAULT_CATALOGS = new HashSet<>(List.of(CyodaConnectorFactory.CATALOG_NAME, SYSTEM_CATALOG_NAME));
    public static final HashSet<String> RESTRICTED_SCHEMAS = new HashSet<>(List.of(CyodaConnectorFactory.MAINTENANCE_SCHEMA_NAME));

    public static class Factory
            implements SystemAccessControlFactory
    {
        private final CyodaAuthorizationManager cyodaAuthorization;

        public Factory(CyodaAuthorizationManager cyodaAuthorization) {
            this.cyodaAuthorization = cyodaAuthorization;
        }

        @Override
        public String getName()
        {
            return NAME;
        }

        @Override
        public SystemAccessControl create(Map<String, String> config)
        {
            return new CyodaSystemAccessControl(cyodaAuthorization, config);
        }
    }

    private final CyodaAuthorizationManager authHandler;
    private final boolean isTestingMode;

    public CyodaSystemAccessControl(CyodaAuthorizationManager authHandler, Map<String, String> config) {
        this.authHandler = authHandler;
        this.isTestingMode = config.getOrDefault("access-control.testing-mode", "false").equals("true");
    }

    private String getPrincipal(Optional<Principal> optPrincipal){
        if (optPrincipal.isPresent()){
            return optPrincipal.get().toString();
        } else
            throw new AccessDeniedException("Identity have no principal");
    }
    private TimedAuth verifyAuth(Identity identity) {
        String principal = getPrincipal(identity.getPrincipal());
        if (!isTestingMode) {
            return authHandler.verifyAuth(principal);
        } else
            return new TimedAuth.DummyTimedAuth(principal);
    }
    private boolean hasNoAdminRights(Identity identity){
        TimedAuth timedAuth = verifyAuth(identity);
        return !timedAuth.getUserInfo().isAdmin();
    }
    private Set<String> getAvailableCatalogs(Identity identity) {
        return DEFAULT_CATALOGS;
    }
    private Set<String> getRestrictedSchemas(Identity identity) {
        return RESTRICTED_SCHEMAS;
    }
    private void verifyDDL(SystemSecurityContext context, String what) {
        Identity identity = context.getIdentity();
        if (hasNoAdminRights(identity)) {
            throw new AccessDeniedException("Cannot "+ what +" from here, please use Cyoda UI for that.");
        }
    }

    @Override
    public void checkCanSetUser(Optional<Principal> principal, String userName) {
        if (!isTestingMode) {
            authHandler.verifyAuth(getPrincipal(principal));
        }
    }

    @Override
    public void checkCanImpersonateUser(Identity identity, String userName) {
        TimedAuth timedAuth = verifyAuth(identity);
        if (!timedAuth.hasUserName(userName)) {
            throw new AccessDeniedException("User \"" + userName + "\" cannot be impersonated by user \"" + identity.getUser() + "\"");
        }
    }

    @Override
    public void checkCanReadSystemInformation(Identity identity)
    {
        if (hasNoAdminRights(identity)) {
            denyReadSystemInformationAccess();
        }
    }

    @Override
    public void checkCanWriteSystemInformation(Identity identity)
    {
        if (hasNoAdminRights(identity)) {
            denyWriteSystemInformationAccess();
        }
    }

    @Override
    public void checkCanExecuteQuery(Identity identity, QueryId queryId)
    {
        verifyAuth(identity);
    }

    @Override
    public void checkCanViewQueryOwnedBy(Identity identity, Identity queryOwner)
    {
        if (hasNoAdminRights(identity)) {
            denyViewQuery();
        }
    }

    @Override
    public void checkCanKillQueryOwnedBy(Identity identity, Identity queryOwner)
    {
        if (hasNoAdminRights(identity)) {
            denyKillQuery();
        }
    }

    @Override
    public Collection<Identity> filterViewQueryOwnedBy(Identity identity, Collection<Identity> queryOwners)
    {
        if (hasNoAdminRights(identity)) {
            return emptySet();
        } else {
            return queryOwners;
        }
    }

    @Override
    public void checkCanSetSystemSessionProperty(Identity identity, QueryId queryId, String propertyName)
    {
        if (hasNoAdminRights(identity)) {
            denySetSystemSessionProperty(propertyName);
        }
    }

    @Override
    public boolean canAccessCatalog(SystemSecurityContext context, String catalogName)
    {
        Identity identity = context.getIdentity();
        if (hasNoAdminRights(identity)) {
            return getAvailableCatalogs(identity).contains(catalogName);
        } else return true;
    }

    @Override
    public void checkCanCreateCatalog(SystemSecurityContext context, String catalog)
    {
        Identity identity = context.getIdentity();
        if (hasNoAdminRights(identity)) {
            denyCreateCatalog(catalog);
        }
    }

    @Override
    public void checkCanDropCatalog(SystemSecurityContext context, String catalog)
    {
        Identity identity = context.getIdentity();
        if (hasNoAdminRights(identity)) {
            denyDropCatalog(catalog);
        }
    }

    @Override
    public Set<String> filterCatalogs(SystemSecurityContext context, Set<String> catalogs)
    {
        Identity identity = context.getIdentity();
        if (hasNoAdminRights(identity)) {
            return getAvailableCatalogs(identity);
        } else return catalogs;
    }

    @Override
    public void checkCanCreateSchema(SystemSecurityContext context, CatalogSchemaName schema, Map<String, Object> properties)
    {
        verifyDDL(context, "CREATE SCHEMA");
    }

    @Override
    public void checkCanDropSchema(SystemSecurityContext context, CatalogSchemaName schema)
    {
        verifyDDL(context, "DROP SCHEMA");
    }

    @Override
    public void checkCanRenameSchema(SystemSecurityContext context, CatalogSchemaName schema, String newSchemaName)
    {
        verifyDDL(context, "RENAME SCHEMA");
    }

    @Override
    public void checkCanSetSchemaAuthorization(SystemSecurityContext context, CatalogSchemaName schema, TrinoPrincipal principal)
    {
        Identity identity = context.getIdentity();
        if (hasNoAdminRights(identity)) {
            denySetSchemaAuthorization(schema.toString(), principal);
        }
    }

    @Override
    public void checkCanShowSchemas(SystemSecurityContext context, String catalogName)
    {
        if (!canAccessCatalog(context, catalogName)) {
            denyShowSchemas();
        }
    }

    @Override
    public Set<String> filterSchemas(SystemSecurityContext context, String catalogName, Set<String> schemaNames)
    {
        if (!canAccessCatalog(context, catalogName)) {
            return emptySet();
        }
        Identity identity = context.getIdentity();
        if (hasNoAdminRights(identity)){
            Set<String> restrictedSchemas = getRestrictedSchemas(identity);
            return schemaNames.stream().filter(schemaName -> !restrictedSchemas.contains(schemaName)).collect(Collectors.toSet());
        } else return schemaNames;
    }

    @Override
    public void checkCanShowCreateSchema(SystemSecurityContext context, CatalogSchemaName schemaName)
    {
        Identity identity = context.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public void checkCanShowCreateTable(SystemSecurityContext context, CatalogSchemaTableName table)
    {
        Identity identity = context.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public void checkCanCreateTable(SystemSecurityContext context, CatalogSchemaTableName table, Map<String, Object> properties)
    {
        verifyDDL(context, "CREATE TABLE");
    }

    @Override
    public void checkCanDropTable(SystemSecurityContext context, CatalogSchemaTableName table)
    {
        verifyDDL(context, "DROP TABLE");
    }

    @Override
    public void checkCanRenameTable(SystemSecurityContext context, CatalogSchemaTableName table, CatalogSchemaTableName newTable)
    {
        verifyDDL(context, "RENAME TABLE");
    }

    @Override
    public void checkCanSetTableProperties(SystemSecurityContext context, CatalogSchemaTableName table, Map<String, Optional<Object>> properties)
    {
        Identity identity = context.getIdentity();
        verifyAuth(identity);
        //TODO not sure if this needs to be restricted
    }

    @Override
    public void checkCanSetTableComment(SystemSecurityContext context, CatalogSchemaTableName table)
    {
        Identity identity = context.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public void checkCanSetViewComment(SystemSecurityContext context, CatalogSchemaTableName view)
    {
        Identity identity = context.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public void checkCanSetColumnComment(SystemSecurityContext context, CatalogSchemaTableName table)
    {
        Identity identity = context.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public void checkCanShowTables(SystemSecurityContext context, CatalogSchemaName schema)
    {
        Identity identity = context.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public Set<SchemaTableName> filterTables(SystemSecurityContext context, String catalogName, Set<SchemaTableName> tableNames)
    {
        return tableNames;
    }

    @Override
    public void checkCanShowColumns(SystemSecurityContext context, CatalogSchemaTableName table)
    {
        Identity identity = context.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public Map<SchemaTableName, Set<String>> filterColumns(SystemSecurityContext context, String catalogName, Map<SchemaTableName, Set<String>> tableColumns)
    {
        return tableColumns;
    }

    @Override
    public void checkCanAddColumn(SystemSecurityContext context, CatalogSchemaTableName table)
    {
        verifyDDL(context, "CREATE COLUMN");
    }

    @Override
    public void checkCanDropColumn(SystemSecurityContext context, CatalogSchemaTableName table)
    {
        verifyDDL(context, "DROP COLUMN");
    }

    @Override
    public void checkCanRenameColumn(SystemSecurityContext context, CatalogSchemaTableName table)
    {
        verifyDDL(context, "RENAME COLUMN");
    }

    @Override
    public void checkCanAlterColumn(SystemSecurityContext context, CatalogSchemaTableName table)
    {
        verifyDDL(context, "ALTER COLUMN");
    }

    @Override
    public void checkCanSetTableAuthorization(SystemSecurityContext context, CatalogSchemaTableName table, TrinoPrincipal principal)
    {
        Identity identity = context.getIdentity();
        if (hasNoAdminRights(identity)) {
            denySetTableAuthorization(table.toString(), principal);
        }
    }

    @Override
    public void checkCanSelectFromColumns(SystemSecurityContext context, CatalogSchemaTableName table, Set<String> columns)
    {
    }

    @Override
    public void checkCanInsertIntoTable(SystemSecurityContext context, CatalogSchemaTableName table)
    {
    }

    @Override
    public void checkCanDeleteFromTable(SystemSecurityContext context, CatalogSchemaTableName table)
    {
    }

    @Override
    public void checkCanTruncateTable(SystemSecurityContext context, CatalogSchemaTableName table)
    {
    }

    @Override
    public void checkCanUpdateTableColumns(SystemSecurityContext securityContext, CatalogSchemaTableName table, Set<String> updatedColumnNames)
    {
    }

    @Override
    public void checkCanCreateView(SystemSecurityContext context, CatalogSchemaTableName view)
    {
        Identity identity = context.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public void checkCanRenameView(SystemSecurityContext context, CatalogSchemaTableName view, CatalogSchemaTableName newView)
    {
        Identity identity = context.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public void checkCanSetViewAuthorization(SystemSecurityContext context, CatalogSchemaTableName view, TrinoPrincipal principal)
    {
        Identity identity = context.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public void checkCanDropView(SystemSecurityContext context, CatalogSchemaTableName view)
    {
        Identity identity = context.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public void checkCanCreateViewWithSelectFromColumns(SystemSecurityContext context, CatalogSchemaTableName table, Set<String> columns)
    {
        Identity identity = context.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public void checkCanCreateMaterializedView(SystemSecurityContext context, CatalogSchemaTableName materializedView, Map<String, Object> properties)
    {
        Identity identity = context.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public void checkCanRefreshMaterializedView(SystemSecurityContext context, CatalogSchemaTableName materializedView)
    {
        Identity identity = context.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public void checkCanDropMaterializedView(SystemSecurityContext context, CatalogSchemaTableName materializedView)
    {
        Identity identity = context.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public void checkCanRenameMaterializedView(SystemSecurityContext context, CatalogSchemaTableName view, CatalogSchemaTableName newView)
    {
        Identity identity = context.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public void checkCanSetMaterializedViewProperties(SystemSecurityContext context, CatalogSchemaTableName materializedView, Map<String, Optional<Object>> properties)
    {
        Identity identity = context.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public void checkCanSetCatalogSessionProperty(SystemSecurityContext context, String catalogName, String propertyName)
    {
        Identity identity = context.getIdentity();
        if (hasNoAdminRights(identity)) {
            denySetCatalogSessionProperty(propertyName);
        }
    }

    @Override
    public void checkCanGrantSchemaPrivilege(SystemSecurityContext context, Privilege privilege, CatalogSchemaName schema, TrinoPrincipal grantee, boolean grantOption)
    {
        Identity identity = context.getIdentity();
        if (hasNoAdminRights(identity)) {
            denyGrantSchemaPrivilege(privilege.toString(), schema.toString());
        }
    }

    @Override
    public void checkCanDenySchemaPrivilege(SystemSecurityContext context, Privilege privilege, CatalogSchemaName schema, TrinoPrincipal grantee)
    {
        Identity identity = context.getIdentity();
        if (hasNoAdminRights(identity)) {
            denyDenySchemaPrivilege(privilege.toString(), schema.toString());
        }
    }

    @Override
    public void checkCanRevokeSchemaPrivilege(SystemSecurityContext context, Privilege privilege, CatalogSchemaName schema, TrinoPrincipal revokee, boolean grantOption)
    {
        Identity identity = context.getIdentity();
        if (hasNoAdminRights(identity)) {
            denyRevokeSchemaPrivilege(privilege.toString(), schema.toString());
        }
    }

    @Override
    public void checkCanGrantTablePrivilege(SystemSecurityContext context, Privilege privilege, CatalogSchemaTableName table, TrinoPrincipal grantee, boolean grantOption)
    {
        Identity identity = context.getIdentity();
        if (hasNoAdminRights(identity)) {
            denyGrantTablePrivilege(privilege.toString(), table.toString());
        }
    }

    @Override
    public void checkCanDenyTablePrivilege(SystemSecurityContext context, Privilege privilege, CatalogSchemaTableName table, TrinoPrincipal grantee)
    {
        Identity identity = context.getIdentity();
        if (hasNoAdminRights(identity)) {
            denyDenyTablePrivilege(privilege.toString(), table.toString());
        }
    }

    @Override
    public void checkCanRevokeTablePrivilege(SystemSecurityContext context, Privilege privilege, CatalogSchemaTableName table, TrinoPrincipal revokee, boolean grantOption)
    {
        Identity identity = context.getIdentity();
        if (hasNoAdminRights(identity)) {
            denyRevokeTablePrivilege(privilege.toString(), table.toString());
        }
    }

    @Override
    public void checkCanGrantEntityPrivilege(SystemSecurityContext context, EntityPrivilege privilege, EntityKindAndName entity, TrinoPrincipal grantee, boolean grantOption)
    {
        Identity identity = context.getIdentity();
        if (hasNoAdminRights(identity)) {
            denyGrantEntityPrivilege(privilege.toString(), entity);
        }
    }

    @Override
    public void checkCanDenyEntityPrivilege(SystemSecurityContext context, EntityPrivilege privilege, EntityKindAndName entity, TrinoPrincipal grantee)
    {
        Identity identity = context.getIdentity();
        if (hasNoAdminRights(identity)) {
            denyDenyEntityPrivilege(privilege.toString(), entity);
        }
    }

    @Override
    public void checkCanRevokeEntityPrivilege(SystemSecurityContext context, EntityPrivilege privilege, EntityKindAndName entity, TrinoPrincipal revokee, boolean grantOption)
    {
        Identity identity = context.getIdentity();
        if (hasNoAdminRights(identity)) {
            denyRevokeEntityPrivilege(privilege.toString(), entity);
        }
    }

    @Override
    public void checkCanShowRoles(SystemSecurityContext context)
    {
        Identity identity = context.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public void checkCanCreateRole(SystemSecurityContext context, String role, Optional<TrinoPrincipal> grantor)
    {
        Identity identity = context.getIdentity();
        if (hasNoAdminRights(identity)) {
            denyCreateRole(role);
        }
    }

    @Override
    public void checkCanDropRole(SystemSecurityContext context, String role)
    {
        Identity identity = context.getIdentity();
        if (hasNoAdminRights(identity)) {
            denyDropRole(role);
        }
    }

    @Override
    public void checkCanGrantRoles(
            SystemSecurityContext context,
            Set<String> roles,
            Set<TrinoPrincipal> grantees,
            boolean adminOption,
            Optional<TrinoPrincipal> grantor)
    {
        Identity identity = context.getIdentity();
        if (hasNoAdminRights(identity)) {
            denyGrantRoles(roles, grantees);
        }
    }

    @Override
    public void checkCanRevokeRoles(
            SystemSecurityContext context,
            Set<String> roles,
            Set<TrinoPrincipal> grantees,
            boolean adminOption,
            Optional<TrinoPrincipal> grantor)
    {
        Identity identity = context.getIdentity();
        if (hasNoAdminRights(identity)) {
            denyRevokeRoles(roles, grantees);
        }
    }

    @Override
    public void checkCanShowCurrentRoles(SystemSecurityContext context)
    {
        Identity identity = context.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public void checkCanShowRoleGrants(SystemSecurityContext context)
    {
        Identity identity = context.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public void checkCanExecuteProcedure(SystemSecurityContext systemSecurityContext, CatalogSchemaRoutineName procedure)
    {
        Identity identity = systemSecurityContext.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public boolean canExecuteFunction(SystemSecurityContext systemSecurityContext, CatalogSchemaRoutineName functionName)
    {
        Identity identity = systemSecurityContext.getIdentity();
        verifyAuth(identity);
        return true;
    }

    @Override
    public boolean canCreateViewWithExecuteFunction(SystemSecurityContext systemSecurityContext, CatalogSchemaRoutineName functionName)
    {
        Identity identity = systemSecurityContext.getIdentity();
        verifyAuth(identity);
        return true;
    }

    @Override
    public void checkCanExecuteTableProcedure(SystemSecurityContext systemSecurityContext, CatalogSchemaTableName table, String procedure)
    {
        Identity identity = systemSecurityContext.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public void checkCanShowFunctions(SystemSecurityContext context, CatalogSchemaName schema)
    {
        Identity identity = context.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public Set<SchemaFunctionName> filterFunctions(SystemSecurityContext context, String catalogName, Set<SchemaFunctionName> functionNames)
    {
        return functionNames;
    }

    @Override
    public void checkCanCreateFunction(SystemSecurityContext systemSecurityContext, CatalogSchemaRoutineName functionName)
    {
        Identity identity = systemSecurityContext.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public void checkCanDropFunction(SystemSecurityContext systemSecurityContext, CatalogSchemaRoutineName functionName)
    {
        Identity identity = systemSecurityContext.getIdentity();
        verifyAuth(identity);
    }

    @Override
    public Iterable<EventListener> getEventListeners()
    {
        return ImmutableSet.of();
    }

}
