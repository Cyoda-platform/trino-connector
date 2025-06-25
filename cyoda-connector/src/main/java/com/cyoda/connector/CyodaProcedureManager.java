package com.cyoda.connector;

import com.cyoda.connector.auth.AuthService;
import com.cyoda.connector.client.reporting.calls.RunReportApi;
import com.cyoda.connector.procedures.CyodaProcedure;
import com.cyoda.connector.procedures.RunReportProcedure;
import io.trino.spi.procedure.Procedure;

import jakarta.inject.Inject;
import java.util.HashSet;
import java.util.Set;

public class CyodaProcedureManager {

    private final Set<Procedure> procedures = new HashSet<>();
    private final CyodaConfig config;

    @Inject
    public CyodaProcedureManager(CyodaConfig config, AuthService auth, RunReportApi runReportApiHandler){
        this.config = config;
        registerProcedure(RunReportProcedure.getInstance(auth, runReportApiHandler));
    }
    private void registerProcedure(CyodaProcedure procedure){
        String reportingSchemaName = config.getReportingSchemaName();
        if(reportingSchemaName != null)
            procedures.add(new Procedure(reportingSchemaName, procedure.getName(), procedure.getArguments(), procedure.getMethodHandle()));
    }

    public Set<Procedure> getProcedures() {
        return procedures;
    }
}
