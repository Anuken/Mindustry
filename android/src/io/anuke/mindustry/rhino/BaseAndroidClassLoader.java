package io.anuke.mindustry.rhino;

import com.android.dex.*;
import com.android.dx.cf.direct.*;
import com.android.dx.command.dexer.*;
import com.android.dx.dex.*;
import com.android.dx.dex.cf.*;
import com.android.dx.dex.file.*;
import com.android.dx.merge.*;
import org.mozilla.javascript.*;

import java.io.*;

/**
 * Compiles java bytecode to dex bytecode and loads it
 * @author F43nd1r
 * @since 11.01.2016
 */
abstract class BaseAndroidClassLoader extends ClassLoader implements GeneratedClassLoader{

    /**
     * Create a new instance with the given parent classloader
     * @param parent the parent
     */
    public BaseAndroidClassLoader(ClassLoader parent){
        super(parent);
    }

    /**
     * {@inheritDoc}
     */
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
            throw new FatalLoadingException(e);
        }
    }

    protected abstract Class<?> loadClass(Dex dex, String name) throws ClassNotFoundException;

    protected abstract Dex getLastDex();

    protected abstract void reset();

    /**
     * Does nothing
     * @param aClass ignored
     */
    @Override
    public void linkClass(Class<?> aClass){
        //doesn't make sense on android
    }

    /**
     * Try to load a class. This will search all defined classes, all loaded jars and the parent class loader.
     * @param name the name of the class to load
     * @param resolve ignored
     * @return the class
     * @throws ClassNotFoundException if the class could not be found in any of the locations
     */
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

    /**
     * Might be thrown in any Rhino method that loads bytecode if the loading failed
     */
    public static class FatalLoadingException extends RuntimeException{
        FatalLoadingException(Throwable t){
            super("Failed to define class", t);
        }
    }
}
