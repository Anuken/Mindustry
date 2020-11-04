package mindustry.desktop.steam;

public enum SAchievement{
    //completeTutorial,
    kill1kEnemies(SStat.unitsDestroyed, 1000),
    kill100kEnemies(SStat.unitsDestroyed, 1000 * 100),
    //TODO achievements for reaching 10k/min production or something
    launch10kItems(SStat.itemsLaunched, 1000 * 10),
    //TODO reduce amounts required here
    launch1milItems(SStat.itemsLaunched, 1000 * 1000),
    win10Attack(SStat.attacksWon, 10),
    win10PvP(SStat.pvpsWon, 10),
    defeatAttack5Waves,
    //TODO
    launch30Times(SStat.timesLaunched, 30),
    //TODO
    captureBackground,
    //TODO
    survive100Waves(SStat.maxWavesSurvived, 100),
    survive500Waves(SStat.maxWavesSurvived, 500),
    researchAll,
    //TODO
    useAllUnits,
    shockWetEnemy,
    killEnemyPhaseWall,
    researchRouter,
    place10kBlocks(SStat.blocksBuilt, 10 * 1000),
    destroy1kBlocks(SStat.blocksDestroyed, 1000),
    overheatReactor(SStat.reactorsOverheated, 1),
    make10maps(SStat.mapsMade, 10),
    downloadMapWorkshop,
    publishMap(SStat.mapsPublished, 1),
    defeatBoss(SStat.bossesDefeated, 1),
    //TODO
    captureAllSectors,
    //TODO
    capture10Sectors,
    //TODO
    drop10kitems,
    powerupImpactReactor,
    obtainThorium,
    obtainTitanium,
    suicideBomb,
    buildGroundFactory,
    issueAttackCommand,
    active100Units(SStat.maxUnitActive, 100),
    build1000Units,
    buildAllUnits(SStat.unitsBuilt, 30),
    //TODO
    activeAllT5,
    dieExclusion,
    drown,
    fillCoreAllCampaign,
    hostServer10(SStat.maxPlayersServer, 10),
    buildMeltdownSpectreForeshadow,
    launchItemPad,
    chainRouters,
    //TODO
    circleConveyor,
    //TODO
    becomeRouter,
    //TODO
    save20Schematics,
    //TODO
    destroyEnemyBase,
    survive10WavesNoBlocks,
    useFlameAmmo,
    coolTurret,
    enablePixelation,
    openWiki,
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
