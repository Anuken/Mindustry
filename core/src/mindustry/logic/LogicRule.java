package mindustry.logic;

public enum LogicRule{
    currentWaveTime,
    waveTimer,
    waves,
    wave,
    waveSpacing,
    waveSending,
    attackMode,
    enemyCoreBuildRadius,
    dropZoneRadius,
    unitCap,
    mapArea,
    lighting,
    canGameOver,
    ambientLight,
    solarMultiplier,
    dragMultiplier,
    ban,
    unban,

    //team specific
    buildSpeed,
    unitHealth,
    unitBuildSpeed,
    unitMineSpeed,
    unitCost,
    unitDamage,
    blockHealth,
    blockDamage,
    rtsMinWeight,
    rtsMinSquad;

    public static final LogicRule[] all = values();
}
