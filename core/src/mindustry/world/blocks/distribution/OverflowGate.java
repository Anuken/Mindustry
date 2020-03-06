package mindustry.world.blocks.distribution;

import arc.math.*;
import arc.util.*;
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
        unloadable = false;
    }

    @Override
    public boolean outputsItems(){
        return true;
    }

    public class OverflowGateEntity extends TileEntity{
        Item lastItem;
        Tile lastInput;
        float time;

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

                time += 1f / speed * Time.delta();
                Tilec target = getTileTarget(lastItem, lastInput, false);

                if(target != null && (time >= 1f)){
                    getTileTarget(lastItem, lastInput, true);
                    target.handleItem(this, lastItem);
                    items.remove(lastItem, 1);
                    lastItem = null;
                }
            }
        }

        @Override
        public boolean acceptItem(Tilec source, Item item){
            return team == source.team() && lastItem == null && items.total() == 0;
        }

        @Override
        public void handleItem(Tilec source, Item item){
            items.add(item, 1);
            lastItem = item;
            time = 0f;
            lastInput = source.tile();

            updateTile();
        }

        public Tilec getTileTarget(Item item, Tile src, boolean flip){
            int from = relativeTo(src.x, src.y);
            if(from == -1) return null;
            Tilec to = nearby((from + 2) % 4);
            if(to == null) return null;
            boolean canForward = to.acceptItem(this, item) && to.team() == team && !(to.block() instanceof OverflowGate);

            if(!canForward || invert){
                Tilec a = nearby(Mathf.mod(from - 1, 4));
                Tilec b = nearby(Mathf.mod(from + 1, 4));
                boolean ac = a != null && a.acceptItem(this, item) && !(a.block() instanceof OverflowGate) && a.team() == team;
                boolean bc = b != null && b.acceptItem(this, item) && !(b.block() instanceof OverflowGate) && b.team() == team;

                if(!ac && !bc){
                    return invert && canForward ? to : null;
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
        public byte version(){
            return 3;
        }

        @Override
        public void write(Writes write){
            write.i(lastInput == null ? -1 : lastInput.pos());
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision == 1){
                new DirectionalItemBuffer(25, 50f).read(read);
            }else if(revision == 3){
                lastInput = world.tile(read.i());
                lastItem = items.first();
            }
        }
    }
}
