package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.power.*;

public class PowerBlocks extends BlockList implements ContentList{
    public static Block combustionGenerator, thermalGenerator, turbineGenerator, rtgGenerator, solarPanel, largeSolarPanel,
            thoriumReactor, fusionReactor, battery, batteryLarge, powerNode, powerNodeLarge;

    @Override
    public void load(){
        combustionGenerator = new BurnerGenerator("combustion-generator"){{
            powerProduction = 0.09f;
            itemDuration = 40f;
        }};

        thermalGenerator = new LiquidHeatGenerator("thermal-generator"){{
            maxLiquidGenerate = 4f;
            // TODO: Balance
            powerProduction = 0.17f;
            liquidPowerMultiplier = 0.1f;
            generateEffect = BlockFx.redgeneratespark;
            size = 2;
        }};

        turbineGenerator = new TurbineGenerator("turbine-generator"){{
            // TODO: Balance
            powerProduction = 0.28f;
            liquidPowerMultiplier = 0.3f;
            itemDuration = 30f;
            consumes.liquid(Liquids.water, 0.05f);
            size = 2;
        }};

        rtgGenerator = new DecayGenerator("rtg-generator"){{
            size = 2;
            powerProduction = 0.3f;
            itemDuration = 220f;
        }};

        solarPanel = new SolarGenerator("solar-panel"){{
            powerProduction = 0.0045f;
        }};

        largeSolarPanel = new PowerGenerator("solar-panel-large"){{
            powerProduction = 0.055f;
        }};

        thoriumReactor = new NuclearReactor("thorium-reactor"){{
            size = 3;
            health = 700;
            powerMultiplier = 1.1f;
        }};

        fusionReactor = new FusionReactor("fusion-reactor"){{
            size = 4;
            health = 600;
        }};

        battery = new Battery("battery"){{
            consumes.powerBuffered(320f, 120f);
        }};

        batteryLarge = new Battery("battery-large"){{
            size = 3;
            consumes.powerBuffered(2000f, 600f);
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

    }
}
