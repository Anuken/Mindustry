package mindustry.maps;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.ai.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

public class SectorDamage{
    private static final boolean rubble = true;

    public static void apply(float fraction){
        Tiles tiles = world.tiles;

        Queue<Tile> frontier = new Queue<>();
        float[][] values = new float[tiles.width][tiles.height];

        //phase one: find all spawnpoints
        for(Tile tile : tiles){
            if((tile.block() instanceof CoreBlock && tile.team() == state.rules.waveTeam) || tile.overlay() == Blocks.spawn){
                frontier.add(tile);
                values[tile.x][tile.y] = fraction * 24;
            }
        }

        Building core = state.rules.defaultTeam.core();
        if(core != null && !frontier.isEmpty()){
            for(Tile spawner : frontier){
                //find path from spawn to core
                Seq<Tile> path = Astar.pathfind(spawner, core.tile, SectorDamage::cost, t -> !(t.block().isStatic() && t.solid()));
                Seq<Building> removal = new Seq<>();

                int radius = 3;

                //only penetrate a certain % by health, not by distance
                float totalHealth = fraction >= 1f ? 1f : path.sumf(t -> {
                    float s = 0;
                    for(int dx = -radius; dx <= radius; dx++){
                        for(int dy = -radius; dy <= radius; dy++){
                            int wx = dx + t.x, wy = dy + t.y;
                            if(wx >= 0 && wy >= 0 && wx < world.width() && wy < world.height() && Mathf.within(dx, dy, radius)){
                                Tile other = world.rawTile(wx, wy);
                                if(!(other.block() instanceof CoreBlock)){
                                    s += other.team() == state.rules.defaultTeam ? other.build.health / (other.block().size * other.block().size) : 0f;
                                }
                            }
                        }
                    }
                    return s;
                });
                float targetHealth = totalHealth * fraction;
                float healthCount = 0;

                out:
                for(int i = 0; i < path.size && (healthCount < targetHealth || fraction >= 1f); i++){
                    Tile t = path.get(i);

                    for(int dx = -radius; dx <= radius; dx++){
                        for(int dy = -radius; dy <= radius; dy++){
                            int wx = dx + t.x, wy = dy + t.y;
                            if(wx >= 0 && wy >= 0 && wx < world.width() && wy < world.height() && Mathf.within(dx, dy, radius)){
                                Tile other = world.rawTile(wx, wy);

                                //just remove all the buildings in the way - as long as they're not cores
                                if(other.build != null && other.team() == state.rules.defaultTeam && !(other.block() instanceof CoreBlock)){
                                    if(rubble && !other.floor().solid && !other.floor().isLiquid && Mathf.chance(0.4)){
                                        Effect.rubble(other.build.x, other.build.y, other.block().size);
                                    }

                                    //since the whole block is removed, count the whole health
                                    healthCount += other.build.health;

                                    removal.add(other.build);

                                    if(healthCount >= targetHealth && fraction < 0.999f){
                                        break out;
                                    }
                                }
                            }
                        }
                    }
                }

                for(Building r : removal){
                    if(r.tile.build == r){
                        r.addPlan(false);
                        r.tile.remove();
                    }
                }
            }
        }

        //kill every core if damage is maximum
        if(fraction >= 1){
            for(Building c : state.rules.defaultTeam.cores().copy()){
                c.tile.remove();
            }
        }

        float falloff = (fraction) / (Math.max(tiles.width, tiles.height) * Mathf.sqrt2);
        int peak = 0;

        if(fraction > 0.15f){
            //phase two: propagate the damage
            while(!frontier.isEmpty()){
                peak = Math.max(peak, frontier.size);
                Tile tile = frontier.removeFirst();
                float currDamage = values[tile.x][tile.y] - falloff;

                for(int i = 0; i < 4; i++){
                    int cx = tile.x + Geometry.d4x[i], cy = tile.y + Geometry.d4y[i];

                    //propagate to new tiles
                    if(tiles.in(cx, cy) && values[cx][cy] < currDamage){
                        Tile other = tiles.getn(cx, cy);
                        float resultDamage = currDamage;

                        //damage the tile if it's the player team (derelict blocks get ignored)
                        if(other.build != null && other.team() == state.rules.defaultTeam){
                            resultDamage -= other.build.health();

                            other.build.health -= currDamage;
                            //don't kill the core!
                            if(other.block() instanceof CoreBlock) other.build.health = Math.max(other.build.health, 1f);

                            //remove the block when destroyed
                            if(other.build.health < 0){
                                //rubble
                                if(rubble && !other.floor().solid && !other.floor().isLiquid && Mathf.chance(0.4)){
                                    Effect.rubble(other.build.x, other.build.y, other.block().size);
                                }

                                other.build.addPlan(false);
                                other.remove();
                            }else{
                                indexer.notifyHealthChanged(other.build);
                            }

                        }else if(other.solid() && !other.synthetic()){ //skip damage propagation through solid blocks
                            continue;
                        }

                        if(resultDamage > 0 && values[cx][cy] < resultDamage){
                            frontier.addLast(other);
                            values[cx][cy] = resultDamage;
                        }
                    }
                }
            }
        }

    }

    static float cost(Tile tile){
        return 1f +
            (tile.block().isStatic() && tile.solid() ? 200f : 0f) +
            (tile.build != null ? tile.build.health / (tile.build.block.size * tile.build.block.size) / 20f : 0f) +
            (tile.floor().isLiquid ? 10f : 0f);
    }
}
