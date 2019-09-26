package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.arc.util.Time;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.entities.type.Unit;
import io.anuke.mindustry.gen.BufferItem;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.DirectionalItemBuffer;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockGroup;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.content;

public class Junction extends Block{
    protected float speed = 26; //frames taken to go through this junction
    protected int capacity = 6;

    public Junction(String name){
        super(name);
        update = true;
        solid = true;
        instantTransfer = true;
        group = BlockGroup.transportation;
        unloadable = false;
    }

    @Override
    public int acceptStack(Item item, int amount, Tile tile, Unit source){
        return 0;
    }

    @Override
    public boolean outputsItems(){
        return true;
    }

    @Override
    public void update(Tile tile){
        JunctionEntity entity = tile.entity();
        DirectionalItemBuffer buffer = entity.buffer;

        for(int i = 0; i < 4; i++){
            if(buffer.indexes[i] > 0){
                if(buffer.indexes[i] > capacity) buffer.indexes[i] = capacity;
                long l = buffer.buffers[i][0];
                float time = BufferItem.time(l);

                if(Time.time() >= time + speed || Time.time() < time){

                    Item item = content.item(BufferItem.item(l));
                    Tile dest = tile.getNearby(i);
                    if(dest != null) dest = dest.link();

                    //skip blocks that don't want the item, keep waiting until they do
                    if(dest == null || !dest.block().acceptItem(item, dest, tile)){
                        continue;
                    }

                    dest.block().handleItem(item, dest, tile);
                    System.arraycopy(buffer.buffers[i], 1, buffer.buffers[i], 0, buffer.indexes[i] - 1);
                    buffer.indexes[i] --;
                }
            }
        }
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        JunctionEntity entity = tile.entity();
        int relative = source.relativeTo(tile.x, tile.y);
        entity.buffer.accept(relative, item);
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        JunctionEntity entity = tile.entity();
        int relative = source.relativeTo(tile.x, tile.y);

        if(entity == null || relative == -1 || !entity.buffer.accepts(relative))
            return false;
        Tile to = tile.getNearby(relative);
        return to != null && to.link().entity != null;
    }

    @Override
    public TileEntity newEntity(){
        return new JunctionEntity();
    }

    class JunctionEntity extends TileEntity{
        DirectionalItemBuffer buffer = new DirectionalItemBuffer(capacity, speed);

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            buffer.write(stream);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            buffer.read(stream);
        }
    }
}
