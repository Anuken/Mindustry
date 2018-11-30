package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.distribution.*;

import io.anuke.ucore.core.Settings;

public class DistributionBlocks extends BlockList implements ContentList{
    public static Block conveyor, titaniumconveyor, distributor, junction,
    itemBridge, phaseConveyor, sorter, router, overflowGate, massDriver;
    public static float distributionSpeedMultiplier;

    @Override
    public void load(){
        distributionSpeedMultiplier = ((float)Settings.getInt("distribution-speed", 1));

        conveyor = new Conveyor("conveyor"){{
            health = 45;
            speed = 0.03f;
            speed *= distributionSpeedMultiplier;
        }};

        titaniumconveyor = new Conveyor("titanium-conveyor"){{
            health = 65;
            speed = 0.07f;
            speed *= distributionSpeedMultiplier;
        }};

        junction = new Junction("junction"){{
            speed = 26;
            capacity = 32;
        }};

        itemBridge = new BufferedItemBridge("bridge-conveyor"){{
            range = 4;
            speed = 60f;
            bufferCapacity = 15;
            speed *= distributionSpeedMultiplier;
        }};

        phaseConveyor = new ItemBridge("phase-conveyor"){{
            range = 11;
            hasPower = true;
            consumes.power(0.05f);
        }};

        sorter = new Sorter("sorter");

        router = new Router("router");

        distributor = new Router("distributor"){{
            size = 2;
        }};

        overflowGate = new OverflowGate("overflow-gate");

        massDriver = new MassDriver("mass-driver"){{
            size = 3;
            itemCapacity = 80;
            range = 340f;
        }};
    }
}
