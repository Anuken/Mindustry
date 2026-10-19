package arc.mock;

import arc.*;
import arc.files.*;
import arc.util.*;

import java.io.*;

public class MockFiles implements Files{

    @Override
    public Fi get(String fileName, FileType type){
        return new Fi(fileName, type);
    }

    @Override
    public String getExternalStoragePath(){
        return OS.userHome + File.separator;
    }

    @Override
    public boolean isExternalStorageAvailable(){
        return true;
    }

    @Override
    public String getLocalStoragePath(){
        return new File("").getAbsolutePath() + File.separator;
    }

    @Override
    public boolean isLocalStorageAvailable(){
        return true;
    }
}
