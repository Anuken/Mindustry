package mindustry.maps.planet;

import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.ai.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.maps.generators.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class ErekirPlanetGenerator extends PlanetGenerator{
    static final int seed = 2;

    public float scl = 2f;
    public float heightScl = 0.9f, octaves = 8, persistence = 0.7f, heightPow = 3f, heightMult = 1.6f;

    Block[][] arr = {
    //{Blocks.regolith, Blocks.regolith, Blocks.yellowStone, Blocks.rhyolite, Blocks.basalt}
    {Blocks.regolith, Blocks.regolith, Blocks.yellowStone, Blocks.crystallineStone, Blocks.basalt}
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
        return 2000 * 1.06f;
    }

    @Override
    public Schematic getDefaultLoadout(){
        return Loadouts.basicBastion;
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
                //TODO bio(?) luminescent stuff
                return Blocks.ferricStone; //TODO perhaps something else
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

        if(Ridged.noise3d(1, position.x, position.y, position.z, 2, 14) > 0.14){
            tile.block = Blocks.air;
        }

        if(Ridged.noise3d(2, position.x, position.y + 4f, position.z, 3, 6f) > 0.65){
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
                        if(noise > 0.635f){
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
        brush(pathfind(spawnX, spawnY, endX, endY, tile -> (tile.solid() ? 300f : 0f) + maxd - tile.dst(width/2f, height/2f)/10f, Astar.manhattan), 9);
        erase(endX, endY, 15);

        //arkycite
        //TODO arkycite biome
        pass((x, y) -> {
            if(nearWall(x, y)) return;

            float noise = noise(x + 300, y - x*1.6f + 100, 4, 0.8f, 120f, 1f);

            if(noise > 0.71f){
                floor = Blocks.arkyciteFloor;
            }
        });

        median(2, 0.6, Blocks.arkyciteFloor);

        blend(Blocks.arkyciteFloor, Blocks.arkyicStone, 4);

        distort(10f, 12f);
        distort(5f, 7f);

        //does arkycite need smoothing?
        median(2, 0.6, Blocks.arkyciteFloor);

        //smooth out slag to prevent random 1-tile patches
        median(3, 0.6, Blocks.slag);

        pass((x, y) -> {
            if((floor == Blocks.arkyciteFloor || floor == Blocks.arkyicStone) && block.isStatic()){
                block = Blocks.arkyicWall;
            }

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

        //make sure enemies have room
        erase(endX, endY, 6);

        tiles.getn(endX, endY).setOverlay(Blocks.spawn);

        //TODO tech is lazy and boring
        //tech(Blocks.darkPanel3, Blocks.darkPanel5, Blocks.darkMetal);

        //ores
        pass((x, y) -> {

            if(block != Blocks.air){
                if(nearAir(x, y)){
                    if(block == Blocks.carbonWall && noise(x + 78, y, 4, 0.7f, 33f, 1f) > 0.52f){
                        block = Blocks.graphiticWall;
                    }else if(block != Blocks.carbonWall && noise(x + 782, y, 4, 0.8f, 38f, 1f) > 0.665f){
                        ore = Blocks.wallOreBeryl;
                    }
                    //TODO generate tungsten, or not?
                    //else if(block == Blocks.yellowStoneWall && noise(x, y + 942, 4, 0.7f, 38f, 1f) > 0.71f){
                    //    ore = Blocks.wallOreTungsten;
                    //}
                }
            }else if(!nearWall(x, y)){

                if(noise(x + 150, y + x*2 + 100, 4, 0.8f, 55f, 1f) > 0.76f/* && floor == Blocks.yellowStone*/){
                    ore = Blocks.oreTungsten;
                }

                //TODO design ore generation so it doesn't overlap
                if(noise(x + 999, y + 600 - x, 4, 0.63f, 50f, 1f) < 0.25f && floor == Blocks.crystallineStone){
                    ore = Blocks.oreCrystalThorium;
                }

            }

            if(noise(x + 999, y + 600 - x, 5, 0.8f, 50f, 1f) < 0.38f && floor == Blocks.crystallineStone){
                floor = Blocks.crystalFloor;
            }

            if(block == Blocks.air && (floor == Blocks.crystallineStone || floor == Blocks.crystalFloor) && rand.chance(0.09) && nearWall(x, y)
                && !near(x, y, 4, Blocks.crystalCluster) && !near(x, y, 4, Blocks.vibrantCrystalCluster)){
                block = floor == Blocks.crystalFloor ? Blocks.vibrantCrystalCluster : Blocks.crystalCluster;
            }
        });

        //vents
        outer:
        for(Tile tile : tiles){
            if(floor == Blocks.rhyolite && rand.chance(0.0011)){
                int radius = 2;
                for(int x = -radius; x <= radius; x++){
                    for(int y = -radius; y <= radius; y++){
                        Tile other = tiles.get(x + tile.x, y + tile.y);
                        if(other == null || other.floor() != Blocks.rhyolite || other.block().solid){
                            continue outer;
                        }
                    }
                }

                for(var pos : SteamVent.offsets){
                    Tile other = tiles.get(pos.x + tile.x + 1, pos.y + tile.y + 1);
                    other.setFloor(Blocks.steamVent.asFloor());
                }
            }
        }

        for(Tile tile : tiles){
            if(tile.overlay().needsSurface && !tile.floor().hasSurface()){
                tile.setOverlay(Blocks.air);
            }
        }

        decoration(0.017f);

        //not allowed
        state.rules.hiddenBuildItems.addAll(Items.copper, Items.titanium, Items.coal, Items.lead, Items.blastCompound, Items.pyratite, Items.sporePod, Items.metaglass, Items.plastanium);

        //it is very hot
        state.rules.attributes.set(Attribute.heat, 0.8f);
        state.rules.environment = Env.scorching | Env.terrestrial | Env.groundWater;
        Schematics.placeLaunchLoadout(spawnX, spawnY);

        //TODO this is only for testing
        state.rules.defaultTeam.items().add(Seq.with(ItemStack.with(Items.beryllium, 1000, Items.graphite, 1000)));

        //TODO proper waves
        state.rules.waves = true;
        state.rules.showSpawns = true;
        state.rules.waveTimer = true;
        state.rules.waveSpacing = 60f * 60f * 7.5f;
        state.rules.spawns = Seq.with(new SpawnGroup(){{
            type = UnitTypes.vanquish;
            spacing = 1;
            shieldScaling = 60;
            unitScaling = 2f;
            healthFraction = 0.1f;
        }});
    }
}
