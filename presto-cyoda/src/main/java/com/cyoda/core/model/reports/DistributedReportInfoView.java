/*
 * Copyright (C) 2022 Cyoda Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.cyoda.core.model.reports;

import com.cyoda.core.reports.columns.ReportColumns;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import java.util.Date;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@BeanDefinition
public class DistributedReportInfoView implements ImmutableBean {
    
    // Fields from DistributedReportStatisticsCql

    @JsonProperty
    @PropertyDefinition
    private final String id;

    @JsonProperty
    @PropertyDefinition
    private final Date createTime;

    @JsonProperty
    @PropertyDefinition
    private final Date finishTime;

    @JsonProperty
    @PropertyDefinition
    private final boolean reportFailed;

    @JsonProperty
    @PropertyDefinition
    private final boolean secondPhaseFinished;

    @JsonProperty
    @PropertyDefinition
    private final int groupsCount;

    @JsonProperty
    @PropertyDefinition
    private final long totalRowsCount;

    @JsonProperty
    @PropertyDefinition
    private final boolean markedAsCancelled;

    @JsonProperty
    @PropertyDefinition
    private final Map<String, Integer> rowsCountForFinishedShards;

    @JsonProperty
    @PropertyDefinition
    private final Map<String, String> reportFailedShards;

    // Fields from DistributedReport

    @JsonProperty
    @PropertyDefinition
    private final String userName;

    @JsonProperty
    @PropertyDefinition
    private final UUID userId;

    @JsonProperty
    @PropertyDefinition
    private final String configName;

    @JsonProperty
    @PropertyDefinition
    private final String gridConfigId;

    @JsonProperty
    @PropertyDefinition
    private final Long version; //timestamp of report

    @JsonProperty
    @PropertyDefinition
    private final Date pointTime;

    @JsonProperty
    @PropertyDefinition
    private final Date valuationPointTime;

    @JsonProperty
    @PropertyDefinition
    private final String description;

    // Fields from DRGroupingInfo
    @JsonProperty
    @PropertyDefinition
    private final UUID groupingVersion;

    @JsonProperty
    @PropertyDefinition
    private final boolean hierarchy;

    @JsonProperty
    @PropertyDefinition
    private final boolean regroupingPossible;

    @JsonProperty
    @PropertyDefinition
    private final ReportColumns groupingCols;

    public DistributedReportInfoView(
            @JsonProperty("id") String id,
            @JsonProperty("createTime") Date createTime,
            @JsonProperty("finishTime") Date finishTime,
            @JsonProperty("reportFailed") boolean reportFailed,
            @JsonProperty("secondPhaseFinished") boolean secondPhaseFinished,
            @JsonProperty("groupsCount") int groupsCount,
            @JsonProperty("totalRowsCount") long totalRowsCount,
            @JsonProperty("markedAsCancelled") boolean markedAsCancelled,
            @JsonProperty("rowsCountForFinishedShards") Map<String, Integer> rowsCountForFinishedShards,
            @JsonProperty("reportFailedShards") Map<String, String> reportFailedShards,
            @JsonProperty("userName") String userName,
            @JsonProperty("userId") UUID userId,
            @JsonProperty("configName") String configName,
            @JsonProperty("gridConfigId") String gridConfigId,
            @JsonProperty("version") Long version,
            @JsonProperty("pointTime") Date pointTime,
            @JsonProperty("valuationPointTime") Date valuationPointTime,
            @JsonProperty("description") String description,
            @JsonProperty("groupingVersion") UUID groupingVersion,
            @JsonProperty("hierarchy") boolean hierarchy,
            @JsonProperty("regroupingPossible") boolean regroupingPossible,
            @JsonProperty("groupingCols") ReportColumns groupingCols
    ) {
        
        this.id = id;
        this.createTime = createTime;
        this.finishTime = finishTime;
        this.reportFailed = reportFailed;
        this.secondPhaseFinished = secondPhaseFinished;
        this.groupsCount = groupsCount;
        this.totalRowsCount = totalRowsCount;
        this.markedAsCancelled = markedAsCancelled;
        this.rowsCountForFinishedShards = rowsCountForFinishedShards;
        this.reportFailedShards = reportFailedShards;
        this.userName = userName;
        this.userId = userId;
        this.configName = configName;
        this.gridConfigId = gridConfigId;
        this.version = version;
        this.pointTime = pointTime;
        this.valuationPointTime = valuationPointTime;
        this.description = description;
        this.groupingVersion = groupingVersion;
        this.hierarchy = hierarchy;
        this.regroupingPossible = regroupingPossible;
        this.groupingCols = groupingCols;
    }

    //------------------------- AUTOGENERATED START -------------------------
    /**
     * The meta-bean for {@code DistributedReportInfoView}.
     * @return the meta-bean, not null
     */
    public static DistributedReportInfoView.Meta meta() {
        return DistributedReportInfoView.Meta.INSTANCE;
    }

    static {
        MetaBean.register(DistributedReportInfoView.Meta.INSTANCE);
    }

    /**
     * Returns a builder used to create an instance of the bean.
     * @return the builder, not null
     */
    public static DistributedReportInfoView.Builder builder() {
        return new DistributedReportInfoView.Builder();
    }

    /**
     * Restricted constructor.
     * @param builder  the builder to copy from, not null
     */
    protected DistributedReportInfoView(DistributedReportInfoView.Builder builder) {
        this.id = builder.id;
        this.createTime = (builder.createTime != null ? (Date) builder.createTime.clone() : null);
        this.finishTime = (builder.finishTime != null ? (Date) builder.finishTime.clone() : null);
        this.reportFailed = builder.reportFailed;
        this.secondPhaseFinished = builder.secondPhaseFinished;
        this.groupsCount = builder.groupsCount;
        this.totalRowsCount = builder.totalRowsCount;
        this.markedAsCancelled = builder.markedAsCancelled;
        this.rowsCountForFinishedShards = (builder.rowsCountForFinishedShards != null ? ImmutableMap.copyOf(builder.rowsCountForFinishedShards) : null);
        this.reportFailedShards = (builder.reportFailedShards != null ? ImmutableMap.copyOf(builder.reportFailedShards) : null);
        this.userName = builder.userName;
        this.userId = builder.userId;
        this.configName = builder.configName;
        this.gridConfigId = builder.gridConfigId;
        this.version = builder.version;
        this.pointTime = (builder.pointTime != null ? (Date) builder.pointTime.clone() : null);
        this.valuationPointTime = (builder.valuationPointTime != null ? (Date) builder.valuationPointTime.clone() : null);
        this.description = builder.description;
        this.groupingVersion = builder.groupingVersion;
        this.hierarchy = builder.hierarchy;
        this.regroupingPossible = builder.regroupingPossible;
        this.groupingCols = builder.groupingCols;
    }

    @Override
    public DistributedReportInfoView.Meta metaBean() {
        return DistributedReportInfoView.Meta.INSTANCE;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the id.
     * @return the value of the property
     */
    public String getId() {
        return id;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the createTime.
     * @return the value of the property
     */
    public Date getCreateTime() {
        return (createTime != null ? (Date) createTime.clone() : null);
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the finishTime.
     * @return the value of the property
     */
    public Date getFinishTime() {
        return (finishTime != null ? (Date) finishTime.clone() : null);
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the reportFailed.
     * @return the value of the property
     */
    public boolean isReportFailed() {
        return reportFailed;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the secondPhaseFinished.
     * @return the value of the property
     */
    public boolean isSecondPhaseFinished() {
        return secondPhaseFinished;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the groupsCount.
     * @return the value of the property
     */
    public int getGroupsCount() {
        return groupsCount;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the totalRowsCount.
     * @return the value of the property
     */
    public long getTotalRowsCount() {
        return totalRowsCount;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the markedAsCancelled.
     * @return the value of the property
     */
    public boolean isMarkedAsCancelled() {
        return markedAsCancelled;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the rowsCountForFinishedShards.
     * @return the value of the property
     */
    public Map<String, Integer> getRowsCountForFinishedShards() {
        return rowsCountForFinishedShards;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the reportFailedShards.
     * @return the value of the property
     */
    public Map<String, String> getReportFailedShards() {
        return reportFailedShards;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the userName.
     * @return the value of the property
     */
    public String getUserName() {
        return userName;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the userId.
     * @return the value of the property
     */
    public UUID getUserId() {
        return userId;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the configName.
     * @return the value of the property
     */
    public String getConfigName() {
        return configName;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the gridConfigId.
     * @return the value of the property
     */
    public String getGridConfigId() {
        return gridConfigId;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the version.
     * @return the value of the property
     */
    public Long getVersion() {
        return version;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the pointTime.
     * @return the value of the property
     */
    public Date getPointTime() {
        return (pointTime != null ? (Date) pointTime.clone() : null);
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the valuationPointTime.
     * @return the value of the property
     */
    public Date getValuationPointTime() {
        return (valuationPointTime != null ? (Date) valuationPointTime.clone() : null);
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the description.
     * @return the value of the property
     */
    public String getDescription() {
        return description;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the groupingVersion.
     * @return the value of the property
     */
    public UUID getGroupingVersion() {
        return groupingVersion;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the hierarchy.
     * @return the value of the property
     */
    public boolean isHierarchy() {
        return hierarchy;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the regroupingPossible.
     * @return the value of the property
     */
    public boolean isRegroupingPossible() {
        return regroupingPossible;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the groupingCols.
     * @return the value of the property
     */
    public ReportColumns getGroupingCols() {
        return groupingCols;
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a builder that allows this bean to be mutated.
     * @return the mutable builder, not null
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj != null && obj.getClass() == this.getClass()) {
            DistributedReportInfoView other = (DistributedReportInfoView) obj;
            return JodaBeanUtils.equal(id, other.id) &&
                    JodaBeanUtils.equal(createTime, other.createTime) &&
                    JodaBeanUtils.equal(finishTime, other.finishTime) &&
                    (reportFailed == other.reportFailed) &&
                    (secondPhaseFinished == other.secondPhaseFinished) &&
                    (groupsCount == other.groupsCount) &&
                    (totalRowsCount == other.totalRowsCount) &&
                    (markedAsCancelled == other.markedAsCancelled) &&
                    JodaBeanUtils.equal(rowsCountForFinishedShards, other.rowsCountForFinishedShards) &&
                    JodaBeanUtils.equal(reportFailedShards, other.reportFailedShards) &&
                    JodaBeanUtils.equal(userName, other.userName) &&
                    JodaBeanUtils.equal(userId, other.userId) &&
                    JodaBeanUtils.equal(configName, other.configName) &&
                    JodaBeanUtils.equal(gridConfigId, other.gridConfigId) &&
                    JodaBeanUtils.equal(version, other.version) &&
                    JodaBeanUtils.equal(pointTime, other.pointTime) &&
                    JodaBeanUtils.equal(valuationPointTime, other.valuationPointTime) &&
                    JodaBeanUtils.equal(description, other.description) &&
                    JodaBeanUtils.equal(groupingVersion, other.groupingVersion) &&
                    (hierarchy == other.hierarchy) &&
                    (regroupingPossible == other.regroupingPossible) &&
                    JodaBeanUtils.equal(groupingCols, other.groupingCols);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = getClass().hashCode();
        hash = hash * 31 + JodaBeanUtils.hashCode(id);
        hash = hash * 31 + JodaBeanUtils.hashCode(createTime);
        hash = hash * 31 + JodaBeanUtils.hashCode(finishTime);
        hash = hash * 31 + JodaBeanUtils.hashCode(reportFailed);
        hash = hash * 31 + JodaBeanUtils.hashCode(secondPhaseFinished);
        hash = hash * 31 + JodaBeanUtils.hashCode(groupsCount);
        hash = hash * 31 + JodaBeanUtils.hashCode(totalRowsCount);
        hash = hash * 31 + JodaBeanUtils.hashCode(markedAsCancelled);
        hash = hash * 31 + JodaBeanUtils.hashCode(rowsCountForFinishedShards);
        hash = hash * 31 + JodaBeanUtils.hashCode(reportFailedShards);
        hash = hash * 31 + JodaBeanUtils.hashCode(userName);
        hash = hash * 31 + JodaBeanUtils.hashCode(userId);
        hash = hash * 31 + JodaBeanUtils.hashCode(configName);
        hash = hash * 31 + JodaBeanUtils.hashCode(gridConfigId);
        hash = hash * 31 + JodaBeanUtils.hashCode(version);
        hash = hash * 31 + JodaBeanUtils.hashCode(pointTime);
        hash = hash * 31 + JodaBeanUtils.hashCode(valuationPointTime);
        hash = hash * 31 + JodaBeanUtils.hashCode(description);
        hash = hash * 31 + JodaBeanUtils.hashCode(groupingVersion);
        hash = hash * 31 + JodaBeanUtils.hashCode(hierarchy);
        hash = hash * 31 + JodaBeanUtils.hashCode(regroupingPossible);
        hash = hash * 31 + JodaBeanUtils.hashCode(groupingCols);
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(736);
        buf.append("DistributedReportInfoView{");
        int len = buf.length();
        toString(buf);
        if (buf.length() > len) {
            buf.setLength(buf.length() - 2);
        }
        buf.append('}');
        return buf.toString();
    }

    protected void toString(StringBuilder buf) {
        buf.append("id").append('=').append(JodaBeanUtils.toString(id)).append(',').append(' ');
        buf.append("createTime").append('=').append(JodaBeanUtils.toString(createTime)).append(',').append(' ');
        buf.append("finishTime").append('=').append(JodaBeanUtils.toString(finishTime)).append(',').append(' ');
        buf.append("reportFailed").append('=').append(JodaBeanUtils.toString(reportFailed)).append(',').append(' ');
        buf.append("secondPhaseFinished").append('=').append(JodaBeanUtils.toString(secondPhaseFinished)).append(',').append(' ');
        buf.append("groupsCount").append('=').append(JodaBeanUtils.toString(groupsCount)).append(',').append(' ');
        buf.append("totalRowsCount").append('=').append(JodaBeanUtils.toString(totalRowsCount)).append(',').append(' ');
        buf.append("markedAsCancelled").append('=').append(JodaBeanUtils.toString(markedAsCancelled)).append(',').append(' ');
        buf.append("rowsCountForFinishedShards").append('=').append(JodaBeanUtils.toString(rowsCountForFinishedShards)).append(',').append(' ');
        buf.append("reportFailedShards").append('=').append(JodaBeanUtils.toString(reportFailedShards)).append(',').append(' ');
        buf.append("userName").append('=').append(JodaBeanUtils.toString(userName)).append(',').append(' ');
        buf.append("userId").append('=').append(JodaBeanUtils.toString(userId)).append(',').append(' ');
        buf.append("configName").append('=').append(JodaBeanUtils.toString(configName)).append(',').append(' ');
        buf.append("gridConfigId").append('=').append(JodaBeanUtils.toString(gridConfigId)).append(',').append(' ');
        buf.append("version").append('=').append(JodaBeanUtils.toString(version)).append(',').append(' ');
        buf.append("pointTime").append('=').append(JodaBeanUtils.toString(pointTime)).append(',').append(' ');
        buf.append("valuationPointTime").append('=').append(JodaBeanUtils.toString(valuationPointTime)).append(',').append(' ');
        buf.append("description").append('=').append(JodaBeanUtils.toString(description)).append(',').append(' ');
        buf.append("groupingVersion").append('=').append(JodaBeanUtils.toString(groupingVersion)).append(',').append(' ');
        buf.append("hierarchy").append('=').append(JodaBeanUtils.toString(hierarchy)).append(',').append(' ');
        buf.append("regroupingPossible").append('=').append(JodaBeanUtils.toString(regroupingPossible)).append(',').append(' ');
        buf.append("groupingCols").append('=').append(JodaBeanUtils.toString(groupingCols)).append(',').append(' ');
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-bean for {@code DistributedReportInfoView}.
     */
    public static class Meta extends DirectMetaBean {
        /**
         * The singleton instance of the meta-bean.
         */
        static final Meta INSTANCE = new Meta();

        /**
         * The meta-property for the {@code id} property.
         */
        private final MetaProperty<String> id = DirectMetaProperty.ofImmutable(
                this, "id", DistributedReportInfoView.class, String.class);
        /**
         * The meta-property for the {@code createTime} property.
         */
        private final MetaProperty<Date> createTime = DirectMetaProperty.ofImmutable(
                this, "createTime", DistributedReportInfoView.class, Date.class);
        /**
         * The meta-property for the {@code finishTime} property.
         */
        private final MetaProperty<Date> finishTime = DirectMetaProperty.ofImmutable(
                this, "finishTime", DistributedReportInfoView.class, Date.class);
        /**
         * The meta-property for the {@code reportFailed} property.
         */
        private final MetaProperty<Boolean> reportFailed = DirectMetaProperty.ofImmutable(
                this, "reportFailed", DistributedReportInfoView.class, Boolean.TYPE);
        /**
         * The meta-property for the {@code secondPhaseFinished} property.
         */
        private final MetaProperty<Boolean> secondPhaseFinished = DirectMetaProperty.ofImmutable(
                this, "secondPhaseFinished", DistributedReportInfoView.class, Boolean.TYPE);
        /**
         * The meta-property for the {@code groupsCount} property.
         */
        private final MetaProperty<Integer> groupsCount = DirectMetaProperty.ofImmutable(
                this, "groupsCount", DistributedReportInfoView.class, Integer.TYPE);
        /**
         * The meta-property for the {@code totalRowsCount} property.
         */
        private final MetaProperty<Long> totalRowsCount = DirectMetaProperty.ofImmutable(
                this, "totalRowsCount", DistributedReportInfoView.class, Long.TYPE);
        /**
         * The meta-property for the {@code markedAsCancelled} property.
         */
        private final MetaProperty<Boolean> markedAsCancelled = DirectMetaProperty.ofImmutable(
                this, "markedAsCancelled", DistributedReportInfoView.class, Boolean.TYPE);
        /**
         * The meta-property for the {@code rowsCountForFinishedShards} property.
         */
        @SuppressWarnings({"unchecked", "rawtypes" })
        private final MetaProperty<Map<String, Integer>> rowsCountForFinishedShards = DirectMetaProperty.ofImmutable(
                this, "rowsCountForFinishedShards", DistributedReportInfoView.class, (Class) Map.class);
        /**
         * The meta-property for the {@code reportFailedShards} property.
         */
        @SuppressWarnings({"unchecked", "rawtypes" })
        private final MetaProperty<Map<String, String>> reportFailedShards = DirectMetaProperty.ofImmutable(
                this, "reportFailedShards", DistributedReportInfoView.class, (Class) Map.class);
        /**
         * The meta-property for the {@code userName} property.
         */
        private final MetaProperty<String> userName = DirectMetaProperty.ofImmutable(
                this, "userName", DistributedReportInfoView.class, String.class);
        /**
         * The meta-property for the {@code userId} property.
         */
        private final MetaProperty<UUID> userId = DirectMetaProperty.ofImmutable(
                this, "userId", DistributedReportInfoView.class, UUID.class);
        /**
         * The meta-property for the {@code configName} property.
         */
        private final MetaProperty<String> configName = DirectMetaProperty.ofImmutable(
                this, "configName", DistributedReportInfoView.class, String.class);
        /**
         * The meta-property for the {@code gridConfigId} property.
         */
        private final MetaProperty<String> gridConfigId = DirectMetaProperty.ofImmutable(
                this, "gridConfigId", DistributedReportInfoView.class, String.class);
        /**
         * The meta-property for the {@code version} property.
         */
        private final MetaProperty<Long> version = DirectMetaProperty.ofImmutable(
                this, "version", DistributedReportInfoView.class, Long.class);
        /**
         * The meta-property for the {@code pointTime} property.
         */
        private final MetaProperty<Date> pointTime = DirectMetaProperty.ofImmutable(
                this, "pointTime", DistributedReportInfoView.class, Date.class);
        /**
         * The meta-property for the {@code valuationPointTime} property.
         */
        private final MetaProperty<Date> valuationPointTime = DirectMetaProperty.ofImmutable(
                this, "valuationPointTime", DistributedReportInfoView.class, Date.class);
        /**
         * The meta-property for the {@code description} property.
         */
        private final MetaProperty<String> description = DirectMetaProperty.ofImmutable(
                this, "description", DistributedReportInfoView.class, String.class);
        /**
         * The meta-property for the {@code groupingVersion} property.
         */
        private final MetaProperty<UUID> groupingVersion = DirectMetaProperty.ofImmutable(
                this, "groupingVersion", DistributedReportInfoView.class, UUID.class);
        /**
         * The meta-property for the {@code hierarchy} property.
         */
        private final MetaProperty<Boolean> hierarchy = DirectMetaProperty.ofImmutable(
                this, "hierarchy", DistributedReportInfoView.class, Boolean.TYPE);
        /**
         * The meta-property for the {@code regroupingPossible} property.
         */
        private final MetaProperty<Boolean> regroupingPossible = DirectMetaProperty.ofImmutable(
                this, "regroupingPossible", DistributedReportInfoView.class, Boolean.TYPE);
        /**
         * The meta-property for the {@code groupingCols} property.
         */
        private final MetaProperty<ReportColumns> groupingCols = DirectMetaProperty.ofImmutable(
                this, "groupingCols", DistributedReportInfoView.class, ReportColumns.class);
        /**
         * The meta-properties.
         */
        private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
                this, null,
                "id",
                "createTime",
                "finishTime",
                "reportFailed",
                "secondPhaseFinished",
                "groupsCount",
                "totalRowsCount",
                "markedAsCancelled",
                "rowsCountForFinishedShards",
                "reportFailedShards",
                "userName",
                "userId",
                "configName",
                "gridConfigId",
                "version",
                "pointTime",
                "valuationPointTime",
                "description",
                "groupingVersion",
                "hierarchy",
                "regroupingPossible",
                "groupingCols");

        /**
         * Restricted constructor.
         */
        protected Meta() {
        }

        @Override
        protected MetaProperty<?> metaPropertyGet(String propertyName) {
            switch (propertyName.hashCode()) {
                case 3355:  // id
                    return id;
                case 1369213417:  // createTime
                    return createTime;
                case 1151521280:  // finishTime
                    return finishTime;
                case 213393041:  // reportFailed
                    return reportFailed;
                case -211268871:  // secondPhaseFinished
                    return secondPhaseFinished;
                case 1630112667:  // groupsCount
                    return groupsCount;
                case -1591291950:  // totalRowsCount
                    return totalRowsCount;
                case 1979766131:  // markedAsCancelled
                    return markedAsCancelled;
                case -1621482374:  // rowsCountForFinishedShards
                    return rowsCountForFinishedShards;
                case 664454726:  // reportFailedShards
                    return reportFailedShards;
                case -266666762:  // userName
                    return userName;
                case -836030906:  // userId
                    return userId;
                case 831324397:  // configName
                    return configName;
                case 1685196131:  // gridConfigId
                    return gridConfigId;
                case 351608024:  // version
                    return version;
                case 1564529789:  // pointTime
                    return pointTime;
                case -1843609732:  // valuationPointTime
                    return valuationPointTime;
                case -1724546052:  // description
                    return description;
                case -93847979:  // groupingVersion
                    return groupingVersion;
                case 2115146293:  // hierarchy
                    return hierarchy;
                case 1375666119:  // regroupingPossible
                    return regroupingPossible;
                case -69040490:  // groupingCols
                    return groupingCols;
            }
            return super.metaPropertyGet(propertyName);
        }

        @Override
        public DistributedReportInfoView.Builder builder() {
            return new DistributedReportInfoView.Builder();
        }

        @Override
        public Class<? extends DistributedReportInfoView> beanType() {
            return DistributedReportInfoView.class;
        }

        @Override
        public Map<String, MetaProperty<?>> metaPropertyMap() {
            return metaPropertyMap$;
        }

        //-----------------------------------------------------------------------
        /**
         * The meta-property for the {@code id} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<String> id() {
            return id;
        }

        /**
         * The meta-property for the {@code createTime} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<Date> createTime() {
            return createTime;
        }

        /**
         * The meta-property for the {@code finishTime} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<Date> finishTime() {
            return finishTime;
        }

        /**
         * The meta-property for the {@code reportFailed} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<Boolean> reportFailed() {
            return reportFailed;
        }

        /**
         * The meta-property for the {@code secondPhaseFinished} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<Boolean> secondPhaseFinished() {
            return secondPhaseFinished;
        }

        /**
         * The meta-property for the {@code groupsCount} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<Integer> groupsCount() {
            return groupsCount;
        }

        /**
         * The meta-property for the {@code totalRowsCount} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<Long> totalRowsCount() {
            return totalRowsCount;
        }

        /**
         * The meta-property for the {@code markedAsCancelled} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<Boolean> markedAsCancelled() {
            return markedAsCancelled;
        }

        /**
         * The meta-property for the {@code rowsCountForFinishedShards} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<Map<String, Integer>> rowsCountForFinishedShards() {
            return rowsCountForFinishedShards;
        }

        /**
         * The meta-property for the {@code reportFailedShards} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<Map<String, String>> reportFailedShards() {
            return reportFailedShards;
        }

        /**
         * The meta-property for the {@code userName} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<String> userName() {
            return userName;
        }

        /**
         * The meta-property for the {@code userId} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<UUID> userId() {
            return userId;
        }

        /**
         * The meta-property for the {@code configName} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<String> configName() {
            return configName;
        }

        /**
         * The meta-property for the {@code gridConfigId} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<String> gridConfigId() {
            return gridConfigId;
        }

        /**
         * The meta-property for the {@code version} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<Long> version() {
            return version;
        }

        /**
         * The meta-property for the {@code pointTime} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<Date> pointTime() {
            return pointTime;
        }

        /**
         * The meta-property for the {@code valuationPointTime} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<Date> valuationPointTime() {
            return valuationPointTime;
        }

        /**
         * The meta-property for the {@code description} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<String> description() {
            return description;
        }

        /**
         * The meta-property for the {@code groupingVersion} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<UUID> groupingVersion() {
            return groupingVersion;
        }

        /**
         * The meta-property for the {@code hierarchy} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<Boolean> hierarchy() {
            return hierarchy;
        }

        /**
         * The meta-property for the {@code regroupingPossible} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<Boolean> regroupingPossible() {
            return regroupingPossible;
        }

        /**
         * The meta-property for the {@code groupingCols} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<ReportColumns> groupingCols() {
            return groupingCols;
        }

        //-----------------------------------------------------------------------
        @Override
        protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
            switch (propertyName.hashCode()) {
                case 3355:  // id
                    return ((DistributedReportInfoView) bean).getId();
                case 1369213417:  // createTime
                    return ((DistributedReportInfoView) bean).getCreateTime();
                case 1151521280:  // finishTime
                    return ((DistributedReportInfoView) bean).getFinishTime();
                case 213393041:  // reportFailed
                    return ((DistributedReportInfoView) bean).isReportFailed();
                case -211268871:  // secondPhaseFinished
                    return ((DistributedReportInfoView) bean).isSecondPhaseFinished();
                case 1630112667:  // groupsCount
                    return ((DistributedReportInfoView) bean).getGroupsCount();
                case -1591291950:  // totalRowsCount
                    return ((DistributedReportInfoView) bean).getTotalRowsCount();
                case 1979766131:  // markedAsCancelled
                    return ((DistributedReportInfoView) bean).isMarkedAsCancelled();
                case -1621482374:  // rowsCountForFinishedShards
                    return ((DistributedReportInfoView) bean).getRowsCountForFinishedShards();
                case 664454726:  // reportFailedShards
                    return ((DistributedReportInfoView) bean).getReportFailedShards();
                case -266666762:  // userName
                    return ((DistributedReportInfoView) bean).getUserName();
                case -836030906:  // userId
                    return ((DistributedReportInfoView) bean).getUserId();
                case 831324397:  // configName
                    return ((DistributedReportInfoView) bean).getConfigName();
                case 1685196131:  // gridConfigId
                    return ((DistributedReportInfoView) bean).getGridConfigId();
                case 351608024:  // version
                    return ((DistributedReportInfoView) bean).getVersion();
                case 1564529789:  // pointTime
                    return ((DistributedReportInfoView) bean).getPointTime();
                case -1843609732:  // valuationPointTime
                    return ((DistributedReportInfoView) bean).getValuationPointTime();
                case -1724546052:  // description
                    return ((DistributedReportInfoView) bean).getDescription();
                case -93847979:  // groupingVersion
                    return ((DistributedReportInfoView) bean).getGroupingVersion();
                case 2115146293:  // hierarchy
                    return ((DistributedReportInfoView) bean).isHierarchy();
                case 1375666119:  // regroupingPossible
                    return ((DistributedReportInfoView) bean).isRegroupingPossible();
                case -69040490:  // groupingCols
                    return ((DistributedReportInfoView) bean).getGroupingCols();
            }
            return super.propertyGet(bean, propertyName, quiet);
        }

        @Override
        protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
            metaProperty(propertyName);
            if (quiet) {
                return;
            }
            throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
        }

    }

    //-----------------------------------------------------------------------
    /**
     * The bean-builder for {@code DistributedReportInfoView}.
     */
    public static class Builder extends DirectFieldsBeanBuilder<DistributedReportInfoView> {

        private String id;
        private Date createTime;
        private Date finishTime;
        private boolean reportFailed;
        private boolean secondPhaseFinished;
        private int groupsCount;
        private long totalRowsCount;
        private boolean markedAsCancelled;
        private Map<String, Integer> rowsCountForFinishedShards;
        private Map<String, String> reportFailedShards;
        private String userName;
        private UUID userId;
        private String configName;
        private String gridConfigId;
        private Long version;
        private Date pointTime;
        private Date valuationPointTime;
        private String description;
        private UUID groupingVersion;
        private boolean hierarchy;
        private boolean regroupingPossible;
        private ReportColumns groupingCols;

        /**
         * Restricted constructor.
         */
        protected Builder() {
        }

        /**
         * Restricted copy constructor.
         * @param beanToCopy  the bean to copy from, not null
         */
        protected Builder(DistributedReportInfoView beanToCopy) {
            this.id = beanToCopy.getId();
            this.createTime = (beanToCopy.getCreateTime() != null ? (Date) beanToCopy.getCreateTime().clone() : null);
            this.finishTime = (beanToCopy.getFinishTime() != null ? (Date) beanToCopy.getFinishTime().clone() : null);
            this.reportFailed = beanToCopy.isReportFailed();
            this.secondPhaseFinished = beanToCopy.isSecondPhaseFinished();
            this.groupsCount = beanToCopy.getGroupsCount();
            this.totalRowsCount = beanToCopy.getTotalRowsCount();
            this.markedAsCancelled = beanToCopy.isMarkedAsCancelled();
            this.rowsCountForFinishedShards = (beanToCopy.getRowsCountForFinishedShards() != null ? ImmutableMap.copyOf(beanToCopy.getRowsCountForFinishedShards()) : null);
            this.reportFailedShards = (beanToCopy.getReportFailedShards() != null ? ImmutableMap.copyOf(beanToCopy.getReportFailedShards()) : null);
            this.userName = beanToCopy.getUserName();
            this.userId = beanToCopy.getUserId();
            this.configName = beanToCopy.getConfigName();
            this.gridConfigId = beanToCopy.getGridConfigId();
            this.version = beanToCopy.getVersion();
            this.pointTime = (beanToCopy.getPointTime() != null ? (Date) beanToCopy.getPointTime().clone() : null);
            this.valuationPointTime = (beanToCopy.getValuationPointTime() != null ? (Date) beanToCopy.getValuationPointTime().clone() : null);
            this.description = beanToCopy.getDescription();
            this.groupingVersion = beanToCopy.getGroupingVersion();
            this.hierarchy = beanToCopy.isHierarchy();
            this.regroupingPossible = beanToCopy.isRegroupingPossible();
            this.groupingCols = beanToCopy.getGroupingCols();
        }

        //-----------------------------------------------------------------------
        @Override
        public Object get(String propertyName) {
            switch (propertyName.hashCode()) {
                case 3355:  // id
                    return id;
                case 1369213417:  // createTime
                    return createTime;
                case 1151521280:  // finishTime
                    return finishTime;
                case 213393041:  // reportFailed
                    return reportFailed;
                case -211268871:  // secondPhaseFinished
                    return secondPhaseFinished;
                case 1630112667:  // groupsCount
                    return groupsCount;
                case -1591291950:  // totalRowsCount
                    return totalRowsCount;
                case 1979766131:  // markedAsCancelled
                    return markedAsCancelled;
                case -1621482374:  // rowsCountForFinishedShards
                    return rowsCountForFinishedShards;
                case 664454726:  // reportFailedShards
                    return reportFailedShards;
                case -266666762:  // userName
                    return userName;
                case -836030906:  // userId
                    return userId;
                case 831324397:  // configName
                    return configName;
                case 1685196131:  // gridConfigId
                    return gridConfigId;
                case 351608024:  // version
                    return version;
                case 1564529789:  // pointTime
                    return pointTime;
                case -1843609732:  // valuationPointTime
                    return valuationPointTime;
                case -1724546052:  // description
                    return description;
                case -93847979:  // groupingVersion
                    return groupingVersion;
                case 2115146293:  // hierarchy
                    return hierarchy;
                case 1375666119:  // regroupingPossible
                    return regroupingPossible;
                case -69040490:  // groupingCols
                    return groupingCols;
                default:
                    throw new NoSuchElementException("Unknown property: " + propertyName);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public Builder set(String propertyName, Object newValue) {
            switch (propertyName.hashCode()) {
                case 3355:  // id
                    this.id = (String) newValue;
                    break;
                case 1369213417:  // createTime
                    this.createTime = (Date) newValue;
                    break;
                case 1151521280:  // finishTime
                    this.finishTime = (Date) newValue;
                    break;
                case 213393041:  // reportFailed
                    this.reportFailed = (Boolean) newValue;
                    break;
                case -211268871:  // secondPhaseFinished
                    this.secondPhaseFinished = (Boolean) newValue;
                    break;
                case 1630112667:  // groupsCount
                    this.groupsCount = (Integer) newValue;
                    break;
                case -1591291950:  // totalRowsCount
                    this.totalRowsCount = (Long) newValue;
                    break;
                case 1979766131:  // markedAsCancelled
                    this.markedAsCancelled = (Boolean) newValue;
                    break;
                case -1621482374:  // rowsCountForFinishedShards
                    this.rowsCountForFinishedShards = (Map<String, Integer>) newValue;
                    break;
                case 664454726:  // reportFailedShards
                    this.reportFailedShards = (Map<String, String>) newValue;
                    break;
                case -266666762:  // userName
                    this.userName = (String) newValue;
                    break;
                case -836030906:  // userId
                    this.userId = (UUID) newValue;
                    break;
                case 831324397:  // configName
                    this.configName = (String) newValue;
                    break;
                case 1685196131:  // gridConfigId
                    this.gridConfigId = (String) newValue;
                    break;
                case 351608024:  // version
                    this.version = (Long) newValue;
                    break;
                case 1564529789:  // pointTime
                    this.pointTime = (Date) newValue;
                    break;
                case -1843609732:  // valuationPointTime
                    this.valuationPointTime = (Date) newValue;
                    break;
                case -1724546052:  // description
                    this.description = (String) newValue;
                    break;
                case -93847979:  // groupingVersion
                    this.groupingVersion = (UUID) newValue;
                    break;
                case 2115146293:  // hierarchy
                    this.hierarchy = (Boolean) newValue;
                    break;
                case 1375666119:  // regroupingPossible
                    this.regroupingPossible = (Boolean) newValue;
                    break;
                case -69040490:  // groupingCols
                    this.groupingCols = (ReportColumns) newValue;
                    break;
                default:
                    throw new NoSuchElementException("Unknown property: " + propertyName);
            }
            return this;
        }

        @Override
        public Builder set(MetaProperty<?> property, Object value) {
            super.set(property, value);
            return this;
        }

        @Override
        public DistributedReportInfoView build() {
            return new DistributedReportInfoView(this);
        }

        //-----------------------------------------------------------------------
        /**
         * Sets the id.
         * @param id  the new value
         * @return this, for chaining, not null
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the createTime.
         * @param createTime  the new value
         * @return this, for chaining, not null
         */
        public Builder createTime(Date createTime) {
            this.createTime = createTime;
            return this;
        }

        /**
         * Sets the finishTime.
         * @param finishTime  the new value
         * @return this, for chaining, not null
         */
        public Builder finishTime(Date finishTime) {
            this.finishTime = finishTime;
            return this;
        }

        /**
         * Sets the reportFailed.
         * @param reportFailed  the new value
         * @return this, for chaining, not null
         */
        public Builder reportFailed(boolean reportFailed) {
            this.reportFailed = reportFailed;
            return this;
        }

        /**
         * Sets the secondPhaseFinished.
         * @param secondPhaseFinished  the new value
         * @return this, for chaining, not null
         */
        public Builder secondPhaseFinished(boolean secondPhaseFinished) {
            this.secondPhaseFinished = secondPhaseFinished;
            return this;
        }

        /**
         * Sets the groupsCount.
         * @param groupsCount  the new value
         * @return this, for chaining, not null
         */
        public Builder groupsCount(int groupsCount) {
            this.groupsCount = groupsCount;
            return this;
        }

        /**
         * Sets the totalRowsCount.
         * @param totalRowsCount  the new value
         * @return this, for chaining, not null
         */
        public Builder totalRowsCount(long totalRowsCount) {
            this.totalRowsCount = totalRowsCount;
            return this;
        }

        /**
         * Sets the markedAsCancelled.
         * @param markedAsCancelled  the new value
         * @return this, for chaining, not null
         */
        public Builder markedAsCancelled(boolean markedAsCancelled) {
            this.markedAsCancelled = markedAsCancelled;
            return this;
        }

        /**
         * Sets the rowsCountForFinishedShards.
         * @param rowsCountForFinishedShards  the new value
         * @return this, for chaining, not null
         */
        public Builder rowsCountForFinishedShards(Map<String, Integer> rowsCountForFinishedShards) {
            this.rowsCountForFinishedShards = rowsCountForFinishedShards;
            return this;
        }

        /**
         * Sets the reportFailedShards.
         * @param reportFailedShards  the new value
         * @return this, for chaining, not null
         */
        public Builder reportFailedShards(Map<String, String> reportFailedShards) {
            this.reportFailedShards = reportFailedShards;
            return this;
        }

        /**
         * Sets the userName.
         * @param userName  the new value
         * @return this, for chaining, not null
         */
        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        /**
         * Sets the userId.
         * @param userId  the new value
         * @return this, for chaining, not null
         */
        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Sets the configName.
         * @param configName  the new value
         * @return this, for chaining, not null
         */
        public Builder configName(String configName) {
            this.configName = configName;
            return this;
        }

        /**
         * Sets the gridConfigId.
         * @param gridConfigId  the new value
         * @return this, for chaining, not null
         */
        public Builder gridConfigId(String gridConfigId) {
            this.gridConfigId = gridConfigId;
            return this;
        }

        /**
         * Sets the version.
         * @param version  the new value
         * @return this, for chaining, not null
         */
        public Builder version(Long version) {
            this.version = version;
            return this;
        }

        /**
         * Sets the pointTime.
         * @param pointTime  the new value
         * @return this, for chaining, not null
         */
        public Builder pointTime(Date pointTime) {
            this.pointTime = pointTime;
            return this;
        }

        /**
         * Sets the valuationPointTime.
         * @param valuationPointTime  the new value
         * @return this, for chaining, not null
         */
        public Builder valuationPointTime(Date valuationPointTime) {
            this.valuationPointTime = valuationPointTime;
            return this;
        }

        /**
         * Sets the description.
         * @param description  the new value
         * @return this, for chaining, not null
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the groupingVersion.
         * @param groupingVersion  the new value
         * @return this, for chaining, not null
         */
        public Builder groupingVersion(UUID groupingVersion) {
            this.groupingVersion = groupingVersion;
            return this;
        }

        /**
         * Sets the hierarchy.
         * @param hierarchy  the new value
         * @return this, for chaining, not null
         */
        public Builder hierarchy(boolean hierarchy) {
            this.hierarchy = hierarchy;
            return this;
        }

        /**
         * Sets the regroupingPossible.
         * @param regroupingPossible  the new value
         * @return this, for chaining, not null
         */
        public Builder regroupingPossible(boolean regroupingPossible) {
            this.regroupingPossible = regroupingPossible;
            return this;
        }

        /**
         * Sets the groupingCols.
         * @param groupingCols  the new value
         * @return this, for chaining, not null
         */
        public Builder groupingCols(ReportColumns groupingCols) {
            this.groupingCols = groupingCols;
            return this;
        }

        //-----------------------------------------------------------------------
        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder(736);
            buf.append("DistributedReportInfoView.Builder{");
            int len = buf.length();
            toString(buf);
            if (buf.length() > len) {
                buf.setLength(buf.length() - 2);
            }
            buf.append('}');
            return buf.toString();
        }

        protected void toString(StringBuilder buf) {
            buf.append("id").append('=').append(JodaBeanUtils.toString(id)).append(',').append(' ');
            buf.append("createTime").append('=').append(JodaBeanUtils.toString(createTime)).append(',').append(' ');
            buf.append("finishTime").append('=').append(JodaBeanUtils.toString(finishTime)).append(',').append(' ');
            buf.append("reportFailed").append('=').append(JodaBeanUtils.toString(reportFailed)).append(',').append(' ');
            buf.append("secondPhaseFinished").append('=').append(JodaBeanUtils.toString(secondPhaseFinished)).append(',').append(' ');
            buf.append("groupsCount").append('=').append(JodaBeanUtils.toString(groupsCount)).append(',').append(' ');
            buf.append("totalRowsCount").append('=').append(JodaBeanUtils.toString(totalRowsCount)).append(',').append(' ');
            buf.append("markedAsCancelled").append('=').append(JodaBeanUtils.toString(markedAsCancelled)).append(',').append(' ');
            buf.append("rowsCountForFinishedShards").append('=').append(JodaBeanUtils.toString(rowsCountForFinishedShards)).append(',').append(' ');
            buf.append("reportFailedShards").append('=').append(JodaBeanUtils.toString(reportFailedShards)).append(',').append(' ');
            buf.append("userName").append('=').append(JodaBeanUtils.toString(userName)).append(',').append(' ');
            buf.append("userId").append('=').append(JodaBeanUtils.toString(userId)).append(',').append(' ');
            buf.append("configName").append('=').append(JodaBeanUtils.toString(configName)).append(',').append(' ');
            buf.append("gridConfigId").append('=').append(JodaBeanUtils.toString(gridConfigId)).append(',').append(' ');
            buf.append("version").append('=').append(JodaBeanUtils.toString(version)).append(',').append(' ');
            buf.append("pointTime").append('=').append(JodaBeanUtils.toString(pointTime)).append(',').append(' ');
            buf.append("valuationPointTime").append('=').append(JodaBeanUtils.toString(valuationPointTime)).append(',').append(' ');
            buf.append("description").append('=').append(JodaBeanUtils.toString(description)).append(',').append(' ');
            buf.append("groupingVersion").append('=').append(JodaBeanUtils.toString(groupingVersion)).append(',').append(' ');
            buf.append("hierarchy").append('=').append(JodaBeanUtils.toString(hierarchy)).append(',').append(' ');
            buf.append("regroupingPossible").append('=').append(JodaBeanUtils.toString(regroupingPossible)).append(',').append(' ');
            buf.append("groupingCols").append('=').append(JodaBeanUtils.toString(groupingCols)).append(',').append(' ');
        }

    }

    //-------------------------- AUTOGENERATED END --------------------------
}
