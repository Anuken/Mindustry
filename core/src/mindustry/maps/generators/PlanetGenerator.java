package mindustry.maps.generators;

import arc.math.geom.*;
import arc.struct.*;
import arc.struct.ObjectIntMap.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.graphics.g3d.*;
import mindustry.graphics.g3d.PlanetGrid.*;
import mindustry.type.*;
import mindustry.type.Weather.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public abstract class PlanetGenerator extends BasicGenerator implements HexMesher{
    protected IntSeq ints = new IntSeq();
    protected Sector sector;

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

    public void addWeather(Sector sector, Rules rules){

        //apply weather based on terrain
        ObjectIntMap<Block> floorc = new ObjectIntMap<>();
        ObjectSet<UnlockableContent> content = new ObjectSet<>();

        for(Tile tile : world.tiles){
            if(world.getDarkness(tile.x, tile.y) >= 3){
                continue;
            }

            Liquid liquid = tile.floor().liquidDrop;
            if(tile.floor().itemDrop != null) content.add(tile.floor().itemDrop);
            if(tile.overlay().itemDrop != null) content.add(tile.overlay().itemDrop);
            if(liquid != null) content.add(liquid);

            if(!tile.block().isStatic()){
                floorc.increment(tile.floor());
                if(tile.overlay() != Blocks.air){
                    floorc.increment(tile.overlay());
                }
            }
        }

        //sort counts in descending order
        Seq<Entry<Block>> entries = floorc.entries().toArray();
        entries.sort(e -> -e.value);
        //remove all blocks occuring < 30 times - unimportant
        entries.removeAll(e -> e.value < 30);

        Block[] floors = new Block[entries.size];
        for(int i = 0; i < entries.size; i++){
            floors[i] = entries.get(i).key;
        }

        //TODO bad code
        boolean hasSnow = floors.length > 0 && (floors[0].name.contains("ice") || floors[0].name.contains("snow"));
        boolean hasRain = floors.length > 0 && !hasSnow && content.contains(Liquids.water) && !floors[0].name.contains("sand");
        boolean hasDesert = floors.length > 0 && !hasSnow && !hasRain && floors[0] == Blocks.sand;
        boolean hasSpores = floors.length > 0 && (floors[0].name.contains("spore") || floors[0].name.contains("moss") || floors[0].name.contains("tainted"));

        if(hasSnow){
            rules.weather.add(new WeatherEntry(Weathers.snow));
        }

        if(hasRain){
            rules.weather.add(new WeatherEntry(Weathers.rain));
            rules.weather.add(new WeatherEntry(Weathers.fog));
        }

        if(hasDesert){
            rules.weather.add(new WeatherEntry(Weathers.sandstorm));
        }

        if(hasSpores){
            rules.weather.add(new WeatherEntry(Weathers.sporestorm));
        }
    }

    protected void genTile(Vec3 position, TileGen tile){

    }

    @Override
    protected float noise(float x, float y, double octaves, double falloff, double scl, double mag){
        Vec3 v = sector.rect.project(x, y);
        return (float)Simplex.noise3d(0, octaves, falloff, 1f / scl, v.x, v.y, v.z) * (float)mag;
    }

    /** @return the scaling factor for sector rects. */
    public float getSizeScl(){
        return 3200;
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
