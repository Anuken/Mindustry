package io.anuke.mindustry.world.blocks.types.distribution;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.IntSet.IntSetIterator;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;
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

public class ItemBridge extends Block {
    protected static int lastPlaced;

    protected int timerTransport = timers++;
    protected int range;
    protected float powerUse = 0.05f;
    protected float transportTime = 2f;
    protected IntArray removals = new IntArray();

    public ItemBridge(String name) {
        super(name);
        update = true;
        solid = true;
        hasPower = true;
        layer = Layer.power;
        expanded = true;
        itemCapacity = 30;
    }

    @Override
    public void placed(Tile tile) {
        Tile last = world.tile(lastPlaced);
        if(linkValid(tile, last)){
            ItemBridgeEntity entity = last.entity();
            if(!linkValid(last, world.tile(entity.link))){
                link(last, tile);
            }
        }
        lastPlaced = tile.packedPosition();
    }

    @Override
    public boolean isConfigurable(Tile tile) {
        return true;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        Lines.stroke(2f);
        Draw.color("place");
        for(int i = 0; i < 4; i ++){
            Lines.dashLine(
                    x * tilesize + Geometry.d4[i].x * tilesize/2f,
                    y * tilesize + Geometry.d4[i].y * tilesize/2f,
                    x * tilesize + Geometry.d4[i].x * range * tilesize,
                    y * tilesize + Geometry.d4[i].y * range * tilesize,
                    range);
        }

        Draw.reset();
    }

    @Override
    public void drawConfigure(Tile tile){
        ItemBridgeEntity entity = tile.entity();

        Draw.color("accent");
        Lines.stroke(1f);
        Lines.square(tile.drawx(), tile.drawy(),
                tile.block().size * tilesize / 2f + 1f);

        for(int i = 1; i <= range; i ++){
            for(int j = 0; j < 4; j ++){
                Tile other = tile.getNearby(Geometry.d4[j].x * i, Geometry.d4[j].y * i);
                if(linkValid(tile, other)){
                    boolean linked = other.packedPosition() == entity.link;
                    Draw.color(linked ? "place" : "breakInvalid");

                    Lines.square(other.drawx(), other.drawy(),
                            other.block().size * tilesize / 2f + 1f + (linked ? 0f : Mathf.absin(Timers.time(), 4f, 1f)));
                }
            }
        }

        Draw.reset();
    }

    @Override
    public boolean onConfigureTileTapped(Tile tile, Tile other) {
        ItemBridgeEntity entity = tile.entity();

        if(linkValid(tile, other)){
            if(entity.link == other.packedPosition()){
                unlink(tile, other);
            }else{
                link(tile, other);
            }
            return false;
        }
        return true;
    }

    @Override
    public void update(Tile tile) {
        ItemBridgeEntity entity = tile.entity();

        entity.time += entity.cycleSpeed*Timers.delta();
        entity.time2 += (entity.cycleSpeed-1f)*Timers.delta();

        removals.clear();

        IntSetIterator it = entity.incoming.iterator();

        while(it.hasNext){
            int i = it.next();
            Tile other = world.tile(i);
            if(!linkValid(tile, other, false)){
                removals.add(i);
            }
        }

        for(int j = 0; j < removals.size; j ++)
            entity.incoming.remove(removals.get(j));

        Tile other = world.tile(entity.link);
        if(!linkValid(tile, other)){
            tryDump(tile);
            entity.uptime = 0f;
        }else{
            float use = Math.min(powerCapacity, powerUse * Timers.delta());

            if(!hasPower || entity.power.amount >= use){
                entity.uptime = Mathf.lerpDelta(entity.uptime, 1f, 0.04f);
                if(hasPower) entity.power.amount -= use;
            }else{
                entity.uptime = Mathf.lerpDelta(entity.uptime, 0f, 0.02f);
            }

            updateTransport(tile, other);
        }
    }

    public void updateTransport(Tile tile, Tile other){
        ItemBridgeEntity entity = tile.entity();

        if(entity.uptime >= 0.5f && entity.timer.get(timerTransport, transportTime)){
            Item item = entity.items.takeItem();
            if(item != null && other.block().acceptItem(item, other, tile)){
                other.block().handleItem(item, other, tile);
                entity.cycleSpeed = Mathf.lerpDelta(entity.cycleSpeed, 4f, 0.05f);
            }else{
                entity.cycleSpeed = Mathf.lerpDelta(entity.cycleSpeed, 1f, 0.01f);
                if(item != null) entity.items.addItem(item, 1);
            }
        }
    }

    @Override
    public void drawLayer(Tile tile) {
        ItemBridgeEntity entity = tile.entity();

        Tile other = world.tile(entity.link);
        if(!linkValid(tile, other)) return;

        int i = tile.absoluteRelativeTo(other.x, other.y);

        Draw.color(Color.WHITE, Color.BLACK, Mathf.absin(Timers.time(), 6f, 0.07f));
        Draw.alpha(Math.max(entity.uptime, 0.25f));

        Draw.rect(name + "-end", tile.drawx(), tile.drawy(), i*90 + 90);
        Draw.rect(name + "-end", other.drawx(), other.drawy(), i*90 + 270);

        Lines.stroke(8f);
        Lines.line(Draw.region(name + "-bridge"),
                tile.worldx(),
                tile.worldy(),
                other.worldx(),
                other.worldy(), CapStyle.none, -tilesize/2f);

        int dist = Math.max(Math.abs(other.x - tile.x), Math.abs(other.y - tile.y));

        float time = entity.time2/1.7f;
        int arrows = (dist)*tilesize/4-2;

        Draw.color();

        for(int a = 0; a < arrows; a ++){
            Draw.alpha(Mathf.absin(a/(float)arrows - entity.time/100f, 0.1f, 1f) * entity.uptime);
            Draw.rect(name + "-arrow",
                    tile.worldx() + Geometry.d4[i].x*(tilesize/2f + a*4f + time % 4f),
                    tile.worldy() + Geometry.d4[i].y*(tilesize/2f + a*4f + time % 4f),
                    i*90f);
        }
        Draw.reset();
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source) {
        return tile.entity.items.totalItems() < itemCapacity;
    }

    @Override
    public boolean canDump(Tile tile, Tile to, Item item) {
        ItemBridgeEntity entity = tile.entity();

        Tile other = world.tile(entity.link);
        if(!linkValid(tile, other)){
            int i = tile.absoluteRelativeTo(to.x, to.y);

            IntSetIterator it = entity.incoming.iterator();

            while(it.hasNext){
                int v = it.next();
                int x = v % world.width();
                int y = v / world.width();
                if(tile.absoluteRelativeTo(x, y) == i){
                    return false;
                }
            }
            return true;
        }

        int rel = tile.absoluteRelativeTo(other.x, other.y);
        int rel2 = tile.relativeTo(to.x, to.y);

        return rel != rel2;
    }

    @Override
    public TileEntity getEntity() {
        return new ItemBridgeEntity();
    }

    public void link(Tile tile, Tile other){
        ItemBridgeEntity entity = tile.entity();
        ItemBridgeEntity oe = other.entity();
        entity.link = other.packedPosition();
        oe.incoming.add(tile.packedPosition());
    }

    public void unlink(Tile tile, Tile other){
        ItemBridgeEntity entity = tile.entity();
        entity.link = -1;
        if(other != null) {
            ItemBridgeEntity oe = other.entity();
            oe.incoming.remove(tile.packedPosition());
        }
    }

    public boolean linkValid(Tile tile, Tile other){
        return linkValid(tile, other, true);
    }

    public boolean linkValid(Tile tile, Tile other, boolean checkDouble){
        if(other == null) return false;
        if(tile.x == other.x){
            if(Math.abs(tile.y - other.y) > range) return false;
        }else if(tile.y == other.y){
            if(Math.abs(tile.x - other.x) > range) return false;
        }else{
            return false;
        }

        return other.block() == this && (!checkDouble || other.<ItemBridgeEntity>entity().link != tile.packedPosition());
    }

    public static class ItemBridgeEntity extends TileEntity{
        public int link = -1;
        public IntSet incoming = new IntSet();
        public float uptime;
        public float time;
        public float time2;
        public float cycleSpeed = 1f;

        @Override
        public void write(DataOutputStream stream) throws IOException {
            stream.writeInt(link);
            stream.writeFloat(uptime);
            stream.writeByte(incoming.size);

            IntSetIterator it = incoming.iterator();

            while(it.hasNext){
                stream.writeInt(it.next());
            }
        }

        @Override
        public void read(DataInputStream stream) throws IOException {
            link = stream.readInt();
            uptime = stream.readFloat();
            byte links = stream.readByte();
            for(int i = 0; i < links; i ++){
                incoming.add(stream.readInt());
            }
        }
    }
}
