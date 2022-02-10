package mindustry.logic;

public enum LogicRule{
    currentWaveTime,
    waveTimer,
    waves,
    waveSpacing,
    attackMode,
    enemyCoreBuildRadius,
    dropZoneRadius,
    unitCap,
    lighting,
    ambientLight;

    public static final LogicRule[] all = values();
}
