package mindustry.world.blocks.distribution;

import arc.math.*;
import mindustry.type.*;
import mindustry.world.*;

import java.io.*;

public class BufferedItemBridge extends ExtendingItemBridge{
    public final int timerAccept = timers++;

    public float speed = 40f;
    public int bufferCapacity = 50;

    public BufferedItemBridge(String name){
        super(name);
        hasPower = false;
        hasItems = true;
        entityType = BufferedItemBridgeEntity::new;
    }

    @Override
    public void updateTransport(Tile tile, Tile other){
        BufferedItemBridgeEntity entity = tile.ent();

        if(entity.buffer.accepts() && entity.items.total() > 0){
            entity.buffer.accept(entity.items.take());
        }

        Item item = entity.buffer.poll();
        if(entity.timer.get(timerAccept, 4) && item != null && other.block().acceptItem(item, other, tile)){
            entity.cycleSpeed = Mathf.lerpDelta(entity.cycleSpeed, 4f, 0.05f);
            other.block().handleItem(item, other, tile);
            entity.buffer.remove();
        }else{
            entity.cycleSpeed = Mathf.lerpDelta(entity.cycleSpeed, 0f, 0.008f);
        }
    }

    class BufferedItemBridgeEntity extends ItemBridgeEntity{
        ItemBuffer buffer = new ItemBuffer(bufferCapacity, speed);

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
