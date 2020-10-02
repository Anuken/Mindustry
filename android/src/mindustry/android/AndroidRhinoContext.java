package mindustry.android;

import android.annotation.*;
import android.os.*;
import android.os.Build.*;
import arc.*;
import arc.backend.android.*;
import com.android.dex.*;
import com.android.dx.cf.direct.*;
import com.android.dx.command.dexer.*;
import com.android.dx.dex.*;
import com.android.dx.dex.cf.*;
import com.android.dx.dex.file.DexFile;
import com.android.dx.merge.*;
import dalvik.system.*;
import rhino.*;

import java.io.*;
import java.nio.*;

/**
 * Helps to prepare a Rhino Context for usage on android.
 * @author F43nd1r
 * @since 11.01.2016
 */
public class AndroidRhinoContext{

    /**
     * call this instead of {@link Context#enter()}
     * @return a context prepared for android
     */
    public static Context enter(File cacheDirectory){
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

                @Override
                public Object callWithDomain(Object o, Context context, Callable callable, Scriptable scriptable, Scriptable scriptable1, Object[] objects){
                    return null;
                }
            });

        AndroidContextFactory factory;
        if(!ContextFactory.hasExplicitGlobal()){
            factory = new AndroidContextFactory(cacheDirectory);
            ContextFactory.getGlobalSetter().setContextFactoryGlobal(factory);
        }else if(!(ContextFactory.getGlobal() instanceof AndroidContextFactory)){
            throw new IllegalStateException("Cannot initialize factory for Android Rhino: There is already another factory");
        }else{
            factory = (AndroidContextFactory)ContextFactory.getGlobal();
        }

        return factory.enterContext();
    }

    /**
     * Ensures that the classLoader used is correct
     * @author F43nd1r
     * @since 11.01.2016
     */
    public static class AndroidContextFactory extends ContextFactory{
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

    /**
     * Compiles java bytecode to dex bytecode and loads it
     * @author F43nd1r
     * @since 11.01.2016
     */
    abstract static class BaseAndroidClassLoader extends ClassLoader implements GeneratedClassLoader{

        public BaseAndroidClassLoader(ClassLoader parent){
            super(parent);
        }

        @Override
        public Class<?> defineClass(String name, byte[] data){
            try{
                DexOptions dexOptions = new DexOptions();
                DexFile dexFile = new DexFile(dexOptions);
                DirectClassFile classFile = new DirectClassFile(data, name.replace('.', '/') + ".class", true);
                classFile.setAttributeFactory(StdAttributeFactory.THE_ONE);
                classFile.getMagic();
                DxContext context = new DxContext();
                dexFile.add(CfTranslator.translate(context, classFile, null, new CfOptions(), dexOptions, dexFile));
                Dex dex = new Dex(dexFile.toDex(null, false));
                Dex oldDex = getLastDex();
                if(oldDex != null){
                    dex = new DexMerger(new Dex[]{dex, oldDex}, CollisionPolicy.KEEP_FIRST, context).merge();
                }
                return loadClass(dex, name);
            }catch(IOException | ClassNotFoundException e){
                throw new RuntimeException("Failed to define class", e);
            }
        }

        protected abstract Class<?> loadClass(Dex dex, String name) throws ClassNotFoundException;

        protected abstract Dex getLastDex();

        protected abstract void reset();

        @Override
        public void linkClass(Class<?> aClass){}

        @Override
        public Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException{
            Class<?> loadedClass = findLoadedClass(name);
            if(loadedClass == null){
                Dex dex = getLastDex();
                if(dex != null){
                    loadedClass = loadClass(dex, name);
                }
                if(loadedClass == null){
                    loadedClass = getParent().loadClass(name);
                }
            }
            return loadedClass;
        }
    }

    static class FileAndroidClassLoader extends BaseAndroidClassLoader{
        private static int instanceCounter = 0;
        private final File dexFile;

        public FileAndroidClassLoader(ClassLoader parent, File cacheDir){
            super(parent);
            int id = instanceCounter++;
            dexFile = new File(cacheDir, id + ".dex");
            cacheDir.mkdirs();
            reset();
        }

        @Override
        protected Class<?> loadClass(Dex dex, String name) throws ClassNotFoundException{
            try{
                dex.writeTo(dexFile);
            }catch(IOException e){
                e.printStackTrace();
            }
            android.content.Context context = ((AndroidApplication) Core.app).getContext();
            return new DexClassLoader(dexFile.getPath(), VERSION.SDK_INT >= 21 ? context.getCodeCacheDir().getPath() : context.getCacheDir().getAbsolutePath(), null, getParent()).loadClass(name);
        }

        @Override
        protected Dex getLastDex(){
            if(dexFile.exists()){
                try{
                    return new Dex(dexFile);
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void reset(){
            dexFile.delete();
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    static class InMemoryAndroidClassLoader extends BaseAndroidClassLoader{
        private Dex last;

        public InMemoryAndroidClassLoader(ClassLoader parent){
            super(parent);
        }

        @Override
        protected Class<?> loadClass(Dex dex, String name) throws ClassNotFoundException{
            last = dex;
            return new InMemoryDexClassLoader(ByteBuffer.wrap(dex.getBytes()), getParent()).loadClass(name);
        }

        @Override
        protected Dex getLastDex(){
            return last;
        }

        @Override
        protected void reset(){
            last = null;
        }
    }
}
