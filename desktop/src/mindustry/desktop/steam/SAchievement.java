package mindustry.desktop.steam;

public enum SAchievement{
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
    survive10WavesNoBlocks,
    captureNoBlocksBroken,
    useFlameAmmo,
    coolTurret,
    enablePixelation,
    openWiki,
    useAccelerator,

    ;

    private final SStat stat;
    private final int statGoal;
    public static final SAchievement[] all = values();

    /** Creates an achievement that is triggered when this stat reaches a number.*/
    SAchievement(SStat stat, int goal){
        this.stat = stat;
        this.statGoal = goal;
    }

    SAchievement(){
        this(null, 0);
    }

    public void complete(){
        if(!isAchieved()){
            SVars.stats.stats.setAchievement(name());
            SVars.stats.stats.storeStats();
        }
    }

    public void checkCompletion(){
        if(!isAchieved() && stat != null && stat.get() >= statGoal){
            complete();
        }
    }

    public boolean isAchieved(){
        return SVars.stats.stats.isAchieved(name(), false);
    }
}
