package mindustry.game;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.game.Schematic.*;
import mindustry.game.SectorInfo.*;
import mindustry.gen.*;
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
    private float turnCounter;

    private @Nullable Schematic lastLoadout;
    private ItemSeq lastLaunchResources = new ItemSeq();

    public Universe(){
        load();

        //update base coverage on capture
        Events.on(SectorCaptureEvent.class, e -> {
            if(!net.client() && state.isCampaign()){
                state.getSector().planet.updateBaseCoverage();
            }
        });
    }

    /** Update regardless of whether the player is in the campaign. */
    public void updateGlobal(){
        for(Planet planet : content.planets()){
            //update all parentless planets (solar system root), regardless of which one the player is in
            if(planet.parent == null) updatePlanet(planet);
        }
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

    /** Update planet rotations, global time and relevant state. */
    public void update(){

        //only update time when not in multiplayer
        if(!net.client()){
            secondCounter += Time.delta / 60f;
            turnCounter += Time.delta;

            //auto-run turns
            if(turnCounter >= turnDuration){
                turnCounter = 0;
                runTurn();
            }

            if(secondCounter >= 1){
                seconds += (int)secondCounter;
                secondCounter %= 1f;

                //save every few seconds
                if(seconds % 10 == 1){
                    save();
                }
            }
        }

        if(state.hasSector() && state.getSector().planet.updateLighting && !(state.getSector().preset != null && state.getSector().preset.noLighting)){
            var planet = state.getSector().planet;
            //update sector light
            float light = state.getSector().getLight();
            float alpha = Mathf.clamp(Mathf.map(light, planet.lightSrcFrom, planet.lightSrcTo, planet.lightDstFrom, planet.lightDstTo));

            //assign and map so darkness is not 100% dark
            state.rules.ambientLight.a = 1f - alpha;
            state.rules.lighting = !Mathf.equal(alpha, 1f);
        }
    }

    public void clearLoadoutInfo(){
        lastLoadout = null;
        lastLaunchResources = new ItemSeq();
        Core.settings.remove("launch-resources-seq");
        Core.settings.remove("lastloadout-core-shard");
        Core.settings.remove("lastloadout-core-nucleus");
        Core.settings.remove("lastloadout-core-foundation");
    }

    public ItemSeq getLaunchResources(){
        lastLaunchResources = Core.settings.getJson("launch-resources-seq", ItemSeq.class, ItemSeq::new);
        return lastLaunchResources;
    }

    public void updateLaunchResources(ItemSeq stacks){
        this.lastLaunchResources = stacks;
        Core.settings.putJson("launch-resources-seq", lastLaunchResources);
    }

    /** Updates selected loadout for future deployment. Creates an empty schematic with a single core block. */
    public void updateLoadout(CoreBlock block){
        updateLoadout(block, new Schematic(Seq.with(new Stile(block, 0, 0, null, (byte)0)), new StringMap(), block.size, block.size));
    }

    /** Updates selected loadout for future deployment. */
    public void updateLoadout(CoreBlock block, Schematic schem){
        Core.settings.put("lastloadout-" + block.name, schem.file == null ? "" : schem.file.nameWithoutExtension());
        lastLoadout = schem;
    }

    public Schematic getLastLoadout(){
        if(lastLoadout == null) lastLoadout = state.rules.sector == null || state.rules.sector.planet.generator == null ? Loadouts.basicShard : state.rules.sector.planet.generator.defaultLoadout;
        return lastLoadout;
    }

    /** @return the last selected loadout for this specific core type. */
    @Nullable
    public Schematic getLoadout(CoreBlock core){
        //for tools - schem
        if(schematics == null) return Loadouts.basicShard;

        //find last used loadout file name
        String file = Core.settings.getString("lastloadout-" + core.name, "");

        //use default (first) schematic if not found
        Seq<Schematic> all = schematics.getLoadouts(core);
        Schematic schem = all.find(s -> s.file != null && s.file.nameWithoutExtension().equals(file));

        return schem == null ? all.any() ? all.first() : null : schem;
    }

    /** Runs possible events. Resets event counter. */
    public void runTurn(){
        turn++;

        int newSecondsPassed = (int)(turnDuration / 60);
        Planet current = state.getPlanet();

        //update relevant sectors
        for(Planet planet : content.planets()){

            //planets with different wave simulation status are not updated
            if(current != null && current.allowWaveSimulation != planet.allowWaveSimulation){
                continue;
            }

            //don't simulate the planet if there is an in-progress mission on that planet
            if(!planet.allowWaveSimulation && planet.sectors.contains(s -> s.hasBase() && !s.isBeingPlayed() && s.isAttacked())){
                continue;
            }

            if(planet.campaignRules.legacyLaunchPads){
                //first pass: clear import stats
                for(Sector sector : planet.sectors){
                    if(sector.hasBase() && !sector.isBeingPlayed()){
                        sector.info.lastImported.clear();
                    }
                }

                //second pass: update export & import statistics
                for(Sector sector : planet.sectors){
                    if(sector.hasBase() && !sector.isBeingPlayed()){

                        //export to another sector
                        if(sector.info.destination != null){
                            Sector to = sector.info.destination;
                            if(to.hasBase() && to.planet == planet){
                                ItemSeq items = new ItemSeq();
                                //calculated exported items to this sector
                                sector.info.export.each((item, stat) -> items.add(item, (int)(stat.mean * newSecondsPassed * sector.getProductionScale())));
                                to.addItems(items);
                                to.info.lastImported.add(items);
                            }
                        }
                    }
                }
            }

            //third pass: everything else
            for(Sector sector : planet.sectors){
                if(sector.hasBase()){
                    if(sector.info.importRateCache != null){
                        sector.info.refreshImportRates(planet);
                    }

                    //if it is being attacked, capture time is 0; otherwise, increment the timer
                    if(sector.isAttacked()){
                        sector.info.minutesCaptured = 0;
                    }else{
                        sector.info.minutesCaptured += turnDuration / 60 / 60;
                    }

                    //increment seconds passed for this sector by the time that just passed with this turn
                    if(!sector.isBeingPlayed()){

                        //TODO: if a planet has sectors under attack and simulation is OFF, just don't simulate it

                        //increment time if attacked
                        if(sector.isAttacked()){
                            sector.info.secondsPassed += turnDuration/60f;
                        }

                        int wavesPassed = (int)(sector.info.secondsPassed*60f / sector.info.waveSpacing);
                        boolean attacked = sector.info.waves && sector.planet.allowWaveSimulation;

                        if(attacked){
                            sector.info.wavesPassed = wavesPassed;
                        }

                        float damage = attacked ? SectorDamage.getDamage(sector) : 0f;

                        //damage never goes down until the player visits the sector, so use max
                        sector.info.damage = Math.max(sector.info.damage, damage);

                        //check if the sector has been attacked too many times...
                        if(attacked && damage >= 0.999f){
                            //fire event for losing the sector
                            Events.fire(new SectorLoseEvent(sector));

                            //sector is dead.
                            sector.info.items.clear();
                            sector.info.damage = 1f;
                            sector.info.hasCore = false;
                            sector.info.production.clear();
                        }else if(attacked && wavesPassed > 0 && sector.info.winWave > 1 && sector.info.wave + wavesPassed >= sector.info.winWave && !sector.hasEnemyBase()){
                            //autocapture the sector
                            sector.info.waves = false;
                            boolean was = sector.info.wasCaptured;
                            sector.info.wasCaptured = true;

                            //fire the event
                            Events.fire(new SectorCaptureEvent(sector, !was));
                        }

                        float scl = sector.getProductionScale();

                        //add production, making sure that it's capped
                        sector.info.production.each((item, stat) -> sector.info.items.add(item, Math.min((int)(stat.mean * newSecondsPassed * scl), sector.info.storageCapacity - sector.info.items.get(item))));

                        if(planet.campaignRules.legacyLaunchPads){
                            sector.info.export.each((item, stat) -> {
                                if(sector.info.items.get(item) <= 0 && sector.info.production.get(item, ExportStat::new).mean < 0 && stat.mean > 0){
                                    //cap export by import when production is negative.
                                    //TODO remove
                                    stat.mean = Math.min(sector.info.lastImported.get(item) / (float)newSecondsPassed, stat.mean);
                                }
                            });
                        }

                        //prevent negative values with unloaders
                        sector.info.items.checkNegative();

                        sector.saveInfo();
                    }

                    //queue random invasions
                    if(!sector.isAttacked() && sector.planet.campaignRules.sectorInvasion && sector.info.minutesCaptured > invasionGracePeriod && sector.info.hasSpawns){
                        int count = sector.near().count(s -> s.hasEnemyBase() && !s.hasBase() && (s.preset == null || !s.preset.requireUnlock));

                        //invasion chance depends on # of nearby bases
                        if(count > 0 && Mathf.chance(baseInvasionChance * (0.8f + (count - 1) * 0.3f))){
                            int waveMax = Math.max(sector.info.winWave, sector.isBeingPlayed() ? state.wave : sector.info.wave + sector.info.wavesPassed) + Mathf.random(2, 4) * 5;

                            //assign invasion-related things
                            if(sector.isBeingPlayed()){
                                state.rules.winWave = waveMax;
                                state.rules.waves = true;
                                state.rules.attackMode = false;
                                planet.campaignRules.apply(planet, state.rules); //enabling waves may force changes in campaign rules
                                //update rules in multiplayer
                                if(net.server()){
                                    Call.setRules(state.rules);
                                }
                            }else{
                                sector.info.winWave = waveMax;
                                sector.info.waves = true;
                                sector.info.attack = false;
                                sector.saveInfo();
                            }

                            Events.fire(new SectorInvasionEvent(sector));
                        }
                    }
                }
            }
        }

        Events.fire(new TurnEvent());

        save();
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
