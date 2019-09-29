package io.anuke.mindustry.core;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.io.*;
import io.anuke.mindustry.mod.Mods.*;

/** Handles files in a modded context. */
public class FileTree{
    private ObjectMap<String, FileHandle> files = new ObjectMap<>();
    private ObjectMap<String, Array<FileHandle>> bundles = new ObjectMap<>();

    public void buildFiles(Array<LoadedMod> mods){
        //TODO many files should not be replaced
        for(LoadedMod mod : mods){
            mod.root.walk(f -> {
                //TODO calling child/parent on these files will give you gibberish; create wrapper class.
                files.put(f.path(), f);
            });

            //load up bundles.
            FileHandle folder = mod.root.child("bundles");
            if(folder.exists()){
                for(FileHandle file : folder.list()){
                    if(file.name().startsWith("bundle") && file.extension().equals("properties")){
                        String name = file.nameWithoutExtension();
                        bundles.getOr(name, Array::new).add(file);
                    }
                }
            }
        }

        //add new keys to each bundle
        I18NBundle bundle = Core.bundle;
        while(bundle != null){
            String str = bundle.getLocale().toString();
            String locale = "bundle" + (str.isEmpty() ? "" : "_" + str);
            for(FileHandle file : bundles.getOr(locale, Array::new)){
                try{
                    PropertiesUtils.load(bundle.getProperties(), file.reader());
                }catch(Exception e){
                    throw new RuntimeException("Error loading bundle: " + file + "/" + locale, e);
                }
            }
            bundle = bundle.getParent();
        }
    }

    /** Gets an asset file.*/
    public FileHandle get(String path){
        if(files.containsKey(path)){
            return files.get(path);
        }else{
            return Core.files.internal(path);
        }
    }
}
