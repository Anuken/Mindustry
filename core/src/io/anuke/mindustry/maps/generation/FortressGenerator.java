package io.anuke.mindustry.maps.generation;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.Predicate;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.content.blocks.*;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Edges;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.PowerBlock;
import io.anuke.mindustry.world.blocks.defense.Door;
import io.anuke.mindustry.world.blocks.defense.ForceProjector;
import io.anuke.mindustry.world.blocks.defense.MendProjector;
import io.anuke.mindustry.world.blocks.defense.Wall;
import io.anuke.mindustry.world.blocks.defense.turrets.ItemTurret;
import io.anuke.mindustry.world.blocks.defense.turrets.LiquidTurret;
import io.anuke.mindustry.world.blocks.defense.turrets.PowerTurret;
import io.anuke.mindustry.world.blocks.defense.turrets.Turret;
import io.anuke.mindustry.world.blocks.power.NuclearReactor;
import io.anuke.mindustry.world.blocks.power.PowerGenerator;
import io.anuke.mindustry.world.blocks.power.SolarGenerator;
import io.anuke.mindustry.world.blocks.storage.CoreBlock;
import io.anuke.mindustry.world.blocks.storage.StorageBlock;
import io.anuke.mindustry.world.blocks.units.UnitFactory;
import io.anuke.ucore.function.BiFunction;
import io.anuke.ucore.function.IntPositionConsumer;
import io.anuke.ucore.function.TriFunction;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.content;

public class FortressGenerator{
    private final static int coreDst = 60;

    private int enemyX, enemyY, coreX, coreY;
    private Team team;
    private Generation gen;

    public void generate(Generation gen, Team team, int coreX, int coreY, int enemyX, int enemyY){
        this.enemyX = enemyX;
        this.enemyY = enemyY;
        this.coreX = coreX;
        this.coreY = coreY;
        this.gen = gen;
        this.team = team;

        gen();
    }

    void gen(){
        gen.setBlock(enemyX, enemyY, StorageBlocks.core, team);
        gen.random.nextBoolean();

        float difficultyScl = Mathf.clamp(gen.sector.difficulty / 20f + gen.random.range(0.25f), 0f, 0.9999f);
        float dscl2 = Mathf.clamp(0.5f + gen.sector.difficulty / 20f + gen.random.range(0.1f), 0f, 1.5f);

        Array<Block> turrets = find(b -> b instanceof ItemTurret);
        Array<Block> powerTurrets = find(b -> b instanceof PowerTurret);
        Array<Block> walls = find(b -> b instanceof Wall && !(b instanceof Door) && b.size == 1);

        Block wall = walls.get((int)(difficultyScl * walls.size));

        Turret powerTurret = (Turret) powerTurrets.get((int)(difficultyScl * powerTurrets.size));
        Turret bigTurret = (Turret) turrets.get(Mathf.clamp((int)((difficultyScl+0.2f+gen.random.range(0.2f)) * turrets.size), 0, turrets.size-1));
        Turret turret1 = (Turret) turrets.get(Mathf.clamp((int)((difficultyScl+gen.random.range(0.2f)) * turrets.size), 0, turrets.size-1));
        Turret turret2 = (Turret) turrets.get(Mathf.clamp((int)((difficultyScl+gen.random.range(0.2f)) * turrets.size), 0, turrets.size-1));
        float placeChance = difficultyScl*0.75f+0.25f;

        IntIntMap ammoPerType = new IntIntMap();
        for(Block turret : turrets){
            if(!(turret instanceof ItemTurret)) continue;
            ItemTurret t = (ItemTurret)turret;
            int size = t.getAmmoTypes().length;
            ammoPerType.put(t.id, Mathf.clamp((int)(size* difficultyScl) + gen.random.range(1), 0, size - 1));
        }

        TriFunction<Tile, Block, Predicate<Tile>, Boolean> checker = (current, block, pred) -> {
            for(GridPoint2 point : Edges.getEdges(block.size)){
                Tile tile = gen.tile(current.x + point.x, current.y + point.y);
                if(tile != null){
                    tile = tile.target();
                    if(tile.getTeamID() == team.ordinal() && pred.evaluate(tile)){
                        return true;
                    }
                }
            }
            return false;
        };

        BiFunction<Block, Predicate<Tile>, IntPositionConsumer> seeder = (block, pred) -> (x, y) -> {
            if(gen.canPlace(x, y, block) && ((block instanceof Wall && block.size == 1) || gen.random.chance(placeChance)) && checker.get(gen.tile(x, y), block, pred)){
                gen.setBlock(x, y, block, team);
            }
        };

        BiFunction<Block, Float, IntPositionConsumer> placer = (block, chance) -> (x, y) -> {
            if(gen.canPlace(x, y, block) && gen.random.chance(chance)){
                gen.setBlock(x, y, block, team);
            }
        };

        Array<IntPositionConsumer> passes = Array.with(
            //initial seeding solar panels
            placer.get(PowerBlocks.largeSolarPanel, 0.001f),

            //extra seeding
            seeder.get(PowerBlocks.solarPanel, tile -> tile.block() == PowerBlocks.largeSolarPanel && gen.random.chance(0.3)),

            //coal gens
            seeder.get(PowerBlocks.combustionGenerator, tile -> tile.block() instanceof SolarGenerator && gen.random.chance(0.2)),

            //water extractors
            seeder.get(ProductionBlocks.waterExtractor, tile -> tile.block() instanceof NuclearReactor && gen.random.chance(0.5)),

            //mend projectors
            seeder.get(DefenseBlocks.mendProjector, tile -> tile.block() instanceof PowerGenerator && gen.random.chance(0.04)),

            //power turrets
            seeder.get(powerTurret, tile -> tile.block() instanceof PowerGenerator && gen.random.chance(0.04)),

            //repair point
            seeder.get(UnitBlocks.repairPoint, tile -> tile.block() instanceof PowerGenerator && gen.random.chance(0.1)),

            //turrets1
            seeder.get(turret1, tile -> tile.block() instanceof PowerBlock && gen.random.chance(0.22 - turret1.size*0.02)),

            //turrets2
            seeder.get(turret2, tile -> tile.block() instanceof PowerBlock && gen.random.chance(0.12 - turret2.size*0.02)),

            //shields
            seeder.get(DefenseBlocks.forceProjector, tile -> (tile.block() instanceof CoreBlock || tile.block() instanceof UnitFactory) && gen.random.chance(0.2 * dscl2)),

            //unit pads (assorted)
            seeder.get(UnitBlocks.daggerFactory, tile -> (tile.block() instanceof MendProjector || tile.block() instanceof ForceProjector) && gen.random.chance(0.3 * dscl2)),

            //unit pads (assorted)
            seeder.get(UnitBlocks.wraithFactory, tile -> (tile.block() instanceof MendProjector || tile.block() instanceof ForceProjector) && gen.random.chance(0.3 * dscl2)),

            //unit pads (assorted)
            seeder.get(UnitBlocks.titanFactory, tile -> (tile.block() instanceof MendProjector || tile.block() instanceof ForceProjector) && gen.random.chance(0.23 * dscl2)),

            //unit pads (assorted)
            seeder.get(UnitBlocks.ghoulFactory, tile -> (tile.block() instanceof MendProjector || tile.block() instanceof ForceProjector) && gen.random.chance(0.23 * dscl2)),

            //vaults
            seeder.get(StorageBlocks.vault, tile -> (tile.block() instanceof CoreBlock || tile.block() instanceof ForceProjector) && gen.random.chance(0.4)),

            //big turrets
            seeder.get(bigTurret, tile -> tile.block() instanceof StorageBlock && gen.random.chance(0.65)),

            //walls
            (x, y) -> {
                if(!gen.canPlace(x, y, wall)) return;

                for(GridPoint2 point : Geometry.d8){
                    Tile tile = gen.tile(x + point.x, y + point.y);
                    if(tile != null){
                        tile = tile.target();
                        if(tile.getTeamID() == team.ordinal() && !(tile.block() instanceof Wall) && !(tile.block() instanceof UnitFactory)){
                            gen.setBlock(x, y, wall, team);
                            break;
                        }
                    }
                }
            },

            //mines
            placer.get(DefenseBlocks.shockMine, 0.02f * difficultyScl),

            //fill up turrets w/ ammo
            (x, y) -> {
                Tile tile = gen.tile(x, y);
                Block block = tile.block();

                if(block instanceof PowerTurret){
                    tile.entity.power.satisfaction = 1.0f;
                }else if(block instanceof ItemTurret){
                    ItemTurret turret = (ItemTurret)block;
                    AmmoType[] type = turret.getAmmoTypes();
                    int index = ammoPerType.get(block.id, 0);
                    block.handleStack(type[index].item, block.acceptStack(type[index].item, 1000, tile, null), tile, null);
                }else if(block instanceof NuclearReactor){
                    tile.entity.items.add(Items.thorium, 30);
                }else if(block instanceof LiquidTurret){
                    tile.entity.liquids.add(Liquids.water, tile.block().liquidCapacity);
                }
            }
        );

        for(IntPositionConsumer i : passes){
            for(int x = 0; x < gen.width; x++){
                for(int y = 0; y < gen.height; y++){
                    if(Vector2.dst(x, y, enemyX, enemyY) > coreDst){
                        continue;
                    }

                    i.accept(x, y);
                }
            }
        }
    }

    Array<Block> find(Predicate<Block> pred){
        Array<Block> out = new Array<>();
        for(Block block : content.blocks()){
            if(pred.evaluate(block) && Recipe.getByResult(block) != null){
                out.add(block);
            }
        }
        return out;
    }
}
