package mindustry.mod;

import mindustry.annotations.Annotations.*;

import java.net.*;

public class ClassLoaderCloser{

    /** Workaround for the close() method not being available on Android. */
    @IgnoreAndroidApi
    public static void close(ClassLoader loader) throws Exception{
        if(loader instanceof URLClassLoader u){
            u.close();
        }
    }
}
