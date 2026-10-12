package arc.backend.robovm;

import arc.*;
import arc.files.*;
import org.robovm.apple.foundation.*;

public class IOSFiles implements Files{
    // TODO: Use NSSearchPathForDirectoriesInDomains instead?
    // $HOME should point to the app root dir.
    static final String appDir = System.getenv("HOME");
    static final String externalPath = appDir + "/Documents/";
    static final String localPath = appDir + "/Library/local/";
    static final String cachePath = appDir + "/Library/Caches/";
    static final String internalPath = NSBundle.getMainBundle().getBundlePath();

    public IOSFiles(){
        new Fi(externalPath).mkdirs();
        new Fi(localPath).mkdirs();
    }

    @Override
    public String getInternalStoragePath(){
        return internalPath;
    }

    @Override
    public String getCachePath(){
        try{
            return NSFileManager.getDefaultManager().getURLsForDirectory(NSSearchPathDirectory.CachesDirectory, NSSearchPathDomainMask.AllDomainsMask).get(0).getPath();
        }catch(Throwable ignored){
            return cachePath;
        }
    }

    @Override
    public Fi get(String fileName, FileType type){
        return new IOSFi(fileName, type);
    }

    @Override
    public String getExternalStoragePath(){
        return externalPath;
    }

    @Override
    public boolean isExternalStorageAvailable(){
        return true;
    }

    @Override
    public String getLocalStoragePath(){
        return localPath;
    }

    @Override
    public boolean isLocalStorageAvailable(){
        return true;
    }
}
