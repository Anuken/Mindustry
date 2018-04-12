package io.anuke.mindustry.world.blocks.types.distribution;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.ItemBuffer;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.graphics.CapStyle;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class BufferedItemBridge extends ItemBridge {
    protected int timerAccept = timers ++;

    protected float speed = 40f;
    protected int bufferCapacity = 50;

    public BufferedItemBridge(String name) {
        super(name);
        hasPower = false;
    }

    @Override
    public void updateTransport(Tile tile, Tile other){
        BufferedItemBridgeEntity entity = tile.entity();

        if(entity.buffer.accepts() && entity.inventory.totalItems() > 0){
            entity.buffer.accept(entity.inventory.takeItem());
        }

        Item item = entity.buffer.poll();
        if(entity.timer.get(timerAccept, 4) && item != null && other.block().acceptItem(item, other, tile)){
            entity.cycleSpeed = Mathf.lerpDelta(entity.cycleSpeed, 4f, 0.05f);
            other.block().handleItem(item, other, tile);
            entity.buffer.remove();
        }else{
            entity.cycleSpeed = Mathf.lerpDelta(entity.cycleSpeed, 0f, 0.008f);
        }
    }

    @Override
    public void drawLayer(Tile tile) {
        BufferedItemBridgeEntity entity = tile.entity();

        Tile other = world.tile(entity.link);
        if(!linkValid(tile, other)) return;

        int i = tile.absoluteRelativeTo(other.x, other.y);

        float ex = other.worldx() - tile.worldx(),
                ey = other.worldy() - tile.worldy();

        ex *= entity.uptime;
        ey *= entity.uptime;

        Lines.stroke(8f);
        Lines.line(Draw.region(name + "-bridge"),
                tile.worldx(),
                tile.worldy(),
                tile.worldx() + ex,
                tile.worldy() + ey, CapStyle.none, -tilesize/2f);

        Draw.rect(name + "-end", tile.drawx(), tile.drawy(), i*90 + 90);
        Draw.rect(name + "-end", tile.worldx() + ex, tile.worldy() + ey, i*90 + 270);

        int dist = Math.max(Math.abs(other.x - tile.x), Math.abs(other.y - tile.y));

        int arrows = (dist)*tilesize/6-1;

        Draw.color();

        for(int a = 0; a < arrows; a ++){
            Draw.alpha(Mathf.absin(a/(float)arrows - entity.time/100f, 0.1f, 1f) * entity.uptime);
            Draw.rect(name + "-arrow",
                    tile.worldx() + Geometry.d4[i].x*(tilesize/2f + a*6f + 2) * entity.uptime,
                    tile.worldy() + Geometry.d4[i].y*(tilesize/2f + a*6f + 2) * entity.uptime,
                    i*90f);
        }
        Draw.reset();
    }

    @Override
    public TileEntity getEntity() {
        return new BufferedItemBridgeEntity();
    }

    class BufferedItemBridgeEntity extends ItemBridgeEntity{
        ItemBuffer buffer = new ItemBuffer(bufferCapacity, speed);

        @Override
        public void write(DataOutputStream stream) throws IOException {
            super.write(stream);
        }

        @Override
        public void read(DataInputStream stream) throws IOException {
            super.read(stream);
        }
    }
}
