package io.anuke.mindustry.io;

import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.maps.Map;

import java.util.Date;

import static io.anuke.mindustry.Vars.world;

public class SaveMeta{
    public int version;
    public int build;
    public String date;
    public long timePlayed;
    public GameMode mode;
    public Map map;
    public int wave;
    public Difficulty difficulty;

    public SaveMeta(int version, long date, long timePlayed, int build, int mode, String map, int wave, Difficulty difficulty){
        this.version = version;
        this.build = build;
        this.date = Platform.instance.format(new Date(date));
        this.timePlayed = timePlayed;
        this.mode = GameMode.values()[mode];
        this.map = world.maps().getByName(map);
        this.wave = wave;
        this.difficulty = difficulty;
    }
}
