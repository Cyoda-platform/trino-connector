package com.cyoda.presto.procedures;

import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.auth.AuthService;
import com.cyoda.presto.client.reporting.calls.RunReportApi;
import com.cyoda.presto.client.reporting.meta.ReportConfigKey;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.procedure.Procedure;
import io.trino.spi.type.VarcharType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.List;

public class RunReportProcedure implements CyodaProcedure{
    private static volatile RunReportProcedure INSTANCE = null;

    private final AuthService auth;
    private final RunReportApi apiHandler;
    private RunReportProcedure(AuthService auth, RunReportApi apiHandler){
        this.auth = auth;
        this.apiHandler = apiHandler;
    }

    public static RunReportProcedure getInstance(AuthService auth, RunReportApi apiHandler){
        if (INSTANCE == null){
            synchronized (RunReportProcedure.class){
                if (INSTANCE == null){
                    INSTANCE = new RunReportProcedure(auth, apiHandler);
                }
            }
        }
        return INSTANCE;
    }
    @Override
    public String getName() {
        return "run_report";
    }

    public List<Procedure.Argument> getArguments() {
        return Collections.singletonList(new Procedure.Argument("CONFIG_ID", VarcharType.VARCHAR));
    }

    public MethodHandle getMethodHandle() {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType mt = MethodType.methodType(void.class, ConnectorSession.class, String.class);
        try {
            return lookup.findStatic(RunReportProcedure.class, "runReport", mt);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void runReport(ConnectorSession session, String CONFIG_ID){
        AuthContext authContext = INSTANCE.auth.fromSession(session);
        INSTANCE.apiHandler.runReport(session.getQueryId(), authContext, new ReportConfigKey(CONFIG_ID, session.getQueryId()));
    }
}
