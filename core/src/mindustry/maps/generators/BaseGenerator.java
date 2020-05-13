package mindustry.maps.generators;

import arc.struct.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.type.*;
import mindustry.world.*;

public class BaseGenerator{

    public void generate(Tiles tiles, Array<Tile> cores, Tile spawn, Team team, Sector sector){

        for(Tile tile : cores){
            tile.clearOverlay();
            tile.setBlock(Blocks.coreShard, team);
        }


    }
}
