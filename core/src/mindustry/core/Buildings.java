package mindustry.core;

import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.world.blocks.distribution.BufferedItemBridge.*;
import mindustry.world.blocks.distribution.Conveyor.*;
import mindustry.world.blocks.distribution.Duct.*;
import mindustry.world.blocks.distribution.ItemBridge.*;
import mindustry.world.blocks.distribution.Junction.*;
import mindustry.world.blocks.distribution.StackConveyor.*;
import mindustry.world.blocks.liquid.Conduit.*;
import mindustry.world.blocks.liquid.LiquidBridge.*;
import mindustry.world.blocks.liquid.LiquidRouter.*;
import mindustry.world.blocks.storage.Unloader.*;

@BuildingListDef(qualifiedType = "mindustry.gen.Building", method = "update")

@BuildingListDef(type = ConveyorBuild.class, method = "updateConveyor")
@BuildingListDef(type = DuctBuild.class, method = "updateDuct")
@BuildingListDef(type = JunctionBuild.class, method = "updateJunction")
@BuildingListDef(type = ItemBridgeBuild.class, method = "update") //TODO: has consume power, meaning updateConsumption needs to be called too (bad)
@BuildingListDef(type = BufferedItemBridgeBuild.class, method = "updateTile")
@BuildingListDef(type = StackConveyorBuild.class, method = "updateStackConveyor")
@BuildingListDef(type = UnloaderBuild.class, method = "updateUnloader")

@BuildingListDef(type = ConduitBuild.class, method = "updateConduit")
@BuildingListDef(type = LiquidRouterBuild.class, method = "updateLiquidRouter")
@BuildingListDef(type = LiquidBridgeBuild.class, method = "update")  //TODO: has consume power, meaning updateConsumption needs to be called too (bad)

//TODO: fix overdrive
//TODO: make enable/disable just remove them from the list of things that need to update
public class Buildings{
    public final BuildingList buildings = new BuildingList();

    public final ConveyorList conveyors = new ConveyorList();
    public final DuctList ducts = new DuctList();
    public final JunctionList junctions = new JunctionList();
    public final BufferedItemBridgeList bufferedItemBridges = new BufferedItemBridgeList();
    public final ItemBridgeList itemBridges = new ItemBridgeList();
    public final StackConveyorList stackConveyors = new StackConveyorList();
    public final UnloaderList unloaders = new UnloaderList();

    public final ConduitList conduits = new ConduitList();
    public final LiquidRouterList liquidRouters = new LiquidRouterList();
    public final LiquidBridgeList liquidBridges = new LiquidBridgeList();

    public void update(){

        var updateItems = Vars.mainExecutor.submit(() -> {
            conveyors.update();
            ducts.update();
            junctions.update();
            bufferedItemBridges.update();
            itemBridges.update();
            stackConveyors.update();
            unloaders.update();
        });

        var updateLiquids = Vars.mainExecutor.submit(() -> {
            conduits.update();
            liquidRouters.update();
            liquidBridges.update();
        });

        Threads.await(updateItems);
        Threads.await(updateLiquids);

        buildings.update();
    }

    public void clear(){
        //TODO: call clear on all the above
    }
}
