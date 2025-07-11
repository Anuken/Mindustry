package mindustry.type;

import arc.*;
import arc.func.*;
import arc.util.*;
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
    /** If true, this sector must be unlocked before landing is permitted. */
    public boolean requireUnlock = true;
    public boolean showSectorLandInfo = true;
    /** If true, uses this sector's launch fields instead */
    public boolean overrideLaunchDefaults = false;
    /** Whether to allow users to specify a custom launch schematic for this map. */
    public boolean allowLaunchSchematics = false;
    /** Whether to allow users to specify the resources they take to this map. */
    public boolean allowLaunchLoadout = false;
    /** If true, switches to attack mode after waves end. */
    public boolean attackAfterWaves = false;
    /** The original position of this sector; used for migration. Internal use for vanilla campaign only! */
    public int originalPosition;

    public SectorPreset(String name, Planet planet, int sector){
        this(name, null, planet, sector);
    }

    public SectorPreset(String name, String fileName, Planet planet, int sector){
        this(name, fileName, null);
        initialize(planet, sector);
    }

    /** Internal use only! */
    public SectorPreset(String name, LoadedMod mod){
        this(name, null, mod);
    }

    /** Internal use only! */
    public SectorPreset(String name, @Nullable String fileName, LoadedMod mod){
        super(name);
        if(mod != null){
            this.minfo.mod = mod;
        }
        //this.name can change based on the mod being loaded, so if a fileName is not specified, make sure to use the newly assigned this.name
        this.generator = new FileMapGenerator(fileName == null ? this.name : fileName, this);
    }

    public void initialize(Planet planet, int sector){
        this.planet = planet;
        this.originalPosition = sector;
        //auto remap based on data
        var data = planet.getData();
        if(data != null){
            sector = data.presets.get(name, sector);
        }
        sector %= planet.sectors.size;
        this.sector = planet.sectors.get(sector);

        planet.preset(sector, this);
    }

    @Override
    public void loadIcon(){
        if(Icon.terrain != null){
            uiIcon = fullIcon = Core.atlas.find("sector-" + name, Icon.terrain.getRegion());
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
