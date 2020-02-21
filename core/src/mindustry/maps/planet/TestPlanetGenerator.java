package mindustry.maps.planet;

import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.maps.generators.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.schematics;

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
    {Blocks.water, Blocks.darksandWater, Blocks.darksand, Blocks.sand, Blocks.salt, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.darksandTaintedWater, Blocks.snow, Blocks.ice, Blocks.ice},
    {Blocks.water, Blocks.sandWater, Blocks.sand, Blocks.salt, Blocks.salt, Blocks.salt, Blocks.sand, Blocks.sand, Blocks.iceSnow, Blocks.snow, Blocks.snow, Blocks.ice, Blocks.ice},
    {Blocks.deepwater, Blocks.water, Blocks.sandWater, Blocks.sand, Blocks.salt, Blocks.sand, Blocks.sand, Blocks.moss, Blocks.snow, Blocks.snow, Blocks.snow, Blocks.snow, Blocks.ice},
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
        Block block = getBlock(position);
        //replace salt with sand color
        return block == Blocks.salt ? Blocks.sand.color : block.color;
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

        new Terrain().generate(tiles);
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

    class Terrain extends BasicGenerator{
        Array<Block> ores = Array.with(Blocks.oreCopper, Blocks.oreLead, Blocks.oreCoal, Blocks.oreCopper);

        @Override
        protected void generate(){

            cells(4);
            distort(20f, 12f);

            float constraint = 1.3f;
            float radius = width / 2f / Mathf.sqrt3;
            int rooms = Mathf.random(2, 5);
            Array<Point3> array = new Array<>();

            //TODO replace random calls with seed

            for(int i = 0; i < rooms; i++){
                Tmp.v1.trns(Mathf.random(360f), Mathf.random(radius / constraint));
                float rx = (width/2f + Tmp.v1.x);
                float ry = (height/2f + Tmp.v1.y);
                float maxrad = radius - Tmp.v1.len();
                float rrad = Math.min(Mathf.random(9f, maxrad / 2f), 30f);
                array.add(new Point3((int)rx, (int)ry, (int)rrad));
            }

            for(Point3 room : array){
                erase(room.x, room.y, room.z);
            }

            int connections = Mathf.random(Math.max(rooms - 1, 1), rooms + 3);
            for(int i = 0; i < connections; i++){
                Point3 from = array.random();
                Point3 to = array.random();

                float nscl = Mathf.random(20f, 60f);
                int stroke = Mathf.random(4, 12);
                brush(pathfind(from.x, from.y, to.x, to.y, tile -> (tile.solid() ? 5f : 0f) + (float)sim.octaveNoise2D(1, 1, 1f / nscl, tile.x, tile.y) * 50, manhattan), stroke);
            }

            cells(1);
            distort(20f, 6f);

            Point3 spawn = array.random();
            inverseFloodFill(tiles.getn(spawn.x, spawn.y));

            ores(ores);

            for(Point3 other : array){
                if(other != spawn){
                   // tiles.getn(other.x, other.y).setOverlay(Blocks.spawn);
                }
            }

            schematics.placeLoadout(Loadouts.advancedShard, spawn.x, spawn.y);
        }

        void cells(int iterations){
            GridBits write = new GridBits(tiles.width, tiles.height);
            GridBits read = new GridBits(tiles.width, tiles.height);

            tiles.each((x, y) -> read.set(x, y, !tiles.get(x, y).block().isAir()));

            int birthLimit = 16, deathLimit = 16, cradius = 3;

            for(int i = 0; i < iterations; i++){
                tiles.each((x, y) -> {
                    int alive = 0;

                    for(int cx = -cradius; cx <= cradius; cx++){
                        for(int cy = -cradius; cy <= cradius; cy++){
                            if((cx == 0 && cy == 0) || !Mathf.within(cx, cy, cradius)) continue;
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
        }
    }
}
