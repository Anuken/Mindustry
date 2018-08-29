package io.anuke.mindustry.maps.generation;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Predicate;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.blocks.StorageBlocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.defense.turrets.ItemTurret;
import io.anuke.mindustry.world.blocks.defense.turrets.PowerTurret;

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
        gen.tiles[enemyX][enemyY].setBlock(StorageBlocks.core, team);
        Item[] acceptedItems = {Items.copper, Items.densealloy, Items.silicon, Items.thorium};

        Array<Block> turrets = find(b -> (b instanceof ItemTurret && accepts(((ItemTurret) b).getAmmoTypes(), acceptedItems) || b instanceof PowerTurret));

        for(int x = 0; x < gen.width; x++){
            for(int y = 0; y < gen.height; y++){
                if(Vector2.dst(x, y, enemyX, enemyY) > coreDst){
                    continue;
                }
            }
        }

    }

    boolean accepts(AmmoType[] types, Item[] items){
        for(AmmoType type : types){
            for(Item item : items){
                if(type.item == item){
                    return true;
                }
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
