package io.anuke.mindustry.maps.generation;

import io.anuke.mindustry.content.blocks.StorageBlocks;
import io.anuke.mindustry.game.Team;

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
    }
}
