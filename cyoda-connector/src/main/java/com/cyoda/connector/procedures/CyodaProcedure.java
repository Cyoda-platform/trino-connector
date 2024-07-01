package com.cyoda.connector.procedures;

import io.trino.spi.procedure.Procedure;

import java.lang.invoke.MethodHandle;
import java.util.List;

public interface CyodaProcedure {
    String getName();
    List<Procedure.Argument> getArguments();
    MethodHandle getMethodHandle();
}
