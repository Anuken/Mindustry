package io.anuke.mindustry.ai;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.Queue;
import com.badlogic.gdx.utils.async.AsyncExecutor;
import io.anuke.mindustry.game.EventType.TileChangeEvent;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.TeamInfo.TeamData;
import io.anuke.mindustry.world.BlockFlag;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Log;

import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.world;

public class Pathfinder {
    private static final float unitBlockCost = 4f;

    private AsyncExecutor executor = new AsyncExecutor(8);
    private float[][][] weights;
    private IntArray blocked = new IntArray();

    public Pathfinder(){
        Events.on(WorldLoadEvent.class, this::clear);

        Events.on(TileChangeEvent.class, tile -> {

        });
    }

    public Tile getTargetTile(Team team, Tile tile){
        float[][] values = weights[team.ordinal()];

        if(values == null) return tile;

        float value = values[tile.x][tile.y];

        Tile target = null;
        float tl = 0f;
        for(GridPoint2 point : Geometry.d8) {
            int dx = tile.x + point.x, dy = tile.y + point.y;

            Tile other = world.tile(dx, dy);
            if(other == null) continue;

            if(values[dx][dy] < value && (target == null || values[dx][dy] < tl) &&
                    (other.getWallID() == 0 || state.teams.areEnemies(team, other.getTeam()))){
                target = other;
                tl = values[dx][dy];
            }
        }

        if(target == null || tl == Float.MAX_VALUE) return tile;


        return target;
    }

    public float getDebugValue(int x, int y){
        return weights[Team.red.ordinal()][x][y];
    }

    private boolean passable(Tile tile){
        return (tile.getWallID() == 0 && !(tile.floor().liquid && (tile.floor().damageTaken > 0 || tile.floor().drownTime > 0))) || tile.breakable();
    }

    private void clear(){
        Timers.markNs();

        weights = new float[Team.values().length][0][0];
        blocked.clear();

        for(TeamData data : state.teams.getTeams()){
            float[][] values = new float[world.width()][world.height()];
            weights[data.team.ordinal()] = values;

            Queue<Tile> frontier = new Queue<>();
            frontier.ensureCapacity(world.width() * world.height() / 2);

            for (int x = 0; x < world.width(); x++) {
                for (int y = 0; y < world.height(); y++) {
                    Tile tile = world.tile(x, y);
                    float min = Float.MAX_VALUE;

                    if (tile.block().flags != null && state.teams.areEnemies(tile.getTeam(), data.team)) {
                        for (BlockFlag flag : tile.block().flags) {
                            min = Math.min(flag.cost, min);
                        }

                        frontier.addFirst(tile);
                    }

                    values[x][y] = min;
                }
            }

            while (frontier.size > 0) {
                Tile tile = frontier.removeLast();
                float cost = values[tile.x][tile.y];

                if (cost < Float.MAX_VALUE) {
                    for (GridPoint2 point : Geometry.d4) {

                        int dx = tile.x + point.x, dy = tile.y + point.y;
                        Tile other = world.tile(dx, dy);

                        if (other != null && values[dx][dy] > cost + 1 && passable(other)) {

                            frontier.addFirst(world.tile(dx, dy));
                            values[dx][dy] = cost + other.cost;
                        }
                    }
                }
            }
        }

        Log.info("Elapsed calculation time: {0}", Timers.elapsedNs());
    }
}
