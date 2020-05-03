package mindustry.world.blocks.storage;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.*;

public class Unloader extends Block{
    protected static Item lastItem;

    public float speed = 1f;
    public final int timerUnload = timers++;

    public Unloader(String name){
        super(name);
        update = true;
        solid = true;
        health = 70;
        hasItems = true;

        configurable = true;
        config(Item.class, (tile, item) -> {
            tile.items().clear();
            ((UnloaderEntity)tile).sortItem = item;
        });
        configClear(tile -> ((UnloaderEntity)tile).sortItem = null);

        Events.on(Trigger.resetFilters, () -> {
            lastItem = null;
        });
    }

    @Override
    public void drawRequestConfig(BuildRequest req, Eachable<BuildRequest> list){
        drawRequestConfigCenter(req, (Item)req.config, "unloader-center");
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.remove("items");
    }

    public class UnloaderEntity extends TileEntity{
        public Item sortItem = null;

        private Item removeItem(Tilec tile, Item item){
            if(item == null){
                return tile.items().take();
            }else{
                if(tile.items().has(item)){
                    tile.items().remove(item, 1);
                    return item;
                }

                return null;
            }
        }

        private boolean hasItem(Tilec tile, Item item){
            if(item == null){
                return tile.items().total() > 0;
            }else{
                return tile.items().has(item);
            }
        }

        @Override
        public void playerPlaced(){
            if(lastItem != null){
                tile.configure(lastItem);
            }
        }

        @Override
        public void updateTile(){
            if(timer(timerUnload, speed / timeScale()) && items.total() == 0){
                for(Tilec other : proximity){
                    if(other.interactable(team) && other.block().unloadable && other.block().hasItems && items.total() == 0 &&
                        ((sortItem == null && items.total() > 0) || hasItem(other, sortItem))){
                        offloadNear(removeItem(other, sortItem));
                    }
                }
            }

            dump();
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
            ItemSelection.buildTable(table, content.items(), () -> tile.<UnloaderEntity>ent().sortItem, item -> tile.configure(lastItem = item));
        }

        @Override
        public boolean onConfigureTileTapped(Tilec other){
            if(this == other){
                control.input.frag.config.hideConfig();
                tile.configure(lastItem = null);
                return false;
            }

            return true;
        }

        @Override
        public boolean canDump(Tilec to, Item item){
            return !(to.block() instanceof StorageBlock);
        }

        @Override
        public Item config(){
            return sortItem;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(sortItem == null ? -1 : sortItem.id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            byte id = read.b();
            sortItem = id == -1 ? null : content.items().get(id);
        }
    }
}
