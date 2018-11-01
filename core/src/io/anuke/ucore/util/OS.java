package io.anuke.ucore.util;

import io.anuke.ucore.UCore;

public class OS {
    static public boolean isWindows = property("os.name").contains("Windows");
    static public boolean isLinux = property("os.name").contains("Linux");
    static public boolean isMac = property("os.name").contains("Mac");
    static public boolean isIos = false;
    static public boolean isAndroid = false;
    static public boolean isARM = property("os.arch").startsWith("arm");
    static public boolean is64Bit = property("os.arch").equals("amd64")
            || property("os.arch").equals("x86_64");

    // JDK 8 only.
    static public String abi = (property("sun.arch.abi") != null ? property("sun.arch.abi") : "");

    static {
        boolean isMOEiOS = "iOS".equals(property("moe.platform.name"));
        String vm = property("java.runtime.name");
        if (vm != null && vm.contains("Android Runtime")) {
            isAndroid = true;
            isWindows = false;
            isLinux = false;
            isMac = false;
            is64Bit = false;
        }
        if (isMOEiOS || (!isAndroid && !isWindows && !isLinux && !isMac)) {
            isIos = true;
            isAndroid = false;
            isWindows = false;
            isLinux = false;
            isMac = false;
            is64Bit = false;
        }
    }

    private static String property(String name){
        return UCore.getPropertyNotNull(name);
    }
}
