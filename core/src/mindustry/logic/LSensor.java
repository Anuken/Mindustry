package mindustry.logic;

public enum LSensor{
    totalItems,
    totalLiquids,
    totalPower,
    itemCapacity,
    liquidCapacity,
    powerCapacity,
    powerNetStored,
    powerNetCapacity,
    powerNetIn,
    powerNetOut,
    health,
    heat,
    efficiency;

    public static final LSensor[] all = values();
}
