package mindustry.server.nydus;

import arc.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class JunctionYeeter implements ApplicationListener{

    private Interval timer = new Interval();

    @Override
    public void update(){
        if(!state.is(State.playing)) return;
        if(!timer.get(60 * 5)) return;

        state.teams.getActive().each(teamData -> {
            indexer.getAllied(teamData.team, BlockFlag.junction).each(tile -> {
                if(tile.block != Blocks.junction) return;

                if(air(tile, Compass.north) && air(tile, Compass.south)){
                    if(tile.getNearby(Compass.east).front() == tile) Core.app.post(() -> tile.constructNet(tile.getNearby(Compass.east).block, tile.getNearby(Compass.east).getTeam(), tile.getNearby(Compass.east).rotation));
                    if(tile.getNearby(Compass.west).front() == tile) Core.app.post(() -> tile.constructNet(tile.getNearby(Compass.west).block, tile.getNearby(Compass.west).getTeam(), tile.getNearby(Compass.west).rotation));
                }

                if(air(tile, Compass.east) && air(tile, Compass.west)){
                    if(tile.getNearby(Compass.north).front() == tile) Core.app.post(() -> tile.constructNet(tile.getNearby(Compass.north).block, tile.getNearby(Compass.north).getTeam(), tile.getNearby(Compass.north).rotation));
                    if(tile.getNearby(Compass.south).front() == tile) Core.app.post(() -> tile.constructNet(tile.getNearby(Compass.south).block, tile.getNearby(Compass.south).getTeam(), tile.getNearby(Compass.south).rotation));
                }
            });
        });
    }

    private boolean air(Tile tile, int compass){
        Tile next = tile;
        while(true){
            next = next.getNearby(compass);
            if(next == null) return false;
            if(next.block == Blocks.air) return true;
            if(next.block == Blocks.junction) continue;
            return false;
        }
    }
}
