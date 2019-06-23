package io.anuke.mindustry.io;

import io.anuke.mindustry.game.Rules;
import io.anuke.mindustry.maps.Map;

import static io.anuke.mindustry.Vars.world;

public class SaveMeta{
    public int version;
    public int build;
    public long timestamp;
    public long timePlayed;
    public Map map;
    public int wave;
    public Rules rules;

    public SaveMeta(int version, long timestamp, long timePlayed, int build, String map, int wave, Rules rules){
        this.version = version;
        this.build = build;
        this.timestamp = timestamp;
        this.timePlayed = timePlayed;
        this.map = world.maps.all().find(m -> m.name().equals(map));
        this.wave = wave;
        this.rules = rules;
    }
}
