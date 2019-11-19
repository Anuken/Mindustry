package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.meta.BlockGroup;

public class Router extends Block{
    protected float speed = 8f;
    private TextureRegion overlayArrowRegion, overlayArrowBodyRegion, overlayArrowShadeRegion;

    public Router(String name){
        super(name);
        solid = true;
        update = true;
        hasItems = true;
        itemCapacity = 1;
        group = BlockGroup.transportation;
        unloadable = false;
        entityType = RouterEntity::new;
    }

    @Override
    public void load(){
        super.load();

        overlayArrowRegion = Core.atlas.find("overlay-arrow");
        overlayArrowBodyRegion = Core.atlas.find("overlay-arrow-body");
        overlayArrowShadeRegion = Core.atlas.find("overlay-arrow-shade");
    }

    @Override
    public void drawDebug(Tile tile){
        super.drawDebug(tile);

        int proximityTiles = 0;
        int proximityAlternatives = 0;
        for (Tile proximityTile : tile.entity.proximity()) {
            byte relativePosition = tile.relativeTo(proximityTile);
            if (relativePosition >= 0) {
                if (proximityTile.block().outputsItems()) {
                    proximityTiles |= 1 << relativePosition;
                }

                if (proximityTile.block() instanceof Junction) {
                    proximityAlternatives |= 1 << relativePosition;
                }
            }
        }

        for (int i = 0; i < 4; i++) {
            // Always has horizontal and vertical bodies
            Draw.color(1, 1, 1, 0.8f);
            Draw.rect(overlayArrowBodyRegion, tile.drawx(), tile.drawy(), 90 * i - 90);

            if ((proximityTiles & 1 << i) == 0) {
                Draw.color(1, 1, 1, 0.8f);
                Draw.rect(overlayArrowRegion, tile.drawx(), tile.drawy(), 90 * i - 90);
            }

            if ((proximityAlternatives & 1 << i) > 0) {
                if (i % 2 == 0) {
                    Draw.color(0.2f, 0.8f, 0.2f, 0.5f);
                } else {
                    Draw.color(0.2f, 0.2f, 0.8f, 0.5f);
                }

                Draw.rect(overlayArrowShadeRegion, tile.drawx(), tile.drawy(), 90 * i - 90);
            }
        }

        Draw.color();
    }

    @Override
    public void update(Tile tile){
        RouterEntity entity = tile.entity();

        if(entity.lastItem == null && entity.items.total() > 0){
            entity.items.clear();
        }

        if(entity.lastItem != null){
            entity.time += 1f / speed * Time.delta();
            Tile target = getTileTarget(tile, entity.lastItem, entity.lastInput, false);

            if(target != null && (entity.time >= 1f || !(target.block() instanceof Router))){
                getTileTarget(tile, entity.lastItem, entity.lastInput, true);
                target.block().handleItem(entity.lastItem, target, Edges.getFacingEdge(tile, target));
                entity.items.remove(entity.lastItem, 1);
                entity.lastItem = null;
            }
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        RouterEntity entity = tile.entity();

        return tile.getTeam() == source.getTeam() && entity.lastItem == null && entity.items.total() == 0;
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        RouterEntity entity = tile.entity();
        entity.items.add(item, 1);
        entity.lastItem = item;
        entity.time = 0f;
        entity.lastInput = source;
    }

    Tile getTileTarget(Tile tile, Item item, Tile from, boolean set){
        Array<Tile> proximity = tile.entity.proximity();
        int counter = tile.rotation();
        for(int i = 0; i < proximity.size; i++){
            Tile other = proximity.get((i + counter) % proximity.size);
            if(set) tile.rotation((byte)((tile.rotation() + 1) % proximity.size));
            if(other == from && from.block() == Blocks.overflowGate) continue;
            if(other.block().acceptItem(item, other, Edges.getFacingEdge(tile, other))){
                return other;
            }
        }
        return null;
    }

    @Override
    public int removeStack(Tile tile, Item item, int amount){
        RouterEntity entity = tile.entity();
        int result = super.removeStack(tile, item, amount);
        if(result != 0 && item == entity.lastItem){
            entity.lastItem = null;
        }
        return result;
    }

    public class RouterEntity extends TileEntity{
        Item lastItem;
        Tile lastInput;
        float time;
    }
}
