package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.meta.BlockGroup;

import java.io.*;

public class OverflowGate extends Block{
    protected float speed = 1f;

    public OverflowGate(String name){
        super(name);
        hasItems = true;
        solid = true;
        update = true;
        group = BlockGroup.transportation;
        unloadable = false;
    }

    @Override
    public boolean outputsItems(){
        return true;
    }

    @Override
    public int removeStack(Tile tile, Item item, int amount){
        OverflowGateEntity entity = tile.entity();
        int result = super.removeStack(tile, item, amount);
        if(result != 0 && item == entity.lastItem){
            entity.lastItem = null;
        }
        return result;
    }

    @Override
    public void update(Tile tile){
        OverflowGateEntity entity = tile.entity();

        if(entity.lastItem == null && entity.items.total() > 0){
            entity.items.clear();
        }

        if(entity.lastItem != null){
            entity.time += 1f / speed * Time.delta();
            Tile target = getTileTarget(tile, entity.lastItem, entity.lastInput, false);

            if(target != null && (entity.time >= 1f)){
                getTileTarget(tile, entity.lastItem, entity.lastInput, true);
                target.block().handleItem(entity.lastItem, target, Edges.getFacingEdge(tile, target));
                entity.items.remove(entity.lastItem, 1);
                entity.lastItem = null;
            }
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        OverflowGateEntity entity = tile.entity();

        return tile.getTeam() == source.getTeam() && entity.lastItem == null && entity.items.total() == 0;
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        OverflowGateEntity entity = tile.entity();
        entity.items.add(item, 1);
        entity.lastItem = item;
        entity.time = 0f;
        entity.lastInput = source;
    }

    Tile getTileTarget(Tile tile, Item item, Tile src, boolean flip){
        int from = tile.relativeTo(src.x, src.y);
        if(from == -1) return null;
        Tile to = tile.getNearby((from + 2) % 4);
        if(to == null) return null;
        Tile edge = Edges.getFacingEdge(tile, to);

        if(!to.block().acceptItem(item, to, edge) || (to.block() instanceof OverflowGate)){
            Tile a = tile.getNearby(Mathf.mod(from - 1, 4));
            Tile b = tile.getNearby(Mathf.mod(from + 1, 4));
            boolean ac = a != null && a.block().acceptItem(item, a, edge) && !(a.block() instanceof OverflowGate);
            boolean bc = b != null && b.block().acceptItem(item, b, edge) && !(b.block() instanceof OverflowGate);

            if(!ac && !bc){
                return null;
            }

            if(ac && !bc){
                to = a;
            }else if(bc && !ac){
                to = b;
            }else{
                if(tile.rotation() == 0){
                    to = a;
                    if(flip) tile.rotation((byte) 1);
                }else{
                    to = b;
                    if(flip) tile.rotation((byte) 0);
                }
            }
        }

        return to;
    }

    @Override
    public TileEntity newEntity(){
        return new OverflowGateEntity();
    }

    public class OverflowGateEntity extends TileEntity{
        Item lastItem;
        Tile lastInput;
        float time;

        @Override
        public byte version(){
            return 2;
        }

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            if(revision == 1){
                new DirectionalItemBuffer(25, 50f).read(stream);
            }
        }
    }
}
