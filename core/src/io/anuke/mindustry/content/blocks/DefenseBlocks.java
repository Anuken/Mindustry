package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.Wall;
import io.anuke.mindustry.world.blocks.types.defense.Door;
import io.anuke.mindustry.world.blocks.types.defense.ShieldedWallBlock;

public class DefenseBlocks implements ContentList {
    public static Block stonewall, ironwall, steelwall, titaniumwall, diriumwall, compositewall, steelwalllarge, titaniumwalllarge, diriumwalllarge, titaniumshieldwall, door, largedoor;

    @Override
    public void load() {
        int wallHealthMultiplier = 4;

        stonewall = new Wall("stonewall") {{
            health = 40 * wallHealthMultiplier;
        }};

        ironwall = new Wall("ironwall") {{
            health = 80 * wallHealthMultiplier;
        }};

        steelwall = new Wall("steelwall") {{
            health = 110 * wallHealthMultiplier;
        }};

        titaniumwall = new Wall("titaniumwall") {{
            health = 150 * wallHealthMultiplier;
        }};

        diriumwall = new Wall("duriumwall") {{
            health = 190 * wallHealthMultiplier;
        }};

        compositewall = new Wall("compositewall") {{
            health = 270 * wallHealthMultiplier;
        }};

        steelwalllarge = new Wall("steelwall-large") {{
            health = 110 * 4 * wallHealthMultiplier;
            size = 2;
        }};

        titaniumwalllarge = new Wall("titaniumwall-large") {{
            health = 150 * 4 * wallHealthMultiplier;
            size = 2;
        }};

        diriumwalllarge = new Wall("duriumwall-large") {{
            health = 190 * 4 * wallHealthMultiplier;
            size = 2;
        }};

        titaniumshieldwall = new ShieldedWallBlock("titaniumshieldwall") {{
            health = 150 * wallHealthMultiplier;
        }};

        door = new Door("door") {{
            health = 90 * wallHealthMultiplier;
        }};

        largedoor = new Door("door-large") {{
            openfx = BlockFx.dooropenlarge;
            closefx = BlockFx.doorcloselarge;
            health = 90 * 4 * wallHealthMultiplier;
            size = 2;
        }};
    }
}
