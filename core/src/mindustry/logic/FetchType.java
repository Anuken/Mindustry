package mindustry.logic;

public enum FetchType{
    unit,
    unitCount,
    player,
    playerCount,
    core,
    coreCount;

    public static final FetchType[] all = values();
}
