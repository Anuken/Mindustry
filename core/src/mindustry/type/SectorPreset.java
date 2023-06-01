package mindustry.type;

import arc.func.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.maps.generators.*;
import mindustry.mod.Mods.*;

public class SectorPreset extends UnlockableContent{
    public FileMapGenerator generator;
    public Planet planet;
    public Sector sector;

    public int captureWave = 0;
    public Cons<Rules> rules = rules -> rules.winWave = captureWave;
    /** Difficulty, 0-10. */
    public float difficulty;
    public float startWaveTimeMultiplier = 2f;
    public boolean addStartingItems = false;
    public boolean noLighting = false;
    /** If true, this is the last sector in its planetary campaign. */
    public boolean isLastSector;
    public boolean showSectorLandInfo = true;
    /** If true, uses this sector's launch fields instead */
    public boolean overrideLaunchDefaults = false;
    /** Whether to allow users to specify a custom launch schematic for this map. */
    public boolean allowLaunchSchematics = false;
    /** Whether to allow users to specify the resources they take to this map. */
    public boolean allowLaunchLoadout = false;
    /** If true, switches to attack mode after waves end. */
    public boolean attackAfterWaves = false;

    public SectorPreset(String name, Planet planet, int sector){
        this(name);
        initialize(planet, sector);
    }

    /** Internal use only! */
    public SectorPreset(String name, LoadedMod mod){
        super(name);
        this.minfo.mod = mod;
        this.generator = new FileMapGenerator(name, this);
    }

    /** Internal use only! */
    public SectorPreset(String name){
       this(name, null);
    }

    public void initialize(Planet planet, int sector){
        this.planet = planet;
        sector %= planet.sectors.size;
        this.sector = planet.sectors.get(sector);
        inlineDescription = false;

        planet.preset(sector, this);
    }

    @Override
    public void loadIcon(){
        if(Icon.terrain != null){
            uiIcon = fullIcon = Icon.terrain.getRegion();
        }
    }

    @Override
    public boolean isHidden(){
        return description == null;
    }

    @Override
    public ContentType getContentType(){
        return ContentType.sector;
    }

}
