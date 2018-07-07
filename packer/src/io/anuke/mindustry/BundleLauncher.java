package io.anuke.mindustry;

import io.anuke.ucore.util.Log;

import java.io.File;
import java.nio.file.Paths;

public class BundleLauncher {

    public static void main(String[] args){
        File file = new File("bundle.properties");
        Paths.get("").forEach(child -> {
            Log.info("Directory: {0}", child);
        });
    }

}
