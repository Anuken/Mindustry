package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.production.*;

public class ProductionBlocks {
    public static final Block

    smelter = new Smelter("smelter") {{
        health = 70;
        inputs = new Item[]{Items.iron};
        fuel = Items.coal;
        result = Items.steel;
        craftTime = 25f;
    }},

    alloysmelter = new Smelter("alloysmelter") {{
        health = 90;
        inputs = new Item[]{Items.titanium, Items.steel};
        fuel = Items.coal;
        result = Items.densealloy;
        burnDuration = 45f;
        craftTime = 25f;
    }},

    powersmelter = new PowerSmelter("powersmelter") {{
        /*
        health = 90;
        inputs = new Item[]{Item.titanium, Item.steel};
        fuel = Item.coal;
        results = Item.dirium;
        burnDuration = 45f;
        craftTime = 25f;
        size = 2;*/
    }},

    cryofluidmixer = new LiquidMixer("cryofluidmixer") {{
        health = 200;
        inputLiquid = Liquids.water;
        outputLiquid = Liquids.cryofluid;
        inputItem = Items.titanium;
        liquidPerItem = 50f;
        itemCapacity = 50;
        powerUse = 0.1f;
        size = 2;
    }},

    coalextractor = new LiquidCrafter("coalextractor") {{
        input = Items.stone;
        inputAmount = 6;
        inputLiquid = Liquids.water;
        liquidAmount = 19f;
        output = Items.coal;
        health = 50;
        purifyTime = 50;
        health = 60;
    }},

    titaniumextractor = new LiquidCrafter("titaniumextractor") {{
        input = Items.stone;
        inputAmount = 8;
        inputLiquid = Liquids.water;
        liquidAmount = 40f;
        liquidCapacity = 41f;
        purifyTime = 60;
        output = Items.titanium;
        health = 70;
    }},

    oilrefinery = new LiquidCrafter("oilrefinery") {{
        inputLiquid = Liquids.oil;
        liquidAmount = 55f;
        liquidCapacity = 56f;
        purifyTime = 65;
        output = Items.coal;
        health = 80;
        craftEffect = Fx.purifyoil;
    }},

    stoneformer = new LiquidCrafter("stoneformer") {{
        input = null;
        inputLiquid = Liquids.lava;
        liquidAmount = 16f;
        liquidCapacity = 21f;
        purifyTime = 12;
        output = Items.stone;
        health = 80;
        craftEffect = Fx.purifystone;
    }},

    lavasmelter = new LiquidCrafter("lavasmelter") {{
        input = Items.iron;
        inputAmount = 1;
        inputLiquid = Liquids.lava;
        liquidAmount = 40f;
        liquidCapacity = 41f;
        purifyTime = 30;
        output = Items.steel;
        health = 80;
        craftEffect = Fx.purifystone;
    }},

    siliconextractor = new LiquidCrafter("siliconextractor") {{
        input = Items.stone;
        inputAmount = 5;
        inputLiquid = Liquids.water;
        liquidAmount = 18.99f;
        output = Items.silicon;
        health = 50;
        purifyTime = 50;
    }},

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

    cultivator = new GenericDrill("cultivator") {{
        resource = Blocks.grass;
        result = Items.biomatter;
        inputLiquid = Liquids.water;
        liquidUse = 0.1f;
        drillTime = 300;
        size = 2;
        hasLiquids = true;
        hasPower = true;
    }},

    weaponFactory = new WeaponFactory("weaponfactory") {{
        size = 2;
        health = 250;
    }};
}
