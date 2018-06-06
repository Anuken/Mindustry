package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.distribution.*;
import io.anuke.mindustry.world.blocks.production.Pump;

public class LiquidBlocks extends BlockList implements ContentList{
    public static Block pump, fluxpump, conduit, pulseconduit, liquidrouter, liquidtank, liquidjunction, bridgeconduit, laserconduit;

    @Override
    public void load() {

        pump = new Pump("pump") {{
            pumpAmount = 0.1f;
        }};

        fluxpump = new Pump("fluxpump") {{
            pumpAmount = 0.2f;
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
