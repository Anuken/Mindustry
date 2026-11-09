package mindustry.async;

import arc.util.*;

import static mindustry.Vars.*;

/** JNI bridge to the Rust avoidance-field builder. Falls back to Java when the native library is unavailable. */
public class RustAvoidance{
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
            Log.debug("Rust avoidance unavailable, using Java fallback: @", t);
        }
    }

    /** Fills the avoidance buffer from `requests` triples. Returns false so the caller can fall back to Java. */
    public static native boolean build(int[] requests, int total, int[] buffer, int width, int height, boolean clear);
}
