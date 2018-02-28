package io.anuke.mindustry.net;

public class Host {
    public final String name;
    public final String address;
    public final String mapname;
    public final int wave;
    public final int players;

    public Host(String name, String address, String mapname, int wave, int players){
        this.name = name;
        this.address = address;
        this.players = players;
        this.mapname = mapname;
        this.wave = wave;
    }
}
