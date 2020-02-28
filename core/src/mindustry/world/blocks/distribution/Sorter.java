package mindustry.world.blocks.distribution;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.entities.traits.BuilderTrait.*;
import mindustry.entities.type.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

import java.io.*;

import static mindustry.Vars.*;

public class Sorter extends Block{
    private static Item lastItem;
    public boolean invert;

    public Sorter(String name){
        super(name);
        update = true;
        solid = true;
        instantTransfer = true;
        group = BlockGroup.transportation;
        configurable = true;
        unloadable = false;
        entityType = SorterEntity::new;
    }

    @Override
    public boolean outputsItems(){
        return true;
    }

    @Override
    public void playerPlaced(Tile tile){
        if(lastItem != null){
            tile.configure(lastItem.id);
        }
    }

    @Override
    public void configured(Tile tile, Player player, int value){
        tile.<SorterEntity>ent().sortItem = content.item(value);
        if(!headless){
            renderer.minimap.update(tile);
        }
    }

    @Override
    public void drawRequestConfig(BuildRequest req, Eachable<BuildRequest> list){
        drawRequestConfigCenter(req, content.item(req.config), "center");
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        SorterEntity entity = tile.ent();
        if(entity.sortItem == null) return;

        Draw.color(entity.sortItem.color);
        Draw.rect("center", tile.worldx(), tile.worldy());
        Draw.color();
    }

    @Override
    public int minimapColor(Tile tile){
        return tile.<SorterEntity>ent().sortItem == null ? 0 : tile.<SorterEntity>ent().sortItem.color.rgba();
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        Tile to = getTileTarget(item, tile, source, false);

        return to != null && to.block().acceptItem(item, to, tile) && to.getTeam() == tile.getTeam();
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        Tile to = getTileTarget(item, tile, source, true);

        to.block().handleItem(item, to, tile);
    }

    boolean isSame(Tile tile, Tile other){
        //uncomment comment below to prevent sorter/gate chaining (hacky)
        return other != null && (other.block() instanceof Sorter/* || other.block() instanceof OverflowGate */);
    }

    Tile getTileTarget(Item item, Tile dest, Tile source, boolean flip){
        SorterEntity entity = dest.ent();

        int dir = source.relativeTo(dest.x, dest.y);
        if(dir == -1) return null;
        Tile to;

        if((item == entity.sortItem) != invert){
            //prevent 3-chains
            if(isSame(dest, source) && isSame(dest, dest.getNearby(dir))){
                return null;
            }
            to = dest.getNearby(dir);
        }else{
            Tile a = dest.getNearby(Mathf.mod(dir - 1, 4));
            Tile b = dest.getNearby(Mathf.mod(dir + 1, 4));
            boolean ac = a != null && !(a.block().instantTransfer && source.block().instantTransfer) &&
                    a.block().acceptItem(item, a, dest);
            boolean bc = b != null && !(b.block().instantTransfer && source.block().instantTransfer) &&
                    b.block().acceptItem(item, b, dest);

            if(ac && !bc){
                to = a;
            }else if(bc && !ac){
                to = b;
            }else if(!bc){
                return null;
            }else{
                if(dest.rotation() == 0){
                    to = a;
                    if(flip) dest.rotation((byte)1);
                }else{
                    to = b;
                    if(flip) dest.rotation((byte)0);
                }
            }
        }

        return to;
    }

    @Override
    public void buildConfiguration(Tile tile, Table table){
        SorterEntity entity = tile.ent();
        ItemSelection.buildTable(table, content.items(), () -> entity.sortItem, item -> {
            lastItem = item;
            tile.configure(item == null ? -1 : item.id);
        });
    }

    public class SorterEntity extends TileEntity{
        @Nullable Item sortItem;

        @Override
        public int config(){
            return sortItem == null ? -1 : sortItem.id;
        }

        @Override
        public byte version(){
            return 2;
        }

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeShort(sortItem == null ? -1 : sortItem.id);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            sortItem = content.item(stream.readShort());
            if(revision == 1){
                new DirectionalItemBuffer(20, 45f).read(stream);
            }
        }
    }
}
