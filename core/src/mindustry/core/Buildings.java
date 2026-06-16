package mindustry.core;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.distribution.BufferedItemBridge.*;
import mindustry.world.blocks.distribution.Conveyor.*;
import mindustry.world.blocks.distribution.Duct.*;
import mindustry.world.blocks.distribution.ItemBridge.*;
import mindustry.world.blocks.distribution.Junction.*;
import mindustry.world.blocks.distribution.StackConveyor.*;
import mindustry.world.blocks.liquid.Conduit.*;
import mindustry.world.blocks.liquid.LiquidBridge.*;
import mindustry.world.blocks.liquid.LiquidRouter.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.storage.Unloader.*;

import java.util.concurrent.*;

import static mindustry.Vars.*;

@BuildingListDef(qualifiedType = "mindustry.gen.Building", method = "updateTile")

@BuildingListDef(type = PowerGraph.class, method = "update")
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

/*
TODO: make enable/disable just remove them from the list of things that need to update
- subpoint: remove all contents of update() as they would no longer be needed

TODO: remove the 'devirtualized' methods and see if it affects performance at all (check weaker devices too)

TODO:
 - refactor checkAllowUpdate in World to not access Groups.build
 - refactor fog scanning Groups.build (2 different places) and move that into a new thread
 - finally, remove it from the group list def (footgun avoidance)
*
 */
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

    public final PowerGraphList powerGraphs = new PowerGraphList();

    private final Seq<Building> timeScaleBuilds = new Seq<>(false, 20, Building.class);
    private final Seq<Building> timeScaleQueue = new Seq<>(false, 20, Building.class);
    private final Seq<Building> ambientSoundBuilds = new Seq<>(false, 20, Building.class);
    private final Seq<Building> ambientSoundQueue = new Seq<>(false, 20, Building.class);
    private final Seq<LiquidUpdater> liquidUpdateBuilds = new Seq<>(false, 20, LiquidUpdater.class);

    private final Seq<Future<?>> consFutures = new Seq<>();

    public void update(){

        timeScaleBuilds.addAll(timeScaleQueue);
        timeScaleQueue.clear();

        ambientSoundBuilds.addAll(ambientSoundQueue);
        ambientSoundQueue.clear();

        Future<?> updateSound = null;

        if(!headless){
            updateSound = mainExecutor.submit(() -> {
                Building[] items = ambientSoundBuilds.items;
                int len = ambientSoundBuilds.size;
                for(int i = 0; i < len; i++){
                    var build = items[i];

                    if(!build.isValid()){
                        ambientSoundBuilds.remove(i);
                        i --;
                        len --;
                        continue;
                    }

                    if(build.shouldAmbientSound()){
                        control.sound.loop(build.block.ambientSound, build, build.block.ambientSoundVolume * build.ambientVolume());
                    }
                }
            });
        }

        float delta = Time.delta;
        var updateTimeScale = Vars.mainExecutor.submit(() -> {
            Building[] items = timeScaleBuilds.items;
            int len = timeScaleBuilds.size;
            for(int i = 0; i < len; i++){
                var build = items[i];
                if((build.timeScaleDuration -= delta) <= 0f){
                    build.timeScale = 1f;
                    build.hadTimeScale = false;
                    timeScaleBuilds.remove(i);
                    len --;
                    i --;
                }
            }
        });

        var updatePower = mainExecutor.submit(() -> {
            PerfCounter.powerUpdate.begin();
            powerGraphs.update();
            PerfCounter.powerUpdate.end();
        });

        var updateItems = Vars.mainExecutor.submit(() -> {
            PerfCounter.itemsUpdate.begin();
            conveyors.update();
            ducts.update();
            junctions.update();
            bufferedItemBridges.update();
            itemBridges.update();
            stackConveyors.update();
            unloaders.update();
            PerfCounter.itemsUpdate.end();
        });

        var updateLiquids = Vars.mainExecutor.submit(() -> {
            PerfCounter.liquidsUpdate.begin();
            conduits.update();
            liquidRouters.update();
            liquidBridges.update();

            {
                LiquidUpdater[] items = liquidUpdateBuilds.items;
                int len = liquidUpdateBuilds.size;
                for(int i = 0; i < len; i++){
                    var build = items[i];
                    if(!build.isAdded()){
                        liquidUpdateBuilds.remove(i);
                        len --;
                        i --;
                    }else{
                        build.processLiquids();
                    }
                }
            }

            PerfCounter.liquidsUpdate.end();
        });

        Threads.await(updateItems);
        Threads.await(updateLiquids);
        Threads.await(updateTimeScale);
        Threads.await(updatePower);

        {
            PerfCounter.consumeUpdate.begin();

            consFutures.clear();
            int chunks = Mathf.clamp(buildings.size / 400, 1, OS.cores);
            int chunkSize = Math.max(buildings.size / chunks, 1);

            Building[] items = buildings.values;
            for(int i = 0; i < buildings.size; i += chunkSize){
                int from = i;
                int to = Math.min(buildings.size, i + chunkSize);

                consFutures.add(mainExecutor.submit(() -> {
                    for(int index = from; index < to; index++){
                        items[index].updateConsumption();
                    }
                }));
            }

            Threads.awaitAll(consFutures);
            PerfCounter.consumeUpdate.end();
        }

        if(updateSound != null){
            Threads.await(updateSound);
        }

        PerfCounter.otherBuildingsUpdate.begin();
        buildings.update();
        PerfCounter.otherBuildingsUpdate.end();
    }

    public void addLiquidUpdater(LiquidUpdater build){
        liquidUpdateBuilds.add(build);
    }

    public void addAmbientSound(Building build){
        if(headless) return;

        ambientSoundQueue.add(build);
    }

    public void addTimeScaled(Building build){
        build.hadTimeScale = true;
        timeScaleQueue.add(build);
    }

    public void clear(){
        //TODO: call clear on all the above
    }
}
