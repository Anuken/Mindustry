package mindustry.service;

import arc.util.*;

import static mindustry.Vars.*;

public enum Achievement{
    kill1kEnemies(SStat.unitsDestroyed, 1000),
    kill100kEnemies(SStat.unitsDestroyed, 100_000),
    launch100kItems(SStat.itemsLaunched, 100_000),

    produce5kMin(SStat.maxProduction, 5000),
    produce50kMin(SStat.maxProduction, 50_000),
    win10Attack(SStat.attacksWon, 10),
    win10PvP(SStat.pvpsWon, 10),
    defeatAttack5Waves,
    launch30Times(SStat.timesLaunched, 30),
    captureBackground,
    survive100Waves(SStat.maxWavesSurvived, 100),
    researchAll,
    shockWetEnemy,
    killEnemyPhaseWall,
    researchRouter,
    place10kBlocks(SStat.blocksBuilt, 10_000),
    destroy1kBlocks(SStat.blocksDestroyed, 1000),
    overheatReactor(SStat.reactorsOverheated, 1),
    make10maps(SStat.mapsMade, 10),
    downloadMapWorkshop,
    publishMap(SStat.mapsPublished, 1),
    defeatBoss(SStat.bossesDefeated, 1),
    captureAllSectors,
    control10Sectors(SStat.sectorsControlled, 10),
    drop10kitems,
    powerupImpactReactor,
    obtainThorium,
    obtainTitanium,
    suicideBomb,
    buildGroundFactory,
    issueAttackCommand,
    active100Units(SStat.maxUnitActive, 100),
    build1000Units(SStat.unitsBuilt, 1000),
    buildAllUnits(SStat.unitTypesBuilt, 30),
    buildT5,
    pickupT5,
    active10Polys,
    dieExclusion,
    drown,
    fillCoreAllCampaign,
    hostServer10(SStat.maxPlayersServer, 10),
    buildMeltdownSpectre, //technically inaccurate
    launchItemPad,
    chainRouters,
    circleConveyor,
    becomeRouter,
    create20Schematics(SStat.schematicsCreated, 20),
    create500Schematics(SStat.schematicsCreated, 500),
    survive10WavesNoBlocks,
    captureNoBlocksBroken,
    useFlameAmmo,
    coolTurret,
    enablePixelation,
    openWiki,
    allTransportOneMap,
    buildOverdriveProjector,
    buildMendProjector,
    buildWexWater,

    have10mItems(SStat.totalCampaignItems, 10_000_000),
    killEclipseDuo,

    completeErekir,
    completeSerpulo,

    launchCoreSchematic,
    nucleusGroundZero,

    neoplasmWater,
    blastFrozenUnit,

    allBlocksSerpulo,
    allBlocksErekir,

    breakForceProjector,
    researchLogic,

    negative10kPower,
    positive100kPower,
    store1milPower,

    blastGenerator,
    neoplasiaExplosion,

    installMod,
    routerLanguage,
    joinCommunityServer,
    openConsole,

    controlTurret,
    dropUnitsCoreZone,
    destroyScatterFlare,
    boostUnit,
    boostBuildingFloor,

    hoverUnitLiquid,

    break100Boulders(SStat.bouldersDeconstructed, 100),
    break10000Boulders(SStat.bouldersDeconstructed, 10_000),

    shockwaveTowerUse,

    useAnimdustryEmoji,

    ;

    private final SStat stat;
    private final int statGoal;
    private boolean completed = false;

    public static final Achievement[] all = values();

    /** Creates an achievement that is triggered when this stat reaches a number.*/
    Achievement(SStat stat, int goal){
        this.stat = stat;
        this.statGoal = goal;
    }

    Achievement(){
        this(null, 0);
    }

    public void complete(){
        if(!isAchieved()){
            //can't complete achievements with the dev console shown.
            if(ui != null && ui.consolefrag != null && ui.consolefrag.shown() && !OS.username.equals("anuke") && this != openConsole) return;

            service.completeAchievement(name());
            service.storeStats();
        }
    }

    public void uncomplete(){
        if(isAchieved()){
            service.clearAchievement(name());
            completed = false;
        }
    }

    public void checkCompletion(){
        if(!isAchieved() && stat != null && stat.get() >= statGoal){
            complete();
        }
    }

    public boolean isAchieved(){
        if(completed){
            return true;
        }
        return (completed = service.isAchieved(name()));
    }
}
