package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

/**
 * Used for multiblocks. Each block that is not the center of the multiblock is a blockpart.
 * Think of these as delegates to the actual block; all events are passed to the target block.
 * They are made to share all properties from the linked tile/block.
 */
public class BlockPart extends Block{

    public BlockPart(){
        super("blockpart");
        solid = false;
        hasPower = hasItems = hasLiquids = true;
        viewRange = -1;
    }

    @Override
    public void draw(Tile tile){
        //do nothing
    }

    @Override
    public void drawShadow(Tile tile){
        //also do nothing
    }

    @Override
    public boolean isSolidFor(Tile tile){
        return tile.getLinked() == null
                || (tile.getLinked().block() instanceof BlockPart || tile.getLinked().solid()
                || tile.getLinked().block().isSolidFor(tile.getLinked()));
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        tile.getLinked().block().handleItem(item, tile.getLinked(), source);
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return tile.getLinked().block().acceptItem(item, tile.getLinked(), source);
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        Block block = linked(tile);
        return block.hasLiquids
                && block.acceptLiquid(tile.getLinked(), source, liquid, amount);
    }

    @Override
    public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        Block block = linked(tile);
        block.handleLiquid(tile.getLinked(), source, liquid, amount);
    }

    private Block linked(Tile tile){
        return tile.getLinked().block();
    }

}
