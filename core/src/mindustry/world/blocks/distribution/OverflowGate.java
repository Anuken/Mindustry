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

    @Override
    public int acceptStack(Item item, int amount, Teamc source){
        return 0;
    }

    @Override
    public int removeStack(Item item, int amount){
        int result = super.removeStack(tile, item, amount);
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
            Tile target = getTileTarget(tile, lastItem, lastInput, false);

            if(target != null && (time >= 1f)){
                getTileTarget(tile, lastItem, lastInput, true);
                target.block().handleItem(target, Edges.getFacingEdge(tile, target), lastItem);
                items.remove(lastItem, 1);
                lastItem = null;
            }
        }
    }

    @Override
    public boolean acceptItem(Tile source, Item item){
        return team == source.team() && lastItem == null && items.total() == 0;
    }

    @Override
    public void handleItem(Tile source, Item item){
        items.add(item, 1);
        lastItem = item;
        time = 0f;
        lastInput = source;

        update(tile);
    }

    public Tile getTileTarget(Item item, Tile src, boolean flip){
        int from = tile.relativeTo(src.x, src.y);
        if(from == -1) return null;
        Tile to = tile.getNearby((from + 2) % 4);
        if(to == null) return null;
        Tile edge = Edges.getFacingEdge(tile, to);
        boolean canForward = to.block().acceptItem(to, edge, item) && to.team() == team && !(to.block() instanceof OverflowGate);

        if(!canForward || invert){
            Tile a = tile.getNearby(Mathf.mod(from - 1, 4));
            Tile b = tile.getNearby(Mathf.mod(from + 1, 4));
            boolean ac = a != null && a.block().acceptItem(a, edge, item) && !(a.block() instanceof OverflowGate) && a.team() == team;
            boolean bc = b != null && b.block().acceptItem(b, edge, item) && !(b.block() instanceof OverflowGate) && b.team() == team;

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

    public class OverflowGateEntity extends TileEntity{
        Item lastItem;
        Tile lastInput;
        float time;

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
