package io.anuke.mindustry.rhino;

import android.os.*;
import org.mozilla.javascript.*;

import java.io.*;

/**
 * Ensures that the classLoader used is correct
 * @author F43nd1r
 * @since 11.01.2016
 */
public class AndroidContextFactory extends ContextFactory{

    private final File cacheDirectory;

    /**
     * Create a new factory. It will cache generated code in the given directory
     * @param cacheDirectory the cache directory
     */
    public AndroidContextFactory(File cacheDirectory){
        this.cacheDirectory = cacheDirectory;
        initApplicationClassLoader(createClassLoader(AndroidContextFactory.class.getClassLoader()));
    }

    /**
     * Create a ClassLoader which is able to deal with bytecode
     * @param parent the parent of the create classloader
     * @return a new ClassLoader
     */
    @Override
    public BaseAndroidClassLoader createClassLoader(ClassLoader parent){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            return new InMemoryAndroidClassLoader(parent);
        }
        return new FileAndroidClassLoader(parent, cacheDirectory);
    }

    @Override
    protected void onContextReleased(final Context cx){
        super.onContextReleased(cx);
        ((BaseAndroidClassLoader)cx.getApplicationClassLoader()).reset();
    }
}
