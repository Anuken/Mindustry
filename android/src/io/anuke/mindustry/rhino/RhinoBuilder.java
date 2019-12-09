package io.anuke.mindustry.rhino;

import org.mozilla.javascript.*;

import java.io.*;

/**
 * Helps to prepare a Rhino Context for usage on android.
 * @author F43nd1r
 * @since 11.01.2016
 */
public class RhinoBuilder{
    private final File cacheDirectory;

    /**
     * Constructs a new helper using the default temporary directory.
     * Note: It is recommended to use a custom directory, so no permission problems occur.
     */
    public RhinoBuilder(){
        this(new File(System.getProperty("java.io.tmpdir", "."), "classes"));
    }

    /**
     * Constructs a new helper using a directory in the applications cache.
     * @param context any context
     */
    public RhinoBuilder(android.content.Context context){
        this(new File(context.getCacheDir(), "classes"));
    }

    /**
     * Constructs a helper using the specified directory as cache.
     * @param cacheDirectory the cache directory to use
     */
    public RhinoBuilder(File cacheDirectory){
        this.cacheDirectory = cacheDirectory;
    }

    /**
     * call this instead of {@link Context#enter()}
     * @return a context prepared for android
     */
    public Context enterContext(){
        if(!SecurityController.hasGlobal())
            SecurityController.initGlobal(new SecurityController(){
                @Override
                public GeneratedClassLoader createClassLoader(ClassLoader classLoader, Object o){
                    return Context.getCurrentContext().createClassLoader(classLoader);
                }

                @Override
                public Object getDynamicSecurityDomain(Object o){
                    return null;
                }
            });
        return getContextFactory().enterContext();
    }

    /**
     * @return The Context factory which has to be used on android.
     */
    public AndroidContextFactory getContextFactory(){
        AndroidContextFactory factory;
        if(!ContextFactory.hasExplicitGlobal()){
            factory = new AndroidContextFactory(cacheDirectory);
            ContextFactory.getGlobalSetter().setContextFactoryGlobal(factory);
        }else if(!(ContextFactory.getGlobal() instanceof AndroidContextFactory)){
            throw new IllegalStateException("Cannot initialize factory for Android Rhino: There is already another factory");
        }else{
            factory = (AndroidContextFactory)ContextFactory.getGlobal();
        }
        return factory;
    }

    /**
     * @return a context prepared for android
     * @deprecated use {@link #enterContext()} instead
     */
    @Deprecated
    public static Context prepareContext(){
        return new RhinoBuilder().enterContext();
    }
}
