package mindustry.tools;

import arc.*;
import arc.backend.headless.*;
import arc.files.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.editor.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.maps.Map;
import mindustry.net.*;
import mindustry.type.*;

import java.util.*;

import static mindustry.Vars.*;

public class MapFixer{

    public static void main(String[] args){
        Log.useColors = false;

        Reflect.set(Maps.class, "useDefaultFolder", false);
        Reflect.set(Maps.class, "defaultMapNames", new Fi("maps")
        .findAll(f -> f.extEquals("msav"))
        .map(f -> f.path().replace(".msav", "").replace("maps/", "")).toArray(String.class));

        ApplicationCore core = new ApplicationCore(){
            @Override
            public void setup(){
                Core.settings.setDataDirectory(new Fi("../../tools/build/test_data"));
                Core.bundle = I18NBundle.createBundle(Core.files.internal("bundles/bundle"), Locale.ENGLISH);
                headless = true;
                net = new Net(null);
                tree = new FileTree();
                Vars.init();
                world = new World();
                content.createBaseContent();
                mods.loadScripts();
                content.createModContent();

                add(logic = new Logic());
                add(netServer = new NetServer());

                content.init();
                editor = new MapEditor();
            }

            @Override
            public void init(){
                super.init();

                fixMapNames();
            }
        };

        new HeadlessApplication(core);
    }

    static void fixMapNames(){
        Fi root = Core.files.local("maps");
        root.walk(f -> {
            try{
                Map map = maps.all().find(m -> m.file.absolutePath().equals(f.absolutePath()));
                if(map == null) return;

                SectorPreset preset = content.sectors().find(s -> s.generator.map.file.absolutePath().equals(f.absolutePath()));
                if(preset == null) return;

                String targetName = preset.requireUnlock ? preset.localizedName : f.nameWithoutExtension();

                editor.beginEdit(map);
                boolean changed = false;

                if(!state.rules.bannedBlocks.isEmpty()) Log.warn("@: Banned blocks found: @", map.name(), state.rules.bannedBlocks);
                if(!state.rules.bannedUnits.isEmpty()) Log.warn("@: Banned units found: @", map.name(), state.rules.bannedUnits);

                if(state.wave > 1){
                    Log.warn("@: Wave is @, but should be 1.", map.name(), state.wave);
                    state.wave = 1;
                    changed = true;
                }

                if(!state.rules.revealedBlocks.isEmpty()){
                    Log.info("@: Clearing revealed blocks: @", map.name(), state.rules.revealedBlocks);
                    state.rules.revealedBlocks.clear();
                    changed = true;
                }

                if(!map.name().equals(targetName)){
                    Log.info("Changed name: '@' -> '@'", map.name(), targetName);
                    map.tags.put("name", targetName);
                    changed = true;
                }

                if(changed){
                    MapIO.writeMap(f, map);
                }
            }catch(Throwable t){
                Log.err(t);
                System.exit(1);
            }
        });

        System.exit(0);
    }
}
