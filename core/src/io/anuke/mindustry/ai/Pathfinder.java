package io.anuke.mindustry.ai;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.Queue;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.game.EventType.TileChangeEvent;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.Teams.TeamData;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockFlag;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Structs;

import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.world;

public class Pathfinder{
    private long maxUpdate = TimeUtils.millisToNanos(4);
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

    public void activateTeamPath(Team team){
        createFor(team);
    }

    public void update(){
        if(Net.client()) return;

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
        for(GridPoint2 point : Geometry.d8){
            int dx = tile.x + point.x, dy = tile.y + point.y;

            Tile other = world.tile(dx, dy);
            if(other == null) continue;

            if(values[dx][dy] < value && (target == null || values[dx][dy] < tl) &&
                    !other.solid() &&
                    !(point.x != 0 && point.y != 0 && (world.solid(tile.x + point.x, tile.y) || world.solid(tile.x, tile.y + point.y)))){ //diagonal corner trap
                target = other;
                tl = values[dx][dy];
            }
        }

        if(target == null || tl == Float.MAX_VALUE) return tile;

        return target;
    }

    public float getValueforTeam(Team team, int x, int y){
        return paths == null || team.ordinal() >= paths.length ? 0 : Structs.inBounds(x, y, paths[team.ordinal()].weights) ? paths[team.ordinal()].weights[x][y] : 0;
    }

    private boolean passable(Tile tile, Team team){
        return (!tile.solid()) || (tile.breakable() && (tile.target().getTeam() != team));
    }

    /**Clears the frontier, increments the search and sets up all flow sources.
     * This only occurs for active teams.*/
    private void update(Tile tile, Team team){
        //make sure team exists
        if(paths[team.ordinal()] != null){
            PathData path = paths[team.ordinal()];

            //impassable tiles have a weight of float.max
            if(!passable(tile, team)){
                path.weights[tile.x][tile.y] = Float.MAX_VALUE;
            }

            //increment search, clear frontier
            path.search++;
            path.frontier.clear();
            path.lastSearchTime = TimeUtils.millis();

            //add all targets to the frontier
            for(Tile other : world.indexer.getEnemy(team, BlockFlag.target)){
                path.weights[other.x][other.y] = 0;
                path.searches[other.x][other.y] = path.search;
                path.frontier.addFirst(other);
            }
        }
    }

    private void createFor(Team team){
        PathData path = new PathData();
        path.search++;
        path.frontier.ensureCapacity((world.width() + world.height()) * 3);

        paths[team.ordinal()] = path;

        for(int x = 0; x < world.width(); x++){
            for(int y = 0; y < world.height(); y++){
                Tile tile = world.tile(x, y);

                if(tile.block().flags != null && state.teams.areEnemies(tile.getTeam(), team)
                        && tile.block().flags.contains(BlockFlag.target)){
                    path.frontier.addFirst(tile);
                    path.weights[x][y] = 0;
                    path.searches[x][y] = path.search;
                }else{
                    path.weights[x][y] = Float.MAX_VALUE;
                }
            }
        }

        updateFrontier(team, -1);
    }

    private void updateFrontier(Team team, long nsToRun){
        PathData path = paths[team.ordinal()];

        long start = TimeUtils.nanoTime();

        while(path.frontier.size > 0 && (nsToRun < 0 || TimeUtils.timeSinceNanos(start) <= nsToRun)){
            Tile tile = path.frontier.removeLast();
            float cost = path.weights[tile.x][tile.y];

            if(cost < Float.MAX_VALUE){
                for(GridPoint2 point : Geometry.d4){

                    int dx = tile.x + point.x, dy = tile.y + point.y;
                    Tile other = world.tile(dx, dy);

                    if(other != null && (path.weights[dx][dy] > cost + other.cost || path.searches[dx][dy] < path.search)
                            && passable(other, team)){
                        path.frontier.addFirst(world.tile(dx, dy));
                        path.weights[dx][dy] = cost + other.cost;
                        path.searches[dx][dy] = path.search;
                    }
                }
            }
        }
    }

    private void clear(){
        Timers.mark();

        paths = new PathData[Team.all.length];
        blocked.clear();

        for(Team team : Team.all){
            PathData path = new PathData();
            paths[team.ordinal()] = path;

            if(state.teams.isActive(team)){
                createFor(team);
            }
        }

        world.spawner.checkAllQuadrants();
    }

    class PathData{
        float[][] weights;
        int[][] searches;
        int search = 0;
        long lastSearchTime;
        Queue<Tile> frontier = new Queue<>();

        PathData(){
            weights = new float[world.width()][world.height()];
            searches = new int[world.width()][world.height()];
        }
    }
}
