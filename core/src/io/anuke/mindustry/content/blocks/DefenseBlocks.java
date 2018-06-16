package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.Wall;
import io.anuke.mindustry.world.blocks.defense.DeflectorWall;
import io.anuke.mindustry.world.blocks.defense.Door;

public class DefenseBlocks extends BlockList implements ContentList {
    public static Block stonewall, ironwall, steelwall, titaniumwall, diriumwall, steelwalllarge, titaniumwalllarge, diriumwalllarge, door, largedoor, deflectorwall, deflectorwalllarge;

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

        deflectorwall = new DeflectorWall("deflector-wall") {{
            health = 150 * wallHealthMultiplier;
        }};

        deflectorwalllarge = new DeflectorWall("deflector-wall-large") {{
            health = 150 * 4 * wallHealthMultiplier;
            size = 2;
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
