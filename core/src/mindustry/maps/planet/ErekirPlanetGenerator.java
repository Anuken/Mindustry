package mindustry.maps.planet;

import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.maps.generators.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class ErekirPlanetGenerator extends PlanetGenerator{
    static final int seed = 2;

    public float scl = 2f;
    public float heightScl = 0.9f, octaves = 8, persistence = 0.7f, heightPow = 3f, heightMult = 1.6f;

    Block[][] arr = {
    {Blocks.regolith, Blocks.regolith, Blocks.yellowStone, Blocks.rhyolite, Blocks.basalt}
    };

    @Override
    public void generateSector(Sector sector){
        //no bases right now
    }

    @Override
    public float getHeight(Vec3 position){
        return Mathf.pow(rawHeight(position), heightPow) * heightMult;
    }

    @Override
    public Color getColor(Vec3 position){
        Block block = getBlock(position);
        return Tmp.c1.set(block.mapColor).a(1f - block.albedo);
    }

    @Override
    public float getSizeScl(){
        return 2000;
    }

    float rawHeight(Vec3 position){
        return Simplex.noise3d(seed, octaves, persistence, 1f/heightScl, 10f + position.x, 10f + position.y, 10f + position.z);
    }

    float rawTemp(Vec3 position){
        return position.dst(0, 0, 1)*2.2f - Simplex.noise3d(seed, 8, 0.54f, 1.4f, 10f + position.x, 10f + position.y, 10f + position.z) * 2.9f;
    }

    Block getBlock(Vec3 position){
        float ice = rawTemp(position);

        float height = rawHeight(position);
        Tmp.v31.set(position);
        position = Tmp.v33.set(position).scl(scl);
        float temp = Simplex.noise3d(seed, 8, 0.6, 1f/2f, 10f + position.x, 10f + position.y + 99f, 10f + position.z);
        height *= 1.2f;
        height = Mathf.clamp(height);

        Block result = arr[Mathf.clamp((int)(temp * arr.length), 0, arr[0].length - 1)][Mathf.clamp((int)(height * arr[0].length), 0, arr[0].length - 1)];

        if(ice < 0.6){
            if(result == Blocks.rhyolite || result == Blocks.yellowStone || result == Blocks.regolith){
                return Blocks.redIce;
            }
        }

        return result;
    }

    @Override
    public void genTile(Vec3 position, TileGen tile){
        tile.floor = getBlock(position);

        if(tile.floor == Blocks.rhyolite && rand.chance(0.01)){
            tile.floor = Blocks.rhyoliteCrater;
        }

        tile.block = tile.floor.asFloor().wall;

        if(Ridged.noise3d(1, position.x, position.y, position.z, 2, 25) > 0.2){
            tile.block = Blocks.air;
        }

        if(Ridged.noise3d(2, position.x, position.y + 4f, position.z, 3, 7f) > 0.7){
            tile.floor = Blocks.carbonStone;
        }
    }

    @Override
    protected void generate(){
        float temp = rawTemp(sector.tile.v);

        if(temp > 0.7){

            pass((x, y) -> {
                if(floor != Blocks.redIce){
                    float noise = noise(x + 782, y, 7, 0.8f, 280f, 1f);
                    if(noise > 0.62f){
                        if(noise > 0.7f){
                            floor = Blocks.slag;
                        }else{
                            floor = Blocks.yellowStone;
                        }
                        ore = Blocks.air;
                    }
                }
            });
        }

        cells(4);

        float length = width/3f;
        Vec2 trns = Tmp.v1.trns(rand.random(360f), length);
        int
        spawnX = (int)(trns.x + width/2f), spawnY = (int)(trns.y + height/2f),
        endX = (int)(-trns.x + width/2f), endY = (int)(-trns.y + height/2f);
        float maxd = Mathf.dst(width/2f, height/2f);

        erase(spawnX, spawnY, 15);
        brush(pathfind(spawnX, spawnY, endX, endY, tile -> (tile.solid() ? 300f : 0f) + maxd - tile.dst(width/2f, height/2f)/10f, Astar.manhattan), 7);

        distort(10f, 12f);
        distort(5f, 7f);

        pass((x, y) -> {
            float max = 0;
            for(Point2 p : Geometry.d8){
                max = Math.max(max, world.getDarkness(x + p.x, y + p.y));
            }
            if(max > 0){
                block = floor.asFloor().wall;
                if(block == Blocks.air) block = Blocks.yellowStoneWall;
            }

        });

        inverseFloodFill(tiles.getn(spawnX, spawnY));

        tiles.getn(endX, endY).setOverlay(Blocks.spawn);

        tech(Blocks.darkPanel3, Blocks.darkPanel5, Blocks.darkMetal);

        //ores
        pass((x, y) -> {

            if(block != Blocks.air){
                boolean empty = false;
                for(Point2 p : Geometry.d8){
                    Tile other = tiles.get(x + p.x, y + p.y);
                    if(other != null && other.block() == Blocks.air){
                        empty = true;
                        break;
                    }
                }

                if(empty && noise(x + 78, y, 4, 0.7f, 35f, 1f) > 0.67f && block == Blocks.carbonWall){
                    block = Blocks.graphiticWall;
                }else if(empty && noise(x + 782, y, 4, 0.8f, 40f, 1f) > 0.7f && block != Blocks.carbonWall){
                    ore = Blocks.wallOreBeryl;
                }
            }
        });

        Vars.state.rules.environment = Env.scorching | Env.terrestrial | Env.groundWater;
        Schematics.placeLaunchLoadout(spawnX, spawnY);
    }
}
