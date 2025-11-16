package com.aicon.tos.shared.util;

public class VersionUtil {
    public static String getVersion() {
        Package pkg = VersionUtil.class.getPackage();
        return (pkg != null && pkg.getImplementationVersion() != null)
                ? pkg.getImplementationVersion()
                : "Development";
    }
}