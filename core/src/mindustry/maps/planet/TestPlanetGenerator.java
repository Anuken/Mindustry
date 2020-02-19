package mindustry.maps.planet;

import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.type.*;
import mindustry.world.*;

//TODO refactor into generic planet class
public class TestPlanetGenerator implements PlanetGenerator{
    Simplex noise = new Simplex();
    RidgedPerlin rid = new RidgedPerlin(1, 2);
    float scl = 5f;
    Sector sector;
    Tiles tiles;

    //TODO generate array from planet image later
    Block[][] arr = {
    {Blocks.water, Blocks.darksandWater, Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.darksandTaintedWater, Blocks.snow, Blocks.ice},
    {Blocks.water, Blocks.darksandWater, Blocks.darksand, Blocks.darksand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.darksandTaintedWater, Blocks.snow, Blocks.snow, Blocks.ice},
    {Blocks.water, Blocks.darksandWater, Blocks.darksand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.darksandTaintedWater, Blocks.snow, Blocks.ice, Blocks.ice},
    {Blocks.water, Blocks.sandWater, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.iceSnow, Blocks.snow, Blocks.snow, Blocks.ice, Blocks.ice},
    {Blocks.deepwater, Blocks.water, Blocks.sandWater, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.moss, Blocks.snow, Blocks.snow, Blocks.snow, Blocks.snow, Blocks.ice},
    {Blocks.deepwater, Blocks.water, Blocks.sandWater, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.moss, Blocks.iceSnow, Blocks.snow, Blocks.snow, Blocks.ice, Blocks.snow, Blocks.ice},
    {Blocks.deepwater, Blocks.sandWater, Blocks.sand, Blocks.sand, Blocks.moss, Blocks.moss, Blocks.moss, Blocks.snow, Blocks.snow, Blocks.ice, Blocks.ice, Blocks.snow, Blocks.ice},
    {Blocks.taintedWater, Blocks.darksandTaintedWater, Blocks.darksand, Blocks.darksand, Blocks.darksandTaintedWater, Blocks.moss, Blocks.snow, Blocks.snow, Blocks.snow, Blocks.ice, Blocks.snow, Blocks.ice, Blocks.ice},
    {Blocks.darksandWater, Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.moss, Blocks.sporeMoss, Blocks.snow, Blocks.snow, Blocks.snow, Blocks.snow, Blocks.snow, Blocks.ice, Blocks.ice},
    {Blocks.darksandWater, Blocks.darksand, Blocks.darksand, Blocks.sporeMoss, Blocks.ice, Blocks.ice, Blocks.snow, Blocks.snow, Blocks.snow, Blocks.snow, Blocks.ice, Blocks.ice, Blocks.ice},
    {Blocks.taintedWater, Blocks.darksandTaintedWater, Blocks.darksand, Blocks.sporeMoss, Blocks.sporeMoss, Blocks.ice, Blocks.ice, Blocks.snow, Blocks.snow, Blocks.ice, Blocks.ice, Blocks.ice, Blocks.ice},
    {Blocks.darksandTaintedWater, Blocks.darksandTaintedWater, Blocks.darksand, Blocks.sporeMoss, Blocks.moss, Blocks.sporeMoss, Blocks.iceSnow, Blocks.snow, Blocks.ice, Blocks.ice, Blocks.ice, Blocks.ice, Blocks.ice},
    {Blocks.darksandWater, Blocks.darksand, Blocks.darksand, Blocks.ice, Blocks.iceSnow, Blocks.iceSnow, Blocks.snow, Blocks.snow, Blocks.ice, Blocks.ice, Blocks.ice, Blocks.ice, Blocks.ice}
    };

    float water = 2f / arr[0].length;

    float rawHeight(Vec3 position){
        position = Tmp.v33.set(position).scl(scl);
        return Mathf.pow((float)noise.octaveNoise3D(7, 0.48f, 1f/3f, position.x, position.y, position.z), 2.3f);
    }

    @Override
    public float getHeight(Vec3 position){
        float height = rawHeight(position);
        if(height <= water){
            return water;
        }
        return height;
    }

    @Override
    public Color getColor(Vec3 position){
        return getBlock(position).color;
    }

    @Override
    public void generate(Vec3 position, TileGen tile){
        tile.floor = getBlock(position);
        tile.block = tile.floor.asFloor().wall;

        if(noise.octaveNoise3D(5, 0.6, 8.0, position.x, position.y, position.z) > 0.65){
            //tile.block = Blocks.air;
        }

        if(rid.getValue(position.x, position.y, position.z, 22) > 0.34){
            tile.block = Blocks.air;
        }
    }

    @Override
    public void decorate(Tiles tiles, Sector sec){
        this.tiles = tiles;
        this.sector = sec;

        //OvergrowthGenerator generator = new OvergrowthGenerator(tiles.width, tiles.height);
        //generator.init(Loadouts.basicNucleus);
        //generator.generate(tiles);
        GridBits write = new GridBits(tiles.width, tiles.height);
        GridBits read = new GridBits(tiles.width, tiles.height);

        //double removalChance = 0.2;
        //remove random tiles
        //tiles.each((x, y) -> {

        //});

        tiles.each((x, y) -> read.set(x, y, !tiles.get(x, y).block().isAir()));

        int iterations = 4, birthLimit = 16, deathLimit = 16, radius = 3;

        for(int i = 0; i < iterations; i++){
            tiles.each((x, y) -> {
                int alive = 0;

                for(int cx = -radius; cx <= radius; cx++){
                    for(int cy = -radius; cy <= radius; cy++){
                        if((cx == 0 && cy == 0) || !Mathf.within(cx, cy, radius)) continue;
                        if(!Structs.inBounds(x + cx, y + cy, tiles.width, tiles.height) || read.get(x + cx, y + cy)){
                            alive++;
                        }
                    }
                }

                if(read.get(x, y)){
                    write.set(x, y, alive >= deathLimit);
                }else{
                    write.set(x, y, alive > birthLimit);
                }
            });

            //flush results
            read.set(write);
        }

        tiles.each((x, y) -> tiles.get(x, y).setBlock(!read.get(x, y) ? Blocks.air : tiles.get(x, y).floor().wall));
        distort(0.01f, 8f);

        tiles.get(tiles.width /2, tiles.height /2).setBlock(Blocks.coreShard, Team.sharded);
    }

    void distort(float scl, float mag){
        short[] blocks = new short[tiles.width * tiles.height];
        short[] floors = new short[blocks.length];

        tiles.each((x, y) -> {
            int idx = y*tiles.width + x;
            float cx = x + noise(x, y, scl, mag) - mag / 2f, cy = y + noise(x, y + 152f, scl, mag) - mag / 2f;
            Tile other = tiles.getn(Mathf.clamp((int)cx, 0, tiles.width-1), Mathf.clamp((int)cy, 0, tiles.height-1));
            blocks[idx] = other.block().id;
            floors[idx] = other.floor().id;
        });

        for(int i = 0; i < blocks.length; i++){
            Tile tile = tiles.geti(i);
            tile.setFloor(Vars.content.block(floors[i]).asFloor());
            tile.setBlock(Vars.content.block(blocks[i]));
        }
    }

    protected float noise(float x, float y, float scl, float mag){
        Vec3 v = sector.rect.project(x / tiles.width, y / tiles.height);
        return (float)noise.octaveNoise3D(1f, 0f, 1f / scl, v.x, v.y, v.z) * mag;
    }

    Block getBlock(Vec3 position){
        float height = rawHeight(position);
        position = Tmp.v33.set(position).scl(scl);
        float rad = scl;
        float temp = Mathf.clamp(Math.abs(position.y * 2f) / (rad));
        float tnoise = (float)noise.octaveNoise3D(7, 0.48f, 1f/3f, position.x, position.y + 999f, position.z);
        temp = Mathf.lerp(temp, tnoise, 0.5f);
        height *= 1.2f;
        height = Mathf.clamp(height);

        return arr[Mathf.clamp((int)(temp * arr.length), 0, arr.length - 1)][Mathf.clamp((int)(height * arr[0].length), 0, arr[0].length - 1)];
    }
}
