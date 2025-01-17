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
    ambientLight,
    solarMultiplier,
    dragMultiplier,
    ban,
    unban,

    //team specific
    buildSpeed,
    unitHealth,
    unitBuildSpeed,
    unitCost,
    unitDamage,
    blockHealth,
    blockDamage,
    rtsMinWeight,
    rtsMinSquad;

    public static final LogicRule[] all = values();
}
