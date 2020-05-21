package mindustry.maps;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

public class SectorDamage{
    //direct damage is for testing only
    private static final boolean direct = false;

    //TODO amount of damage could be related to wave spacing
    public static void apply(float turns){
        Tiles tiles = world.tiles;

        Queue<Tile> frontier = new Queue<>();
        float[][] values = new float[tiles.width][tiles.height];
        float damage = turns*50;

        //phase one: find all spawnpoints
        for(Tile tile : tiles){
            if((tile.block() instanceof CoreBlock && tile.team() == state.rules.waveTeam) || tile.overlay() == Blocks.spawn){
                frontier.add(tile);
                values[tile.x][tile.y] = damage;
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
                    if(other.entity != null && other.team() != state.rules.waveTeam){
                        resultDamage -= other.entity.health();

                        if(direct){
                            other.entity.damage(currDamage);
                        }else{ //indirect damage happens at game load time
                            other.entity.health(other.entity.health() - currDamage);

                            //remove the block when destroyed
                            if(other.entity.health() < 0){
                                //rubble currently disabled
                                //if(!other.floor().solid && !other.floor().isLiquid && Mathf.chance(0.4)){
                                //    Effects.rubble(other.entity.x(), other.entity.y(), other.block().size);
                                //}

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
