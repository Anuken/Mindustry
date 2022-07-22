package mindustry.logic;

public enum LogicRule{
    currentWaveTime,
    waveTimer,
    waves,
    wave,
    waveSpacing,
    attackMode,
    enemyCoreBuildRadius,
    dropZoneRadius,
    unitCap,
    mapArea,
    lighting,
    ambientLight,
    solarMultiplier,

    //team specific
    buildSpeed,
    unitBuildSpeed,
    unitDamage,
    blockHealth,
    blockDamage,
    rtsMinWeight,
    rtsMinSquad;

    public static final LogicRule[] all = values();
}
