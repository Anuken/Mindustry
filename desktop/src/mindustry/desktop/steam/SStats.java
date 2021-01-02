package mindustry.desktop.steam;

import arc.*;
import arc.struct.*;
import arc.util.*;
import com.codedisaster.steamworks.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;

import static mindustry.Vars.*;
import static mindustry.desktop.steam.SAchievement.*;

@SuppressWarnings("unchecked")
public class SStats implements SteamUserStatsCallback{
    public final SteamUserStats stats = new SteamUserStats(this);

    private boolean updated = false;
    private int statSavePeriod = 4; //in minutes

    private ObjectSet<String> blocksBuilt = new ObjectSet<>(), unitsBuilt = new ObjectSet<>();
    private ObjectSet<UnitType> t5s = new ObjectSet<>();
    private IntSet checked = new IntSet();

    public SStats(){
        stats.requestCurrentStats();

        Events.on(ClientLoadEvent.class, e -> {
            unitsBuilt = Core.settings.getJson("units-built" , ObjectSet.class, String.class, ObjectSet::new);
            blocksBuilt = Core.settings.getJson("blocks-built" , ObjectSet.class, String.class, ObjectSet::new);
            t5s = ObjectSet.with(UnitTypes.omura, UnitTypes.reign, UnitTypes.toxopid, UnitTypes.eclipse, UnitTypes.oct, UnitTypes.corvus);

            Core.app.addListener(new ApplicationListener(){
                Interval i = new Interval();

                @Override
                public void update(){
                    if(i.get(60f)){
                        checkUpdate();
                    }
                }
            });

            Timer.schedule(() -> {
                if(updated){
                    stats.storeStats();
                }
            }, statSavePeriod * 60, statSavePeriod * 60);
        });
    }

    public void onUpdate(){
        this.updated = true;
    }

    void checkUpdate(){
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

    private void registerEvents(){

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
                        for(var v : sec.info.production.values()){
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

        Events.on(CommandIssueEvent.class, e -> {
            if(campaign() && e.command == UnitCommand.attack){
                issueAttackCommand.complete();
            }
        });

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

                if(e.tile.block() instanceof Conveyor){
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
                }

                if(t5s.contains(e.unit.type)){
                    buildT5.complete();
                }
            }
        });

        Events.on(UnitControlEvent.class, e -> {
            if(e.unit instanceof BlockUnitc block && block.tile().block == Blocks.router){
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

    private void save(){
        Core.settings.putJson("units-built" , String.class, unitsBuilt);
        Core.settings.putJson("blocks-built" , String.class, blocksBuilt);
    }

    private void trigger(Trigger trigger, SAchievement ach){
        Events.run(trigger, () -> {
            if(campaign()){
                ach.complete();
            }
        });
    }

    private boolean campaign(){
        return Vars.state.isCampaign();
    }

    @Override
    public void onUserStatsReceived(long gameID, SteamID steamID, SteamResult result){
        registerEvents();

        if(result != SteamResult.OK){
            Log.err("Failed to receive steam stats: @", result);
        }else{
            Log.info("Received steam stats.");
        }
    }

    @Override
    public void onUserStatsStored(long gameID, SteamResult result){
        Log.info("Stored stats: @", result);

        if(result == SteamResult.OK){
            updated = true;
        }
    }

    @Override
    public void onUserStatsUnloaded(SteamID steamID){

    }

    @Override
    public void onUserAchievementStored(long l, boolean b, String s, int i, int i1){

    }

    @Override
    public void onLeaderboardFindResult(SteamLeaderboardHandle steamLeaderboardHandle, boolean b){

    }

    @Override
    public void onLeaderboardScoresDownloaded(SteamLeaderboardHandle steamLeaderboardHandle, SteamLeaderboardEntriesHandle steamLeaderboardEntriesHandle, int i){

    }

    @Override
    public void onLeaderboardScoreUploaded(boolean b, SteamLeaderboardHandle steamLeaderboardHandle, int i, boolean b1, int i1, int i2){

    }

    @Override
    public void onGlobalStatsReceived(long l, SteamResult steamResult){

    }
}
