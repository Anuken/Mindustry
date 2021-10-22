package mindustry.world.blocks.storage;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Unloader extends Block{
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
        unloadable = false;
        envEnabled = Env.any;

        config(Item.class, (UnloaderBuild tile, Item item) -> tile.sortItem = item);
        configClear((UnloaderBuild tile) -> tile.sortItem = null);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.speed, 60f / speed, StatUnit.itemsSecond);
    }

    @Override
    public void drawRequestConfig(BuildPlan req, Eachable<BuildPlan> list){
        drawRequestConfigCenter(req, req.config, "unloader-center");
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.remove("items");
    }

    public class UnloaderBuild extends Building{
        public float unloadTimer = 0f;
        public Item sortItem = null;
        public int offset = 0;
        public int rotations = 0;
        public Seq<ContainerStat> possibleBlocks = new Seq<>();

        public class ContainerStat{
            Building building;
            float loadFactor;
            boolean canLoad;
            boolean canUnload;
        }

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

                //sort so it gives full priority to blocks that can give but not receive (mainly plast and storage), and then by load
                possibleBlocks.sort((e1, e2) -> {
                    // TODO: instead of canLoad it should be ((instance of Storage) || (is it a plast belt i can unload from))
                    //  otherwise a 100% full factory will get full priority over the storage/plast, barely an issue but still wasting trades and thus speed
                    int canLoad = Boolean.compare(e2.canLoad, e1.canLoad);
                    return (canLoad != 0) ? canLoad : Float.compare(e1.loadFactor, e2.loadFactor);
                });

                ContainerStat dumpingFrom = null;
                ContainerStat dumpingTo = null;

                //choose the building to accept the item
                for(int i = 0; i < possibleBlocks.size; i++){
                    if(possibleBlocks.get(i).canLoad){
                        dumpingTo = possibleBlocks.get(i);
                        break;
                    }
                }

                //choose the building to give the item
                for(int i = possibleBlocks.size - 1; i >= 0; i--){
                    if(possibleBlocks.get(i).canUnload){
                        dumpingFrom = possibleBlocks.get(i);
                        break;
                    }
                }

                //trade the items
                if(dumpingFrom != null && dumpingTo != null && dumpingFrom.loadFactor != dumpingTo.loadFactor){
                    dumpingTo.building.handleItem(this, item);
                    dumpingFrom.building.removeStack(item, 1);
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
            Draw.rect("unloader-center", x, y);
            Draw.color();
        }

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(table, content.items(), () -> sortItem, this::configure);
        }

        @Override
        public boolean onConfigureTileTapped(Building other){
            if(this == other){
                deselect();
                configure(null);
                return false;
            }

            return true;
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
            sortItem = id == -1 ? null : content.items().get(id);
        }
    }
}