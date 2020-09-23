package mindustry.world.blocks.distribution;

import arc.math.*;
import arc.util.*;
import arc.util.ArcAnnotate.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.world;

public class OverflowGate extends Block{
    public float speed = 1f;
    public boolean invert = false;

    public OverflowGate(String name){
        super(name);
        hasItems = true;
        solid = true;
        update = true;
        group = BlockGroup.transportation;
        instantTransfer = true;
        unloadable = false;
        canOverdrive = false;
    }

    @Override
    public boolean outputsItems(){
        return true;
    }

    public class OverflowGateBuild extends Building{
        public Item lastItem;
        public Tile lastInput;
        public float time;

        @Override
        public int acceptStack(Item item, int amount, Teamc source){
            return 0;
        }

        @Override
        public int removeStack(Item item, int amount){
            int result = super.removeStack(item, amount);
            if(result != 0 && item == lastItem){
                lastItem = null;
            }
            return result;
        }

        @Override
        public void updateTile(){
            if(lastItem == null && items.total() > 0){
                items.clear();
            }

            if(lastItem != null){
                if(lastInput == null){
                    lastItem = null;
                    return;
                }

                time += 1f / speed * Time.delta;
                Building target = getTileTarget(lastItem, lastInput, false);

                if(target != null && (time >= 1f)){
                    getTileTarget(lastItem, lastInput, true);
                    target.handleItem(this, lastItem);
                    items.remove(lastItem, 1);
                    lastItem = null;
                }
            }
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return team == source.team && lastItem == null && items.total() == 0;
        }

        @Override
        public void handleItem(Building source, Item item){
            items.add(item, 1);
            lastItem = item;
            time = 0f;
            lastInput = source.tile();
        }

        public @Nullable Building getTileTarget(Item item, Tile src, boolean flip){
            int from = relativeToEdge(src);
            if(from == -1) return null;
            Building to = nearby((from + 2) % 4);
            boolean canForward = to != null && to.acceptItem(this, item) && to.team == team && !(to.block instanceof OverflowGate);
            boolean inv = invert == enabled;

            if(!canForward || inv){
                Building a = nearby(Mathf.mod(from - 1, 4));
                Building b = nearby(Mathf.mod(from + 1, 4));
                boolean ac = a != null && a.acceptItem(this, item) && !(a.block instanceof OverflowGate) && a.team == team;
                boolean bc = b != null && b.acceptItem(this, item) && !(b.block instanceof OverflowGate) && b.team == team;

                if(!ac && !bc){
                    return inv && canForward ? to : null;
                }

                if(ac && !bc){
                    to = a;
                }else if(bc && !ac){
                    to = b;
                }else{
                    if(rotation == 0){
                        to = a;
                        if(flip) rotation =1;
                    }else{
                        to = b;
                        if(flip) rotation = 0;
                    }
                }
            }

            return to;
        }

        @Override
        public byte version(){
            return 3;
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.i(lastInput == null ? -1 : lastInput.pos());
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            if(revision == 1){
                new DirectionalItemBuffer(25).read(read);
            }else if(revision == 3){
                lastInput = world.tile(read.i());
                lastItem = items.first();
            }
        }
    }
}
