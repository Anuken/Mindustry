package io.anuke.mindustry.world;

import com.badlogic.gdx.math.GridPoint2;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.effect.Puddle;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Translator;

public abstract class BaseBlock {
    public boolean hasItems = true;
    public boolean hasLiquids;
    public boolean hasPower;

    public int itemCapacity;
    public float liquidCapacity = 10f;
    public float liquidFlowFactor = 4.9f;
    public float powerCapacity = 10f;

    /**Returns the amount of items this block can accept.*/
    public int acceptStack(Item item, int amount, Tile tile, Unit source){
        if(acceptItem(item, tile, tile) && hasItems && source.team == tile.getTeam()){
            return Math.min(itemCapacity - tile.entity.items.totalItems(), amount);
        }else{
            return 0;
        }
    }

    /**Remove a stack from this inventory, and return the amount removed.*/
    public int removeStack(Tile tile, Item item, int amount){
        tile.entity.items.removeItem(item, amount);
        return amount;
    }

    /**Handle a stack input.*/
    public void handleStack(Item item, int amount, Tile tile, Unit source){
        tile.entity.items.addItem(item, amount);
    }

    /**Returns offset for stack placement.*/
    public void getStackOffset(Item item, Tile tile, Translator trns){

    }

    public void handleItem(Item item, Tile tile, Tile source){
        tile.entity.items.addItem(item, 1);
    }

    public boolean acceptItem(Item item, Tile tile, Tile source){
        return false;
    }

    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        return tile.entity.liquids.amount + amount < liquidCapacity
                && (tile.entity.liquids.liquid == liquid || tile.entity.liquids.amount <= 0.1f);
    }

    public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        tile.entity.liquids.liquid = liquid;
        tile.entity.liquids.amount += amount;
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
        if(tile.entity.liquids.amount < 0.001f) return;

        int size = tile.block().size;

        GridPoint2[] nearby = Edges.getEdges(size);
        byte i = tile.getDump();

        for (int j = 0; j < nearby.length; j ++) {
            Tile other = tile.getNearby(nearby[i]);
            Tile in = tile.getNearby(Edges.getInsideEdges(size)[i]);

            if(other != null) other = other.target();

            if (other != null && other.block().hasLiquids) {
                float ofract = other.entity.liquids.amount / other.block().liquidCapacity;
                float fract = tile.entity.liquids.amount / liquidCapacity;

                if(ofract < fract) tryMoveLiquid(tile, in, other, (fract - ofract) * liquidCapacity / 2f);
            }

            i = (byte) ((i + 1) % nearby.length);
        }

    }

    public void tryMoveLiquid(Tile tile, Tile tileSource, Tile next, float amount){
        float flow = Math.min(next.block().liquidCapacity - next.entity.liquids.amount - 0.001f, amount);

        if(next.block().acceptLiquid(next, tileSource, tile.entity.liquids.liquid, flow)){
            next.block().handleLiquid(next, tileSource, tile.entity.liquids.liquid, flow);
            tile.entity.liquids.amount -= flow;
        }
    }

    public float tryMoveLiquid(Tile tile, Tile next, boolean leak){
        if(next == null) return 0;

        next = next.target();

        if(next.block().hasLiquids && tile.entity.liquids.amount > 0f){
            float ofract = next.entity.liquids.amount / next.block().liquidCapacity;
            float fract = tile.entity.liquids.amount / liquidCapacity;

            if(ofract > fract) return 0;

            float flow = Math.min(Mathf.clamp((fract - ofract)*(1f)) * (liquidCapacity), tile.entity.liquids.amount);

            flow = Math.min(flow, next.block().liquidCapacity - next.entity.liquids.amount - 0.001f);

            if(flow <= 0f) return 0;

            float amount = flow;

            if(next.block().acceptLiquid(next, tile, tile.entity.liquids.liquid, amount)){
                next.block().handleLiquid(next, tile, tile.entity.liquids.liquid, amount);
                tile.entity.liquids.amount -= amount;
                return flow;
            }
        }else if(leak && !next.block().solid && !next.block().hasLiquids){
            float leakAmount = Math.min(tile.entity.liquids.amount, tile.entity.liquids.amount/1.5f);
            Puddle.deposit(next, tile, tile.entity.liquids.liquid, leakAmount);
            tile.entity.liquids.amount -= leakAmount;
        }
        return 0;
    }

    /**
     * Tries to put this item into a nearby container, if there are no available
     * containers, it gets added to the block's inventory.*/
    public void offloadNear(Tile tile, Item item){
        int size = tile.block().size;

        GridPoint2[] nearby = Edges.getEdges(size);
        byte i = (byte)(tile.getDump() % nearby.length);

        for(int j = 0; j < nearby.length; j ++){
            tile.setDump((byte)((i + 1) % nearby.length));
            Tile other = tile.getNearby(nearby[i]);
            Tile in = tile.getNearby(Edges.getInsideEdges(size)[i]);
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

                if(tile.entity.items.hasItem(item) && other != null && other.block().acceptItem(item, other, in) && canDump(tile, other, item)){
                    other.block().handleItem(item, other, in);
                    tile.entity.items.removeItem(item, 1);
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
