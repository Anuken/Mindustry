package mindustry.logic;

public enum LogicTileLayer{
    floor,
    ore,
    block,
    building;

    public static final LogicTileLayer[] all = values(), settable = {floor, ore, block};
}
