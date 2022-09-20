package mindustry.service;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.game.SectorInfo.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;

import static mindustry.Vars.*;
import static mindustry.service.Achievement.*;

/**
 * Interface for handling game service across multiple platforms.
 *
 * This includes:
 * - Desktop (Steam)
 * - iOS (Game Center)
 * - Android (Google Play Games)
 *
 * The default implementation does nothing.
 * */
public class GameService{
    private ObjectSet<String> blocksBuilt = new ObjectSet<>(), unitsBuilt = new ObjectSet<>();
    private ObjectSet<UnitType> t5s = new ObjectSet<>();
    private IntSet checked = new IntSet();

    /** Begin listening for new achievement events, once the game service is activated. This can be called at any time, but only once. */
    public void init(){
        if(clientLoaded){
            registerEvents();
        }else{
            Events.on(ClientLoadEvent.class, e -> registerEvents());
        }
    }

    public boolean enabled(){
        return false;
    }

    public void completeAchievement(String name){

    }

    public boolean isAchieved(String name){
        return false;
    }

    public int getStat(String name, int def){
        return def;
    }

    public void setStat(String name, int amount){

    }

    public void storeStats(){

    }

    private void registerEvents(){
        unitsBuilt = Core.settings.getJson("units-built" , ObjectSet.class, String.class, ObjectSet::new);
        blocksBuilt = Core.settings.getJson("blocks-built" , ObjectSet.class, String.class, ObjectSet::new);
        t5s = ObjectSet.with(UnitTypes.omura, UnitTypes.reign, UnitTypes.toxopid, UnitTypes.eclipse, UnitTypes.oct, UnitTypes.corvus);

        //periodically check for various conditions
        float updateInterval = 2f;
        Timer.schedule(this::checkUpdate, updateInterval, updateInterval);

        if(Items.thorium.unlocked()) obtainThorium.complete();
        if(Items.titanium.unlocked()) obtainTitanium.complete();
        if(!content.sectors().contains(UnlockableContent::locked)){
            unlockAllZones.complete();
        }

        Events.on(UnitDestroyEvent.class, e -> {
            if(campaign()){
                if(e.unit.team != Vars.player.team()){
                    SStat.unitsDestroyed.add();

                    if(e.unit.isBoss()){
                        SStat.bossesDefeated.add();
                    }
                }
            }
        });

        Events.on(TurnEvent.class, e -> {
            float total = 0;
            for(Planet planet : content.planets()){
                for(Sector sec : planet.sectors){
                    if(sec.hasBase()){
                        for(ExportStat v : sec.info.production.values()){
                            if(v.mean > 0) total += v.mean * 60;
                        }
                    }
                }
            }

            SStat.maxProduction.max(Math.round(total));
        });

        Events.run(Trigger.newGame, () -> Core.app.post(() -> {
            if(campaign() && player.core() != null && player.core().items.total() >= 10 * 1000){
                drop10kitems.complete();
            }
        }));

        Events.on(BlockBuildEndEvent.class, e -> {
            if(campaign() && e.unit != null && e.unit.isLocal() && !e.breaking){
                SStat.blocksBuilt.add();

                if(e.tile.block() == Blocks.router && e.tile.build.proximity().contains(t -> t.block == Blocks.router)){
                    chainRouters.complete();
                }

                if(e.tile.block() == Blocks.groundFactory){
                    buildGroundFactory.complete();
                }

                if(blocksBuilt.add(e.tile.block().name)){
                    if(blocksBuilt.contains("meltdown") && blocksBuilt.contains("spectre") && blocksBuilt.contains("foreshadow")){
                        buildMeltdownSpectre.complete();
                    }

                    save();
                }

                if(!circleConveyor.isAchieved() && e.tile.block() instanceof Conveyor){
                    checked.clear();
                    check: {
                        Tile current = e.tile;
                        for(int i = 0; i < 4; i++){
                            checked.add(current.pos());
                            if(current.build == null) break check;
                            Tile next = current.nearby(current.build.rotation);
                            if(next != null && next.block() instanceof Conveyor){
                                current = next;
                            }else{
                                break check;
                            }
                        }

                        if(current == e.tile && checked.size == 4){
                            circleConveyor.complete();
                        }
                    }
                }
            }
        });

        Events.on(UnitCreateEvent.class, e -> {
            if(campaign()){
                if(unitsBuilt.add(e.unit.type.name)){
                    SStat.unitTypesBuilt.set(content.units().count(u -> unitsBuilt.contains(u.name) && !u.isHidden()));
                    save();
                }

                if(t5s.contains(e.unit.type)){
                    buildT5.complete();
                }
            }
        });

        Events.on(UnitControlEvent.class, e -> {
            if(e.unit instanceof BlockUnitc unit && unit.tile().block == Blocks.router){
                becomeRouter.complete();
            }
        });

        Events.on(SchematicCreateEvent.class, e -> {
            SStat.schematicsCreated.add();
        });

        Events.on(BlockDestroyEvent.class, e -> {
            if(campaign() && e.tile.team() != player.team()){
                SStat.blocksDestroyed.add();
            }
        });

        Events.on(MapMakeEvent.class, e -> SStat.mapsMade.add());

        Events.on(MapPublishEvent.class, e -> SStat.mapsPublished.add());

        Events.on(UnlockEvent.class, e -> {
            if(e.content == Items.thorium) obtainThorium.complete();
            if(e.content == Items.titanium) obtainTitanium.complete();
            if(e.content instanceof SectorPreset && !content.sectors().contains(s -> s.locked())){
                unlockAllZones.complete();
            }
        });

        Events.run(Trigger.openWiki, openWiki::complete);

        Events.run(Trigger.exclusionDeath, dieExclusion::complete);

        Events.on(UnitDrownEvent.class, e -> {
            if(campaign() && e.unit.isPlayer()){
                drown.complete();
            }
        });

        trigger(Trigger.acceleratorUse, useAccelerator);

        trigger(Trigger.impactPower, powerupImpactReactor);

        trigger(Trigger.flameAmmo, useFlameAmmo);

        trigger(Trigger.turretCool, coolTurret);

        trigger(Trigger.suicideBomb, suicideBomb);

        Events.run(Trigger.enablePixelation, enablePixelation::complete);

        Events.run(Trigger.thoriumReactorOverheat, () -> {
            if(campaign()){
                SStat.reactorsOverheated.add();
            }
        });

        trigger(Trigger.shock, shockWetEnemy);

        trigger(Trigger.phaseDeflectHit, killEnemyPhaseWall);

        Events.on(LaunchItemEvent.class, e -> {
            if(campaign()){
                launchItemPad.complete();
            }
        });

        Events.on(PickupEvent.class, e -> {
            if(e.carrier.isPlayer() && campaign() && e.unit != null && t5s.contains(e.unit.type)){
                pickupT5.complete();
            }
        });

        Events.on(UnitCreateEvent.class, e -> {
            if(campaign() && e.unit.team() == player.team()){
                SStat.unitsBuilt.add();
            }
        });

        Events.on(SectorLaunchEvent.class, e -> {
            SStat.timesLaunched.add();
        });

        Events.on(LaunchItemEvent.class, e -> {
            SStat.itemsLaunched.add(e.stack.amount);
        });

        Events.on(WaveEvent.class, e -> {
            if(campaign()){
                SStat.maxWavesSurvived.max(Vars.state.wave);

                if(state.stats.buildingsBuilt == 0 && state.wave >= 10){
                    survive10WavesNoBlocks.complete();
                }
            }
        });

        Events.on(PlayerJoin.class, e -> {
            if(Vars.net.server()){
                SStat.maxPlayersServer.max(Groups.player.size());
            }
        });

        Runnable checkUnlocks = () -> {
            if(Blocks.router.unlocked()) researchRouter.complete();

            if(!TechTree.all.contains(t -> t.content.locked())){
                researchAll.complete();
            }
        };

        //check unlocked stuff on load as well
        Events.on(ResearchEvent.class, e -> checkUnlocks.run());
        Events.on(UnlockEvent.class, e -> checkUnlocks.run());
        Events.on(ClientLoadEvent.class, e -> checkUnlocks.run());

        Events.on(WinEvent.class, e -> {
            if(state.rules.pvp){
                SStat.pvpsWon.add();
            }
        });

        Events.on(SectorCaptureEvent.class, e -> {
            if(e.sector.isBeingPlayed() || net.client()){
                if(Vars.state.wave <= 5 && state.rules.attackMode){
                    defeatAttack5Waves.complete();
                }

                if(state.stats.buildingsDestroyed == 0){
                    captureNoBlocksBroken.complete();
                }
            }

            if(Vars.state.rules.attackMode){
                SStat.attacksWon.add();
            }

            if(!e.sector.isBeingPlayed() && !net.client()){
                captureBackground.complete();
            }

            if(!e.sector.planet.sectors.contains(s -> !s.hasBase())){
                captureAllSectors.complete();
            }

            SStat.sectorsControlled.set(e.sector.planet.sectors.count(Sector::hasBase));
        });
    }

    private void checkUpdate(){
        if(campaign()){
            SStat.maxUnitActive.max(Groups.unit.count(t -> t.team == player.team()));

            if(Groups.unit.count(u -> u.type == UnitTypes.poly && u.team == player.team()) >= 10){
                active10Polys.complete();
            }

            for(Building entity : player.team().cores()){
                if(!content.items().contains(i -> entity.items.get(i) < entity.block.itemCapacity)){
                    fillCoreAllCampaign.complete();
                    break;
                }
            }
        }
    }

    private void save(){
        Core.settings.putJson("units-built" , String.class, unitsBuilt);
        Core.settings.putJson("blocks-built" , String.class, blocksBuilt);
    }

    private void trigger(Trigger trigger, Achievement ach){
        Events.run(trigger, () -> {
            if(campaign()){
                ach.complete();
            }
        });
    }

    private boolean campaign(){
        return Vars.state.isCampaign();
    }
}
