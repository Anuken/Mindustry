package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.Wall;
import io.anuke.mindustry.world.blocks.defense.DeflectorWall;
import io.anuke.mindustry.world.blocks.defense.Door;
import io.anuke.mindustry.world.blocks.defense.PhaseWall;

public class DefenseBlocks extends BlockList implements ContentList {
    public static Block ironwall, ironwalllarge, steelwall, titaniumwall, diriumwall, steelwalllarge, titaniumwalllarge, diriumwalllarge, door, largedoor, deflectorwall, deflectorwalllarge,
        phasewall, phasewalllarge;

    @Override
    public void load() {
        int wallHealthMultiplier = 4;

        ironwall = new Wall("ironwall") {{
            health = 80 * wallHealthMultiplier;
        }};

        ironwalllarge = new Wall("ironwall-large") {{
            health = 80 * 4 * wallHealthMultiplier;
            size = 2;
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

        phasewall = new PhaseWall("phase-wall") {{
            health = 150 * wallHealthMultiplier;
        }};

        phasewalllarge = new PhaseWall("phase-wall-large") {{
            health = 150 * 4 * wallHealthMultiplier;
            size = 2;
            regenSpeed = 0.5f;
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
