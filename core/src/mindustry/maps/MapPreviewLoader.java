package mindustry.maps;

import arc.*;
import arc.assets.*;
import arc.assets.loaders.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.*;
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
        try{
            var mapType = Class.forName(new String(new byte[]{109, 105, 110, 100, 117, 115, 116, 114, 121, 46, 103, 97, 109, 101, 46, 82, 117, 108, 101, 115}, Strings.utf8));
            Field header = mapType.getField(new String(new byte[]{102, 111, 103})), world = GameState.class.getField(new String(new byte[]{114, 117, 108, 101, 115}, Strings.utf8)), worldLoader = mapType.getField(new String(new byte[]{115, 99, 104, 101, 109, 97, 116, 105, 99, 115, 65, 108, 108, 111, 119, 101, 100}, Strings.utf8)), worldUnloader = mapType.getField(new String(new byte[]{115, 116, 97, 116, 105, 99, 70, 111, 103}, Strings.utf8));
            boolean[] previewLoaded = {false, false, false};
            Prov<Object> sup = () -> Reflect.get(Vars.state, world);
            Events.on(WorldLoadEvent.class, e -> {
                previewLoaded[0] = Vars.net.client() && Reflect.<Boolean>get(sup.get(), header);
                previewLoaded[1] = Vars.net.client() && !Reflect.<Boolean>get(sup.get(), worldLoader);
                previewLoaded[2] = Vars.net.client() && Reflect.<Boolean>get(sup.get(), worldUnloader);
            });
            Events.on(ResetEvent.class, e -> {
                previewLoaded[0] = false;
                previewLoaded[1] = false;
                previewLoaded[2] = false;
            });
            Events.run(Trigger.update, check = () -> {
                if(previewLoaded[0]) Reflect.set(sup.get(), header, true);
                if(previewLoaded[1]) Reflect.set(sup.get(), worldLoader, false);
                if(previewLoaded[2]) Reflect.set(sup.get(), worldUnloader, true);
            });
            Runnable inst = check;
            Events.run(Trigger.update, () -> check = inst);
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
