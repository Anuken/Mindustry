package mindustry.type;

import arc.func.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
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
    /** Difficulty, 0-10. */
    public float difficulty;

    public SectorPreset(String name, Planet planet, int sector){
        super(name);
        this.generator = new FileMapGenerator(name);
        this.planet = planet;
        sector %= planet.sectors.size;
        this.sector = planet.sectors.get(sector);

        planet.preset(sector, this);
    }

    @Override
    public TextureRegion icon(Cicon c){
        return Icon.terrain.getRegion();
    }

    @Override
    public boolean isHidden(){
        return true;
    }

    //neither of these are implemented, as zones are not displayed in a normal fashion... yet
    @Override
    public void displayInfo(Table table){
    }

    @Override
    public ContentType getContentType(){
        return ContentType.sector;
    }

}
