package mindustry.ai;

import arc.util.*;

import static mindustry.Vars.*;

/** JNI bridge to the Rust flow-field implementation. Falls back to Java when the native library is unavailable. */
public class RustPathfinder{
    private static boolean loaded = false;

    static{
        tryLoad();
    }

    public static boolean available(){
        return loaded;
    }

    /** Loads the bundled native library, if present. Safe to call more than once. */
    public static synchronized void tryLoad(){
        //Android and iOS builds do not ship the desktop JNI library.
        if(loaded || ios || android) return;

        try{
            new SharedLibraryLoader().load("mindustry_native");
            loaded = SharedLibraryLoader.isLoaded("mindustry_native");
        }catch(Throwable t){
            Log.debug("Rust pathfinder unavailable, using Java fallback: @", t);
        }
    }

    /** Computes a complete flow field. Returns false if the native call failed, so the caller falls back to Java. */
    public static native boolean build(int[] tiles, int[] weights, short[] searches, int[] targets, int team, int cost, int search, int width, int height);
}
