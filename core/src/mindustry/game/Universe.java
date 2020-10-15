package mindustry.game;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.maps.*;
import mindustry.type.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

/** Updates and handles state of the campaign universe. Has no relevance to other gamemodes. */
public class Universe{
    private int seconds;
    private int netSeconds;
    private float secondCounter;
    private int turn;

    private Schematic lastLoadout;
    private ItemSeq lastLaunchResources = new ItemSeq();

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

    /** @return sectors attacked on the current planet, minus the ones that are being played on right now. */
    public Seq<Sector> getAttacked(Planet planet){
        return planet.sectors.select(s -> s.isUnderAttack() && s.hasBase() && !s.isBeingPlayed() && s.getWavesPassed() > 0);
    }

    /** Update planet rotations, global time and relevant state. */
    public void update(){

        //only update time when not in multiplayer
        if(!net.client()){
            secondCounter += Time.delta / 60f;

            if(secondCounter >= 1){
                seconds += (int)secondCounter;
                secondCounter %= 1f;

                //save every few seconds
                if(seconds % 10 == 1){
                    save();
                }
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

    public ItemSeq getLaunchResources(){
        lastLaunchResources = Core.settings.getJson("launch-resources-seq", ItemSeq.class, ItemSeq::new);
        return lastLaunchResources;
    }

    public void updateLaunchResources(ItemSeq stacks){
        this.lastLaunchResources = stacks;
        Core.settings.putJson("launch-resources-seq", lastLaunchResources);
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
                        int secPassed = sector.getSecondsPassed() + actuallyPassed;

                        sector.setSecondsPassed(secPassed);

                        boolean attacked = sector.isUnderAttack();

                        int wavesPassed = (int)(secPassed*60f / sector.save.meta.rules.waveSpacing);
                        float damage = attacked ? SectorDamage.getDamage(sector.save.meta.secinfo, sector.save.meta.rules.waveSpacing, sector.save.meta.wave, wavesPassed) : 0f;

                        if(attacked){
                            sector.setWavesPassed(wavesPassed);
                        }

                        sector.setDamage(damage);

                        //check if the sector has been attacked too many times...
                        if(attacked && damage >= 0.999f){
                            //fire event for losing the sector
                            Events.fire(new SectorLoseEvent(sector));

                            //if so, just delete the save for now. it's lost.
                            //TODO don't delete it later maybe
                            sector.save.delete();
                            //clear recieved
                            sector.setExtraItems(new ItemSeq());
                            sector.save = null;
                            sector.setDamage(0f);
                        }else if(attacked && wavesPassed > 0 && sector.save.meta.wave + wavesPassed >= sector.save.meta.rules.winWave && !sector.hasEnemyBase()){
                            //autocapture the sector
                            sector.setUnderAttack(false);

                            //fire the event
                            Events.fire(new SectorCaptureEvent(state.rules.sector));
                        }
                    }

                    //export to another sector
                    if(sector.save != null && sector.save.meta != null && sector.save.meta.secinfo != null && sector.save.meta.secinfo.destination != null){
                        Sector to = sector.save.meta.secinfo.destination;
                        if(to.save != null){
                            ItemSeq items = new ItemSeq();
                            //calculated exported items to this sector
                            sector.save.meta.secinfo.export.each((item, stat) -> items.add(item, (int)(stat.mean * newSecondsPassed)));
                            to.addItems(items);
                        }
                    }

                    //reset time spent to 0
                    sector.setTimeSpent(0f);
                }
            }
        }

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

    public void updateNetSeconds(int value){
        netSeconds = value;
    }

    public float secondsMod(float mod, float scale){
        return (seconds() / scale) % mod;
    }

    public int seconds(){
        //use networked seconds when playing as client
        return net.client() ? netSeconds : seconds;
    }

    public float secondsf(){
        return seconds() + secondCounter;
    }

    private void save(){
        Core.settings.put("utimei", seconds);
        Core.settings.put("turn", turn);
    }

    private void load(){
        seconds = Core.settings.getInt("utimei");
        turn = Core.settings.getInt("turn");
    }

}
