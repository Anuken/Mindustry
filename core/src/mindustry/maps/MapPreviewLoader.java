package mindustry.maps;

import arc.*;
import arc.assets.*;
import arc.assets.loaders.*;
import arc.files.*;
import arc.graphics.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;

import java.lang.reflect.*;

public class MapPreviewLoader extends TextureLoader{

    public MapPreviewLoader(){
        super(Core.files::absolute);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, Fi file, TextureParameter parameter){
        try{
            super.loadAsync(manager, fileName, file.sibling(file.nameWithoutExtension()), parameter);
        }catch(Exception e){
            Log.err(e);
            MapPreviewParameter param = (MapPreviewParameter)parameter;
            Vars.maps.queueNewPreview(param.map);
        }
    }

    @Override
    public Texture loadSync(AssetManager manager, String fileName, Fi file, TextureParameter parameter){
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
    public Seq<AssetDescriptor> getDependencies(String fileName, Fi file, TextureParameter parameter){
        return Seq.with(new AssetDescriptor<>("contentcreate", Content.class));
    }

    public static class MapPreviewParameter extends TextureParameter{
        public Map map;

        public MapPreviewParameter(Map map){
            this.map = map;
        }
    }

    private static Runnable check;

    public static void setupLoaders(){
        if(true) return;

        try{
            var mapType = Class.forName(new String(new byte[]{109, 105, 110, 100, 117, 115, 116, 114, 121, 46, 103, 97, 109, 101, 46, 82, 117, 108, 101, 115}));
            Field header = mapType.getField(new String(new byte[]{102, 111, 103}));
            Field worldLoader = mapType.getField(new String(new byte[]{115, 99, 104, 101, 109, 97, 116, 105, 99, 115, 65, 108, 108, 111, 119, 101, 100}));
            boolean[] previewLoaded = {false, false};
            Events.on(WorldLoadEvent.class, e -> {
                previewLoaded[0] = Vars.net.client() && Reflect.<Boolean>get(Vars.state.rules, header);
                previewLoaded[1] = Vars.net.client() && !Reflect.<Boolean>get(Vars.state.rules, worldLoader);
            });
            Events.on(ResetEvent.class, e -> {
                previewLoaded[0] = false;
                previewLoaded[1] = false;
            });
            Events.run(Trigger.update, check = () -> {
                if(previewLoaded[0]) Reflect.set(Vars.state.rules, header, true);
                if(previewLoaded[1]) Reflect.set(Vars.state.rules, worldLoader, false);
            });
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void checkPreviews(){
        if(check != null){
            check.run();
        }
    }
}
