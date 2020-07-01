package mindustry.game;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

/** Updates and handles state of the campaign universe. Has no relevance to other gamemodes. */
public class Universe{
    private long seconds;
    private float secondCounter;
    private int event;
    private float eventCounter;

    private Schematic lastLoadout;
    private Seq<ItemStack> lastLaunchResources = new Seq<>();

    public Universe(){
        load();
    }

    /** Update regardless of whether the player is in the campaign. */
    public void updateGlobal(){
        //currently only updates one solar system
        updatePlanet(Planets.sun);
    }

    private void updatePlanet(Planet planet){
        planet.position.setZero();
        planet.addParentOffset(planet.position);
        if(planet.parent != null){
            planet.position.add(planet.parent.position);
        }
        for(Planet child : planet.children){
            updatePlanet(child);
        }
    }

    /** Update planet rotations, global time and relevant state. */
    public void update(){
        secondCounter += Time.delta() / 60f;

        if(secondCounter >= 1){
            seconds += (int)secondCounter;
            secondCounter %= 1f;

            //save every few seconds
            if(seconds % 10 == 1){
                save();
            }
        }

        //update event counter - happens only in-game
        eventCounter += Time.delta();

        if(eventCounter >= eventRate){
            runEvents();
        }

        if(state.hasSector()){
            //update sector light
            float light = state.getSector().getLight();
            float alpha = Mathf.clamp(Mathf.map(light, 0f, 0.8f, 0.1f, 1f));

            //assign and map so darkness is not 100% dark
            state.rules.ambientLight.a = 1f - alpha;
            state.rules.lighting = !Mathf.equal(alpha, 1f);
        }
    }

    public Seq<ItemStack> getLaunchResources(){
        lastLaunchResources = Core.settings.getJson("launch-resources", Seq.class, ItemStack.class, Seq::new);
        return lastLaunchResources;
    }

    public void updateLaunchResources(Seq<ItemStack> stacks){
        this.lastLaunchResources = stacks;
        Core.settings.putJson("launch-resources", ItemStack.class, lastLaunchResources);
    }

    /** Updates selected loadout for future deployment. */
    public void updateLoadout(CoreBlock block, Schematic schem){
        Core.settings.put("lastloadout-" + block.name, schem.file == null ? "" : schem.file.nameWithoutExtension());
        lastLoadout = schem;
    }

    public Schematic getLastLoadout(){
        if(lastLoadout == null) lastLoadout = Loadouts.basicShard;
        return lastLoadout;
    }

    /** @return the last selected loadout for this specific core type. */
    public Schematic getLoadout(CoreBlock core){
        //for tools - schem
        if(schematics == null) return Loadouts.basicShard;

        //find last used loadout file name
        String file = Core.settings.getString("lastloadout-" + core.name, "");

        //use default (first) schematic if not found
        Seq<Schematic> all = schematics.getLoadouts(core);
        Schematic schem = all.find(s -> s.file != null && s.file.nameWithoutExtension().equals(file));

        return schem == null ? all.first() : schem;
    }

    /** Runs possible events. Resets event counter. */
    public void runEvents(){
        event++;
        eventCounter = 0;

        //TODO events
    }

    public float secondsMod(float mod, float scale){
        return (seconds / scale) % mod;
    }

    public long seconds(){
        return seconds;
    }

    public float secondsf(){
        return seconds + secondCounter;
    }

    private void save(){
        Core.settings.put("utime", seconds);
        Core.settings.put("event", event);
        Core.settings.put("eventtime", eventCounter);
    }

    private void load(){
        seconds = Core.settings.getLong("utime");
        event = Core.settings.getInt("event");
        eventCounter = Core.settings.getFloat("eventtime");
    }

}
