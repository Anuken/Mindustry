package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.ItemBuffer;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.util.Mathf;

public class BufferedItemBridge extends ExtendingItemBridge{
    protected int timerAccept = timers++;

    protected float speed = 40f;
    protected int bufferCapacity = 50;

    public BufferedItemBridge(String name){
        super(name);
        hasPower = false;
        hasItems = true;
    }

    @Override
    public void updateTransport(Tile tile, Tile other){
        BufferedItemBridgeEntity entity = tile.entity();

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

    @Override
    public TileEntity newEntity(){
        return new BufferedItemBridgeEntity();
    }

    class BufferedItemBridgeEntity extends ItemBridgeEntity{
        ItemBuffer buffer = new ItemBuffer(bufferCapacity, speed);
    }
}
