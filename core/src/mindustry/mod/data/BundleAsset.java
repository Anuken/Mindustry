package mindustry.mod.data;

import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;

public class BundleAsset extends DataAsset{
    public @Nullable OrderedMap<String, String> cachedBundle;

    public void tryLoadCache(){
        if(cachedBundle != null) return;
        try{
            cachedBundle = new OrderedMap<>();
            Fi file = getCacheFile();
            if(file != null){
                PropertiesUtils.load(cachedBundle, file.reader());
            }
        }catch(Exception e){
            Log.err(e);
        }
    }

    @Override
    public void updateData(byte[] data){
        super.updateData(data);
        this.cachedBundle = null;
    }

    @Override
    public DataAssetType getType(){
        return DataAssetType.bundle;
    }
}
