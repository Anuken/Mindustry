package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.defense.*;

public class DefenseBlocks extends BlockList implements ContentList{
    public static Block copperWall, copperWallLarge, compositeWall, compositeWallLarge, thoriumWall, thoriumWallLarge, door, doorLarge,
            phaseWall, phaseWallLarge, surgeWall, surgeWallLarge, mendProjector, shockMine;

    @Override
    public void load(){
        int wallHealthMultiplier = 3;

        copperWall = new Wall("copper-wall"){{
            health = 80 * wallHealthMultiplier;
        }};

        copperWallLarge = new Wall("copper-wall-large"){{
            health = 80 * 4 * wallHealthMultiplier;
            size = 2;
        }};

        compositeWall = new Wall("composite-wall"){{
            health = 110 * wallHealthMultiplier;
        }};

        compositeWallLarge = new Wall("composite-wall-large"){{
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

        phaseWall = new DeflectorWall("phase-wall"){{
            health = 150 * wallHealthMultiplier;
        }};

        phaseWallLarge = new DeflectorWall("phase-wall-large"){{
            health = 150 * 4 * wallHealthMultiplier;
            size = 2;
        }};

        surgeWall = new SurgeWall("surge-wall"){{
            health = 230 * wallHealthMultiplier;
        }};

        surgeWallLarge = new SurgeWall("surge-wall-large"){{
            health = 230 * 4 * wallHealthMultiplier;
            size = 2;
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

        mendProjector = new MendProjector("mend-projector"){{
            consumes.power(0.25f);
            health = 100 * 4 * wallHealthMultiplier;
            size = 2;
            consumes.item(Items.phasematter).optional(true);
        }};

        shockMine = new ShockMine("shock-mine"){{
            health = 40;
            damage = 11;
            tileDamage = 7f;
            length = 10;
            tendrils = 5;
        }};
    }
}
