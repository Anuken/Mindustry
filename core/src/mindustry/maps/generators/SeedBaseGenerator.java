package mindustry.maps.generators;

import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.units.*;
import mindustry.world.consumers.*;

//makes terrible seeded bases
public class SeedBaseGenerator{
    private final static int coreDst = 200;

    private Team team;
    private Rand rand = new Rand();
    private Tiles tiles;

    public void generate(Tiles tiles, Team team, Tile core){
        this.team = team;
        this.tiles = tiles;

        float difficulty = 1f;

        core.setBlock(Blocks.coreShard, team);
        rand.nextBoolean();

        float difficultyScl = Mathf.clamp(difficulty / 20f + rand.range(0.25f), 0f, 0.9999f);
        float dscl2 = Mathf.clamp(0.5f + difficulty / 20f + rand.range(0.1f), 0f, 1.5f);

        Array<Block> turrets = find(b -> b instanceof ItemTurret);
        Array<Block> powerTurrets = find(b -> b instanceof PowerTurret);
        Array<Block> walls = find(b -> b instanceof Wall && !(b instanceof Door) && b.size == 1);
        Array<Block> drills = find(b -> b instanceof Drill && !b.consumes.has(ConsumeType.power));
        Array<Block> powerDrills = find(b -> b instanceof Drill && b.consumes.has(ConsumeType.power));

        Block wall = walls.get((int)(difficultyScl * walls.size));

        Turret powerTurret = (Turret)powerTurrets.get((int)(difficultyScl * powerTurrets.size));
        Turret bigTurret = (Turret)turrets.get(Mathf.clamp((int)((difficultyScl + 0.2f + rand.range(0.2f)) * turrets.size), 0, turrets.size-1));
        Turret turret1 = (Turret)turrets.get(Mathf.clamp((int)((difficultyScl + rand.range(0.2f)) * turrets.size), 0, turrets.size-1));
        Turret turret2 = (Turret)turrets.get(Mathf.clamp((int)((difficultyScl + rand.range(0.2f)) * turrets.size), 0, turrets.size-1));
        Drill drill = (Drill)drills.get((int)(difficultyScl * drills.size));
        Drill powerDrill = (Drill)powerDrills.get((int)(difficultyScl * powerDrills.size));
        float placeChance = difficultyScl*0.75f+0.25f;

        IntIntMap ammoPerType = new IntIntMap();
        for(Block turret : turrets){
            if(!(turret instanceof ItemTurret)) continue;
            ItemTurret t = (ItemTurret)turret;
            int size = t.ammoTypes.size;
            ammoPerType.put(t.id, Mathf.clamp((int)(size* difficultyScl) + rand.range(1), 0, size - 1));
        }

        Func3<Tile, Block, Boolf<Tile>, Boolean> checker = (current, block, pred) -> {
            for(Point2 point : Edges.getEdges(block.size)){
                Tile tile = tiles.get(current.x + point.x, current.y + point.y);
                if(tile != null){
                    if(tile.team() == team && pred.get(tile)){
                        return true;
                    }
                }
            }
            return false;
        };

        Func2<Block, Boolf<Tile>, Intc2> seeder = (block, pred) -> (x, y) -> {
            if(canPlace(x, y, block) && ((block instanceof Wall && block.size == 1) || rand.chance(placeChance)) && checker.get(tiles.get(x, y), block, pred)){
                setBlock(x, y, block);
            }
        };

        Func2<Block, Float, Intc2> placer = (block, chance) -> (x, y) -> {
            if(canPlace(x, y, block) && rand.chance(chance)){
                setBlock(x, y, block);
            }
        };

        Array<Intc2> passes = Array.with(
        //initial seeding solar panels
        (x, y) -> {
            Block block = Blocks.largeSolarPanel;

            if(rand.chance(0.001*placeChance) && canPlace(x, y, block)){
                setBlock(x, y, block);
            }
        },

        //extra seeding
        seeder.get(Blocks.solarPanel, tile -> tile.block() == Blocks.largeSolarPanel && rand.chance(0.3)),

        //drills (not powered)
        (x, y) -> {
            //if(!rand.chance(0.1*placeChance)) return;

            Item item = drillItem(x, y, drill);
            if(item != null && item != Items.sand && canPlace(x, y, drill)){
                setBlock(x, y, drill);
            }
        },

        //pump
        (x, y) -> {
            if(!rand.chance(0.1*placeChance)) return;

            if(tiles.get(x, y).floor().isLiquid && tiles.get(x, y).floor().liquidDrop == Liquids.water){
                setBlock(x, y, Blocks.mechanicalPump);
            }
        },

        //coal gens
        seeder.get(Blocks.combustionGenerator, tile -> tile.block() instanceof Drill && drillItem(tile.entity.tileX(), tile.entity.tileY(), (Drill)tile.block()) == Items.coal && rand.chance(0.2)),

        //drills (powered)
        (x, y) -> {
            if(canPlace(x, y, powerDrill) && drillItem(x, y, powerDrill) == Items.thorium  && checker.get(tiles.get(x, y), powerDrill, other -> other.block() instanceof PowerGenerator)){
                setBlock(x, y, powerDrill);
            }
        },

        //water extractors
        seeder.get(Blocks.waterExtractor, tile -> tile.block() instanceof NuclearReactor && rand.chance(0.5)),

        //mend projectors
        seeder.get(Blocks.mendProjector, tile -> tile.block() instanceof PowerGenerator && rand.chance(0.04)),

        //power turrets
        seeder.get(powerTurret, tile -> tile.block() instanceof PowerGenerator && rand.chance(0.04)),

        //repair point
        seeder.get(Blocks.repairPoint, tile -> tile.block() instanceof PowerGenerator && rand.chance(0.1)),

        //turrets1
        seeder.get(turret1, tile -> tile.block() instanceof Drill && rand.chance(0.12)),

        //turrets2
        seeder.get(turret2, tile -> tile.block() instanceof Drill && rand.chance(0.12)),

        //shields
        seeder.get(Blocks.forceProjector, tile -> (tile.block() instanceof CoreBlock || tile.block() instanceof UnitFactory) && rand.chance(0.2 * dscl2)),

        //unit pads (assorted)
        //seeder.get(Blocks.daggerFactory, tile -> (tile.block() instanceof MendProjector || tile.block() instanceof ForceProjector) && rand.chance(0.3 * dscl2)),

        //unit pads (assorted)
        //seeder.get(Blocks.wraithFactory, tile -> (tile.block() instanceof MendProjector || tile.block() instanceof ForceProjector) && rand.chance(0.3 * dscl2)),

        //unit pads (assorted)
        //seeder.get(Blocks.titanFactory, tile -> (tile.block() instanceof MendProjector || tile.block() instanceof ForceProjector) && rand.chance(0.23 * dscl2)),

        //unit pads (assorted)
        //seeder.get(Blocks.ghoulFactory, tile -> (tile.block() instanceof MendProjector || tile.block() instanceof ForceProjector) && rand.chance(0.23 * dscl2)),

        //vaults
        seeder.get(Blocks.vault, tile -> (tile.block() instanceof CoreBlock || tile.block() instanceof ForceProjector) && rand.chance(0.4)),

        //big turrets
        seeder.get(bigTurret, tile -> tile.block() instanceof StorageBlock && rand.chance(0.65)),

        //walls
        (x, y) -> {
            if(!canPlace(x, y, wall)) return;

            for(Point2 point : Geometry.d8){
                Tile tile = tiles.get(x + point.x, y + point.y);
                if(tile != null){
                    //tile = tile.target();
                    if(tile.team() == team && !(tile.block() instanceof Wall) && !(tile.block() instanceof UnitFactory)){
                        tiles.get(x, y).setBlock(wall, team);
                        break;
                    }
                }
            }
        },

        //mines
        placer.get(Blocks.shockMine, 0.02f * difficultyScl),

        //fill up turrets w/ ammo
        (x, y) -> {
            Tile tile = tiles.get(x, y);
            Block block = tile.block();

            if(block instanceof ItemTurret){
                ItemTurret turret = (ItemTurret)block;
                for(Item item : turret.ammoTypes.keys()){
                    tile.entity.handleStack(item, tile.entity.acceptStack(item, 100, null), null);
                }
            }else if(block instanceof NuclearReactor){
                tile.entity.items().add(Items.thorium, 30);
            }else if(block instanceof LiquidTurret){
                tile.entity.liquids().add(Liquids.water, tile.block().liquidCapacity);
            }
        }
        );

        for(Intc2 i : passes){
            for(int x = 0; x < tiles.width; x++){
                for(int y = 0; y < tiles.height; y++){
                    if(!Mathf.within(x, y, core.x, core.y, coreDst)){
                        continue;
                    }

                    i.get(x, y);
                }
            }
        }
    }

    Item drillItem(int x, int y, Drill block){
        if(block.isMultiblock()){
            Item result = null;
            int offsetx = -(block.size - 1) / 2;
            int offsety = -(block.size - 1) / 2;

            for(int dx = 0; dx < block.size; dx++){
                for(int dy = 0; dy < block.size; dy++){
                    int worldx = dx + offsetx + x;
                    int worldy = dy + offsety + y;
                    if(!tiles.in(worldx, worldy)){
                        return null;
                    }

                    if(!block.canPlaceOn(tiles.get(worldx, worldy)) || tiles.get(worldx, worldy).drop() == null) continue;

                    Item drop = tiles.get(worldx, worldy).drop();

                    if(result == null || drop.id < result.id){
                        result = drop;
                    }

                }
            }
            return result;
        }else{
            return tiles.get(x, y).drop();
        }
    }

    void setBlock(int x, int y, Block block){
        tiles.get(x, y).setBlock(block, team);
    }

    boolean canPlace(int x, int y, Block block){
        return Build.validPlace(team, x, y, block, 0);
    }

    Array<Block> find(Boolf<Block> pred){
        Array<Block> out = new Array<>();
        for(Block block : Vars.content.blocks()){
            if(pred.get(block) && block.isPlaceable()){
                out.add(block);
            }
        }
        return out;
    }
}
