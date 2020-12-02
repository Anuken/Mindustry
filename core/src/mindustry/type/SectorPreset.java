package mindustry.type;

import arc.func.*;
import arc.graphics.g2d.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.maps.generators.*;
import mindustry.ui.*;

public class SectorPreset extends UnlockableContent{
    public FileMapGenerator generator;
    public Planet planet;
    public Sector sector;

    public int captureWave = 0;
    public Cons<Rules> rules = rules -> rules.winWave = captureWave;
    public boolean useAI = true;
    /** Difficulty, 0-10. */
    public float difficulty;
    public boolean addStartingItems = false;

    public SectorPreset(String name, Planet planet, int sector){
        super(name);
        this.generator = new FileMapGenerator(name, this);
        this.planet = planet;
        sector %= planet.sectors.size;
        this.sector = planet.sectors.get(sector);
        inlineDescription = false;

        planet.preset(sector, this);
    }

    @Override
    public TextureRegion icon(Cicon c){
        return Icon.terrain.getRegion();
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
