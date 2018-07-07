package io.anuke.mindustry.world.blocks.distribution;

import com.badlogic.gdx.math.GridPoint2;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Edges;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockGroup;

import static io.anuke.mindustry.Vars.world;

public class Splitter extends Block{

    public Splitter(String name){
        super(name);
        solid = true;
        instantTransfer = true;
        destructible = true;
        group = BlockGroup.transportation;
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        Tile to = getTileTarget(item, tile, source, false);

        return to != null && to.block().acceptItem(item, to, tile);
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        Tile to = getTileTarget(item, tile, source, true);

        to.block().handleItem(item, to, tile);
    }

    Tile getTileTarget(Item item, Tile dest, Tile source, boolean flip){
        GridPoint2[] points = Edges.getEdges(size);
        int counter = source.getDump();
        for (int i = 0; i < points.length; i++) {
            GridPoint2 point = points[(i + counter++) % points.length];
            source.setDump((byte)(counter % points.length));
            Tile tile = world.tile(dest.x + point.x, dest.y + point.y);
            if(tile != source && !(tile.block().instantTransfer && source.block().instantTransfer) &&
                    tile.block().acceptItem(item, tile, dest)){
                return tile;
            }
        }
        return null;
    }
}
