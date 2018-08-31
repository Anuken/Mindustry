package io.anuke.mindustry.maps.generation;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Predicate;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.blocks.StorageBlocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Edges;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.defense.Door;
import io.anuke.mindustry.world.blocks.defense.Wall;
import io.anuke.mindustry.world.blocks.defense.turrets.ItemTurret;
import io.anuke.mindustry.world.blocks.defense.turrets.PowerTurret;
import io.anuke.mindustry.world.blocks.defense.turrets.Turret;
import io.anuke.mindustry.world.blocks.power.PowerGenerator;
import io.anuke.mindustry.world.blocks.power.SolarGenerator;
import io.anuke.mindustry.world.blocks.production.Drill;
import io.anuke.mindustry.world.consumers.ConsumePower;
import io.anuke.ucore.util.Mathf;

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

        float difficultyScl = Mathf.clamp(gen.sector.difficulty / 25f + Mathf.range(1f/2f), 0f, 0.9999f);

        Array<Block> turrets = find(b -> (b instanceof ItemTurret && accepts(((ItemTurret) b).getAmmoTypes(), Items.copper) || b instanceof PowerTurret));
        Array<Block> drills = find(b -> b instanceof Drill && !b.consumes.has(ConsumePower.class));
        Array<Block> gens = find(b -> b instanceof SolarGenerator);
        Array<Block> walls = find(b -> b instanceof Wall && !(b instanceof Door) && b.size == 1);

        Block wall = walls.get((int)(difficultyScl * walls.size));
        Drill drill = (Drill) drills.get((int)(difficultyScl * drills.size));

        //place down drills
        for(int x = 0; x < gen.width; x++){
            for(int y = 0; y < gen.height; y++){
                if(Vector2.dst(x, y, enemyX, enemyY) > coreDst){
                    continue;
                }

                Item item = gen.drillItem(x, y, drill);

                if(item != null && item == Items.copper && gen.canPlace(x, y, drill)){
                    gen.setBlock(x, y, drill, team);
                }
            }
        }

        Turret turret = (Turret) turrets.first();

        //place down turrets
        for(int x = 0; x < gen.width; x++){
            for(int y = 0; y < gen.height; y++){
                if(Vector2.dst(x, y, enemyX, enemyY) > coreDst + 4 || !gen.canPlace(x, y, turret)){
                    continue;
                }

                boolean found = false;

                for(GridPoint2 point : Edges.getEdges(turret.size)){
                    Tile tile = gen.tile(x + point.x, y + point.y);

                    if(tile != null){
                        tile = tile.target();

                        if(turret instanceof PowerTurret && tile.target().block() instanceof PowerGenerator){
                            found = true;
                            break;
                        }else if(turret instanceof ItemTurret && tile.block() instanceof Drill && accepts(((ItemTurret) turret).getAmmoTypes(), gen.drillItem(tile.x, tile.y, (Drill) tile.block()))){
                            found = true;
                            break;
                        }
                    }
                }

                if(found){
                    gen.setBlock(x, y, turret, team);
                }
            }
        }

        //place down drills
        for(int x = 0; x < gen.width; x++){
            for(int y = 0; y < gen.height; y++){
                if(Vector2.dst(x, y, enemyX, enemyY) > coreDst || !gen.canPlace(x, y, wall)){
                    continue;
                }

                boolean found = false;
                for(GridPoint2 point : Edges.getEdges(wall.size)){
                    Tile tile = gen.tile(x + point.x, y + point.y);
                    if(tile != null){
                        tile = tile.target();
                        if(tile.getTeamID() == team.ordinal() && !(tile.block() instanceof Wall)){
                            found = true;
                            break;
                        }
                    }
                }
                if(found){
                    gen.setBlock(x, y, wall, team);
                }
            }
        }

    }

    boolean accepts(AmmoType[] types, Item item){
        for(AmmoType type : types){
            if(type.item == item){
                return true;
            }
        }
        return false;
    }

    Array<Block> find(Predicate<Block> pred){
        Array<Block> out = new Array<>();
        for(Block block : Block.all()){
            if(pred.evaluate(block)){
                out.add(block);
            }
        }
        return out;
    }
}
