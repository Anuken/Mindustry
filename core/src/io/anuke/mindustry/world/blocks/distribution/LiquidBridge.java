package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.arc.collection.IntSet.IntSetIterator;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.meta.BlockGroup;

import static io.anuke.mindustry.Vars.world;

public class LiquidBridge extends ItemBridge{

    public LiquidBridge(String name){
        super(name);
        hasItems = false;
        hasLiquids = true;
        outputsLiquid = true;
        group = BlockGroup.liquids;
    }

    @Override
    public void update(Tile tile){
        ItemBridgeEntity entity = tile.entity();

        entity.time += entity.cycleSpeed * Time.delta();
        entity.time2 += (entity.cycleSpeed - 1f) * Time.delta();

        Tile other = world.tile(entity.link);
        if(!linkValid(tile, other)){
            tryDumpLiquid(tile, entity.liquids.current());
        }else{
            if(entity.cons.valid()){
                float alpha = 0.04f;
                if(hasPower){
                    alpha *= entity.power.satisfaction; // Exceed boot time unless power is at max.
                }
                entity.uptime = Mathf.lerpDelta(entity.uptime, 1f, alpha);
            }else{
                entity.uptime = Mathf.lerpDelta(entity.uptime, 0f, 0.02f);
            }

            if(entity.uptime >= 0.5f){

                if(tryMoveLiquid(tile, other, false, entity.liquids.current()) > 0.1f){
                    entity.cycleSpeed = Mathf.lerpDelta(entity.cycleSpeed, 4f, 0.05f);
                }else{
                    entity.cycleSpeed = Mathf.lerpDelta(entity.cycleSpeed, 1f, 0.01f);
                }
            }
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return false;
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        if(tile.getTeam() != source.getTeam()) return false;

        ItemBridgeEntity entity = tile.entity();
        Tile other = world.tile(entity.link);

        if(linkValid(tile, other)){
            int rel = tile.absoluteRelativeTo(other.x, other.y);
            int rel2 = tile.relativeTo(source.x, source.y);

            if(rel == rel2) return false;
        }else if(!(source.block() instanceof ItemBridge && source.<ItemBridgeEntity>entity().link == tile.pos())){
            return false;
        }

        return tile.entity.liquids.get(liquid) + amount < liquidCapacity && (tile.entity.liquids.current() == liquid || tile.entity.liquids.get(tile.entity.liquids.current()) < 0.2f);
    }

    @Override
    public boolean canDumpLiquid(Tile tile, Tile to, Liquid liquid){
        ItemBridgeEntity entity = tile.entity();

        Tile other = world.tile(entity.link);
        if(!linkValid(tile, other)){
            Tile edge = Edges.getFacingEdge(to, tile);
            int i = tile.absoluteRelativeTo(edge.x, edge.y);

            IntSetIterator it = entity.incoming.iterator();

            while(it.hasNext){
                int v = it.next();
                if(tile.absoluteRelativeTo(Pos.x(v), Pos.y(v)) == i){
                    return false;
                }
            }
            return true;
        }

        int rel = tile.absoluteRelativeTo(other.x, other.y);
        int rel2 = tile.relativeTo(to.x, to.y);

        return rel != rel2;
    }
}
