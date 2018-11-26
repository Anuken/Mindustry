package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.distribution.*;

public class DistributionBlocks extends BlockList implements ContentList{
    public static Block conveyor, titaniumconveyor, distributor, junction,
    itemBridge, phaseConveyor, sorter, router, overflowGate, massDriver;

    @Override
    public void load(){

        conveyor = new Conveyor("conveyor"){{
            health = 45;
            speed = 0.03f;
        }};

        titaniumconveyor = new Conveyor("titanium-conveyor"){{
            health = 65;
            speed = 0.07f;
        }};

        junction = new Junction("junction"){{
            speed = 26;
            capacity = 32;
        }};

        itemBridge = new BufferedItemBridge("bridge-conveyor"){{
            range = 4;
            speed = 60f;
            bufferCapacity = 15;
        }};

        phaseConveyor = new ItemBridge("phase-conveyor"){{
            range = 12;
            hasPower = true;
            consumes.powerDirect(0.03f);
        }};

        sorter = new Sorter("sorter");

        router = new Router("router");

        distributor = new Router("distributor"){{
            size = 2;
        }};

        overflowGate = new OverflowGate("overflow-gate");

        massDriver = new MassDriver("mass-driver"){{
            size = 3;
            itemCapacity = 60;
            range = 440f;
        }};
    }
}
