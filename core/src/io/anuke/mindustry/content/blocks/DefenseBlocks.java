package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.Wall;
import io.anuke.mindustry.world.blocks.defense.DeflectorWall;
import io.anuke.mindustry.world.blocks.defense.Door;
import io.anuke.mindustry.world.blocks.defense.PhaseWall;

public class DefenseBlocks extends BlockList implements ContentList{
    public static Block tungstenWall, tungstenWallLarge, carbideWall, carbideWallLarge, thoriumWall, thoriumWallLarge, door, doorLarge, deflectorwall, deflectorwalllarge,
            phasewall, phasewalllarge;

    @Override
    public void load(){
        int wallHealthMultiplier = 4;

        tungstenWall = new Wall("tungsten-wall"){{
            health = 80 * wallHealthMultiplier;
        }};

        tungstenWallLarge = new Wall("tungsten-wall-large"){{
            health = 80 * 4 * wallHealthMultiplier;
            size = 2;
        }};

        carbideWall = new Wall("carbide-wall"){{
            health = 110 * wallHealthMultiplier;
        }};

        carbideWallLarge = new Wall("carbide-wall-large"){{
            health = 110 * wallHealthMultiplier * 4;
            size = 2;
        }};

        thoriumWall = new Wall("thorium-wall"){{
            health = 200 * wallHealthMultiplier;
        }};

        thoriumWallLarge = new Wall("thorium-wall-large"){{
            health = 200 * wallHealthMultiplier * 4;
            size = 2;
        }};

        deflectorwall = new DeflectorWall("deflector-wall"){{
            health = 150 * wallHealthMultiplier;
        }};

        deflectorwalllarge = new DeflectorWall("deflector-wall-large"){{
            health = 150 * 4 * wallHealthMultiplier;
            size = 2;
        }};

        phasewall = new PhaseWall("phase-wall"){{
            health = 150 * wallHealthMultiplier;
        }};

        phasewalllarge = new PhaseWall("phase-wall-large"){{
            health = 150 * 4 * wallHealthMultiplier;
            size = 2;
            regenSpeed = 0.5f;
        }};

        door = new Door("door"){{
            health = 100 * wallHealthMultiplier;
        }};

        doorLarge = new Door("door-large"){{
            openfx = BlockFx.dooropenlarge;
            closefx = BlockFx.doorcloselarge;
            health = 100 * 4 * wallHealthMultiplier;
            size = 2;
        }};
    }
}
