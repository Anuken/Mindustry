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
    issueAttackCommand, //TODO change desc
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
    create500Schematics(SStat.schematicsCreated, 50), //TODO - Steam - icon done
    survive10WavesNoBlocks,
    captureNoBlocksBroken,
    useFlameAmmo,
    coolTurret,
    enablePixelation,
    openWiki,
    useAccelerator,
    unlockAllZones,

    //TODO new ones

    allTransportOneMap, //TODO - Steam - icon done
    buildOverdriveProjector, //TODO - Steam - icon done
    buildMendProjector, //TODO - Steam - icon done
    buildWexWater, //TODO - Steam - icon done

    have10mItems(SStat.totalCampaignItems, 10_000_000), //TODO - Steam - icon done
    killEclipseDuo, //TODO - Steam - icon done

    allPresetsErekir, //TODO - Steam - icon done

    launchCoreSchematic, //TODO - Steam - icon done
    nucleusGroundZero, //TODO - Steam - icon done

    neoplasmWater, //TODO - Steam - icon done
    blastFrozenUnit, //TODO - Steam - icon done

    allBlocksSerpulo, //TODO - Steam - icon done
    allBlocksErekir, //TODO - Steam - icon done

    //TODO are these necessary?
    //allTurretsSerpulo, //TODO
    //allTurretsErekir, //TODO
    //allTechSerpulo, //TODO
    //allTechErekir, //TODO

    breakForceProjector, //TODO - Steam - icon done
    researchLogic, //TODO - Steam - icon done

    negative10kPower, //TODO - Steam - icon done
    positive100kPower, //TODO - Steam - icon done
    store1milPower, //TODO - Steam - icon done

    blastGenerator, //TODO - Steam - icon done
    neoplasiaExplosion, //TODO - Steam - icon done

    installMod, //TODO - Steam - icon done
    routerLanguage, //TODO - Steam - icon done
    joinCommunityServer, //TODO - Steam - icon done
    openConsole, //TODO - Steam - icon done

    controlTurret, //TODO - Steam - icon done
    dropUnitsCoreZone, //TODO - Steam - icon done
    destroyScatterFlare, //TODO - Steam - icon done
    boostUnit, //TODO - Steam - icon done
    boostBuildingFloor, //TODO - Steam - icon done

    hoverUnitLiquid, //TODO - Steam - icon done

    break100Boulders(SStat.bouldersDeconstructed, 100), //TODO - Steam - icon done
    break10000Boulders(SStat.bouldersDeconstructed, 10_000), //TODO - Steam - icon done

    shockwaveTowerUse, //TODO - Steam - icon done

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
