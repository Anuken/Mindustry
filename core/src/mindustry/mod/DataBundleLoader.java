package mindustry.mod;

import arc.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.mod.data.*;

public class DataBundleLoader{

    public void load(Seq<BundleAsset> assets){
        //add new keys to each bundle
        I18NBundle bundle = Core.bundle;
        while(bundle != null){
            String str = bundle.getLocale().toString();
            String locale = "bundle" + (str.isEmpty() ? "" : "_" + str);
            for(Fi file : bundles.get(locale, Seq::new)){
                try{
                    PropertiesUtils.load(bundle.getProperties(), file.reader());
                }catch(Throwable e){
                    Log.err("Error loading bundle: " + file + "/" + locale, e);
                }
            }
            bundle = bundle.getParent();
        }
    }

    public void unload(){

    }
}
