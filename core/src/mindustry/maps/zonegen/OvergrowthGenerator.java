package mindustry.maps.zonegen;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.maps.generators.*;
import mindustry.world.*;

import static mindustry.Vars.schematics;

//TODO remove
public class OvergrowthGenerator extends BasicGenerator{
    Array<Block> ores = Array.with(Blocks.oreCopper, Blocks.oreLead, Blocks.oreCoal, Blocks.oreCopper);

    @Override
    protected void generate(){
        //terrain(tiles, Blocks.sporePine, 70f, 1.4f, 1f);

        //int rand = 40;
        //int border = 25;
        //int spawnX = Mathf.clamp(30 + Mathf.range(rand), border, width - border), spawnY = Mathf.clamp(30 + Mathf.range(rand), border, height - border);
        //int endX = Mathf.clamp(width - 30 + Mathf.range(rand), border, width - border), endY = Mathf.clamp(height - 30 + Mathf.range(rand), border, height - border);

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
            float rrad = Math.min(Mathf.random(9f, maxrad), 40f);
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

        //
        //brush(tiles, pathfind(tiles, spawnX, spawnY, endX, endY, tile -> (tile.solid() ? 4f : 0f) + (float)sim.octaveNoise2D(1, 1, 1f / 90f, tile.x+999, tile.y) * 70, manhattan), 5);

        //
        //erase(tiles, spawnX, spawnY, 20);
        distort(20f, 6f);

        Point3 spawn = array.random();
        inverseFloodFill(tiles.getn(spawn.x, spawn.y));

        ores(ores);

        for(Point3 other : array){
            if(other != spawn){
               // tiles.getn(other.x, other.y).setOverlay(Blocks.spawn);
            }
        }

        //noise(tiles, Blocks.darksandTaintedWater, Blocks.duneRocks, 4, 0.7f, 120f, 0.64f);
        //scatter(tiles, Blocks.sporePine, Blocks.whiteTreeDead, 1f);

        //tiles.getn(endX, endY).setOverlay(Blocks.spawn);
        schematics.placeLoadout(Loadouts.advancedShard, spawn.x, spawn.y);
    }
}
