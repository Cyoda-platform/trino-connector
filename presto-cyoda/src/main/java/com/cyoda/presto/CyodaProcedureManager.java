package com.cyoda.presto;

import com.cyoda.presto.auth.AuthService;
import com.cyoda.presto.client.reporting.calls.RunReportApi;
import com.cyoda.presto.procedures.CyodaProcedure;
import com.cyoda.presto.procedures.RunReportProcedure;
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
        procedures.add(new Procedure(config.getSchemaName(), procedure.getName(), procedure.getArguments(), procedure.getMethodHandle()));
    }

    public Set<Procedure> getProcedures() {
        return procedures;
    }
}
