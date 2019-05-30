package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.meta.BlockGroup;

import java.io.*;

public class OverflowGate extends Block{
    protected int bufferCapacity = 10;
    protected float speed = 45f;

    public OverflowGate(String name){
        super(name);
        hasItems = true;
        solid = true;
        update = true;
        group = BlockGroup.transportation;
    }

    @Override
    public void update(Tile tile){
        OverflowGateEntity entity = tile.entity();

        for(int i = 0; i < 4; i++){
            Item item = entity.buffer.poll(i);
            if(item != null){
                Tile other = getTileTarget(tile, item, tile.getNearby(i), true);
                if(other != null && other.block().acceptItem(item, other, tile)){
                    other.block().handleItem(item, other, tile);
                    entity.buffer.remove(i);
                }
            }
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        OverflowGateEntity entity = tile.entity();
        return entity.buffer.accepts(tile.relativeTo(source.x, source.y));
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        OverflowGateEntity entity = tile.entity();
        int buffer = tile.relativeTo(source.x, source.y);
        if(entity.buffer.accepts(buffer)){
            entity.buffer.accept(buffer, item);
        }
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
                    if(flip) tile.rotation((byte)1);
                }else{
                    to = b;
                    if(flip) tile.rotation((byte)0);
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
        DirectionalItemBuffer buffer = new DirectionalItemBuffer(bufferCapacity, speed);

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            buffer.write(stream);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            if(revision == 1){
                buffer.read(stream);
            }
        }
    }
}
