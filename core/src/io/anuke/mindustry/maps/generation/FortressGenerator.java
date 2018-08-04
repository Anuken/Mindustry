package io.anuke.mindustry.maps.generation;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.content.blocks.DefenseBlocks;
import io.anuke.mindustry.content.blocks.ProductionBlocks;
import io.anuke.mindustry.content.blocks.TurretBlocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.util.Geometry;

public class FortressGenerator{
    private final Block[] turretBlocks = {TurretBlocks.duo, TurretBlocks.hail, TurretBlocks.wave};
    private final Block[] drillBlocks = {ProductionBlocks.tungstenDrill, ProductionBlocks.carbideDrill};
    private final Block[] armorBlocks = {DefenseBlocks.tungstenWall, DefenseBlocks.carbideWall, DefenseBlocks.thoriumWall};
    private final int minCoreDst = 50;

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

        genOutposts();
    }

    void genOutposts(){
        int index = 0;
        Block turret = turretBlocks[index], drill = drillBlocks[index], armor = armorBlocks[index];
        Item ore = Items.tungsten;

        for(int x = 2; x < gen.width - 2; x++){
            for(int y = 2; y < gen.height - 2; y++){
                if(Vector2.dst(x, y, coreX, coreY) > minCoreDst &&
                    gen.tiles[x][y].floor().dropsItem(ore) && gen.random.chance(0.02)){
                    
                    int elevation = gen.tiles[x][y].getElevation();
                    gen.tiles[x][y].setBlock(drill, team);

                    for(GridPoint2 point : Geometry.d4){
                        gen.tiles[x + point.x][y + point.y].setBlock(turret, team);
                        gen.tiles[x + point.x][y + point.y].setElevation(elevation);
                    }

                    for(int cx = -2; cx <= 2; cx++){
                        for(int cy = -2; cy <= 2; cy++){
                            Tile tile = gen.tiles[x + cx][y + cy];
                            if(tile.block().alwaysReplace || tile.block() == Blocks.air){
                                tile.setElevation(elevation);
                                tile.setBlock(armor, team);
                            }
                        }
                    }
                }
            }
        }
    }
}
