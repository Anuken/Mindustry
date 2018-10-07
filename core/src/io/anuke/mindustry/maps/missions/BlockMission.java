package io.anuke.mindustry.maps.missions;

import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.world.Block;

/**A mission in which the player must place a block somewhere.*/
public class BlockMission extends ContentMission{

    public BlockMission(Block block) {
        super(Recipe.getByResult(block));
    }
}
