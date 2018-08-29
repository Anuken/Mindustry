package io.anuke.mindustry.maps.generation;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Predicate;
import io.anuke.mindustry.content.blocks.StorageBlocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.production.Drill;
import io.anuke.mindustry.world.consumers.ConsumePower;

public class FortressGenerator{
    private final static int minCoreDst = 60;

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

        Array<Block> drills = find(b -> b instanceof Drill && !b.consumes.has(ConsumePower.class));

        for(int x = 0; x < gen.width; x++){
            for(int y = 0; y < gen.height; y++){
                //TODO place valid drills
            }
        }

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
