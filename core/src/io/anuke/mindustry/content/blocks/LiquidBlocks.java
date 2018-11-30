package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.distribution.*;
import io.anuke.mindustry.world.blocks.production.Pump;

import io.anuke.ucore.core.Settings;

public class LiquidBlocks extends BlockList implements ContentList{
    public static Block mechanicalPump, rotaryPump, thermalPump, conduit, pulseConduit, liquidRouter, liquidtank, liquidJunction, bridgeConduit, phaseConduit;
    public static float pumpSpeedMultiplier;

    @Override
    public void load(){
        pumpSpeedMultiplier = (float)Settings.getInt("pump-speed", 1);

        mechanicalPump = new Pump("mechanical-pump"){{
            shadow = "shadow-round-1";
            pumpAmount = 0.1f;
            pumpAmount *= pumpSpeedMultiplier;
            tier = 0;
        }};

        rotaryPump = new Pump("rotary-pump"){{
            shadow = "shadow-rounded-2";
            pumpAmount = 0.2f;
            pumpAmount *= pumpSpeedMultiplier;
            consumes.power(0.015f);
            liquidCapacity = 30f;
            powerCapacity = 20f;
            hasPower = true;
            size = 2;
            tier = 1;
        }};

        thermalPump = new Pump("thermal-pump"){{
            shadow = "shadow-rounded-2";
            pumpAmount = 0.3f;
            pumpAmount *= pumpSpeedMultiplier;
            consumes.power(0.03f);
            liquidCapacity = 40f;
            hasPower = true;
            powerCapacity = 20f;
            size = 2;
            tier = 2;
        }};

        conduit = new Conduit("conduit"){{
            health = 45;
        }};

        pulseConduit = new Conduit("pulse-conduit"){{
            liquidCapacity = 16f;
            liquidFlowFactor = 4.9f;
            health = 90;
        }};

        liquidRouter = new LiquidRouter("liquid-router"){{
            liquidCapacity = 40f;
        }};

        liquidtank = new LiquidRouter("liquid-tank"){{
            size = 3;
            liquidCapacity = 1500f;
            health = 500;
        }};

        liquidJunction = new LiquidJunction("liquid-junction");

        bridgeConduit = new LiquidExtendingBridge("bridge-conduit"){{
            range = 4;
            hasPower = false;
        }};

        phaseConduit = new LiquidBridge("phase-conduit"){{
            range = 11;
            hasPower = true;
            consumes.power(0.05f);
        }};
    }
}
