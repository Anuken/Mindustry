package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.distribution.WarpGate;
import io.anuke.mindustry.world.blocks.power.*;

public class PowerBlocks extends BlockList implements ContentList{
    public static Block combustionGenerator, thermalGenerator, turbineGenerator, rtgGenerator, solarPanel, largeSolarPanel,
            thoriumReactor, fusionReactor, battery, batteryLarge, powerNode, powerNodeLarge, warpGate;

    @Override
    public void load(){
        combustionGenerator = new BurnerGenerator("combustion-generator"){{
            powerOutput = 0.09f;
            powerCapacity = 40f;
            itemDuration = 40f;
        }};

        thermalGenerator = new LiquidHeatGenerator("thermal-generator"){{
            maxLiquidGenerate = 0.5f;
            powerCapacity = 40f;
            powerPerLiquid = 1.5f;
            generateEffect = BlockFx.redgeneratespark;
            size = 2;
        }};

        turbineGenerator = new TurbineGenerator("turbine-generator"){{
            powerOutput = 0.28f;
            powerCapacity = 40f;
            itemDuration = 30f;
            powerPerLiquid = 0.7f;
            consumes.liquid(Liquids.water, 0.05f);
            size = 2;
        }};

        rtgGenerator = new DecayGenerator("rtg-generator"){{
            powerCapacity = 40f;
            powerOutput = 0.02f;
            itemDuration = 500f;
        }};

        solarPanel = new SolarGenerator("solar-panel"){{
            generation = 0.0045f;
        }};

        largeSolarPanel = new SolarGenerator("solar-panel-large"){{
            size = 3;
            generation = 0.055f;
        }};

        thoriumReactor = new NuclearReactor("thorium-reactor"){{
            size = 3;
            health = 700;
            powerMultiplier = 0.8f;
        }};

        fusionReactor = new FusionReactor("fusion-reactor"){{
            size = 4;
            health = 600;
        }};

        battery = new PowerDistributor("battery"){{
            powerCapacity = 320f;
        }};

        batteryLarge = new PowerDistributor("battery-large"){{
            size = 3;
            powerCapacity = 2000f;
        }};

        powerNode = new PowerNode("power-node"){{
            shadow = "shadow-round-1";
            maxNodes = 4;
        }};

        powerNodeLarge = new PowerNode("power-node-large"){{
            size = 2;
            powerSpeed = 1f;
            maxNodes = 6;
            laserRange = 7.5f;
            shadow = "shadow-round-2";
        }};

        warpGate = new WarpGate("warp-gate");

    }
}
