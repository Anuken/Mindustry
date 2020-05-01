package mindustry.world.blocks.distribution;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Sorter extends Block{
    private static Item lastItem;
    private static float lastTime;
    public boolean invert;

    public Sorter(String name){
        super(name);
        update = true;
        solid = true;
        instantTransfer = true;
        group = BlockGroup.transportation;
        configurable = true;
        unloadable = false;
        config(Item.class, (tile, item) -> ((SorterEntity)tile).sortItem = item);
        configClear(tile -> ((SorterEntity)tile).sortItem = null);
    }

    @Override
    public void drawRequestConfig(BuildRequest req, Eachable<BuildRequest> list){
        drawRequestConfigCenter(req, (Item)req.config, "center");
    }

    @Override
    public boolean outputsItems(){
        return true;
    }

    @Override
    public int minimapColor(Tile tile){
        return tile.<SorterEntity>ent().sortItem == null ? 0 : tile.<SorterEntity>ent().sortItem.color.rgba();
    }

    public class SorterEntity extends TileEntity{
        @Nullable Item sortItem;

        @Override
        public void playerPlaced(){
            if(lastItem != null){
                float timeout = Core.settings.getFloat("filtertimeout", 0f);
                if(timeout > 0f && Time.time() - lastTime > timeout){
                    lastItem = null;
                    return;
                }
                tile.configure(lastItem);
            }
        }

        @Override
        public void configured(Playerc player, Object value){
            super.configured(player, value);

            lastTime = Time.time();
            if(!headless){
                renderer.minimap.update(tile);
            }
        }

        @Override
        public void draw(){
            super.draw();

            if(sortItem == null){
                Draw.rect("cross", x, y);
            }else{
                Draw.color(sortItem.color);
                Draw.rect("center", x, y);
                Draw.color();
            }
        }

        @Override
        public boolean acceptItem(Tilec source, Item item){
            Tilec to = getTileTarget(item, source, false);

            return to != null && to.acceptItem(this, item) && to.team() == team;
        }

        @Override
        public void handleItem(Tilec source, Item item){
            Tilec to = getTileTarget(item, source, true);

            to.handleItem(this, item);
        }

        boolean isSame(Tilec other){
            //uncomment comment below to prevent sorter/gate chaining (hacky)
            return other != null && (other.block() instanceof Sorter/* || other.block() instanceof OverflowGate */);
        }

        Tilec getTileTarget(Item item, Tilec source, boolean flip){
            int dir = source.absoluteRelativeTo(tile.x, tile.y);
            if(dir == -1) return null;
            Tilec to;

            if((item == sortItem) != invert){
                //prevent 3-chains
                if(isSame(source) && isSame(nearby(dir))){
                    return null;
                }
                to = nearby(dir);
            }else{
                Tilec a = nearby(Mathf.mod(dir - 1, 4));
                Tilec b = nearby(Mathf.mod(dir + 1, 4));
                boolean ac = a != null && !(a.block().instantTransfer && source.block().instantTransfer) &&
                a.acceptItem(this, item);
                boolean bc = b != null && !(b.block().instantTransfer && source.block().instantTransfer) &&
                b.acceptItem(this, item);

                if(ac && !bc){
                    to = a;
                }else if(bc && !ac){
                    to = b;
                }else if(!bc){
                    return null;
                }else{
                    if(rotation() == 0){
                        to = a;
                        if(flip) rotation((byte)1);
                    }else{
                        to = b;
                        if(flip) rotation((byte)0);
                    }
                }
            }

            return to;
        }

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(table, content.items(), () -> sortItem, item -> tile.configure(lastItem = item));
        }

        @Override
        public Item config(){
            return sortItem;
        }

        @Override
        public byte version(){
            return 2;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(sortItem == null ? -1 : sortItem.id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            sortItem = content.item(read.s());

            if(revision == 1){
                new DirectionalItemBuffer(20, 45f).read(read);
            }
        }
    }
}
