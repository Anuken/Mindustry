package mindustry.world.blocks.distribution;

import arc.util.io.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class BufferedItemBridge extends ItemBridge{
    public final int timerAccept = timers++;

    public float speed = 40f;
    public int bufferCapacity = 50;
    public float displayedSpeed = 11f;

    public BufferedItemBridge(String name){
        super(name);
        hasPower = false;
        hasItems = true;
        canOverdrive = true;
    }
    
    @Override
    public void setStats(){
        super.setStats();

        //Hard to calculate, fps and overdive reliant. Movement speed taken from testing
        stats.add(Stat.itemsMoved, displayedSpeed, StatUnit.itemsSecond);
    }


    public class BufferedItemBridgeBuild extends ItemBridgeBuild{
        ItemBuffer buffer = new ItemBuffer(bufferCapacity);

        @Override
        public void updateTransport(Building other){
            if(buffer.accepts() && items.total() > 0){
                buffer.accept(items.take());
            }

            Item item = buffer.poll(speed / timeScale);
            if(timer(timerAccept, 4 / timeScale) && item != null && other.acceptItem(this, item)){
                moved = true;
                other.handleItem(this, item);
                buffer.remove();
            }
        }

        @Override
        public void doDump(){
            dump();
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