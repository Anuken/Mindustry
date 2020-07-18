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
    //direct damage is for testing only
    private static final boolean direct = false, rubble = true;

    public static void apply(float fraction){
        Tiles tiles = world.tiles;

        Queue<Tile> frontier = new Queue<>();
        float[][] values = new float[tiles.width][tiles.height];
        float damage = fraction*80; //arbitrary damage value

        //phase one: find all spawnpoints
        for(Tile tile : tiles){
            if((tile.block() instanceof CoreBlock && tile.team() == state.rules.waveTeam) || tile.overlay() == Blocks.spawn){
                frontier.add(tile);
                values[tile.x][tile.y] = damage;
            }
        }

        Building core = state.rules.defaultTeam.core();
        if(core != null && !frontier.isEmpty()){
            for(Tile spawner : frontier){
                //find path from spawn to core
                Seq<Tile> path = Astar.pathfind(spawner, core.tile, t -> t.cost, t -> !(t.block().isStatic() && t.solid()));
                int amount = (int)(path.size * fraction);
                for(int i = 0; i < amount; i++){
                    Tile t = path.get(i);
                    Geometry.circle(t.x, t.y, tiles.width, tiles.height, 5, (cx, cy) -> {
                        Tile other = tiles.getn(cx, cy);
                        //just remove all the buildings in the way - as long as they're not cores!
                        if(other.build != null && other.team() == state.rules.defaultTeam && !(other.block() instanceof CoreBlock)){
                            if(rubble && !other.floor().solid && !other.floor().isLiquid && Mathf.chance(0.4)){
                                Effects.rubble(other.build.x, other.build.y, other.block().size);
                            }

                            other.remove();
                        }
                    });
                }
            }
        }

        float falloff = (damage) / (Math.max(tiles.width, tiles.height) * Mathf.sqrt2);
        int peak = 0;

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

                    //damage the tile if it's not friendly
                    if(other.build != null && other.team() != state.rules.waveTeam){
                        resultDamage -= other.build.health();

                        if(direct){
                            other.build.damage(currDamage);
                        }else{ //indirect damage happens at game load time
                            other.build.health -= currDamage;
                            //don't kill the core!
                            if(other.block() instanceof CoreBlock) other.build.health = Math.max(other.build.health, 1f);

                            //remove the block when destroyed
                            if(other.build.health < 0){
                                //rubble
                                if(rubble && !other.floor().solid && !other.floor().isLiquid && Mathf.chance(0.4)){
                                    Effects.rubble(other.build.x, other.build.y, other.block().size);
                                }

                                other.remove();
                            }
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
