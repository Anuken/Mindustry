package io.anuke.mindustry.maps;

import io.anuke.arc.assets.*;
import io.anuke.arc.assets.loaders.*;
import io.anuke.arc.assets.loaders.resolvers.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.ctype.Content;

public class MapPreviewLoader extends TextureLoader{

    public MapPreviewLoader(){
        super(new AbsoluteFileHandleResolver());
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, TextureParameter parameter){
        try{
            super.loadAsync(manager, fileName, file.sibling(file.nameWithoutExtension()), parameter);
        }catch(Exception e){
            Log.err(e);
            MapPreviewParameter param = (MapPreviewParameter)parameter;
            Vars.maps.queueNewPreview(param.map);
        }
    }

    @Override
    public Texture loadSync(AssetManager manager, String fileName, FileHandle file, TextureParameter parameter){
        try{
            return super.loadSync(manager, fileName, file, parameter);
        }catch(Throwable e){
            Log.err(e);
            try{
                return new Texture(file);
            }catch(Throwable e2){
                Log.err(e2);
                return new Texture("sprites/error.png");
            }
        }
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, TextureParameter parameter){
        return Array.with(new AssetDescriptor<>("contentcreate", Content.class));
    }

    public static class MapPreviewParameter extends TextureParameter{
        public Map map;

        public MapPreviewParameter(Map map){
            this.map = map;
        }
    }
}
