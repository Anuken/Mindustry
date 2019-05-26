package io.anuke.mindustry.ai;

import io.anuke.arc.Events;
import io.anuke.arc.collection.IntArray;
import io.anuke.arc.collection.IntQueue;
import io.anuke.arc.math.geom.Geometry;
import io.anuke.arc.math.geom.Point2;
import io.anuke.arc.util.*;
import io.anuke.mindustry.game.EventType.TileChangeEvent;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.Teams.TeamData;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.world.Pos;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockFlag;

import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.world;

public class Pathfinder{
    private static final long maxUpdate = Time.millisToNanos(4);
    private PathData[] paths;
    private IntArray blocked = new IntArray();

    public Pathfinder(){
        Events.on(WorldLoadEvent.class, event -> clear());
        Events.on(TileChangeEvent.class, event -> {
            if(Net.client()) return;

            for(Team team : Team.all){
                TeamData data = state.teams.get(team);
                if(state.teams.isActive(team) && data.team != event.tile.getTeam()){
                    update(event.tile, data.team);
                }
            }

            update(event.tile, event.tile.getTeam());
        });
    }

    public void update(){
        if(Net.client() || paths == null) return;

        for(Team team : Team.all){
            if(state.teams.isActive(team)){
                updateFrontier(team, maxUpdate);
            }
        }
    }

    public Tile getTargetTile(Team team, Tile tile){
        float[][] values = paths[team.ordinal()].weights;

        if(values == null || tile == null) return tile;

        float value = values[tile.x][tile.y];

        Tile target = null;
        float tl = 0f;
        for(Point2 point : Geometry.d8){
            int dx = tile.x + point.x, dy = tile.y + point.y;

            Tile other = world.tile(dx, dy);
            if(other == null) continue;

            if(values[dx][dy] < value && (target == null || values[dx][dy] < tl) &&
            !other.solid() && other.floor().drownTime <= 0 &&
            !(point.x != 0 && point.y != 0 && (world.solid(tile.x + point.x, tile.y) || world.solid(tile.x, tile.y + point.y)))){ //diagonal corner trap
                target = other;
                tl = values[dx][dy];
            }
        }

        if(target == null || tl == Float.MAX_VALUE) return tile;

        return target;
    }

    public float getValueforTeam(Team team, int x, int y){
        return paths == null || paths[team.ordinal()].weights == null || team.ordinal() >= paths.length ? 0 : Structs.inBounds(x, y, paths[team.ordinal()].weights) ? paths[team.ordinal()].weights[x][y] : 0;
    }

    private boolean passable(Tile tile, Team team){
        return (!tile.solid()) || (tile.breakable() && (tile.getTeam() != team));
    }

    /**
     * Clears the frontier, increments the search and sets up all flow sources.
     * This only occurs for active teams.
     */
    private void update(Tile tile, Team team){
        //make sure team exists
        if(paths != null && paths[team.ordinal()] != null && paths[team.ordinal()].weights != null){
            PathData path = paths[team.ordinal()];

            //impassable tiles have a weight of float.max
            if(!passable(tile, team)){
                path.weights[tile.x][tile.y] = Float.MAX_VALUE;
            }

            //increment search, clear frontier
            path.search++;
            path.frontier.clear();
            path.lastSearchTime = Time.millis();

            //add all targets to the frontier
            for(Tile other : world.indexer.getEnemy(team, BlockFlag.target)){
                path.weights[other.x][other.y] = 0;
                path.searches[other.x][other.y] = (short)path.search;
                path.frontier.addFirst(other.pos());
            }
        }
    }

    private void createFor(Team team){
        PathData path = new PathData();
        path.weights = new float[world.width()][world.height()];
        path.searches = new short[world.width()][world.height()];
        path.search++;
        path.frontier.ensureCapacity((world.width() + world.height()) * 3);

        paths[team.ordinal()] = path;

        for(int x = 0; x < world.width(); x++){
            for(int y = 0; y < world.height(); y++){
                Tile tile = world.tile(x, y);

                if(state.teams.areEnemies(tile.getTeam(), team)
                && tile.block().flags.contains(BlockFlag.target)){
                    path.frontier.addFirst(tile.pos());
                    path.weights[x][y] = 0;
                    path.searches[x][y] = (short)path.search;
                }else{
                    path.weights[x][y] = Float.MAX_VALUE;
                }
            }
        }

        updateFrontier(team, -1);
    }

    private void updateFrontier(Team team, long nsToRun){
        PathData path = paths[team.ordinal()];

        long start = Time.nanos();

        while(path.frontier.size > 0 && (nsToRun < 0 || Time.timeSinceNanos(start) <= nsToRun)){
            Tile tile = world.tile(path.frontier.removeLast());
            if(tile == null || path.weights == null) return; //something went horribly wrong, bail
            float cost = path.weights[tile.x][tile.y];

            //pathfinding overflowed for some reason, time to bail. the next block update will handle this, hopefully
            if(path.frontier.size >= world.width() * world.height()){
                path.frontier.clear();
                return;
            }

            if(cost < Float.MAX_VALUE){
                for(Point2 point : Geometry.d4){

                    int dx = tile.x + point.x, dy = tile.y + point.y;
                    Tile other = world.tile(dx, dy);

                    if(other != null && (path.weights[dx][dy] > cost + other.cost || path.searches[dx][dy] < path.search)
                    && passable(other, team)){
                        if(other.cost < 0) throw new IllegalArgumentException("Tile cost cannot be negative! " + other);
                        path.frontier.addFirst(Pos.get(dx, dy));
                        path.weights[dx][dy] = cost + other.cost;
                        path.searches[dx][dy] = (short)path.search;
                    }
                }
            }
        }
    }

    private void clear(){
        Time.mark();

        paths = new PathData[Team.all.length];
        blocked.clear();

        for(Team team : Team.all){
            PathData path = new PathData();
            paths[team.ordinal()] = path;

            if(state.teams.isActive(team)){
                createFor(team);
            }
        }
    }

    class PathData{
        float[][] weights;
        short[][] searches;
        int search = 0;
        long lastSearchTime;
        IntQueue frontier = new IntQueue();
    }
}
