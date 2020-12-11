package mindustry.maps.generators;

import arc.math.geom.*;
import arc.struct.*;
import arc.util.noise.*;
import mindustry.graphics.g3d.*;
import mindustry.graphics.g3d.PlanetGrid.*;
import mindustry.type.*;
import mindustry.world.*;

public abstract class PlanetGenerator extends BasicGenerator implements HexMesher{
    protected IntSeq ints = new IntSeq();
    protected Sector sector;
    protected Simplex noise = new Simplex();

    /** Should generate sector bases for a planet. */
    public void generateSector(Sector sector){
        Ptile tile = sector.tile;

        boolean any = false;
        float noise = Noise.snoise3(tile.v.x, tile.v.y, tile.v.z, 0.001f, 0.5f);

        if(noise > 0.027){
            any = true;
        }

        if(noise < 0.15){
            for(Ptile other : tile.tiles){
                //no sectors near start sector!
                if(sector.planet.getSector(other).id == sector.planet.startSector){
                    return;
                }
                
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

    @Override
    protected float noise(float x, float y, double octaves, double falloff, double scl, double mag){
        Vec3 v = sector.rect.project(x, y);
        return (float)noise.octaveNoise3D(octaves, falloff, 1f / scl, v.x, v.y, v.z) * (float)mag;
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
