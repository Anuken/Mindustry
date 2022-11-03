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
    researchAll, //TODO - remake/change?
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
    issueAttackCommand, //TODO - test
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
    create500Schematics(SStat.schematicsCreated, 500), //TODO - test
    survive10WavesNoBlocks,
    captureNoBlocksBroken,
    useFlameAmmo,
    coolTurret,
    enablePixelation,
    openWiki,

    //TODO new ones

    allTransportOneMap, //TODO - test
    buildOverdriveProjector, //TODO - test
    buildMendProjector, //TODO - test
    buildWexWater, //TODO - test

    have10mItems(SStat.totalCampaignItems, 10_000_000), //TODO - test
    killEclipseDuo, //TODO - test

    completeErekir, //TODO - test
    completeSerpulo, //TODO - test

    launchCoreSchematic, //TODO - test
    nucleusGroundZero, //TODO - test

    neoplasmWater, //TODO - test
    blastFrozenUnit, //TODO - test

    allBlocksSerpulo, //TODO - test
    allBlocksErekir, //TODO - test

    //TODO are these necessary?
    //allTurretsSerpulo, //TODO
    //allTurretsErekir, //TODO
    //allTechSerpulo, //TODO
    //allTechErekir, //TODO

    breakForceProjector, //TODO - test
    researchLogic, //TODO - Steam - test

    negative10kPower, //TODO - test
    positive100kPower, //TODO - test
    store1milPower, //TODO - test

    blastGenerator, //TODO - test
    neoplasiaExplosion, //TODO - test

    installMod, //TODO - test
    routerLanguage, //TODO - test
    joinCommunityServer, //TODO - test
    openConsole, //TODO - test

    controlTurret, //TODO - test
    dropUnitsCoreZone, //TODO - test
    destroyScatterFlare, //TODO - test
    boostUnit, //TODO - test
    boostBuildingFloor, //TODO - test

    hoverUnitLiquid, //TODO - test

    break100Boulders(SStat.bouldersDeconstructed, 100), //TODO - test
    break10000Boulders(SStat.bouldersDeconstructed, 10_000), //TODO - test

    shockwaveTowerUse, //TODO - test

    useAnimdustryEmoji, //TODO - Steam - icon done

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
            if(ui != null && ui.consolefrag != null && ui.consolefrag.shown() && !OS.username.equals("anuke")) return;

            service.completeAchievement(name());
            service.storeStats();
            completed = true;
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
