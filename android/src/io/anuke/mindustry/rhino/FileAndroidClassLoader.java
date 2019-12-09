package io.anuke.mindustry.rhino;

import com.android.dex.*;
import dalvik.system.*;
import io.anuke.arc.*;
import io.anuke.arc.backends.android.surfaceview.*;
import io.anuke.arc.util.ArcAnnotate.*;

import java.io.*;

/**
 * @author F43nd1r
 * @since 24.10.2017
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
class FileAndroidClassLoader extends BaseAndroidClassLoader{
    private static int instanceCounter = 0;
    private final File dexFile;

    /**
     * Create a new instance with the given parent classloader
     * @param parent the parent
     */
    public FileAndroidClassLoader(ClassLoader parent, File cacheDir){
        super(parent);
        int id = instanceCounter++;
        dexFile = new File(cacheDir, id + ".dex");
        cacheDir.mkdirs();
        reset();
    }

    @Override
    protected Class<?> loadClass(@NonNull Dex dex, @NonNull String name) throws ClassNotFoundException{
        try{
            dex.writeTo(dexFile);
        }catch(IOException e){
            e.printStackTrace();
        }
        return new DexClassLoader(dexFile.getPath(), ((AndroidApplication)Core.app).getContext().getCacheDir().getAbsolutePath(), null, getParent()).loadClass(name);
    }

    @Nullable
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
