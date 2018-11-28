package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.distribution.*;
import io.anuke.mindustry.world.blocks.production.Pump;

public class LiquidBlocks extends BlockList implements ContentList{
    public static Block mechanicalPump, rotaryPump, thermalPump, conduit, pulseConduit, liquidRouter, liquidtank, liquidJunction, bridgeConduit, phaseConduit;

    @Override
    public void load(){

        mechanicalPump = new Pump("mechanical-pump"){{
            shadow = "shadow-round-1";
            pumpAmount = 0.1f;
            tier = 0;
        }};

        rotaryPump = new Pump("rotary-pump"){{
            shadow = "shadow-rounded-2";
            pumpAmount = 0.2f;
            consumes.powerDirect(0.015f);
            liquidCapacity = 30f;
            hasPower = true;
            size = 2;
            tier = 1;
        }};

        thermalPump = new Pump("thermal-pump"){{
            shadow = "shadow-rounded-2";
            pumpAmount = 0.275f;
            consumes.powerDirect(0.03f);
            liquidCapacity = 40f;
            hasPower = true;
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
            range = 12;
            hasPower = true;
            consumes.powerDirect(0.03f);
        }};
    }
}
