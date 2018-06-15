package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.distribution.*;
import io.anuke.mindustry.world.blocks.production.Pump;

public class LiquidBlocks extends BlockList implements ContentList{
    public static Block mechanicalPump, rotaryPump, thermalPump, conduit, pulseconduit, liquidrouter, liquidtank, liquidjunction, bridgeconduit, laserconduit;

    @Override
    public void load() {

        mechanicalPump = new Pump("mechanical-pump") {{
            pumpAmount = 0.1f;
            tier = 0;
        }};

        rotaryPump = new Pump("rotary-pump") {{
            pumpAmount = 0.2f;
            powerUse = 0.015f;
            liquidCapacity = 30f;
            size = 2;
            tier = 1;
        }};

        thermalPump = new Pump("thermal-pump") {{
            pumpAmount = 0.3f;
            powerUse = 0.02f;
            liquidCapacity = 40f;
            size = 2;
            tier = 2;
        }};

        conduit = new Conduit("conduit") {{
            health = 45;
        }};

        pulseconduit = new Conduit("pulseconduit") {{
            liquidCapacity = 16f;
            liquidFlowFactor = 4.9f;
            health = 65;
        }};

        liquidrouter = new LiquidRouter("liquidrouter") {{
            liquidCapacity = 40f;
        }};

        liquidtank = new LiquidRouter("liquidtank") {{
            size = 3;
            liquidCapacity = 1500f;
            health = 500;
        }};

        liquidjunction = new LiquidJunction("liquidjunction");

        bridgeconduit = new LiquidExtendingBridge("bridgeconduit") {{
            range = 3;
            hasPower = false;
        }};

        laserconduit = new LiquidBridge("laserconduit") {{
            range = 7;
        }};
    }
}
