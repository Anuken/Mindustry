package arc.backend.android;

import android.app.*;
import android.content.res.*;
import android.os.*;
import arc.*;
import arc.files.*;

/**
 * @author mzechner
 * @author Nathan Sweet
 */
public class AndroidFiles implements Files{
    protected final String sdcard = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
    protected final String localpath;

    protected final AssetManager assets;

    public AndroidFiles(AssetManager assets, String localpath){
        this.assets = assets;
        this.localpath = localpath.endsWith("/") ? localpath : localpath + "/";
    }

    @Override
    public Fi get(String path, FileType type){
        return new AndroidFi(type == FileType.internal ? assets : null, path, type);
    }

    @Override
    public String getCachePath(){
        return ((Activity)Core.app).getCacheDir().getAbsolutePath();
    }

    @Override
    public String getExternalStoragePath(){
        return sdcard;
    }

    @Override
    public boolean isExternalStorageAvailable(){
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    @Override
    public String getLocalStoragePath(){
        return localpath;
    }

    @Override
    public boolean isLocalStorageAvailable(){
        return true;
    }
}
