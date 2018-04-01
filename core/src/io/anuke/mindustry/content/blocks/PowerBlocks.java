package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.graphics.fx.BlockFx;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.defense.RepairTurret;
import io.anuke.mindustry.world.blocks.types.defense.ShieldBlock;
import io.anuke.mindustry.world.blocks.types.distribution.Teleporter;
import io.anuke.mindustry.world.blocks.types.power.*;

public class PowerBlocks {
    public static final Block

    combustiongenerator = new BurnerGenerator("combustiongenerator") {{
        powerOutput = 0.04f;
        powerCapacity = 40f;
    }},

    thermalgenerator = new LiquidHeatGenerator("thermalgenerator") {{
        maxLiquidGenerate = 0.5f;
        powerPerLiquid = 0.08f;
        powerCapacity = 40f;
        generateEffect = BlockFx.redgeneratespark;
    }},

    liquidcombustiongenerator = new LiquidBurnerGenerator("liquidcombustiongenerator") {{
        maxLiquidGenerate = 0.4f;
        powerPerLiquid = 0.12f;
        powerCapacity = 40f;
    }},

    rtgenerator = new DecayGenerator("rtgenerator") {{
        powerCapacity = 40f;
        powerOutput = 0.02f;
        itemDuration = 500f;
    }},

    solarpanel = new SolarGenerator("solarpanel") {{
        generation = 0.003f;
    }},

    largesolarpanel = new SolarGenerator("largesolarpanel") {{
        size = 3;
        generation = 0.012f;
    }},

    nuclearReactor = new NuclearReactor("nuclearreactor") {{
        size = 3;
        health = 600;
        breaktime *= 2.3f;
    }},

    fusionReactor = new FusionReactor("fusionreactor") {{
        size = 4;
        health = 600;
        breaktime *= 4f;
    }},

    repairturret = new RepairTurret("repairturret") {{
        range = 30;
        reload = 20f;
        health = 60;
        powerUsed = 0.08f;
    }},

    megarepairturret = new RepairTurret("megarepairturret") {{
        range = 44;
        reload = 12f;
        health = 90;
        powerUsed = 0.13f;
        size = 2;
    }},

    shieldgenerator = new ShieldBlock("shieldgenerator") {{
        health = 400;
    }},

    battery = new PowerGenerator("battery") {{
        powerCapacity = 320f;
        hasInventory = false;
    }},

    batteryLarge = new PowerGenerator("batterylarge") {{
        size = 3;
        powerCapacity = 2000f;
        hasInventory = false;
    }},

    powernode = new PowerDistributor("powernode"),

    powernodelarge = new PowerDistributor("powernodelarge"){{
        size = 2;
        powerSpeed = 1f;
        maxNodes = 5;
        laserRange = 7.5f;
    }},

    teleporter = new Teleporter("teleporter");
}
