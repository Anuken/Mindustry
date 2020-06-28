package mindustry.maps.generators;

import arc.math.geom.*;
import mindustry.graphics.g3d.*;
import mindustry.type.*;
import mindustry.world.*;

public abstract class PlanetGenerator extends BasicGenerator implements HexMesher{
    protected Sector sector;

    protected void genTile(Vec3 position, TileGen tile){

    }

    public void generate(Tiles tiles, Sector sec){
        this.tiles = tiles;
        this.sector = sec;
        this.rand.setSeed(sec.id);

        TileGen gen = new TileGen();
        tiles.each((x, y) -> {
            gen.reset();
            Vec3 position = sector.rect.project(x / (float)tiles.width, y / (float)tiles.height);

            genTile(position, gen);
            tiles.set(x, y, new Tile(x, y, gen.floor, gen.overlay, gen.block));
        });

        generate(tiles);
    }
}
