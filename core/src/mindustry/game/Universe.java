package mindustry.game;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.game.EventType.*;
import mindustry.type.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

/** Updates and handles state of the campaign universe. Has no relevance to other gamemodes. */
public class Universe{
    private long seconds;
    private float secondCounter;
    private int turn;

    private Schematic lastLoadout;
    private Seq<ItemStack> lastLaunchResources = new Seq<>();

    public Universe(){
        load();

        //update base coverage on capture
        Events.on(SectorCaptureEvent.class, e -> {
            if(state.isCampaign()){
                state.getSector().planet.updateBaseCoverage();
            }
        });
    }

    /** Update regardless of whether the player is in the campaign. */
    public void updateGlobal(){
        //currently only updates one solar system
        updatePlanet(Planets.sun);
    }

    public int turn(){
        return turn;
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

    public void displayTimeEnd(){
        if(!headless){
            //check if any sectors are under attack to display this
            Seq<Sector> attacked = state.getSector().planet.sectors.select(s -> s.hasWaves() && s.hasBase() && !s.isBeingPlayed() && s.getSecondsPassed() > 1);

            if(attacked.any()){
                state.set(State.paused);

                //TODO localize
                String text = attacked.size > 1 ? attacked.size + " sectors attacked." : "Sector " + attacked.first().id + " under attack.";

                ui.hudfrag.sectorText = text;
                ui.hudfrag.attackedSectors = attacked;
                ui.announce(text);
            }else{
                //autorun next turn
                universe.runTurn();
            }
        }
    }

    /** Update planet rotations, global time and relevant state. */
    public void update(){
        secondCounter += Time.delta / 60f;

        if(secondCounter >= 1){
            seconds += (int)secondCounter;
            secondCounter %= 1f;

            //save every few seconds
            if(seconds % 10 == 1){
                save();
            }
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
    public void runTurn(){
        turn++;

        int newSecondsPassed = (int)(turnDuration / 60);

        //update relevant sectors
        for(Planet planet : content.planets()){
            for(Sector sector : planet.sectors){
                if(sector.hasSave()){
                    int spent = (int)(sector.getTimeSpent() / 60);
                    int actuallyPassed = Math.max(newSecondsPassed - spent, 0);

                    //increment seconds passed for this sector by the time that just passed with this turn
                    if(!sector.isBeingPlayed()){
                        sector.setSecondsPassed(sector.getSecondsPassed() + actuallyPassed);

                        //check if the sector has been attacked too many times...
                        if(sector.hasBase() && sector.hasWaves() && sector.getSecondsPassed() * 60f > turnDuration * sectorDestructionTurns){
                            //fire event for losing the sector
                            Events.fire(new SectorLoseEvent(sector));

                            //if so, just delete the save for now. it's lost.
                            //TODO don't delete it later maybe
                            sector.save.delete();
                            sector.save = null;
                        }
                    }

                    //reset time spent to 0
                    sector.setTimeSpent(0f);
                }
            }
        }
        //TODO events

        Events.fire(new TurnEvent());

        save();
    }

    /** This method is expensive to call; only do so sparingly. */
    public ItemSeq getGlobalResources(){
        ItemSeq count = new ItemSeq();

        for(Planet planet : content.planets()){
            for(Sector sector : planet.sectors){
                if(sector.hasSave()){
                    count.add(sector.calculateItems());
                }
            }
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
    }

    private void load(){
        seconds = Core.settings.getLong("utime");
        turn = Core.settings.getInt("turn");
    }

}
