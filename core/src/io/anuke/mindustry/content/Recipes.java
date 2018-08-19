package io.anuke.mindustry.content;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.blocks.DebugBlocks;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.Block;

import static io.anuke.mindustry.type.Category.units;

public class Recipes implements ContentList{

    @Override
    public void load(){
        //WALLS
        /*
        new Recipe(defense, DefenseBlocks.tungstenWall, new ItemStack(Items.tungsten, 12));
        new Recipe(defense, DefenseBlocks.tungstenWallLarge, new ItemStack(Items.tungsten, 12 * 4));

        new Recipe(defense, DefenseBlocks.carbideWall, new ItemStack(Items.carbide, 12));
        new Recipe(defense, DefenseBlocks.carbideWallLarge, new ItemStack(Items.carbide, 12 * 4));

        new Recipe(defense, DefenseBlocks.thoriumWall, new ItemStack(Items.thorium, 12));
        new Recipe(defense, DefenseBlocks.thoriumWallLarge, new ItemStack(Items.thorium, 12 * 4));

        new Recipe(defense, DefenseBlocks.phaseWall, new ItemStack(Items.phasematter, 12));
        new Recipe(defense, DefenseBlocks.phaseWallLarge, new ItemStack(Items.phasematter, 12 * 4));

        new Recipe(defense, DefenseBlocks.door, new ItemStack(Items.carbide, 12), new ItemStack(Items.silicon, 8));
        new Recipe(defense, DefenseBlocks.doorLarge, new ItemStack(Items.carbide, 12 * 4), new ItemStack(Items.silicon, 8 * 4));

        //TURRETS
        new Recipe(weapon, TurretBlocks.duo, new ItemStack(Items.tungsten, 40));
        new Recipe(weapon, TurretBlocks.scorch, new ItemStack(Items.tungsten, 50), new ItemStack(Items.carbide, 20));
        new Recipe(weapon, TurretBlocks.hail, new ItemStack(Items.tungsten, 60), new ItemStack(Items.carbide, 35));

        new Recipe(weapon, TurretBlocks.lancer, new ItemStack(Items.tungsten, 50), new ItemStack(Items.lead, 100), new ItemStack(Items.silicon, 90));
        new Recipe(weapon, TurretBlocks.wave, new ItemStack(Items.carbide, 60), new ItemStack(Items.titanium, 70), new ItemStack(Items.lead, 150));
        new Recipe(weapon, TurretBlocks.swarmer, new ItemStack(Items.carbide, 70), new ItemStack(Items.titanium, 70), new ItemStack(Items.plastanium, 90), new ItemStack(Items.silicon, 60));
        new Recipe(weapon, TurretBlocks.salvo, new ItemStack(Items.tungsten, 210), new ItemStack(Items.carbide, 190), new ItemStack(Items.thorium, 130));
        new Recipe(weapon, TurretBlocks.ripple, new ItemStack(Items.tungsten, 300), new ItemStack(Items.carbide, 220), new ItemStack(Items.thorium, 120));

        //DISTRIBUTION
        new Recipe(distribution, DistributionBlocks.conveyor, new ItemStack(Items.lead, 1));
        new Recipe(distribution, DistributionBlocks.titaniumconveyor, new ItemStack(Items.lead, 2), new ItemStack(Items.titanium, 1));
        new Recipe(distribution, DistributionBlocks.phaseConveyor, new ItemStack(Items.phasematter, 10), new ItemStack(Items.silicon, 15), new ItemStack(Items.lead, 20), new ItemStack(Items.carbide, 20));

        //starter lead transporation
        new Recipe(distribution, DistributionBlocks.junction, new ItemStack(Items.lead, 2));
        new Recipe(distribution, DistributionBlocks.router, new ItemStack(Items.lead, 6));

        //advanced carbide transporation
        new Recipe(distribution, DistributionBlocks.distributor, new ItemStack(Items.carbide, 8), new ItemStack(Items.tungsten, 8));
        new Recipe(distribution, DistributionBlocks.sorter, new ItemStack(Items.carbide, 4), new ItemStack(Items.tungsten, 4));
        new Recipe(distribution, DistributionBlocks.overflowGate, new ItemStack(Items.carbide, 4), new ItemStack(Items.tungsten, 8));
        new Recipe(distribution, DistributionBlocks.bridgeConveyor, new ItemStack(Items.carbide, 8), new ItemStack(Items.tungsten, 8));
        new Recipe(distribution, DistributionBlocks.massDriver, new ItemStack(Items.carbide, 400), new ItemStack(Items.silicon, 300), new ItemStack(Items.lead, 400), new ItemStack(Items.thorium, 250));

        //CRAFTING

        //smelting
        new Recipe(crafting, CraftingBlocks.smelter, new ItemStack(Items.tungsten, 70));
        new Recipe(crafting, CraftingBlocks.arcsmelter, new ItemStack(Items.tungsten, 90), new ItemStack(Items.carbide, 60), new ItemStack(Items.lead, 50));
        new Recipe(crafting, CraftingBlocks.siliconsmelter, new ItemStack(Items.tungsten, 60), new ItemStack(Items.lead, 50));

        //advanced fabrication
        new Recipe(crafting, CraftingBlocks.plastaniumCompressor, new ItemStack(Items.silicon, 160), new ItemStack(Items.lead, 230), new ItemStack(Items.carbide, 120), new ItemStack(Items.titanium, 160));
        new Recipe(crafting, CraftingBlocks.phaseWeaver, new ItemStack(Items.silicon, 260), new ItemStack(Items.lead, 240), new ItemStack(Items.thorium, 150));

        //TODO implement alloy smelter
        //new Recipe(crafting, CraftingBlocks.alloySmelter, new ItemStack(Items.silicon, 160), new ItemStack(Items.lead, 160), new ItemStack(Items.thorium, 140));

        //misc
        new Recipe(crafting, CraftingBlocks.pulverizer, new ItemStack(Items.tungsten, 60), new ItemStack(Items.lead, 50));
        new Recipe(crafting, CraftingBlocks.pyratiteMixer, new ItemStack(Items.tungsten, 100), new ItemStack(Items.lead, 50));
        new Recipe(crafting, CraftingBlocks.blastMixer, new ItemStack(Items.lead, 60), new ItemStack(Items.carbide, 40));
        new Recipe(crafting, CraftingBlocks.cryofluidmixer, new ItemStack(Items.lead, 130), new ItemStack(Items.silicon, 80), new ItemStack(Items.thorium, 90));

        new Recipe(crafting, CraftingBlocks.solidifier, new ItemStack(Items.carbide, 30), new ItemStack(Items.tungsten, 20));
        new Recipe(crafting, CraftingBlocks.melter, new ItemStack(Items.tungsten, 60), new ItemStack(Items.lead, 70), new ItemStack(Items.carbide, 90));
        new Recipe(crafting, CraftingBlocks.incinerator, new ItemStack(Items.carbide, 10), new ItemStack(Items.lead, 30));

        //processing
        new Recipe(crafting, CraftingBlocks.biomatterCompressor, new ItemStack(Items.lead, 70), new ItemStack(Items.silicon, 60));
        new Recipe(crafting, CraftingBlocks.separator, new ItemStack(Items.tungsten, 60), new ItemStack(Items.carbide, 50));
        new Recipe(crafting, CraftingBlocks.centrifuge, new ItemStack(Items.tungsten, 130), new ItemStack(Items.carbide, 130), new ItemStack(Items.silicon, 60), new ItemStack(Items.titanium, 50));

        //POWER
        new Recipe(power, PowerBlocks.powerNode, new ItemStack(Items.tungsten, 2), new ItemStack(Items.lead, 6))
                .setDependencies(PowerBlocks.combustionGenerator);
        new Recipe(power, PowerBlocks.powerNodeLarge, new ItemStack(Items.carbide, 10), new ItemStack(Items.lead, 20), new ItemStack(Items.silicon, 6))
                .setDependencies(PowerBlocks.powerNode);
        new Recipe(power, PowerBlocks.battery, new ItemStack(Items.tungsten, 8), new ItemStack(Items.lead, 30), new ItemStack(Items.silicon, 4))
                .setDependencies(PowerBlocks.powerNode);
        new Recipe(power, PowerBlocks.batteryLarge, new ItemStack(Items.carbide, 40), new ItemStack(Items.lead, 80), new ItemStack(Items.silicon, 30))
                .setDependencies(PowerBlocks.powerNode);

        //generators - combustion
        new Recipe(power, PowerBlocks.combustionGenerator, new ItemStack(Items.tungsten, 50), new ItemStack(Items.lead, 30));
        new Recipe(power, PowerBlocks.turbineGenerator, new ItemStack(Items.tungsten, 70), new ItemStack(Items.carbide, 50), new ItemStack(Items.lead, 80), new ItemStack(Items.silicon, 60));
        new Recipe(power, PowerBlocks.thermalGenerator, new ItemStack(Items.tungsten, 80), new ItemStack(Items.carbide, 70), new ItemStack(Items.lead, 100), new ItemStack(Items.silicon, 70), new ItemStack(Items.thorium, 70));

        //generators - solar
        new Recipe(power, PowerBlocks.solarPanel, new ItemStack(Items.lead, 20), new ItemStack(Items.silicon, 30));
        new Recipe(power, PowerBlocks.largeSolarPanel, new ItemStack(Items.lead, 200), new ItemStack(Items.silicon, 290), new ItemStack(Items.phasematter, 30));

        //generators - other
        new Recipe(power, PowerBlocks.nuclearReactor, new ItemStack(Items.lead, 600), new ItemStack(Items.silicon, 400), new ItemStack(Items.carbide, 300), new ItemStack(Items.thorium, 300));

        //new Recipe(distribution, StorageBlocks.core, new ItemStack(Items.carbide, 50));
        new Recipe(distribution, StorageBlocks.unloader, new ItemStack(Items.carbide, 40), new ItemStack(Items.silicon, 50));
        new Recipe(distribution, StorageBlocks.vault, new ItemStack(Items.carbide, 500), new ItemStack(Items.thorium, 350));

        //DRILLS, PRODUCERS
        new Recipe(production, ProductionBlocks.tungstenDrill, new ItemStack(Items.tungsten, 50));
        new Recipe(production, ProductionBlocks.carbideDrill, new ItemStack(Items.tungsten, 60), new ItemStack(Items.carbide, 50));
        new Recipe(production, ProductionBlocks.laserdrill, new ItemStack(Items.tungsten, 70), new ItemStack(Items.carbide, 90), new ItemStack(Items.silicon, 60), new ItemStack(Items.titanium, 50));
        new Recipe(production, ProductionBlocks.blastdrill, new ItemStack(Items.tungsten, 130), new ItemStack(Items.carbide, 180), new ItemStack(Items.silicon, 120), new ItemStack(Items.titanium, 100), new ItemStack(Items.thorium, 60));

        new Recipe(production, ProductionBlocks.waterextractor, new ItemStack(Items.tungsten, 50), new ItemStack(Items.carbide, 50), new ItemStack(Items.lead, 40));
        new Recipe(production, ProductionBlocks.cultivator, new ItemStack(Items.tungsten, 20), new ItemStack(Items.lead, 50), new ItemStack(Items.silicon, 20));
        new Recipe(production, ProductionBlocks.oilextractor, new ItemStack(Items.tungsten, 300), new ItemStack(Items.carbide, 350), new ItemStack(Items.lead, 230), new ItemStack(Items.thorium, 230), new ItemStack(Items.silicon, 150));

        //UNITS

        //bodies
        new Recipe(units, UpgradeBlocks.dartFactory, new ItemStack(Items.lead, 150), new ItemStack(Items.silicon, 200), new ItemStack(Items.titanium, 240)).setDesktop(); //dart is desktop only, because it's the starter mobile ship
        new Recipe(units, UpgradeBlocks.javelinFactory, new ItemStack(Items.lead, 200), new ItemStack(Items.silicon, 250), new ItemStack(Items.titanium, 300), new ItemStack(Items.plastanium, 200));
        new Recipe(units, UpgradeBlocks.deltaFactory, new ItemStack(Items.carbide, 160), new ItemStack(Items.silicon, 220), new ItemStack(Items.titanium, 250)).setDesktop();

        //new Recipe(units, UpgradeBlocks.deltaFactory, new ItemStack(Items.tungsten, 30), new ItemStack(Items.lead, 50), new ItemStack(Items.silicon, 30));

        //actual unit related stuff
        new Recipe(units, UnitBlocks.dronePad, new ItemStack(Items.tungsten, 70), new ItemStack(Items.lead, 110), new ItemStack(Items.silicon, 130));
        new Recipe(units, UnitBlocks.fabricatorPad, new ItemStack(Items.carbide, 90), new ItemStack(Items.thorium, 80), new ItemStack(Items.lead, 110), new ItemStack(Items.silicon, 210));

        new Recipe(units, UnitBlocks.daggerPad, new ItemStack(Items.lead, 90), new ItemStack(Items.silicon, 80)).setMode(GameMode.noWaves);
        new Recipe(units, UnitBlocks.titanPad, new ItemStack(Items.thorium, 90), new ItemStack(Items.lead, 140), new ItemStack(Items.silicon, 90)).setMode(GameMode.noWaves);

        new Recipe(units, UnitBlocks.interceptorPad, new ItemStack(Items.titanium, 60), new ItemStack(Items.lead, 80), new ItemStack(Items.silicon, 90)).setMode(GameMode.noWaves);
        new Recipe(units, UnitBlocks.monsoonPad, new ItemStack(Items.plastanium, 80), new ItemStack(Items.titanium, 100), new ItemStack(Items.lead, 130), new ItemStack(Items.silicon, 220)).setMode(GameMode.noWaves);

        new Recipe(units, UnitBlocks.repairPoint, new ItemStack(Items.lead, 30), new ItemStack(Items.tungsten, 30), new ItemStack(Items.silicon, 30));
        new Recipe(units, UnitBlocks.commandCenter, new ItemStack(Items.lead, 100), new ItemStack(Items.carbide, 100), new ItemStack(Items.silicon, 200)).setMode(GameMode.noWaves);

        //LIQUIDS
        new Recipe(liquid, LiquidBlocks.conduit, new ItemStack(Items.lead, 1)).setDependencies(CraftingBlocks.smelter);
        new Recipe(liquid, LiquidBlocks.pulseConduit, new ItemStack(Items.titanium, 1), new ItemStack(Items.lead, 1));
        new Recipe(liquid, LiquidBlocks.phaseConduit, new ItemStack(Items.phasematter, 10), new ItemStack(Items.silicon, 15), new ItemStack(Items.lead, 20), new ItemStack(Items.titanium, 20));

        new Recipe(liquid, LiquidBlocks.liquidRouter, new ItemStack(Items.carbide, 4), new ItemStack(Items.lead, 4));
        new Recipe(liquid, LiquidBlocks.liquidtank, new ItemStack(Items.titanium, 50), new ItemStack(Items.lead, 50), new ItemStack(Items.carbide, 20));
        new Recipe(liquid, LiquidBlocks.liquidJunction, new ItemStack(Items.carbide, 4), new ItemStack(Items.lead, 4));
        new Recipe(liquid, LiquidBlocks.bridgeConduit, new ItemStack(Items.carbide, 8), new ItemStack(Items.lead, 8));

        new Recipe(liquid, LiquidBlocks.mechanicalPump, new ItemStack(Items.tungsten, 30), new ItemStack(Items.lead, 20)).setDependencies(CraftingBlocks.smelter);
        new Recipe(liquid, LiquidBlocks.rotaryPump, new ItemStack(Items.tungsten, 140), new ItemStack(Items.lead, 100), new ItemStack(Items.silicon, 40), new ItemStack(Items.titanium, 70));
        new Recipe(liquid, LiquidBlocks.thermalPump, new ItemStack(Items.tungsten, 160), new ItemStack(Items.lead, 130), new ItemStack(Items.silicon, 60), new ItemStack(Items.titanium, 80), new ItemStack(Items.thorium, 70));

        //DEBUG
        new Recipe(units, DebugBlocks.itemSource, new ItemStack(Items.carbide, 10)).setDebug();
        new Recipe(units, DebugBlocks.itemVoid, new ItemStack(Items.carbide, 10)).setDebug();
        new Recipe(units, DebugBlocks.liquidSource, new ItemStack(Items.carbide, 10)).setDebug();
        new Recipe(units, DebugBlocks.powerVoid, new ItemStack(Items.carbide, 10)).setDebug();
        new Recipe(units, DebugBlocks.powerInfinite, new ItemStack(Items.carbide, 10), new ItemStack(Items.surgealloy, 5)).setDebug();*/

        init(units, DebugBlocks.itemSource, Items.carbide, 10);
    }

    static void init(Object... objects){
        Category cat = null;
        Block block = null;
        ItemStack stack = null;
        Array<ItemStack> arr = new Array<>();
        for(Object object : objects){
            if(object instanceof Category){
                if(stack != null) throw new RuntimeException("Finish defining stack before beginning category");
                if(block != null){
                    new Recipe(cat, block, arr.toArray(ItemStack.class));
                    block = null;
                    stack = null;
                    arr.clear();
                }
                cat = (Category) object;
            }else if(object instanceof Block){
                if(cat == null) throw new RuntimeException("Finish defining category before beginning blocks");
                if(block != null){
                    new Recipe(cat, block, arr.toArray(ItemStack.class));
                    stack = null;
                    arr.clear();
                }
                block = (Block) object;
                arr.clear();
            }else if(object instanceof Item){
                if(block == null) throw new RuntimeException("Finish defining block before defining item");
                if(stack != null) throw new RuntimeException("Finish defining stack before defining another stack");
                stack = new ItemStack((Item) object, 1);
            }else if(object instanceof Integer){
                if(stack == null) throw new RuntimeException("Finish defining item before defining item amount");
                stack.amount = (Integer) object;
                arr.add(stack);
                stack = null;
            }
        }
    }

    @Override
    public Array<? extends Content> getAll(){
        return Recipe.all();
    }
}
