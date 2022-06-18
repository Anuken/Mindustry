package mindustry.world.blocks.storage;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

import java.util.*;

import static mindustry.Vars.*;

public class Unloader extends Block{
    public @Load(value = "@-center", fallback = "unloader-center") TextureRegion centerRegion;

    public float speed = 1f;

    public Unloader(String name){
        super(name);
        update = true;
        solid = true;
        health = 70;
        hasItems = true;
        configurable = true;
        saveConfig = true;
        itemCapacity = 0;
        noUpdateDisabled = true;
        clearOnDoubleTap = true;
        unloadable = false;

        config(Item.class, (UnloaderBuild tile, Item item) -> tile.sortItem = item);
        configClear((UnloaderBuild tile) -> tile.sortItem = null);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.speed, 60f / speed, StatUnit.itemsSecond);
    }

    @Override
    public void drawPlanConfig(BuildPlan plan, Eachable<BuildPlan> list){
        drawPlanConfigCenter(plan, plan.config, "unloader-center");
    }

    @Override
    public void setBars(){
        super.setBars();
        removeBar("items");
    }

    public static class ContainerStat{
        Building building;
        float loadFactor;
        boolean canLoad;
        boolean canUnload;
        int index;

        @Override
        public String toString(){
            return "ContainerStat{" +
            "building=" + building.block + "#" + building.id +
            ", loadFactor=" + loadFactor +
            ", canLoad=" + canLoad +
            ", canUnload=" + canUnload +
            ", index=" + index +
            '}';
        }
    }

    public class UnloaderBuild extends Building{
        public float unloadTimer = 0f;
        public Item sortItem = null;
        public int offset = 0;
        public int rotations = 0;
        public Seq<ContainerStat> possibleBlocks = new Seq<>();
        public int[] lastUsed;

        protected final Comparator<ContainerStat> comparator = Structs.comps(
            //sort so it gives priority for blocks that can only either recieve or give (not both), and then by load, and then by last use
            //highest = unload from, lowest = unload to

            Structs.comps(
                Structs.comparingBool(e -> e.building.block.highUnloadPriority && !e.canLoad), //stackConveyors and Storage
                Structs.comps(
                    Structs.comparingBool(e -> e.canUnload && !e.canLoad), //priority to give
                    Structs.comparingBool(e -> e.canUnload || !e.canLoad) //priority to receive
                )
            ),
            Structs.comps(
                Structs.comparingFloat(e -> e.loadFactor),
                Structs.comparingInt(e -> -lastUsed[e.index])
            )
        );

        @Override
        public void updateTile(){
            if(((unloadTimer += delta()) < speed) || (proximity.size < 2)) return;
            Item item = null;
            boolean any = false;
            int itemslength = content.items().size;

            //initialize possibleBlocks only if the new size is bigger than the previous, to avoid unnecessary allocations
            if(possibleBlocks.size != proximity.size){
                int tmp = possibleBlocks.size;
                possibleBlocks.setSize(proximity.size);
                for(int i = tmp; i < proximity.size; i++){
                    possibleBlocks.set(i, new ContainerStat());
                }
                lastUsed = new int[proximity.size];
            }

            if(sortItem != null){
                item = sortItem;

                for(int pos = 0; pos < proximity.size; pos++){
                    var other = proximity.get(pos);
                    boolean interactable = other.interactable(team);

                    //set the stats of all buildings in possibleBlocks
                    ContainerStat pb = possibleBlocks.get(pos);
                    pb.building = other;
                    pb.canUnload = interactable && other.canUnload() && other.items != null && other.items.has(sortItem);
                    pb.canLoad = interactable && !(other.block instanceof StorageBlock) && other.acceptItem(this, sortItem);
                    pb.index = pos;
                }
            }else{
                //select the next item for nulloaders
                //inspired of nextIndex() but for all proximity at once, and also way more powerful
                for(int i = 0; i < itemslength; i++){
                    int total = (rotations + i + 1) % itemslength;
                    boolean hasProvider = false;
                    boolean hasReceiver = false;
                    boolean isDistinct = false;
                    Item possibleItem = content.item(total);

                    for(int pos = 0; pos < proximity.size; pos++){
                        var other = proximity.get(pos);
                        boolean interactable = other.interactable(team);

                        //set the stats of all buildings in possibleBlocks while we are at it
                        ContainerStat pb = possibleBlocks.get(pos);
                        pb.building = other;
                        pb.canUnload = interactable && other.canUnload() && other.items != null && other.items.has(possibleItem);
                        pb.canLoad = interactable && !(other.block instanceof StorageBlock) && other.acceptItem(this, possibleItem);
                        pb.index = pos;

                        //the part handling framerate issues and slow conveyor belts, to avoid skipping items
                        if(hasProvider && pb.canLoad) isDistinct = true;
                        if(hasReceiver && pb.canUnload) isDistinct = true;
                        hasProvider = hasProvider || pb.canUnload;
                        hasReceiver = hasReceiver || pb.canLoad;
                    }
                    if(isDistinct){
                        item = possibleItem;
                        break;
                    }
                }
            }

            if(item != null){
                //only compute the load factor if a transfer is possible
                for(int pos = 0; pos < proximity.size; pos++){
                    ContainerStat pb = possibleBlocks.get(pos);
                    var other = pb.building;
                    pb.loadFactor = (other.getMaximumAccepted(item) == 0) || (other.items == null) ? 0 : other.items.get(item) / (float)other.getMaximumAccepted(item);
                }

                possibleBlocks.sort(comparator);

                ContainerStat dumpingFrom = null;
                ContainerStat dumpingTo = null;

                //choose the building to accept the item
                for(int i = 0; i < possibleBlocks.size; i++){
                    if(possibleBlocks.get(i).canLoad){
                        dumpingTo = possibleBlocks.get(i);
                        break;
                    }
                }

                //choose the building to take the item from
                for(int i = possibleBlocks.size - 1; i >= 0; i--){
                    if(possibleBlocks.get(i).canUnload){
                        dumpingFrom = possibleBlocks.get(i);
                        break;
                    }
                }

                //increment the priority if not used
                for(int i = 0; i < possibleBlocks.size; i++){
                    lastUsed[i] = (lastUsed[i] + 1) % 2147483647;
                }

                //trade the items
                //TODO  && dumpingTo != dumpingFrom ?
                if(dumpingFrom != null && dumpingTo != null && (dumpingFrom.loadFactor != dumpingTo.loadFactor || !dumpingFrom.canLoad)){
                    dumpingTo.building.handleItem(this, item);
                    dumpingFrom.building.removeStack(item, 1);
                    lastUsed[dumpingFrom.index] = 0;
                    lastUsed[dumpingTo.index] = 0;
                    any = true;
                }

                if(sortItem == null) rotations = item.id;
            }

            if(any){
                unloadTimer %= speed;
            }else{
                unloadTimer = Math.min(unloadTimer, speed);
            }

            if(proximity.size > 0){
                offset++;
                offset %= proximity.size;
            }
        }

        @Override
        public void draw(){
            super.draw();

            Draw.color(sortItem == null ? Color.clear : sortItem.color);
            Draw.rect(centerRegion, x, y);
            Draw.color();
        }

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(Unloader.this, table, content.items(), () -> sortItem, this::configure);
        }

        @Override
        public Item config(){
            return sortItem;
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(sortItem == null ? -1 : sortItem.id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            int id = revision == 1 ? read.s() : read.b();
            sortItem = id == -1 ? null : content.item(id);
        }
    }
}
