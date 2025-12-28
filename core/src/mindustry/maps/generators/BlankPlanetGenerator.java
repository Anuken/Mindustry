package mindustry.maps.generators;

import mindustry.game.*;
import mindustry.type.*;
import mindustry.world.*;

/** A planet generator that provides no weather, height, color or bases. Override generate().*/
public class BlankPlanetGenerator extends PlanetGenerator{

    @Override
    public void addWeather(Sector sector, Rules rules){

    }

    @Override
    public void generate(Tiles tiles, Sector sec, WorldParams params){
        this.tiles = tiles;
        this.sector = sec;
        this.rand.setSeed(sec.id + params.seedOffset + baseSeed);

        tiles.fill();

        generate(tiles, params);
    }

}
