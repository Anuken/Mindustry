package mindustry.world.blocks.distribution;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.struct.IntSet.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class ItemBridge extends Block{
    private static BuildRequest otherReq;

    public final int timerTransport = timers++;
    public int range;
    public float transportTime = 2f;
    public @Load("@-end") TextureRegion endRegion;
    public @Load("@-bridge") TextureRegion bridgeRegion;
    public @Load("@-arrow") TextureRegion arrowRegion;
    public int lastPlaced = -1;

    public ItemBridge(String name){
        super(name);
        update = true;
        solid = true;
        hasPower = true;
        expanded = true;
        itemCapacity = 10;
        configurable = true;
        hasItems = true;
        unloadable = false;
        group = BlockGroup.transportation;
        //point2 config is relative
        config(Point2.class, (tile, i) -> ((ItemBridgeEntity)tile).link = Point2.pack(i.x + tile.tileX(), i.y + tile.tileY()));
        //integer is not
        config(Integer.class, (tile, i) -> ((ItemBridgeEntity)tile).link = i);
    }

    @Override
    public void drawRequestConfigTop(BuildRequest req, Eachable<BuildRequest> list){
        otherReq = null;
        list.each(other -> {
            if(other.block == this && req != other && req.config instanceof Point2 && ((Point2)req.config).equals(other.x - req.x, other.y - req.y)){
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

        return other.block() == this && (other.team() == tile.team() || tile.block() != this) && (!checkDouble || other.<ItemBridgeEntity>ent().link != tile.pos());
    }

    public Tile findLink(int x, int y){
        if(world.tiles.in(x, y) && linkValid(world.tile(x, y), world.tile(lastPlaced)) && lastPlaced != Point2.pack(x, y)){
            return world.tile(lastPlaced);
        }
        return null;
    }

    public class ItemBridgeEntity extends TileEntity{
        public int link = -1;
        public IntSet incoming = new IntSet();
        public float uptime;
        public float time;
        public float time2;
        public float cycleSpeed = 1f;

        @Override
        public void playerPlaced(){
            Tile link = findLink(tile.x, tile.y);
            if(linkValid(tile, link)){
                link.entity.configure(tile.pos());
            }

            lastPlaced = tile.pos();
        }

        @Override
        public void drawConfigure(){
            Draw.color(Pal.accent);
            Lines.stroke(1f);
            Lines.square(x, y,
            tile.block().size * tilesize / 2f + 1f);

            for(int i = 1; i <= range; i++){
                for(int j = 0; j < 4; j++){
                    Tile other = tile.getNearby(Geometry.d4[j].x * i, Geometry.d4[j].y * i);
                    if(linkValid(tile, other)){
                        boolean linked = other.pos() == link;
                        Draw.color(linked ? Pal.place : Pal.breakInvalid);

                        Lines.square(other.drawx(), other.drawy(),
                        other.block().size * tilesize / 2f + 1f + (linked ? 0f : Mathf.absin(Time.time(), 4f, 1f)));
                    }
                }
            }

            Draw.reset();
        }

        @Override
        public boolean onConfigureTileTapped(Tilec other){
            if(linkValid(tile, other.tile())){
                if(link == other.pos()){
                    configure(-1);
                }else{
                    configure(other.pos());
                }
                return false;
            }
            return true;
        }

        public void checkIncoming(){
            IntSetIterator it = incoming.iterator();
            while(it.hasNext){
                int i = it.next();
                Tile other = world.tile(i);
                if(!linkValid(tile, other, false) || other.<ItemBridgeEntity>ent().link != tile.pos()){
                    it.remove();
                }
            }
        }

        @Override
        public void updateTile(){
            time += cycleSpeed * delta();
            time2 += (cycleSpeed - 1f) * delta();

            checkIncoming();

            Tile other = world.tile(link);
            if(!linkValid(tile, other)){
                dump();
                uptime = 0f;
            }else{
                ((ItemBridgeEntity)other.entity).incoming.add(tile.pos());

                if(consValid() && Mathf.zero(1f - efficiency())){
                    uptime = Mathf.lerpDelta(uptime, 1f, 0.04f);
                }else{
                    uptime = Mathf.lerpDelta(uptime, 0f, 0.02f);
                }

                updateTransport(other.entity);
            }
        }

        public void updateTransport(Tilec other){
            if(uptime >= 0.5f && timer(timerTransport, transportTime)){
                Item item = items.take();
                if(item != null && other.acceptItem(this, item)){
                    other.handleItem(this, item);
                    cycleSpeed = Mathf.lerpDelta(cycleSpeed, 4f, 0.05f);
                }else{
                    cycleSpeed = Mathf.lerpDelta(cycleSpeed, 1f, 0.01f);
                    if(item != null) items.add(item, 1);
                }
            }
        }

        @Override
        public void draw(){
            super.draw();

            Draw.z(Layer.power);

            Tile other = world.tile(link);
            if(!linkValid(tile, other)) return;

            float opacity = Core.settings.getInt("bridgeopacity") / 100f;
            if(Mathf.zero(opacity)) return;

            int i = tile.absoluteRelativeTo(other.x, other.y);

            Draw.color(Color.white, Color.black, Mathf.absin(Time.time(), 6f, 0.07f));
            Draw.alpha(Math.max(uptime, 0.25f) * opacity);

            Draw.rect(endRegion, x, y, i * 90 + 90);
            Draw.rect(endRegion, other.drawx(), other.drawy(), i * 90 + 270);

            Lines.stroke(8f);
            Lines.line(bridgeRegion,
            x,
            y,
            other.worldx(),
            other.worldy(), CapStyle.none, -tilesize / 2f);

            int dist = Math.max(Math.abs(other.x - tile.x), Math.abs(other.y - tile.y));

            float time = time2 / 1.7f;
            int arrows = (dist) * tilesize / 4 - 2;

            Draw.color();

            for(int a = 0; a < arrows; a++){
                Draw.alpha(Mathf.absin(a / (float)arrows - time / 100f, 0.1f, 1f) * uptime * opacity);
                Draw.rect(arrowRegion,
                x + Geometry.d4[i].x * (tilesize / 2f + a * 4f + time % 4f),
                y + Geometry.d4[i].y * (tilesize / 2f + a * 4f + time % 4f), i * 90f);
            }
            Draw.reset();
        }

        @Override
        public boolean acceptItem(Tilec source, Item item){
            if(team != source.team()) return false;

            Tile other = world.tile(link);

            if(linkValid(tile, other)){
                int rel = tile.absoluteRelativeTo(other.x, other.y);
                int rel2 = tile.relativeTo(source.tileX(), source.tileY());

                if(rel == rel2) return false;
            }else{
                return source.block() instanceof ItemBridge && ((ItemBridgeEntity)source).link == tile.pos() && items.total() < itemCapacity;
            }

            return items.total() < itemCapacity;
        }


        @Override
        public boolean canDumpLiquid(Tilec to, Liquid liquid){
            Tile other = world.tile(link);
            if(!linkValid(tile, other)){
                Tile edge = Edges.getFacingEdge(to.tile(), tile);
                int i = tile.absoluteRelativeTo(edge.x, edge.y);

                IntSetIterator it = incoming.iterator();

                while(it.hasNext){
                    int v = it.next();
                    if(tile.absoluteRelativeTo(Point2.x(v), Point2.y(v)) == i){
                        return false;
                    }
                }
                return true;
            }

            int rel = tile.absoluteRelativeTo(other.x, other.y);
            int rel2 = tile.relativeTo(to.tileX(), to.tileY());

            return rel != rel2;
        }

        @Override
        public boolean acceptLiquid(Tilec source, Liquid liquid, float amount){
            if(team != source.team() || !hasLiquids) return false;

            Tile other = world.tile(link);

            if(linkValid(tile, other)){
                int rel = tile.absoluteRelativeTo(other.x, other.y);
                int rel2 = tile.relativeTo(source.tileX(), source.tileY());

                if(rel == rel2) return false;
            }else if(!(source.block() instanceof ItemBridge && ((ItemBridgeEntity)source).link == tile.pos())){
                return false;
            }

            return liquids.get(liquid) + amount < liquidCapacity && (liquids.current() == liquid || liquids.get(liquids.current()) < 0.2f);
        }

        @Override
        public boolean canDump(Tilec to, Item item){
            Tile other = world.tile(link);
            if(!linkValid(tile, other)){
                Tile edge = Edges.getFacingEdge(to.tile(), tile);
                int i = tile.absoluteRelativeTo(edge.x, edge.y);

                IntSetIterator it = incoming.iterator();

                while(it.hasNext){
                    int v = it.next();
                    if(tile.absoluteRelativeTo(Point2.x(v), Point2.y(v)) == i){
                        return false;
                    }
                }
                return true;
            }

            int rel = tile.absoluteRelativeTo(other.x, other.y);
            int rel2 = tile.relativeTo(to.tileX(), to.tileY());

            return rel != rel2;
        }

        @Override
        public boolean shouldConsume(){
            return linkValid(tile, world.tile(link));
        }

        @Override
        public Point2 config(){
            return Point2.unpack(link).sub(tile.x, tile.y);
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.i(link);
            write.f(uptime);
            write.b(incoming.size);

            IntSetIterator it = incoming.iterator();

            while(it.hasNext){
                write.i(it.next());
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            link = read.i();
            uptime = read.f();
            byte links = read.b();
            for(int i = 0; i < links; i++){
                incoming.add(read.i());
            }
        }
    }
}
