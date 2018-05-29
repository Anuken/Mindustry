package io.anuke.mindustry.content.blocks;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.production.Cultivator;
import io.anuke.mindustry.world.blocks.types.production.Drill;
import io.anuke.mindustry.world.blocks.types.production.Fracker;
import io.anuke.mindustry.world.blocks.types.production.SolidPump;

public class ProductionBlocks implements ContentList {
    public static Block ironDrill, reinforcedDrill, steelDrill, titaniumDrill, laserdrill, nucleardrill, plasmadrill, waterextractor, oilextractor, cultivator;

    @Override
    public void load() {
        ironDrill = new Drill("irondrill") {{
            tier = 1;
            drillTime = 400;
        }};

        reinforcedDrill = new Drill("reinforceddrill") {{
            tier = 2;
            drillTime = 360;
        }};

        steelDrill = new Drill("steeldrill") {{
            tier = 3;
            drillTime = 320;
        }};

        titaniumDrill = new Drill("titaniumdrill") {{
            tier = 4;
            drillTime = 280;
        }};

        laserdrill = new Drill("laserdrill") {{
            drillTime = 220;
            size = 2;
            powerUse = 0.2f;
            hasPower = true;
            tier = 5;
            updateEffect = BlockFx.pulverizeMedium;
            drillEffect = BlockFx.mineBig;
        }};

        nucleardrill = new Drill("nucleardrill") {{
            drillTime = 160;
            size = 3;
            powerUse = 0.5f;
            drawRim = true;
            hasPower = true;
            tier = 5;
            updateEffect = BlockFx.pulverizeRed;
            updateEffectChance = 0.03f;
            drillEffect = BlockFx.mineHuge;
            rotateSpeed = 6f;
            warmupSpeed = 0.01f;
        }};

        plasmadrill = new Drill("plasmadrill") {{
            heatColor = Color.valueOf("ff461b");
            drillTime = 110;
            size = 4;
            powerUse = 0.7f;
            hasLiquids = true;
            hasPower = true;
            tier = 5;
            rotateSpeed = 9f;
            drawRim = true;
            updateEffect = BlockFx.pulverizeRedder;
            updateEffectChance = 0.04f;
            drillEffect = BlockFx.mineHuge;
            warmupSpeed = 0.005f;
        }};

        waterextractor = new SolidPump("waterextractor") {{
            result = Liquids.water;
            powerUse = 0.2f;
            pumpAmount = 0.1f;
            size = 2;
            liquidCapacity = 30f;
            rotateSpeed = 1.4f;
        }};

        oilextractor = new Fracker("oilextractor") {{
            result = Liquids.oil;
            inputLiquid = Liquids.water;
            updateEffect = BlockFx.pulverize;
            updateEffectChance = 0.05f;
            inputLiquidUse = 0.3f;
            powerUse = 0.6f;
            pumpAmount = 0.06f;
            size = 3;
            liquidCapacity = 30f;
        }};

        cultivator = new Cultivator("cultivator") {{
            result = Items.biomatter;
            inputLiquid = Liquids.water;
            liquidUse = 0.2f;
            drillTime = 260;
            size = 2;
            hasLiquids = true;
            hasPower = true;
        }};

    }
}
