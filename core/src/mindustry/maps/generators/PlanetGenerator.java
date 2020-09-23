package mindustry.maps.generators;

import arc.math.geom.*;
import arc.util.noise.*;
import mindustry.graphics.g3d.*;
import mindustry.graphics.g3d.PlanetGrid.*;
import mindustry.type.*;
import mindustry.world.*;

public abstract class PlanetGenerator extends BasicGenerator implements HexMesher{
    protected Sector sector;

    /** Should generate sector bases for a planet. */
    public void generateSector(Sector sector){
        Ptile tile = sector.tile;

        boolean any = false;
        float noise = Noise.snoise3(tile.v.x, tile.v.y, tile.v.z, 0.001f, 0.5f);

        if(noise > 0.028){
            any = true;
        }

        if(noise < 0.15){
            for(Ptile other : tile.tiles){
                if(sector.planet.getSector(other).generateEnemyBase){
                    any = false;
                    break;
                }
            }
        }

        if(any){
            sector.generateEnemyBase = true;
        }
    }

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
