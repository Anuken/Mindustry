package mindustry.world.blocks.storage;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
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
        public Building dumpingTo;
        public int offset = 0;
        public int[] rotations;

        @Override
        public void updateTile(){
            if((unloadTimer += delta()) >= speed){
                boolean any = false;
                if(rotations == null || rotations.length != proximity.size){
                    rotations = new int[proximity.size];
                }

                for(int i = 0; i < proximity.size; i++){
                    int pos = (offset + i) % proximity.size;
                    var other = proximity.get(pos);

                    if(other.interactable(team) && other.block.unloadable && other.canUnload() && other.block.hasItems
                    && ((sortItem == null && other.items.total() > 0) || (sortItem != null && other.items.has(sortItem)))){
                        //make sure the item can't be dumped back into this block
                        dumpingTo = other;

                        //get item to be taken
                        Item item = sortItem == null ? other.items.takeIndex(rotations[pos]) : sortItem;

                        //remove item if it's dumped correctly
                        if(put(item)){
                            other.items.remove(item, 1);
                            any = true;

                            if(sortItem == null){
                                rotations[pos] = item.id + 1;
                            }

                            other.itemTaken(item);
                        }else if(sortItem == null){
                            rotations[pos] = other.items.nextIndex(rotations[pos]);
                        }
                    }
                }

                if(any){
                    unloadTimer %= speed;
                }else{
                    unloadTimer = Math.min(unloadTimer, speed);
                }

                if(proximity.size > 0){
                    offset ++;
                    offset %= proximity.size;
                }
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
        public boolean canDump(Building to, Item item){
            return !(to.block instanceof StorageBlock) && to != dumpingTo;
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
