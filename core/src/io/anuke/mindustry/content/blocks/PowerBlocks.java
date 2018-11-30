package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.distribution.WarpGate;
import io.anuke.mindustry.world.blocks.power.*;

import io.anuke.ucore.core.Settings;

public class PowerBlocks extends BlockList implements ContentList{
    public static Block combustionGenerator, thermalGenerator, turbineGenerator, rtgGenerator, solarPanel, largeSolarPanel,
            thoriumReactor, fusionReactor, battery, batteryLarge, powerNode, powerNodeLarge, warpGate;
    public static float powerAmountMultiplier;

    @Override
    public void load(){
        powerAmountMultiplier = (float)Settings.getInt("power-amount", 1);

        combustionGenerator = new BurnerGenerator("combustion-generator"){{
            powerOutput = 0.09f;
            powerOutput *= powerAmountMultiplier;
            powerCapacity = 40f;
            itemDuration = 40f;
        }};

        thermalGenerator = new LiquidHeatGenerator("thermal-generator"){{
            maxLiquidGenerate = 0.5f;
            powerCapacity = 40f;
            powerPerLiquid = 1f;
            powerPerLiquid *= powerAmountMultiplier;
            generateEffect = BlockFx.redgeneratespark;
            size = 2;
        }};

        turbineGenerator = new TurbineGenerator("turbine-generator"){{
            powerOutput = 0.28f;
            powerOutput *= powerAmountMultiplier;
            powerCapacity = 40f;
            itemDuration = 30f;
            powerPerLiquid = 0.7f;
            powerPerLiquid *= powerAmountMultiplier;
            consumes.liquid(Liquids.water, 0.05f);
            size = 2;
        }};

        rtgGenerator = new DecayGenerator("rtg-generator"){{
            powerCapacity = 40f;
            size = 2;
            powerOutput = 0.3f;
            powerOutput *= powerAmountMultiplier;
            itemDuration = 220f;
        }};

        solarPanel = new SolarGenerator("solar-panel"){{
            generation = 0.0045f;
            generation *= powerAmountMultiplier;
        }};

        largeSolarPanel = new SolarGenerator("solar-panel-large"){{
            size = 3;
            generation = 0.055f;
            generation *= powerAmountMultiplier;
        }};

        thoriumReactor = new NuclearReactor("thorium-reactor"){{
            size = 3;
            health = 700;
            powerMultiplier = 1.1f;
            powerMultiplier *= powerAmountMultiplier;
        }};

        fusionReactor = new FusionReactor("fusion-reactor"){{
            size = 4;
            health = 600;
        }};

        battery = new Battery("battery"){{
            powerCapacity = 320f;
        }};

        batteryLarge = new Battery("battery-large"){{
            size = 3;
            powerCapacity = 2000f;
        }};

        powerNode = new PowerNode("power-node"){{
            shadow = "shadow-round-1";
            maxNodes = 4;
            laserRange = 6;
        }};

        powerNodeLarge = new PowerNode("power-node-large"){{
            size = 2;
            maxNodes = 6;
            laserRange = 9.5f;
            shadow = "shadow-round-2";
        }};

        warpGate = new WarpGate("warp-gate");

    }
}
