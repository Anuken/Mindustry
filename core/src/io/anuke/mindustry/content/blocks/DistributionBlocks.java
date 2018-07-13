package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.distribution.*;

public class DistributionBlocks extends BlockList implements ContentList{
    public static Block conveyor, titaniumconveyor, distributor, junction,
            bridgeConveyor, phaseConveyor, sorter, splitter, overflowGate, massDriver;

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

        bridgeConveyor = new BufferedItemBridge("bridge-conveyor"){{
            range = 3;
            hasPower = false;
        }};

        phaseConveyor = new ItemBridge("phase-conveyor"){{
            range = 7;
        }};

        sorter = new Sorter("sorter");

        splitter = new Splitter("splitter");

        distributor = new Splitter("distributor"){{
            size = 2;
        }};

        overflowGate = new OverflowGate("overflow-gate");

        massDriver = new MassDriver("mass-driver"){{
            size = 3;
            itemCapacity = 80;
            range = 300f;
        }};
    }
}
