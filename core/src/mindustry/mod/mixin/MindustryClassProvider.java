package mindustry.mod.mixin;

import org.spongepowered.asm.service.IClassProvider;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

public class MindustryClassProvider implements IClassProvider{
    private static URLClassLoader modClassLoader;

    /**
     * Set the classloader that has access to mod JARs
     */
    public static void setModClassLoader(URLClassLoader loader){
        modClassLoader = loader;
        System.out.println("[MindustryClassProvider] Mod classloader configured with " +
            (loader.getURLs() != null ? loader.getURLs().length : 0) + " URLs");
    }

    private ClassLoader getEffectiveClassLoader(){
        // Use mod classloader if available
        return modClassLoader != null ? modClassLoader : Thread.currentThread().getContextClassLoader();
    }

    @Override
    public URL[] getClassPath(){
        if(modClassLoader != null){
            return modClassLoader.getURLs();
        }
        return new URL[0];
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException{
        return Class.forName(name, false, getEffectiveClassLoader());
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException{
        return Class.forName(name, initialize, getEffectiveClassLoader());
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException{
        return Class.forName(name, initialize, getEffectiveClassLoader());
    }

    public boolean isClassLoaded(String className){
        try{
            findClass(className, false);
            return true;
        }catch(ClassNotFoundException e){
            return false;
        }
    }

    public InputStream getResourceAsStream(String name){
        InputStream stream = getEffectiveClassLoader().getResourceAsStream(name);
        if(stream == null && modClassLoader != null){
            for(URL url : modClassLoader.getURLs()){
                try{
                    URL resourceUrl = new URL("jar:" + url.toString() + "!/" + name);
                    stream = resourceUrl.openStream();
                    if(stream != null){
                        System.out.println("[MindustryClassProvider] Found resource '" + name + "' in " + url);
                        break;
                    }
                }catch(Exception ignored){
                }
            }
        }
        return stream;
    }
}
