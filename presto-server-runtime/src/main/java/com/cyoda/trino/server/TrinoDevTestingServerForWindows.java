package com.cyoda.trino.server;


import com.google.common.base.MoreObjects;
import com.google.common.base.StandardSystemProperty;
import com.google.common.base.Strings;
import com.google.common.primitives.Ints;

public final class TrinoDevTestingServerForWindows {
    private TrinoDevTestingServerForWindows() {
    }

    public static void main(String[] args) {
        String javaVersion = Strings.nullToEmpty(StandardSystemProperty.JAVA_VERSION.value());
        String majorVersion = javaVersion.split("\\D", 2)[0];
        Integer major = Ints.tryParse(majorVersion);
        if (major == null || major < 17) {
            System.err.println(String.format("ERROR: Trino requires Java 17+ (found %s)", javaVersion));
            System.exit(100);
        }

        String version = TrinoDevTestingServerForWindows.class.getPackage().getImplementationVersion();
        (new DevTestingServerForWindows()).start((String)MoreObjects.firstNonNull(version, "unknown"));
    }
}