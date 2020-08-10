package mindustry.logic;

public enum LSensor{
    totalItems,
    totalLiquids,
    totalPower,
    powerNetStored,
    powerNetCapacity,
    powerNetIn,
    powerNetOut,
    health,
    heat,
    efficiency;

    public static final LSensor[] all = values();
}
