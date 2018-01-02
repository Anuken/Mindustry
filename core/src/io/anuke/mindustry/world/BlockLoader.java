package io.anuke.mindustry.world;

import io.anuke.mindustry.world.blocks.*;

public class BlockLoader {

    public static void load(){

        Block[] blockClasses = {
            Blocks.air,
            DefenseBlocks.compositewall,
            DistributionBlocks.conduit,
            ProductionBlocks.coaldrill,
            WeaponBlocks.chainturret,
            SpecialBlocks.enemySpawn
            //add any new block sections here
        };
    }
}
