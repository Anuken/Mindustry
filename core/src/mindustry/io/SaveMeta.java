package mindustry.io;

import arc.struct.*;
import mindustry.game.*;
import mindustry.maps.*;

import static mindustry.Vars.maps;

public class SaveMeta{
    public int version;
    public int build;
    public long timestamp;
    public long timePlayed;
    public Map map;
    public int wave;
    public Rules rules;
    public SectorInfo secinfo;
    public StringMap tags;
    public String[] mods;
    public boolean hasProduction;

    public SaveMeta(int version, long timestamp, long timePlayed, int build, String map, int wave, Rules rules, SectorInfo secinfo, StringMap tags){
        this.version = version;
        this.build = build;
        this.timestamp = timestamp;
        this.timePlayed = timePlayed;
        this.map = maps.all().find(m -> m.name().equals(map));
        this.wave = wave;
        this.rules = rules;
        this.tags = tags;
        this.mods = JsonIO.read(String[].class, tags.get("mods", "[]"));
        this.secinfo = secinfo;

        secinfo.exportRates().each(e -> hasProduction |= e.value > 0.001f);
    }
}
