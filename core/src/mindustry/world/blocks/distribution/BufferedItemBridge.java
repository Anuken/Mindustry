package mindustry.world.blocks.distribution;

import arc.math.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

public class BufferedItemBridge extends ExtendingItemBridge{
    public final int timerAccept = timers++;

    public float speed = 40f;
    public int bufferCapacity = 50;

    public BufferedItemBridge(String name){
        super(name);
        hasPower = false;
        hasItems = true;
        canOverdrive = true;
    }

    public class BufferedItemBridgeBuild extends ExtendingItemBridgeBuild{
        ItemBuffer buffer = new ItemBuffer(bufferCapacity);

        @Override
        public void updateTransport(Building other){
            if(buffer.accepts() && items.total() > 0){
                buffer.accept(items.take());
            }

            Item item = buffer.poll(speed / timeScale);
            if(timer(timerAccept, 4 / timeScale) && item != null && other.acceptItem(this, item)){
                cycleSpeed = Mathf.lerpDelta(cycleSpeed, 4f, 0.05f);
                other.handleItem(this, item);
                buffer.remove();
            }else{
                cycleSpeed = Mathf.lerpDelta(cycleSpeed, 0f, 0.008f);
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);
            buffer.write(write);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            buffer.read(read);
        }
    }
}
