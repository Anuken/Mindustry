package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.defense.RepairTurret;
import io.anuke.mindustry.world.blocks.types.defense.ShieldBlock;
import io.anuke.mindustry.world.blocks.types.distribution.Teleporter;
import io.anuke.mindustry.world.blocks.types.power.*;

public class PowerBlocks {
    public static final Block

    coalgenerator = new BurnerGenerator("coalgenerator") {{
        //generateItem = Items.coal;
        powerOutput = 0.04f;
        powerCapacity = 40f;
    }},

    thermalgenerator = new LiquidHeatGenerator("thermalgenerator") {{
        maxLiquidGenerate = 0.5f;
        powerPerLiquid = 0.08f;
        powerCapacity = 40f;
        generateEffect = Fx.redgeneratespark;
    }},

    combustiongenerator = new LiquidBurnerGenerator("combustiongenerator") {{
        maxLiquidGenerate = 0.4f;
        powerPerLiquid = 0.12f;
        powerCapacity = 40f;
    }},

    rtgenerator = new BurnerGenerator("rtgenerator") {{
        //generateItem = Items.thorium;
        powerCapacity = 40f;
        powerOutput = 0.03f;
        itemDuration = 240f;
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
    }},

    batteryLarge = new PowerGenerator("batterylarge") {{
        size = 3;
        powerCapacity = 2000f;
    }},

    powernode = new PowerDistributor("powernode"),

    teleporter = new Teleporter("teleporter");
}
