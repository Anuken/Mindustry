package io.anuke.mindustry.world.blocks.distribution;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.IntSet.IntSetIterator;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Edges;
import io.anuke.mindustry.world.Pos;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockGroup;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.CapStyle;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class ItemBridge extends Block{
    protected static int lastPlaced;

    protected int timerTransport = timers++;
    protected int range;
    protected float transportTime = 2f;
    protected IntArray removals = new IntArray();

    protected TextureRegion endRegion, bridgeRegion, arrowRegion;

    public ItemBridge(String name){
        super(name);
        update = true;
        solid = true;
        hasPower = true;
        layer = Layer.power;
        expanded = true;
        itemCapacity = 10;
        configurable = true;
        hasItems = true;
        group = BlockGroup.transportation;
    }

    @Remote(targets = Loc.both, called = Loc.both, forward = true)
    public static void linkItemBridge(Player player, Tile tile, Tile other){
        ItemBridgeEntity entity = tile.entity();
        ItemBridgeEntity oe = other.entity();
        entity.link = other.pos();
        oe.incoming.add(tile.pos());
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true)
    public static void unlinkItemBridge(Player player, Tile tile, Tile other){
        ItemBridgeEntity entity = tile.entity();
        entity.link = -1;
        if(other != null){
            ItemBridgeEntity oe = other.entity();
            oe.incoming.remove(tile.pos());
        }
    }

    @Override
    public void load(){
        super.load();

        endRegion = Draw.region(name + "-end");
        bridgeRegion = Draw.region(name + "-bridge");
        arrowRegion = Draw.region(name + "-arrow");
    }

    @Override
    public void playerPlaced(Tile tile){
        Tile last = world.tile(lastPlaced);
        if(linkValid(tile, last)){
            ItemBridgeEntity entity = last.entity();
            if(!linkValid(last, world.tile(entity.link))){
                Call.linkItemBridge(null, last, tile);
            }
        }
        lastPlaced = tile.pos();
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Lines.stroke(2f);
        Draw.color(Palette.placing);
        for(int i = 0; i < 4; i++){
            Lines.dashLine(
                    x * tilesize + Geometry.d4[i].x * (tilesize / 2f + 2),
                    y * tilesize + Geometry.d4[i].y * (tilesize / 2f + 2),
                    x * tilesize + Geometry.d4[i].x * (range + 0.5f) * tilesize,
                    y * tilesize + Geometry.d4[i].y * (range + 0.5f) * tilesize,
                    range);
        }

        Draw.reset();
    }

    @Override
    public void drawConfigure(Tile tile){
        ItemBridgeEntity entity = tile.entity();

        Draw.color(Palette.accent);
        Lines.stroke(1f);
        Lines.square(tile.drawx(), tile.drawy(),
                tile.block().size * tilesize / 2f + 1f);

        for(int i = 1; i <= range; i++){
            for(int j = 0; j < 4; j++){
                Tile other = tile.getNearby(Geometry.d4[j].x * i, Geometry.d4[j].y * i);
                if(linkValid(tile, other)){
                    boolean linked = other.pos() == entity.link;
                    Draw.color(linked ? Palette.place : Palette.breakInvalid);

                    Lines.square(other.drawx(), other.drawy(),
                            other.block().size * tilesize / 2f + 1f + (linked ? 0f : Mathf.absin(Timers.time(), 4f, 1f)));
                }
            }
        }

        Draw.reset();
    }

    @Override
    public boolean onConfigureTileTapped(Tile tile, Tile other){
        ItemBridgeEntity entity = tile.entity();

        if(linkValid(tile, other)){
            if(entity.link == other.pos()){
                Call.unlinkItemBridge(null, tile, other);
            }else{
                Call.linkItemBridge(null, tile, other);
            }
            return false;
        }
        return true;
    }

    @Override
    public void update(Tile tile){
        ItemBridgeEntity entity = tile.entity();

        entity.time += entity.cycleSpeed * entity.delta();
        entity.time2 += (entity.cycleSpeed - 1f) * entity.delta();

        removals.clear();

        IntSetIterator it = entity.incoming.iterator();

        while(it.hasNext){
            int i = it.next();
            Tile other = world.tile(i);
            if(!linkValid(tile, other, false)){
                removals.add(i);
            }
        }

        for(int j = 0; j < removals.size; j++)
            entity.incoming.remove(removals.get(j));

        Tile other = world.tile(entity.link);
        if(!linkValid(tile, other)){
            tryDump(tile);
            entity.uptime = 0f;
        }else{

            if(entity.cons.valid()){
                entity.uptime = Mathf.lerpDelta(entity.uptime, 1f, 0.04f);
            }else{
                entity.uptime = Mathf.lerpDelta(entity.uptime, 0f, 0.02f);
            }

            updateTransport(tile, other);
        }
    }

    public void updateTransport(Tile tile, Tile other){
        ItemBridgeEntity entity = tile.entity();

        if(entity.uptime >= 0.5f && entity.timer.get(timerTransport, transportTime)){
            Item item = entity.items.take();
            if(item != null && other.block().acceptItem(item, other, tile)){
                other.block().handleItem(item, other, tile);
                entity.cycleSpeed = Mathf.lerpDelta(entity.cycleSpeed, 4f, 0.05f);
            }else{
                entity.cycleSpeed = Mathf.lerpDelta(entity.cycleSpeed, 1f, 0.01f);
                if(item != null) entity.items.add(item, 1);
            }
        }
    }

    @Override
    public void drawLayer(Tile tile){
        ItemBridgeEntity entity = tile.entity();

        Tile other = world.tile(entity.link);
        if(!linkValid(tile, other)) return;

        int i = tile.absoluteRelativeTo(other.x, other.y);

        Draw.color(Color.WHITE, Color.BLACK, Mathf.absin(Timers.time(), 6f, 0.07f));
        Draw.alpha(Math.max(entity.uptime, 0.25f));

        Draw.rect(endRegion, tile.drawx(), tile.drawy(), i * 90 + 90);
        Draw.rect(endRegion, other.drawx(), other.drawy(), i * 90 + 270);

        Lines.stroke(8f);
        Lines.line(bridgeRegion,
                tile.worldx(),
                tile.worldy(),
                other.worldx(),
                other.worldy(), CapStyle.none, -tilesize / 2f);

        int dist = Math.max(Math.abs(other.x - tile.x), Math.abs(other.y - tile.y));

        float time = entity.time2 / 1.7f;
        int arrows = (dist) * tilesize / 4 - 2;

        Draw.color();

        for(int a = 0; a < arrows; a++){
            Draw.alpha(Mathf.absin(a / (float) arrows - entity.time / 100f, 0.1f, 1f) * entity.uptime);
            Draw.rect(arrowRegion,
                    tile.worldx() + Geometry.d4[i].x * (tilesize / 2f + a * 4f + time % 4f),
                    tile.worldy() + Geometry.d4[i].y * (tilesize / 2f + a * 4f + time % 4f),
                    i * 90f);
        }
        Draw.reset();
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        if(tile.getTeamID() != source.target().getTeamID()) return false;

        ItemBridgeEntity entity = tile.entity();
        Tile other = world.tile(entity.link);

        if(linkValid(tile, other)){
            int rel = tile.absoluteRelativeTo(other.x, other.y);
            int rel2 = tile.relativeTo(source.x, source.y);

            if(rel == rel2) return false;
        }else{
            return source.block() instanceof ItemBridge && source.<ItemBridgeEntity>entity().link == tile.pos() && tile.entity.items.total() < itemCapacity;
        }

        return tile.entity.items.total() < itemCapacity;
    }

    @Override
    public boolean canDump(Tile tile, Tile to, Item item){
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

    @Override
    public void transformLinks(Tile tile, int oldWidth, int oldHeight, int newWidth, int newHeight, int shiftX, int shiftY){
        super.transformLinks(tile, oldWidth, oldHeight, newWidth, newHeight, shiftX, shiftY);

        ItemBridgeEntity entity = tile.entity();
        entity.link = world.transform(entity.link, oldWidth, oldHeight, newWidth, shiftX, shiftY);
    }

    @Override
    public TileEntity newEntity(){
        return new ItemBridgeEntity();
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

        return other.block() == this && (!checkDouble || other.<ItemBridgeEntity>entity().link != tile.pos());
    }

    public static class ItemBridgeEntity extends TileEntity{
        public int link = -1;
        public IntSet incoming = new IntSet();
        public float uptime;
        public float time;
        public float time2;
        public float cycleSpeed = 1f;

        @Override
        public void write(DataOutput stream) throws IOException{
            stream.writeInt(link);
            stream.writeFloat(uptime);
            stream.writeByte(incoming.size);

            IntSetIterator it = incoming.iterator();

            while(it.hasNext){
                stream.writeInt(it.next());
            }
        }

        @Override
        public void read(DataInput stream) throws IOException{
            link = stream.readInt();
            uptime = stream.readFloat();
            byte links = stream.readByte();
            for(int i = 0; i < links; i++){
                incoming.add(stream.readInt());
            }
        }
    }
}
