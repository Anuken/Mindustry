package mindustry.maps.generators;

import arc.math.geom.*;
import arc.struct.*;
import mindustry.ai.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.storage.*;

//makes terrible bases
public class BaseGenerator{
    int width, height;
    Cell[][] cells;
    Queue<Tile> frontier;
    Array<Tile> all;
    ObjectMap<Item, Array<Tile>> resources;
    Team team;

    public void generate(Tiles tiles, Array<Tile> cores, Tile spawn, Team team, Sector sector){
        if(true){
            SeedBaseGenerator gen = new SeedBaseGenerator();
            gen.generate(tiles, team, cores.first());

            return;
        }

        this.team = team;
        width = tiles.width;
        height = tiles.height;

        cells = new Cell[width][height];
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                cells[x][y] = new Cell();
            }
        }

        all = new Array<>();
        frontier = new Queue<>();
        for(Tile tile : cores){
            frontier.add(tile);
        }

        int count = 10000;
        int total = 0;

        //create bounds
        while(total++ < count){
            Tile tile = frontier.removeFirst();
            all.add(tile);
            for(int i = 0; i < 4; i++){
                int cx = tile.x + Geometry.d4x[i], cy = tile.y + Geometry.d4y[i];
                if(tiles.in(cx, cy) && !contained(cx, cy)){
                    Tile other = tiles.getn(cx, cy);

                    if(!other.solid()){
                        frontier.addLast(other);
                    }
                    cells[cx][cy].contained = true;
                }
            }
        }

        //walls, mostly for debugging
        for(Tile tile : frontier){
            tile.setBlock(Blocks.copperWall, team);
        }

        for(Tile tile : cores){
            tile.clearOverlay();
            tile.setBlock(Blocks.coreShard, team);
        }

        Block pump = Blocks.mechanicalPump;
        Drill drill = (Drill)Blocks.pneumaticDrill;
        resources = new ObjectMap<>();

        //assign resource collection points
        for(Tile tile : all){
            //place drills.
            if(tile.drop() != null && tile.drop().type == ItemType.material && tile.drop().hardness <= drill.tier && Build.validPlace(team, tile.x, tile.y, drill, 0)){
                tile.setBlock(drill, team);

                //mark item outputs
                tile.getLinkedTiles(t -> {
                    cell(t).item = tile.drop();
                    resources.get(tile.drop(), Array::new).add(t);
                });
            }

            //only water matters right now
            //pumps are only placed on edges of water, even if it's shallow
            if(tile.floor().liquidDrop == Liquids.water && Build.validPlace(team, tile.x, tile.y, pump, 0) && Build.contactsGround(tile.x, tile.y, pump)){
                tile.setBlock(pump, team);
            }
        }

        //clear path for cores
        for(Tile start : cores){
            Array<Tile> path = Astar.pathfind(start, spawn, (tile) -> tile.solid() ? 30f : 0f, tile -> !tile.block().isStatic() && !tile.floor().isDeep());

            for(Tile tile : path){
                tile.circle(2, (x, y) -> {
                    Tile t = tiles.getn(x, y);
                    if(t.team() == team && t.solid() && !(t.block() instanceof CoreBlock)){
                        t.setAir();
                    }
                    cell(t).taken = true;
                });
            }
        }

    }

    Item contactRes(Tile tile){
        for(int i = 0; i < 4; i++){
            if(tile.getNearby(i) == null) continue;
            Cell cell = cell(tile.getNearby(i));
            if(cell.item != null) return cell.item;
        }
        return null;
    }

    Cell cell(Tile tile){
        return cells[tile.x][tile.y];
    }

    boolean contained(int x, int y){
        return cells[x][y].contained;
    }

    static class Cell{
        boolean contained;
        boolean taken;
        Item item;
    }
}
