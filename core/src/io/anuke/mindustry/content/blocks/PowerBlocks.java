package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.defense.RepairTurret;
import io.anuke.mindustry.world.blocks.types.defense.ShieldBlock;
import io.anuke.mindustry.world.blocks.types.distribution.Teleporter;
import io.anuke.mindustry.world.blocks.types.power.ItemPowerGenerator;

public class PowerBlocks {
    public static final Block

    coalgenerator = new io.anuke.mindustry.world.blocks.types.power.ItemPowerGenerator("coalgenerator") {{
        generateItem = Items.coal;
        powerOutput = 0.04f;
        powerCapacity = 40f;
    }},

    thermalgenerator = new io.anuke.mindustry.world.blocks.types.power.LiquidPowerGenerator("thermalgenerator") {{
        generateLiquid = Liquids.lava;
        maxLiquidGenerate = 0.5f;
        powerPerLiquid = 0.08f;
        powerCapacity = 40f;
        generateEffect = Fx.redgeneratespark;
    }},

    combustiongenerator = new io.anuke.mindustry.world.blocks.types.power.LiquidPowerGenerator("combustiongenerator") {{
        generateLiquid = Liquids.oil;
        maxLiquidGenerate = 0.4f;
        powerPerLiquid = 0.12f;
        powerCapacity = 40f;
    }},

    rtgenerator = new ItemPowerGenerator("rtgenerator") {{
        generateItem = Items.thorium;
        powerCapacity = 40f;
        powerOutput = 0.03f;
        itemDuration = 240f;
    }},

    solarpanel = new io.anuke.mindustry.world.blocks.types.power.SolarGenerator("solarpanel") {{
        generation = 0.003f;
    }},

    largesolarpanel = new io.anuke.mindustry.world.blocks.types.power.SolarGenerator("largesolarpanel") {{
        size = 3;
        generation = 0.012f;
    }},

    nuclearReactor = new io.anuke.mindustry.world.blocks.types.power.NuclearReactor("nuclearreactor") {{
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

    battery = new io.anuke.mindustry.world.blocks.types.power.PowerGenerator("battery") {{
        powerCapacity = 320f;
    }},

    batteryLarge = new io.anuke.mindustry.world.blocks.types.power.PowerGenerator("batterylarge") {{
        size = 3;
        powerCapacity = 2000f;
    }},

    powernode = new io.anuke.mindustry.world.blocks.types.power.PowerDistributor("powernode"),

    teleporter = new Teleporter("teleporter");
}
