package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.production.Cultivator;
import io.anuke.mindustry.world.blocks.types.production.Drill;
import io.anuke.mindustry.world.blocks.types.production.GenericDrill;
import io.anuke.mindustry.world.blocks.types.production.SolidPump;

public class ProductionBlocks {
    public static final Block

    stonedrill = new Drill("stonedrill") {{
        resource = Blocks.stone;
        result = Items.stone;
        drillTime = 240;
    }},

    irondrill = new Drill("irondrill") {{
        resource = Blocks.iron;
        result = Items.iron;
        drillTime = 360;
    }},

    leaddrill = new Drill("leaddrill") {{
        resource = Blocks.lead;
        result = Items.lead;
        drillTime = 400;
    }},

    coaldrill = new Drill("coaldrill") {{
        resource = Blocks.coal;
        result = Items.coal;
        drillTime = 420;
    }},

    thoriumdrill = new Drill("thoriumdrill") {{
        resource = Blocks.thorium;
        result = Items.thorium;
        drillTime = 600;
    }},

    titaniumdrill = new Drill("titaniumdrill") {{
        resource = Blocks.titanium;
        result = Items.titanium;
        drillTime = 540;
    }},

    laserdrill = new GenericDrill("laserdrill") {{
        drillTime = 200;
        size = 2;
        powerUse = 0.2f;
        hasPower = true;
    }},

    nucleardrill = new GenericDrill("nucleardrill") {{
        drillTime = 240;
        size = 3;
        powerUse = 0.32f;
        hasPower = true;
    }},

    plasmadrill = new GenericDrill("plasmadrill") {{
        inputLiquid = Liquids.plasma;
        drillTime = 240;
        size = 4;
        powerUse = 0.16f;
        hasLiquids = true;
        hasPower = true;
    }},

    quartzextractor = new GenericDrill("quartzextractor") {{
        powerUse = 0.1f;
        resource = Blocks.sand;
        result = Items.silicon;
        drillTime = 320;
        size = 2;
    }},

    waterextractor = new SolidPump("waterextractor") {{
        result = Liquids.water;
        powerUse = 0.1f;
        pumpAmount = 0.4f;
        size = 2;
        liquidCapacity = 30f;
    }},

    oilextractor = new SolidPump("oilextractor") {{
        result = Liquids.oil;
        powerUse = 0.5f;
        pumpAmount = 0.4f;
        size = 3;
        liquidCapacity = 80f;
    }},

    cultivator = new Cultivator("cultivator") {{
        resource = Blocks.grass;
        result = Items.biomatter;
        inputLiquid = Liquids.water;
        liquidUse = 0.1f;
        drillTime = 300;
        size = 2;
        hasLiquids = true;
        hasPower = true;
    }};
}
