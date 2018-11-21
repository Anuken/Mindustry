package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.defense.*;

public class DefenseBlocks extends BlockList implements ContentList{
    public static Block copperWall, copperWallLarge, denseAlloyWall, denseAlloyWallLarge, thoriumWall, thoriumWallLarge, door, doorLarge,
            phaseWall, phaseWallLarge, surgeWall, surgeWallLarge, mendProjector, overdriveProjector, forceProjector, shockMine;

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

        denseAlloyWall = new Wall("dense-alloy-wall"){{
            health = 110 * wallHealthMultiplier;
        }};

        denseAlloyWallLarge = new Wall("dense-alloy-wall-large"){{
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
            consumes.powerDirect(0.2f);
            size = 2;
            consumes.item(Items.phasefabric).optional(true);
        }};

        overdriveProjector = new OverdriveProjector("overdrive-projector"){{
            consumes.powerDirect(0.35f);
            size = 2;
            consumes.item(Items.phasefabric).optional(true);
        }};

        forceProjector = new ForceProjector("force-projector"){{
            consumes.powerDirect(0.2f);
            size = 3;
            consumes.item(Items.phasefabric).optional(true);
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
