package mindustry.logic;

public enum TileLayer{
    floor,
    ore,
    block,
    building;

    public static final TileLayer[] all = values(), settable = {floor, ore, block};
}
