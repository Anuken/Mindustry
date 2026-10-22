package arc.backend.sdl;

import arc.*;
import arc.files.*;
import arc.util.*;

import java.io.*;

public final class SdlFiles implements Files{
    public static final String externalPath = OS.userHome + File.separator;
    public static final String localPath = new File("").getAbsolutePath() + File.separator;

    @Override
    public Fi get(String fileName, FileType type){
        return new SdlFi(fileName, type);
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

    public static final class SdlFi extends Fi{
        public SdlFi(String fileName, FileType type){
            super(fileName, type);
        }
    
        public SdlFi(File file, FileType type){
            super(file, type);
        }
    
        @Override
        public Fi child(String name){
            if(file.getPath().length() == 0) return new SdlFi(new File(name), type);
            return new SdlFi(new File(file, name), type);
        }
    
        @Override
        public Fi sibling(String name){
            if(file.getPath().length() == 0) throw new ArcRuntimeException("Cannot get the sibling of the root.");
            return new SdlFi(new File(file.getParent(), name), type);
        }
    
        @Override
        public File file(){
            if(type == FileType.external) return new File(externalPath, file.getPath());
            if(type == FileType.local) return new File(localPath, file.getPath());
            return file;
        }
    }
}
