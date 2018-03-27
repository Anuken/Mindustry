package io.anuke.mindustry.world;

import com.badlogic.gdx.math.GridPoint2;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.ucore.core.Timers;

public abstract class BaseBlock {
    public boolean hasInventory = true;
    public boolean hasLiquids;
    public boolean hasPower;

    public int itemCapacity;
    public float liquidCapacity = 10f;
    public float liquidFlowFactor = 4.9f;
    public float powerCapacity = 10f;

    public void handleItem(Item item, Tile tile, Tile source){
        tile.entity.inventory.addItem(item, 1);
    }

    public boolean acceptItem(Item item, Tile tile, Tile source){
        return false;
    }

    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        return tile.entity.liquid.amount + amount < liquidCapacity
                && (tile.entity.liquid.liquid == liquid || tile.entity.liquid.amount <= 0.001f);
    }

    public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        tile.entity.liquid.liquid = liquid;
        tile.entity.liquid.amount += amount;
    }

    public boolean acceptPower(Tile tile, Tile source, float amount){
        return true;
    }

    /**Returns how much power is accepted.*/
    public float addPower(Tile tile, float amount){
        float canAccept = Math.min(powerCapacity - tile.entity.power.amount, amount);

        tile.entity.power.amount += canAccept;

        return canAccept;
    }

    public void tryDumpLiquid(Tile tile){
        if(tile.entity.liquid.amount < 0.001f) return;

        int size = tile.block().size;

        GridPoint2[] nearby = Edges.getEdges(size);
        byte i = tile.getDump();

        for (int j = 0; j < nearby.length; j ++) {
            Tile other = tile.getNearby(nearby[i]);
            Tile in = tile.getNearby(Edges.getInsideEdges(size)[i]);

            if(other != null) other = other.target();

            if (other != null && other.block().hasLiquids) {
                float ofract = other.entity.liquid.amount / other.block().liquidCapacity;
                float fract = tile.entity.liquid.amount / liquidCapacity;

                if(ofract < fract) tryMoveLiquid(tile, in, other, (fract - ofract) * liquidCapacity / 2f);
            }

            i = (byte) ((i + 1) % nearby.length);
        }

    }

    public void tryMoveLiquid(Tile tile, Tile tileSource, Tile next, float amount){
        float flow = Math.min(next.block().liquidCapacity - next.entity.liquid.amount - 0.001f, amount);

        if(next.block().acceptLiquid(next, tileSource, tile.entity.liquid.liquid, flow)){
            next.block().handleLiquid(next, tileSource, tile.entity.liquid.liquid, flow);
            tile.entity.liquid.amount -= flow;
        }
    }

    public void tryMoveLiquid(Tile tile, Tile next){
        if(next == null) return;

        next = next.target();

        if(next.block().hasLiquids && tile.entity.liquid.amount > 0f){
            float ofract = next.entity.liquid.amount / next.block().liquidCapacity;
            float fract = tile.entity.liquid.amount / liquidCapacity;

            if(ofract > fract) return;

            float flow = Math.min((fract - ofract) * (liquidCapacity/(1.1f + tile.entity.liquid.liquid.viscosity)),
                    Math.min(tile.entity.liquid.amount/liquidFlowFactor * Math.max(Timers.delta(), 1f), tile.entity.liquid.amount));

            if(flow <= 0f || tile.entity.liquid.amount < flow) return;

            if(next.block().acceptLiquid(next, tile, tile.entity.liquid.liquid, flow)){
                next.block().handleLiquid(next, tile, tile.entity.liquid.liquid, flow);
                tile.entity.liquid.amount -= flow;
            }
        }
    }

    /**
     * Tries to put this item into a nearby container, if there are no available
     * containers, it gets added to the block's inventory.*/
    public void offloadNear(Tile tile, Item item){
        int size = tile.block().size;

        GridPoint2[] nearby = Edges.getEdges(size);

        for(int j = 0; j < nearby.length; j ++){
            Tile other = tile.getNearby(nearby[j]);
            Tile in = tile.getNearby(Edges.getInsideEdges(size)[j]);
            if(other != null && other.block().acceptItem(item, other, in) && canDump(tile, other, item)){
                other.block().handleItem(item, other, in);
                return;
            }
        }

        handleItem(item, tile, tile);
    }

    /**Try dumping any item near the tile.*/
    public boolean tryDump(Tile tile){
        return tryDump(tile, null);
    }

    /**Try dumping a specific item near the tile.*/
    public boolean tryDump(Tile tile, Item todump){
        int size = tile.block().size;

        GridPoint2[] nearby = Edges.getEdges(size);
        byte i = (byte)(tile.getDump() % nearby.length);

        for(int j = 0; j < nearby.length; j ++){
            Tile other;
            Tile in;

            for(Item item : Item.getAllItems()){
                other = tile.getNearby(nearby[i]);
                in = tile.getNearby(Edges.getInsideEdges(size)[i]);

                if(todump != null && item != todump) continue;

                if(tile.entity.inventory.hasItem(item) && other != null && other.block().acceptItem(item, other, in) && canDump(tile, other, item)){
                    other.block().handleItem(item, other, in);
                    tile.entity.inventory.removeItem(item, 1);
                    i = (byte)((i + 1) % nearby.length);
                    tile.setDump(i);
                    return true;
                }
            }


            i = (byte)((i + 1) % nearby.length);
            tile.setDump(i);
        }

        return false;
    }

    /**Used for dumping items.*/
    public boolean canDump(Tile tile, Tile to, Item item){
        return true;
    }

    /**
     * Try offloading an item to a nearby container in its facing direction. Returns true if success.
     */
    public boolean offloadDir(Tile tile, Item item){
        Tile other = tile.getNearby(tile.getRotation());
        if(other != null && other.block().acceptItem(item, other, tile)){
            other.block().handleItem(item, other, tile);
            return true;
        }
        return false;
    }
}
