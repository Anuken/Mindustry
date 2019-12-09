package io.anuke.mindustry.rhino;

import android.annotation.*;
import android.os.*;
import com.android.dex.*;
import dalvik.system.*;
import io.anuke.arc.util.ArcAnnotate.NonNull;
import io.anuke.arc.util.ArcAnnotate.Nullable;

import java.nio.*;

/**
 * @author F43nd1r
 * @since 24.10.2017
 */

@TargetApi(Build.VERSION_CODES.O)
class InMemoryAndroidClassLoader extends BaseAndroidClassLoader{
    @Nullable
    private Dex last;

    public InMemoryAndroidClassLoader(ClassLoader parent){
        super(parent);
    }

    @Override
    protected Class<?> loadClass(@NonNull Dex dex, @NonNull String name) throws ClassNotFoundException{
        last = dex;
        return new InMemoryDexClassLoader(ByteBuffer.wrap(dex.getBytes()), getParent()).loadClass(name);
    }

    @Nullable
    @Override
    protected Dex getLastDex(){
        return last;
    }

    @Override
    protected void reset(){
        last = null;
    }
}
