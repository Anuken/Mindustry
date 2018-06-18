package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.distribution.WarpGate;
import io.anuke.mindustry.world.blocks.power.*;

public class PowerBlocks extends BlockList implements ContentList {
    public static Block combustiongenerator, thermalgenerator, turbinegenerator, rtgenerator, solarpanel, largesolarpanel,
            nuclearReactor, fusionReactor, battery, batteryLarge, powernode, powernodelarge, warpgate;

    @Override
    public void load() {
        combustiongenerator = new BurnerGenerator("combustion-generator") {{
            powerOutput = 0.04f;
            powerCapacity = 40f;
        }};

        thermalgenerator = new LiquidHeatGenerator("thermal-generator") {{
            maxLiquidGenerate = 0.5f;
            powerPerLiquid = 0.08f;
            powerCapacity = 40f;
            generateEffect = BlockFx.redgeneratespark;
            size = 2;
        }};

        turbinegenerator = new TurbineGenerator("turbine-generator") {{
            powerOutput = 0.04f;
            powerCapacity = 40f;
            size = 2;
        }};

        rtgenerator = new DecayGenerator("rtg-generator") {{
            powerCapacity = 40f;
            powerOutput = 0.02f;
            itemDuration = 500f;
        }};

        solarpanel = new SolarGenerator("solar-panel") {{
            generation = 0.003f;
        }};

        largesolarpanel = new SolarGenerator("large-solar-panel") {{
            size = 3;
            generation = 0.012f;
        }};

        nuclearReactor = new NuclearReactor("nuclear-reactor") {{
            size = 3;
            health = 600;
        }};

        fusionReactor = new FusionReactor("fusion-reactor") {{
            size = 4;
            health = 600;
        }};

        battery = new PowerGenerator("battery") {{
            powerCapacity = 320f;
        }};

        batteryLarge = new PowerGenerator("battery-large") {{
            size = 3;
            powerCapacity = 2000f;
        }};

        powernode = new PowerDistributor("power-node") {{
            shadow = "shadow-round-1";
        }};

        powernodelarge = new PowerDistributor("power-node-large") {{
            size = 2;
            powerSpeed = 1f;
            maxNodes = 5;
            laserRange = 7.5f;
            shadow = "shadow-round-2";
        }};

        warpgate = new WarpGate("warpgate");

    }
}
