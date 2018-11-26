package io.anuke.mindustry.world;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.fx.EnvironmentFx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.effect.Puddle;
import io.anuke.mindustry.game.MappableContent;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.consumers.ConsumeItem;
import io.anuke.mindustry.world.consumers.ConsumeLiquid;
import io.anuke.mindustry.world.consumers.Consumers;
import io.anuke.mindustry.world.meta.Producers;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Translator;

public abstract class BaseBlock extends MappableContent{
    public boolean hasItems;
    public boolean hasLiquids;
    public boolean hasPower;

    public boolean outputsLiquid = false;
    public boolean singleLiquid = true;
    public boolean consumesPower = true;
    public boolean outputsPower = false;

    public int itemCapacity;
    public float liquidCapacity = 10f;
    public float liquidFlowFactor = 4.9f;

    public Consumers consumes = new Consumers();
    public Producers produces = new Producers();

    public boolean shouldConsume(Tile tile){
        return true;
    }

    public float getPowerProduction(Tile tile){
        return 0f;
    }

    /**Returns the amount of items this block can accept.*/
    public int acceptStack(Item item, int amount, Tile tile, Unit source){
        if(acceptItem(item, tile, tile) && hasItems && (source == null || source.getTeam() == tile.getTeam())){
            return Math.min(getMaximumAccepted(tile, item) - tile.entity.items.get(item), amount);
        }else{
            return 0;
        }
    }

    public int getMaximumAccepted(Tile tile, Item item){
        return itemCapacity;
    }

    /**Remove a stack from this inventory, and return the amount removed.*/
    public int removeStack(Tile tile, Item item, int amount){
        tile.entity.noSleep();
        tile.entity.items.remove(item, amount);
        return amount;
    }

    /**Handle a stack input.*/
    public void handleStack(Item item, int amount, Tile tile, Unit source){
        tile.entity.noSleep();
        tile.entity.items.add(item, amount);
    }

    public boolean outputsItems(){
        return hasItems;
    }

    /**Returns offset for stack placement.*/
    public void getStackOffset(Item item, Tile tile, Translator trns){

    }

    public void onProximityUpdate(Tile tile){
        if(tile.entity != null) tile.entity.noSleep();
    }

    public void handleItem(Item item, Tile tile, Tile source){
        tile.entity.items.add(item, 1);
    }

    public boolean acceptItem(Item item, Tile tile, Tile source){
        return tile.entity != null && consumes.has(ConsumeItem.class) && consumes.item() == item &&
            tile.entity.items.get(item) < getMaximumAccepted(tile, item);
    }

    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        return hasLiquids && tile.entity.liquids.get(liquid) + amount < liquidCapacity &&
                (!singleLiquid || (tile.entity.liquids.current() == liquid || tile.entity.liquids.get(tile.entity.liquids.current()) < 0.2f)) &&
                (!consumes.has(ConsumeLiquid.class) || consumes.liquid() == liquid);
    }

    public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        tile.entity.liquids.add(liquid, amount);
    }

    public void tryDumpLiquid(Tile tile, Liquid liquid){
        Array<Tile> proximity = tile.entity.proximity();
        int dump = tile.getDump();

        for(int i = 0; i < proximity.size; i++){
            incrementDump(tile, proximity.size);
            Tile other = proximity.get((i + dump) % proximity.size);
            Tile in = Edges.getFacingEdge(tile, other);

            if(other.getTeamID() == tile.getTeamID() && other.block().hasLiquids && canDumpLiquid(tile, other, liquid)){
                float ofract = other.entity.liquids.get(liquid) / other.block().liquidCapacity;
                float fract = tile.entity.liquids.get(liquid) / liquidCapacity;

                if(ofract < fract) tryMoveLiquid(tile, in, other, (fract - ofract) * liquidCapacity / 2f, liquid);
            }
        }

    }

    public boolean canDumpLiquid(Tile tile, Tile to, Liquid liquid){
        return true;
    }

    public void tryMoveLiquid(Tile tile, Tile tileSource, Tile next, float amount, Liquid liquid){
        float flow = Math.min(next.block().liquidCapacity - next.entity.liquids.get(liquid) - 0.001f, amount);

        if(next.block().acceptLiquid(next, tileSource, liquid, flow)){
            next.block().handleLiquid(next, tileSource, liquid, flow);
            tile.entity.liquids.remove(liquid, flow);
        }
    }

    public float tryMoveLiquid(Tile tile, Tile next, boolean leak, Liquid liquid){
        if(next == null) return 0;

        next = next.target();

        if(next.getTeamID() == tile.getTeamID() && next.block().hasLiquids && tile.entity.liquids.get(liquid) > 0f){

            if(next.block().acceptLiquid(next, tile, liquid, 0f)){
                float ofract = next.entity.liquids.get(liquid) / next.block().liquidCapacity;
                float fract = tile.entity.liquids.get(liquid) / liquidCapacity;
                float flow = Math.min(Mathf.clamp((fract - ofract) * (1f)) * (liquidCapacity), tile.entity.liquids.get(liquid));
                flow = Math.min(flow, next.block().liquidCapacity - next.entity.liquids.get(liquid) - 0.001f);

                if(flow > 0f && ofract <= fract && next.block().acceptLiquid(next, tile, liquid, flow)){
                    next.block().handleLiquid(next, tile, liquid, flow);
                    tile.entity.liquids.remove(liquid, flow);
                    return flow;
                }else if(ofract > 0.1f && fract > 0.1f){
                    Liquid other = next.entity.liquids.current();
                    if((other.flammability > 0.3f && liquid.temperature > 0.7f) ||
                            (liquid.flammability > 0.3f && other.temperature > 0.7f)){
                        tile.entity.damage(1 * Timers.delta());
                        next.entity.damage(1 * Timers.delta());
                        if(Mathf.chance(0.1 * Timers.delta())){
                            Effects.effect(EnvironmentFx.fire, (tile.worldx() + next.worldx()) / 2f, (tile.worldy() + next.worldy()) / 2f);
                        }
                    }else if((liquid.temperature > 0.7f && other.temperature < 0.55f) ||
                            (other.temperature > 0.7f && liquid.temperature < 0.55f)){
                        tile.entity.liquids.remove(liquid, Math.min(tile.entity.liquids.get(liquid), 0.7f * Timers.delta()));
                        if(Mathf.chance(0.2f * Timers.delta())){
                            Effects.effect(EnvironmentFx.steam, (tile.worldx() + next.worldx()) / 2f, (tile.worldy() + next.worldy()) / 2f);
                        }
                    }
                }
            }
        }else if(leak && !next.block().solid && !next.block().hasLiquids){
            float leakAmount = tile.entity.liquids.get(liquid) / 1.5f;
            Puddle.deposit(next, tile, liquid, leakAmount);
            tile.entity.liquids.remove(liquid, leakAmount);
        }
        return 0;
    }

    /**
     * Tries to put this item into a nearby container, if there are no available
     * containers, it gets added to the block's inventory.
     */
    public void offloadNear(Tile tile, Item item){
        Array<Tile> proximity = tile.entity.proximity();
        int dump = tile.getDump();

        for(int i = 0; i < proximity.size; i++){
            incrementDump(tile, proximity.size);
            Tile other = proximity.get((i + dump) % proximity.size);
            Tile in = Edges.getFacingEdge(tile, other);
            if(other.getTeamID() == tile.getTeamID() && other.block().acceptItem(item, other, in) && canDump(tile, other, item)){
                other.block().handleItem(item, other, in);
                return;
            }
        }

        handleItem(item, tile, tile);
    }

    /**
     * Try dumping any item near the tile.
     */
    public boolean tryDump(Tile tile){
        return tryDump(tile, null);
    }

    /**
     * Try dumping a specific item near the tile.
     *
     * @param todump Item to dump. Can be null to dump anything.
     */
    public boolean tryDump(Tile tile, Item todump){
        TileEntity entity = tile.entity;
        if(entity == null || !hasItems || tile.entity.items.total() == 0 || (todump != null && !entity.items.has(todump)))
            return false;

        Array<Tile> proximity = entity.proximity();
        int dump = tile.getDump();

        if(proximity.size == 0) return false;

        for(int i = 0; i < proximity.size; i++){
            Tile other = proximity.get((i + dump) % proximity.size);
            Tile in = Edges.getFacingEdge(tile, other);

            if(todump == null){

                for(int ii = 0; ii < Vars.content.items().size; ii++){
                    Item item = Vars.content.item(ii);

                    if(other.getTeamID() == tile.getTeamID() && entity.items.has(item) && other.block().acceptItem(item, other, in) && canDump(tile, other, item)){
                        other.block().handleItem(item, other, in);
                        tile.entity.items.remove(item, 1);
                        incrementDump(tile, proximity.size);
                        return true;
                    }
                }
            }else{

                if(other.getTeamID() == tile.getTeamID() && other.block().acceptItem(todump, other, in) && canDump(tile, other, todump)){
                    other.block().handleItem(todump, other, in);
                    tile.entity.items.remove(todump, 1);
                    incrementDump(tile, proximity.size);
                    return true;
                }
            }

            incrementDump(tile, proximity.size);
        }

        return false;
    }

    protected void incrementDump(Tile tile, int prox){
        tile.setDump((byte) ((tile.getDump() + 1) % prox));
    }

    /** Used for dumping items.*/
    public boolean canDump(Tile tile, Tile to, Item item){
        return true;
    }

    /** Try offloading an item to a nearby container in its facing direction. Returns true if success.*/
    public boolean offloadDir(Tile tile, Item item){
        Tile other = tile.getNearby(tile.getRotation());
        if(other != null && other.target().getTeamID() == tile.getTeamID() && other.block().acceptItem(item, other, tile)){
            other.block().handleItem(item, other, tile);
            return true;
        }
        return false;
    }
}
