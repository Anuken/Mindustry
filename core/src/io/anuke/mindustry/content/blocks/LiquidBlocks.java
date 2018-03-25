package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.distribution.Conduit;
import io.anuke.mindustry.world.blocks.types.distribution.LiquidJunction;
import io.anuke.mindustry.world.blocks.types.distribution.LiquidRouter;
import io.anuke.mindustry.world.blocks.types.distribution.TunnelConduit;
import io.anuke.mindustry.world.blocks.types.production.Pump;

public class LiquidBlocks {
    public static final Block

    pump = new Pump("pump") {{
        pumpAmount = 0.8f;
    }},

    fluxpump = new Pump("fluxpump") {{
        pumpAmount = 1.2f;
    }},

    conduit = new Conduit("conduit") {{
        health = 45;
    }},

    pulseconduit = new Conduit("pulseconduit") {{
        liquidCapacity = 16f;
        liquidFlowFactor = 4.9f;
        health = 65;
    }},

    liquidrouter = new LiquidRouter("liquidrouter") {{
        liquidCapacity = 40f;
    }},

    liquidtank = new LiquidRouter("liquidtank") {{
        size = 3;
        liquidCapacity = 1500f;
    }},

    liquidjunction = new LiquidJunction("liquidjunction"),

    conduittunnel = new TunnelConduit("conduittunnel") {{
        speed = 53;
    }};
}
