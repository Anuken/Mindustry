package io.anuke.mindustry.ai;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.ObjectSet.ObjectSetIterator;
import com.badlogic.gdx.utils.Queue;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.game.EventType.TileChangeEvent;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.TeamInfo.TeamData;
import io.anuke.mindustry.world.meta.BlockFlag;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Log;

import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.world;

public class Pathfinder {
    private long maxUpdate = TimeUtils.millisToNanos(4);
    private PathData[] paths;
    private IntArray blocked = new IntArray();

    public Pathfinder(){
        Events.on(WorldLoadEvent.class, this::clear);
        Events.on(TileChangeEvent.class, tile -> {

            for(TeamData data : state.teams.getTeams()){
                if(data.team != tile.getTeam() && paths[data.team.ordinal()].weights[tile.x][tile.y] >= Float.MAX_VALUE){
                    update(tile, data.team);
                }
            }

            update(tile, tile.getTeam());
        });
    }

    public void update(){
        ObjectSetIterator<TeamData> iterator = new ObjectSetIterator<>(state.teams.getTeams());

        for(TeamData team : iterator){
            updateFrontier(team.team, maxUpdate);
        }
    }

    public Tile getTargetTile(Team team, Tile tile){
        float[][] values = paths[team.ordinal()].weights;

        if(values == null) return tile;

        float value = values[tile.x][tile.y];

        Tile target = null;
        float tl = 0f;
        for(GridPoint2 point : Geometry.d8) {
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

    public float getDebugValue(int x, int y){
        return paths[Team.red.ordinal()].weights[x][y];
    }

    private boolean passable(Tile tile, Team team){
        return (tile.getWallID() == 0 && !(tile.floor().liquid && (tile.floor().damageTaken > 0 || tile.floor().drownTime > 0)))
                || (tile.breakable() && (tile.getTeam() != team)) || !tile.solid();
    }

    private void update(Tile tile, Team team){
        if(paths[team.ordinal()] != null) {
            PathData path = paths[team.ordinal()];

            if(!passable(tile, team)){
                path.weights[tile.x][tile.y] = Float.MAX_VALUE;
            }

            path.search ++;

            if(path.lastSearchTime + 1000/60*3 > TimeUtils.millis()){
                path.frontier.clear();
            }

            path.lastSearchTime = TimeUtils.millis();

            ObjectSet<Tile> set = world.indexer().getEnemy(team, BlockFlag.target);
            for(Tile other : set){
                path.weights[other.x][other.y] = 0;
                path.searches[other.x][other.y] = path.search;
                path.frontier.addFirst(other);
            }
        }
    }

    private void createFor(Team team){
        PathData path = new PathData();
        path.search ++;
        path.frontier.ensureCapacity(world.width() * world.height() / 2);

        paths[team.ordinal()] = path;

        for (int x = 0; x < world.width(); x++) {
            for (int y = 0; y < world.height(); y++) {
                Tile tile = world.tile(x, y);

                if (tile.block().flags != null && state.teams.areEnemies(tile.getTeam(), team)
                        && tile.block().flags.contains(BlockFlag.target)) {
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

        while (path.frontier.size > 0 && (nsToRun < 0 || TimeUtils.timeSinceNanos(start) <= nsToRun)) {
            Tile tile = path.frontier.removeLast();
            float cost = path.weights[tile.x][tile.y];

            if (cost < Float.MAX_VALUE) {
                for (GridPoint2 point : Geometry.d4) {

                    int dx = tile.x + point.x, dy = tile.y + point.y;
                    Tile other = world.tile(dx, dy);

                    if (other != null && (path.weights[dx][dy] > cost + 1 || path.searches[dx][dy] < path.search)
                            && passable(other, team)){
                        path.frontier.addFirst(world.tile(dx, dy));
                        path.weights[dx][dy] = cost + other.cost/2f;
                        path.searches[dx][dy] = path.search;
                    }
                }
            }
        }
    }

    private void clear(){
        Timers.mark();

        paths = new PathData[Team.values().length];
        blocked.clear();

        for(TeamData data : state.teams.getTeams()){
            PathData path = new PathData();
            paths[data.team.ordinal()] = path;

            createFor(data.team);
        }

        Log.info("Elapsed calculation time: {0}", Timers.elapsed());
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
