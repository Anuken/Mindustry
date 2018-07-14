package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.graphics.CapStyle;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class ExtendingItemBridge extends ItemBridge{

    public ExtendingItemBridge(String name){
        super(name);
        hasItems = true;
    }

    @Override
    public void drawLayer(Tile tile){
        ItemBridgeEntity entity = tile.entity();

        Tile other = world.tile(entity.link);
        if(!linkValid(tile, other)) return;

        int i = tile.absoluteRelativeTo(other.x, other.y);

        float ex = other.worldx() - tile.worldx() - Geometry.d4[i].x * tilesize / 2f,
                ey = other.worldy() - tile.worldy() - Geometry.d4[i].y * tilesize / 2f;

        ex *= entity.uptime;
        ey *= entity.uptime;

        Lines.stroke(8f);
        Lines.line(bridgeRegion,
                tile.worldx() + Geometry.d4[i].x * tilesize / 2f,
                tile.worldy() + Geometry.d4[i].y * tilesize / 2f,
                tile.worldx() + ex,
                tile.worldy() + ey, CapStyle.none, 0f);

        Draw.rect(endRegion, tile.drawx(), tile.drawy(), i * 90 + 90);
        Draw.rect(endRegion,
                tile.worldx() + ex + Geometry.d4[i].x * tilesize / 2f,
                tile.worldy() + ey + Geometry.d4[i].y * tilesize / 2f, i * 90 + 270);

        int dist = Math.max(Math.abs(other.x - tile.x), Math.abs(other.y - tile.y));

        int arrows = (dist) * tilesize / 6 - 1;

        Draw.color();

        for(int a = 0; a < arrows; a++){
            Draw.alpha(Mathf.absin(a / (float) arrows - entity.time / 100f, 0.1f, 1f) * entity.uptime);
            Draw.rect(arrowRegion,
                    tile.worldx() + Geometry.d4[i].x * (tilesize / 2f + a * 6f + 2) * entity.uptime,
                    tile.worldy() + Geometry.d4[i].y * (tilesize / 2f + a * 6f + 2) * entity.uptime,
                    i * 90f);
        }
        Draw.reset();
    }
}
