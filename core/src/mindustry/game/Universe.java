package mindustry.game;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.game.SectorInfo.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

/** Updates and handles state of the campaign universe. Has no relevance to other gamemodes. */
public class Universe{
    private long seconds;
    private float secondCounter;
    private int turn;
    private float turnCounter;

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

        //update turn state - happens only in-game
        turnCounter += Time.delta();

        if(turnCounter >= turnDuration){
            runTurn();
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

    public int[] getTotalExports(){
        int[] exports = new int[Vars.content.items().size];

        for(Planet planet : content.planets()){
            for(Sector sector : planet.sectors){

                //ignore the current sector if the player is in it right now
                if(sector.hasBase() && !sector.isBeingPlayed()){
                    SaveMeta meta = sector.save.meta;

                    for(ObjectMap.Entry<Item, ExportStat> entry : meta.secinfo.export){
                        //total is calculated by  items/sec (value) * turn duration in seconds
                        int total = (int)(entry.value.mean * turnDuration / 60f);

                        exports[entry.key.id] += total;
                    }
                }
            }
        }

        return exports;
    }

    public void runTurns(int amount){
        for(int i = 0; i < amount; i++){
            runTurn();
        }
    }

    /** Runs a turn once. Resets turn counter. */
    public void runTurn(){
        turn ++;
        turnCounter = 0;

        //TODO EVENTS + a notification

        //increment turns passed for sectors with waves
        //TODO a turn passing may break the core; detect this, send an event and mark the sector as having no base!
        for(Planet planet : content.planets()){
            for(Sector sector : planet.sectors){
                //update turns passed for all sectors
                if(!sector.isBeingPlayed() && sector.hasSave()){
                    sector.setTurnsPassed(sector.getTurnsPassed() + 1);
                }
            }
        }

        //calculate passive item generation
        //TODO make exports only update for sector with items
        //TODO items should be added directly to cores!

        Events.fire(new TurnEvent());
    }

    public int getTurn(){
        return turn;
    }

    public int getSectorsAttacked(){
        int count = 0;
        for(Planet planet : content.planets()){
            count += planet.sectors.count(s -> !s.isBeingPlayed() && s.hasSave() && s.hasWaves());
        }
        return count;
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
        Core.settings.put("turn", turn);
        Core.settings.put("turntime", turnCounter);
    }

    private void load(){
        seconds = Core.settings.getLong("utime");
        turn = Core.settings.getInt("turn");
        turnCounter = Core.settings.getFloat("turntime");
    }

}
