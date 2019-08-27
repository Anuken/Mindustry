package io.anuke.mindustry.maps;

import io.anuke.arc.assets.*;
import io.anuke.arc.assets.loaders.*;
import io.anuke.arc.assets.loaders.resolvers.*;
import io.anuke.arc.files.*;
import io.anuke.mindustry.*;

public class MapPreviewLoader extends TextureLoader{

    public MapPreviewLoader(){
        super(new AbsoluteFileHandleResolver());
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, TextureParameter parameter){
        try{
            super.loadAsync(manager, fileName, file.sibling(file.nameWithoutExtension()), parameter);
        }catch(Exception e){
            e.printStackTrace();
            MapPreviewParameter param = (MapPreviewParameter)parameter;
            Vars.maps.createNewPreview(param.map);
        }
    }

    public static class MapPreviewParameter extends TextureParameter{
        public Map map;

        public MapPreviewParameter(Map map){
            this.map = map;
        }
    }
}
