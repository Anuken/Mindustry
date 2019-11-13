package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.collection.IntSet.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.entities.traits.BuilderTrait.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.meta.*;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

public class ItemBridge extends Block{
    protected int timerTransport = timers++;
    protected int range;
    protected float transportTime = 2f;
    protected TextureRegion endRegion, bridgeRegion, arrowRegion;
    protected BuildRequest otherReq;

    private static int lastPlaced = Pos.invalid;

    public ItemBridge(String name){
        super(name);
        update = true;
        solid = true;
        hasPower = true;
        layer = Layer.power;
        expanded = true;
        itemCapacity = 10;
        posConfig = true;
        configurable = true;
        hasItems = true;
        unloadable = false;
        group = BlockGroup.transportation;
    }

    @Override
    public void configured(Tile tile, Player player, int value){
        ItemBridgeEntity entity = tile.entity();

        if(world.tile(entity.link) != null && world.tile(entity.link).entity instanceof ItemBridgeEntity){
            ItemBridgeEntity oe = world.tile(entity.link).entity();
            oe.incoming.remove(tile.pos());
        }

        entity.link = value;

        if(world.tile(value) != null && world.tile(value).entity instanceof ItemBridgeEntity){
            ((ItemBridgeEntity)world.tile(value).entity).incoming.add(tile.pos());
        }
    }

    @Override
    public void load(){
        super.load();

        endRegion = Core.atlas.find(name + "-end");
        bridgeRegion = Core.atlas.find(name + "-bridge");
        arrowRegion = Core.atlas.find(name + "-arrow");
    }

    @Override
    public void drawRequestConfigTop(BuildRequest req, Eachable<BuildRequest> list){
        otherReq = null;
        list.each(other -> {
            if(other.block == this && req.config == Pos.get(other.x, other.y)){
                otherReq = other;
            }
        });

        if(otherReq == null) return;

        Lines.stroke(8f);
        Lines.line(bridgeRegion,
        req.drawx(),
        req.drawy(),
        otherReq.drawx(),
        otherReq.drawy(), CapStyle.none, -tilesize / 2f);
        Draw.rect(arrowRegion, (req.drawx() + otherReq.drawx()) / 2f, (req.drawy() + otherReq.drawy()) / 2f,
            Angles.angle(req.drawx(), req.drawy(), otherReq.drawx(), otherReq.drawy()));
    }

    @Override
    public void playerPlaced(Tile tile){
        Tile link = findLink(tile.x, tile.y);
        if(linkValid(tile, link)){
            link.configure(tile.pos());
        }

        lastPlaced = tile.pos();
    }

    public Tile findLink(int x, int y){
        if(world.tile(x, y) != null && linkValid(world.tile(x, y), world.tile(lastPlaced)) && lastPlaced != Pos.get(x, y)){
            return world.tile(lastPlaced);
        }
        return null;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Tile link = findLink(x, y);

        Lines.stroke(2f, Pal.placing);
        for(int i = 0; i < 4; i++){
            Lines.dashLine(
            x * tilesize + Geometry.d4[i].x * (tilesize / 2f + 2),
            y * tilesize + Geometry.d4[i].y * (tilesize / 2f + 2),
            x * tilesize + Geometry.d4[i].x * (range + 0.5f) * tilesize,
            y * tilesize + Geometry.d4[i].y * (range + 0.5f) * tilesize,
            range);
        }

        Draw.reset();
        Draw.color(Pal.placing);
        Lines.stroke(1f);
        if(link != null){
            int rot = link.absoluteRelativeTo(x, y);
            float w = (link.x == x ? tilesize : Math.abs(link.x - x) * tilesize - tilesize);
            float h = (link.y == y ? tilesize : Math.abs(link.y - y) * tilesize - tilesize);
            Lines.rect((x + link.x) / 2f * tilesize - w / 2f, (y + link.y) / 2f * tilesize - h / 2f, w, h);

            Draw.rect("bridge-arrow", link.x * tilesize + Geometry.d4[rot].x * tilesize, link.y * tilesize + Geometry.d4[rot].y * tilesize, link.absoluteRelativeTo(x, y) * 90);
        }
        Draw.reset();
    }

    @Override
    public void drawConfigure(Tile tile){
        ItemBridgeEntity entity = tile.entity();

        Draw.color(Pal.accent);
        Lines.stroke(1f);
        Lines.square(tile.drawx(), tile.drawy(),
        tile.block().size * tilesize / 2f + 1f);

        for(int i = 1; i <= range; i++){
            for(int j = 0; j < 4; j++){
                Tile other = tile.getNearby(Geometry.d4[j].x * i, Geometry.d4[j].y * i);
                if(linkValid(tile, other)){
                    boolean linked = other.pos() == entity.link;
                    Draw.color(linked ? Pal.place : Pal.breakInvalid);

                    Lines.square(other.drawx(), other.drawy(),
                    other.block().size * tilesize / 2f + 1f + (linked ? 0f : Mathf.absin(Time.time(), 4f, 1f)));
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
                tile.configure(Pos.invalid);
            }else{
                tile.configure(other.pos());
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

        IntSetIterator it = entity.incoming.iterator();
        while(it.hasNext){
            int i = it.next();
            Tile other = world.tile(i);
            if(!linkValid(tile, other, false)){
                it.remove();
            }
        }

        Tile other = world.tile(entity.link);
        if(!linkValid(tile, other)){
            tryDump(tile);
            entity.uptime = 0f;
        }else{

            if(entity.cons.valid() && (!hasPower || Mathf.zero(1f - entity.power.satisfaction))){
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

        Draw.color(Color.white, Color.black, Mathf.absin(Time.time(), 6f, 0.07f));
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
            Draw.alpha(Mathf.absin(a / (float)arrows - entity.time / 100f, 0.1f, 1f) * entity.uptime);
            Draw.rect(arrowRegion,
            tile.worldx() + Geometry.d4[i].x * (tilesize / 2f + a * 4f + time % 4f),
            tile.worldy() + Geometry.d4[i].y * (tilesize / 2f + a * 4f + time % 4f), i * 90f);
        }
        Draw.reset();
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        if(tile.getTeam() != source.getTeam()) return false;

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
    public TileEntity newEntity(){
        return new ItemBridgeEntity();
    }

    public boolean linkValid(Tile tile, Tile other){
        return linkValid(tile, other, true);
    }

    public boolean linkValid(Tile tile, Tile other, boolean checkDouble){
        if(other == null || tile == null) return false;
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
        public int link = Pos.invalid;
        public IntSet incoming = new IntSet();
        public float uptime;
        public float time;
        public float time2;
        public float cycleSpeed = 1f;

        @Override
        public int config(){
            return link;
        }

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeInt(link);
            stream.writeFloat(uptime);
            stream.writeByte(incoming.size);

            IntSetIterator it = incoming.iterator();

            while(it.hasNext){
                stream.writeInt(it.next());
            }
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            link = stream.readInt();
            uptime = stream.readFloat();
            byte links = stream.readByte();
            for(int i = 0; i < links; i++){
                incoming.add(stream.readInt());
            }
        }
    }
}
