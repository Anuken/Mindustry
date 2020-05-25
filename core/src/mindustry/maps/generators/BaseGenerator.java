package mindustry.maps.generators;

import arc.struct.*;
import mindustry.game.*;
import mindustry.type.*;
import mindustry.world.*;

public class BaseGenerator{

    public void generate(Tiles tiles, Array<Tile> cores, Tile spawn, Team team, Sector sector){

        /*
        GridBits used = new GridBits(tiles.width, tiles.height);
        Queue<Tile> frontier = new Queue<>();
        for(Tile tile : cores){
            frontier.add(tile);
        }

        int count = 2000;
        int total = 0;

        while(total++ < count){
            Tile tile = frontier.removeFirst();
            for(int i = 0; i < 4; i++){
                int cx = tile.x + Geometry.d4x[i], cy = tile.y + Geometry.d4y[i];
                if(tiles.in(cx, cy) && !used.get(cx, cy)){
                    Tile other = tiles.getn(cx, cy);

                    if(!other.solid()){
                        frontier.addLast(other);
                    }
                    used.set(cx, cy);
                }
            }
        }

        for(Tile tile : frontier){
            tile.setBlock(Blocks.copperWall, team);
        }

        for(Tile tile : cores){
            tile.clearOverlay();
            tile.setBlock(Blocks.coreShard, team);

        }*/


    }
}
