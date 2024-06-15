package mindustry.service;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.game.SectorInfo.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.Wall.*;
import mindustry.world.blocks.defense.turrets.Turret.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.production.AttributeCrafter.*;
import mindustry.world.blocks.production.SolidPump.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;
import static mindustry.service.Achievement.*;

/**
 * Interface for handling game service across multiple platforms.
 *
 * This includes:
 * - Desktop (Steam)
 *
 * The default implementation does nothing.
 * */
public class GameService{
    private Seq<Tile> tmpTiles = new Seq<>();
    private ObjectSet<String> blocksBuilt = new ObjectSet<>(), unitsBuilt = new ObjectSet<>();
    private ObjectSet<UnitType> t5s = new ObjectSet<>();
    private IntSet checked = new IntSet();

    private Block[] allTransportSerpulo, allTransportErekir, allErekirBlocks, allSerpuloBlocks;

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

    public void clearAchievement(String name){

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

    private void checkAllBlocks(Achievement ach, Block[] blocks){
        if(!Structs.contains(blocks, t -> !blocksBuilt.contains(t.name))){
            ach.complete();
        }
    }

    private void registerEvents(){
        allTransportSerpulo = content.blocks().select(b -> b.category == Category.distribution && b.isVisibleOn(Planets.serpulo) && b.isVanilla() && b.buildVisibility == BuildVisibility.shown).toArray(Block.class);
        allTransportErekir = content.blocks().select(b -> b.category == Category.distribution && b.isVisibleOn(Planets.erekir) && b.isVanilla() && b.buildVisibility == BuildVisibility.shown).toArray(Block.class);

        //cores are ignored since they're upgrades and can be skipped
        allSerpuloBlocks = content.blocks().select(b -> b.synthetic() && b.isVisibleOn(Planets.serpulo) && b.isVanilla() && !(b instanceof CoreBlock) && b.buildVisibility == BuildVisibility.shown).toArray(Block.class);
        allErekirBlocks = content.blocks().select(b -> b.synthetic() && b.isVisibleOn(Planets.erekir) && b.isVanilla() && !(b instanceof CoreBlock) && b.buildVisibility == BuildVisibility.shown).toArray(Block.class);

        unitsBuilt = Core.settings.getJson("units-built" , ObjectSet.class, String.class, ObjectSet::new);
        blocksBuilt = Core.settings.getJson("blocks-built" , ObjectSet.class, String.class, ObjectSet::new);
        t5s = ObjectSet.with(UnitTypes.omura, UnitTypes.reign, UnitTypes.toxopid, UnitTypes.eclipse, UnitTypes.oct, UnitTypes.corvus);

        checkAllBlocks(allBlocksErekir, allErekirBlocks);
        checkAllBlocks(allBlocksSerpulo, allSerpuloBlocks);

        //periodically check for various conditions
        float updateInterval = 2f;
        Timer.schedule(this::checkUpdate, updateInterval, updateInterval);

        if(Items.thorium.unlocked()) obtainThorium.complete();
        if(Items.titanium.unlocked()) obtainTitanium.complete();

        if(SectorPresets.origin.sector.isCaptured()){
            completeErekir.complete();
        }

        if(SectorPresets.planetaryTerminal.sector.isCaptured()){
            completeSerpulo.complete();
        }

        if(mods != null && mods.list().size > 0){
            installMod.complete();
        }

        if(Core.bundle.get("yes").equals("router")){
            routerLanguage.complete();
        }

        if(!Planets.serpulo.sectors.contains(s -> !s.isCaptured())){
            captureAllSectors.complete();
        }

        Events.run(Trigger.openConsole, () -> openConsole.complete());

        Events.run(Trigger.unitCommandAttack, () -> {
            if(campaign()){
                issueAttackCommand.complete();
            }
        });

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

        Events.run(Trigger.update, () -> {
            //extremely lazy timer, I just don't care
            if(campaign() && !hoverUnitLiquid.isAchieved() && Core.graphics.getFrameId() % 20 == 0){
                var units = state.rules.defaultTeam.data().getUnits(UnitTypes.elude);
                if(units != null){
                    for(var unit : units){
                        if(unit.floorOn().isLiquid){
                            hoverUnitLiquid.complete();
                            break;
                        }
                    }
                }
            }

            if(campaign() && player.unit().type.canBoost && player.unit().elevation >= 0.25f){
                boostUnit.complete();
            }
        });

        Events.run(Trigger.newGame, () -> Core.app.post(() -> {
            if(campaign() && player.core() != null && player.core().items.total() >= 10 * 1000){
                drop10kitems.complete();
            }
        }));

        Events.on(BuildingBulletDestroyEvent.class, e -> {
            if(campaign() && e.build.block == Blocks.scatter && e.build.team == state.rules.waveTeam && e.bullet.owner instanceof Unit u && u.type == UnitTypes.flare && u.team == player.team()){
                destroyScatterFlare.complete();
            }
        });

        Events.on(BlockBuildEndEvent.class, e -> {
            if(campaign() && state.rules.sector == SectorPresets.groundZero.sector && e.tile.block() == Blocks.coreNucleus){
                nucleusGroundZero.complete();
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

                if((e.tile.build instanceof AttributeCrafterBuild a && a.attrsum > 0) || (e.tile.build instanceof SolidPumpBuild sp && sp.boost > 0)){
                    boostBuildingFloor.complete();
                }

                if(!allTransportOneMap.isAchieved()){
                    Block[] allTransports = state.rules.sector.planet == Planets.erekir ? allTransportErekir : allTransportSerpulo;
                    boolean all = true;
                    for(var block : allTransports){
                        if(state.rules.defaultTeam.data().getCount(block) == 0){
                            all = false;
                            break;
                        }
                    }
                    if(all){
                        allTransportOneMap.complete();
                    }
                }

                if(e.tile.block() == Blocks.mendProjector) buildMendProjector.complete();
                if(e.tile.block() == Blocks.overdriveProjector) buildOverdriveProjector.complete();

                if(e.tile.block() == Blocks.waterExtractor){
                    if(e.tile.getLinkedTiles(tmpTiles).contains(t -> t.floor().liquidDrop == Liquids.water)){
                        buildWexWater.complete();
                    }
                }

                if(blocksBuilt.add(e.tile.block().name)){
                    if(state.rules.sector.planet == Planets.erekir){
                        checkAllBlocks(allBlocksErekir, allErekirBlocks);
                    }else{
                        checkAllBlocks(allBlocksSerpulo, allSerpuloBlocks);
                    }

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

            if(campaign() && e.unit != null && e.unit.isLocal() && e.breaking){
                //hacky way of testing for boulders without string contains/endsWith
                if(e.tile.block().breakSound == Sounds.rockBreak){
                    SStat.bouldersDeconstructed.add();
                }
            }
        });

        Events.on(TurnEvent.class, e -> {
            int total = 0;
            for(var planet : content.planets()){
                for(var sector : planet.sectors){
                    if(sector.hasBase()){
                        total += sector.items().total;
                    }
                }
            }

            SStat.totalCampaignItems.max(total);
        });

        Events.on(SectorLaunchLoadoutEvent.class, e -> {
            if(e.sector.planet == Planets.serpulo && !schematics.isDefaultLoadout(e.loadout)){
                launchCoreSchematic.complete();
            }
        });

        Events.on(UnitCreateEvent.class, e -> {
            if(campaign()){
                if(unitsBuilt.add(e.unit.type.name)){
                    SStat.unitTypesBuilt.max(content.units().count(u -> unitsBuilt.contains(u.name) && !u.isHidden()));
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

            if(e.unit instanceof BlockUnitc unit && unit.tile() instanceof TurretBuild){
                controlTurret.complete();
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

        Events.run(Trigger.importMod, installMod::complete);

        Events.run(Trigger.exclusionDeath, dieExclusion::complete);

        Events.on(UnitDrownEvent.class, e -> {
            if(campaign() && e.unit.isPlayer()){
                drown.complete();
            }
        });

        trigger(Trigger.impactPower, powerupImpactReactor);

        trigger(Trigger.flameAmmo, useFlameAmmo);

        trigger(Trigger.turretCool, coolTurret);

        trigger(Trigger.suicideBomb, suicideBomb);

        trigger(Trigger.blastGenerator, blastGenerator);

        trigger(Trigger.forceProjectorBreak, breakForceProjector);

        trigger(Trigger.neoplasmReact, neoplasmWater);

        trigger(Trigger.shockwaveTowerUse, shockwaveTowerUse);

        Events.run(Trigger.enablePixelation, enablePixelation::complete);

        Events.run(Trigger.thoriumReactorOverheat, () -> {
            if(campaign()){
                SStat.reactorsOverheated.add();
            }
        });

        Events.on(GeneratorPressureExplodeEvent.class, e -> {
            if(campaign() && e.build.block == Blocks.neoplasiaReactor){
                neoplasiaExplosion.complete();
            }
        });

        trigger(Trigger.shock, shockWetEnemy);

        trigger(Trigger.blastFreeze, blastFrozenUnit);

        Events.on(UnitBulletDestroyEvent.class, e -> {
            if(state.isCampaign() && player != null && player.team() == e.bullet.team){

                if(e.bullet.owner instanceof WallBuild){
                    killEnemyPhaseWall.complete();
                }

                if(e.unit.type == UnitTypes.eclipse && e.bullet.owner instanceof TurretBuild turret && turret.block == Blocks.duo){
                    killEclipseDuo.complete();
                }
            }
        });

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

            if(Blocks.microProcessor.unlocked()) researchLogic.complete();
        };

        //check unlocked stuff on load as well
        Events.on(ResearchEvent.class, e -> checkUnlocks.run());
        Events.on(UnlockEvent.class, e -> checkUnlocks.run());

        checkUnlocks.run();

        Events.on(WinEvent.class, e -> {
            if(state.rules.pvp){
                SStat.pvpsWon.add();
            }
        });

        Events.on(ClientPreConnectEvent.class, e -> {
            if(e.host != null && !e.host.address.startsWith("steam:") && !e.host.address.startsWith("192.")){
                joinCommunityServer.complete();
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

            if(e.sector.planet == Planets.serpulo && !e.sector.planet.sectors.contains(s -> !s.hasBase())){
                captureAllSectors.complete();
            }

            if(e.sector.planet == Planets.erekir && e.sector.preset != null && e.sector.preset.isLastSector){
                completeErekir.complete();
            }

            if(e.sector.planet == Planets.serpulo && e.sector.preset != null && e.sector.preset.isLastSector){
                completeSerpulo.complete();
            }

            //TODO wrong
            if(e.sector.planet == Planets.serpulo){
                SStat.sectorsControlled.set(e.sector.planet.sectors.count(Sector::hasBase));
            }
        });

        Events.on(PayloadDropEvent.class, e -> {
            if(campaign() && e.unit != null && e.carrier.team == state.rules.defaultTeam && state.rules.waveTeam.cores().contains(c -> c.within(e.unit, state.rules.enemyCoreBuildRadius))){
                dropUnitsCoreZone.complete();
            }
        });

        Events.on(ClientChatEvent.class, e -> {
            if(e.message.contains(Iconc.alphaaaa + "")){
                useAnimdustryEmoji.complete();
            }
        });
    }

    private void checkUpdate(){
        if(campaign()){
            SStat.maxUnitActive.max(Groups.unit.count(t -> t.team == player.team()));

            if(Groups.unit.count(u -> u.type == UnitTypes.poly && u.team == player.team()) >= 10){
                active10Polys.complete();
            }

            for(Building entity : player.team().cores()){
                if(!content.items().contains(i -> !state.rules.hiddenBuildItems.contains(i) && entity.items.get(i) < entity.block.itemCapacity)){
                    fillCoreAllCampaign.complete();
                    break;
                }
            }

            for(var up : Groups.powerGraph){
                var graph = up.graph();
                if(graph.all.size > 1 && graph.all.first().team == player.team() && graph.hasPowerBalanceSamples()){
                    float balance = graph.getPowerBalance() * 60f;

                    if(balance < -10_000) negative10kPower.complete();
                    if(balance > 100_000) positive100kPower.complete();
                    if(graph.getBatteryStored() > 1_000_000) store1milPower.complete();
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
