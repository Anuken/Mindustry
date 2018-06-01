package io.anuke.mindustry.io;

import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.world.Map;

import java.util.Date;

import static io.anuke.mindustry.Vars.world;

public class SaveMeta {
    public int version;
    public String date;
    public GameMode mode;
    public Map map;
    public int wave;
    public Difficulty difficulty;

    public SaveMeta(int version, long date, int mode, int map, int wave, Difficulty difficulty){
        this.version = version;
        this.date = Platform.instance.format(new Date(date));
        this.mode = GameMode.values()[mode];
        this.map = world.maps().getMap(map);
        this.wave = wave;
        this.difficulty = difficulty;
    }
}
