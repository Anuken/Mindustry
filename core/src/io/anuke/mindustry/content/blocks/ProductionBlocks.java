package io.anuke.mindustry.content.blocks;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.production.Cultivator;
import io.anuke.mindustry.world.blocks.production.Drill;
import io.anuke.mindustry.world.blocks.production.Fracker;
import io.anuke.mindustry.world.blocks.production.SolidPump;

public class ProductionBlocks extends BlockList implements ContentList{
    public static Block mechanicalDrill, pneumaticDrill, laserDrill, blastDrill, plasmaDrill, waterExtractor, oilExtractor, cultivator;

    @Override
    public void load(){
        mechanicalDrill = new Drill("mechanical-drill"){{
            tier = 2;
            drillTime = 300;
            size = 2;
            drawMineItem = true;
        }};

        pneumaticDrill = new Drill("pneumatic-drill"){{
            tier = 3;
            drillTime = 240;
            size = 2;
            drawMineItem = true;
        }};

        laserDrill = new Drill("laser-drill"){{
            drillTime = 140;
            size = 2;
            hasPower = true;
            tier = 4;
            updateEffect = BlockFx.pulverizeMedium;
            drillEffect = BlockFx.mineBig;

            consumes.powerDirect(0.11f);
        }};

        blastDrill = new Drill("blast-drill"){{
            drillTime = 60;
            size = 3;
            drawRim = true;
            hasPower = true;
            tier = 5;
            updateEffect = BlockFx.pulverizeRed;
            updateEffectChance = 0.03f;
            drillEffect = BlockFx.mineHuge;
            rotateSpeed = 6f;
            warmupSpeed = 0.01f;

            consumes.powerDirect(0.3f);
        }};

        plasmaDrill = new Drill("plasma-drill"){{
            heatColor = Color.valueOf("ff461b");
            drillTime = 50;
            size = 4;
            hasLiquids = true;
            hasPower = true;
            tier = 5;
            rotateSpeed = 9f;
            drawRim = true;
            updateEffect = BlockFx.pulverizeRedder;
            updateEffectChance = 0.04f;
            drillEffect = BlockFx.mineHuge;
            warmupSpeed = 0.005f;

            consumes.powerDirect(0.7f);
        }};

        waterExtractor = new SolidPump("water-extractor"){{
            result = Liquids.water;
            pumpAmount = 0.065f;
            size = 2;
            liquidCapacity = 30f;
            rotateSpeed = 1.4f;

            consumes.powerDirect(0.09f);
        }};

        oilExtractor = new Fracker("oil-extractor"){{
            result = Liquids.oil;
            updateEffect = BlockFx.pulverize;
            liquidCapacity = 50f;
            updateEffectChance = 0.05f;
            pumpAmount = 0.09f;
            size = 3;
            liquidCapacity = 30f;

            consumes.item(Items.sand);
            consumes.powerDirect(0.3f);
            consumes.liquid(Liquids.water, 0.15f);
        }};

        cultivator = new Cultivator("cultivator"){{
            result = Items.biomatter;
            drillTime = 260;
            size = 2;
            hasLiquids = true;
            hasPower = true;

            consumes.powerDirect(0.08f);
            consumes.liquid(Liquids.water, 0.2f);
        }};

    }
}
