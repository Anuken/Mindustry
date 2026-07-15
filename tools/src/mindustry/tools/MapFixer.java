package mindustry.tools;

import arc.*;
import arc.backend.headless.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.editor.*;
import mindustry.game.MapObjectives.*;
import mindustry.io.*;
import mindustry.logic.LExecutor.*;
import mindustry.maps.Map;
import mindustry.maps.*;
import mindustry.net.*;
import mindustry.type.*;
import mindustry.world.blocks.logic.LogicBlock.*;

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

                boolean isHidden = f.path().contains("/hidden/");

                SectorPreset preset = content.sectors().find(s -> s.generator.map.file.absolutePath().equals(f.absolutePath()));
                if(preset == null) return;

                //uniquely broken (TODO: remove and fix 263)
                if(preset.sector.planet == Planets.serpulo && preset.sector.id == 263) return;

                String targetName = preset.requireUnlock ? preset.localizedName : f.nameWithoutExtension();

                editor.beginEdit(map);
                boolean changed = false;

                if(!state.rules.bannedBlocks.isEmpty()){
                    Log.warn("@: Banned blocks found: @", map.name(), state.rules.bannedBlocks);

                    if(isHidden){
                        state.rules.bannedBlocks.clear();
                        changed = true;
                    }
                }
                if(!state.rules.bannedUnits.isEmpty()){
                    Log.warn("@: Banned units found: @", map.name(), state.rules.bannedUnits);

                    if(isHidden){
                        state.rules.bannedUnits.clear();
                        changed = true;
                    }
                }

                if(state.rules.infiniteResources){
                    Log.warn("@: infinite resources enabled!", map.name());
                    state.rules.infiniteResources = false;
                    changed = true;
                }

                if(state.rules.instantBuild){
                    Log.warn("@: instant building enabled!", map.name());
                    state.rules.instantBuild = false;
                    changed = true;
                }

                Seq<TimerObjective> timers = state.rules.objectives.all.select(m -> m instanceof TimerObjective && !m.hidden && ((TimerObjective)m).text != null &&
                !((TimerObjective)m).text.isEmpty() && !((TimerObjective)m).text.contains("@")).as();

                if(!timers.isEmpty()){
                    Log.warn("@: Unlocalized objectives: @", map.name(), timers.toString(", ", t -> "'" + t.text + "'"));
                    if(isHidden){
                        changed = true;
                        timers.each(t -> t.hidden = true);
                    }
                }

                Seq<LogicBuild> logicBlocks = state.teams.getActive().flatMap(t -> t.getBuildings(Blocks.worldProcessor)).as();
                for(var build : logicBlocks){
                    boolean printsFound = false;
                    for(var inst : build.executor.instructions){
                        if(inst instanceof PrintI p && p.value.obj() != null && !String.valueOf(p.value.obj()).startsWith("@")){
                            Log.info("@: suspicious processor print: @", map.name(), p.value.objval);
                            printsFound = true;
                        }
                    }
                    if(printsFound && isHidden){
                        build.code = Seq.with(build.code.split("\n")).removeAll(b -> b.startsWith("message") || b.startsWith("print")).toString("\n");
                        changed = true;
                    }
                }

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

                if(isHidden && !state.rules.attackMode && state.rules.winWave <= 1){
                    Log.warn("@: Attack mode not enabled.",  map.name());
                    state.rules.attackMode = true;
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
