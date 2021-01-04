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
import mindustry.core.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class ItemBridge extends Block{
    private static BuildPlan otherReq;

    public final int timerTransport = timers++;
    public int range;
    public float transportTime = 2f;
    public @Load("@-end") TextureRegion endRegion;
    public @Load("@-bridge") TextureRegion bridgeRegion;
    public @Load("@-arrow") TextureRegion arrowRegion;

    //for autolink
    @Nullable
    public ItemBridgeBuild lastBuild;
    @Nullable
    public BuildPlan lastPlan;

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
        noUpdateDisabled = true;

        //point2 config is relative
        config(Point2.class, (ItemBridgeBuild tile, Point2 i) -> tile.link = Point2.pack(i.x + tile.tileX(), i.y + tile.tileY()));
        //integer is not
        config(Integer.class, (ItemBridgeBuild tile, Integer i) -> tile.link = i);
    }

    @Override
    public void drawRequestConfigTop(BuildPlan req, Eachable<BuildPlan> list){
        otherReq = null;
        list.each(other -> {
            if(other.block == this && req != other && req.config instanceof Point2 p && p.equals(other.x - req.x, other.y - req.y)){
                otherReq = other;
            }
        });

        if(otherReq != null){
            drawBridge(req, otherReq.drawx(), otherReq.drawy(), 0);
        }
    }

    public void drawBridge(BuildPlan req, float ox, float oy, float flip){
        if(Mathf.zero(Renderer.bridgeOpacity)) return;
        Draw.alpha(Renderer.bridgeOpacity);

        Lines.stroke(8f);

        Tmp.v1.set(ox, oy).sub(req.drawx(), req.drawy()).setLength(tilesize/2f);

        Lines.line(
        bridgeRegion,
        req.drawx() + Tmp.v1.x,
        req.drawy() + Tmp.v1.y,
        ox - Tmp.v1.x,
        oy - Tmp.v1.y, false
        );

        Draw.rect(arrowRegion, (req.drawx() + ox) / 2f, (req.drawy() + oy) / 2f,
        Angles.angle(req.drawx(), req.drawy(), ox, oy) + flip);

        Draw.reset();
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
        if(link != null && Math.abs(link.x - x) + Math.abs(link.y - y) > 1){
            int rot = link.absoluteRelativeTo(x, y);
            float w = (link.x == x ? tilesize : Math.abs(link.x - x) * tilesize - tilesize);
            float h = (link.y == y ? tilesize : Math.abs(link.y - y) * tilesize - tilesize);
            Lines.rect((x + link.x) / 2f * tilesize - w / 2f, (y + link.y) / 2f * tilesize - h / 2f, w, h);

            Draw.rect("bridge-arrow", link.x * tilesize + Geometry.d4(rot).x * tilesize, link.y * tilesize + Geometry.d4(rot).y * tilesize, link.absoluteRelativeTo(x, y) * 90);
        }
        Draw.reset();
    }

    public boolean linkValid(Tile tile, Tile other){
        return linkValid(tile, other, true);
    }

    public boolean linkValid(Tile tile, Tile other, boolean checkDouble){
        if(other == null || tile == null || !positionsValid(tile.x, tile.y, other.x, other.y)) return false;

        return ((other.block() == tile.block() && tile.block() == this) || (!(tile.block() instanceof ItemBridge) && other.block() == this))
            && (other.team() == tile.team() || tile.block() != this)
            && (!checkDouble || ((ItemBridgeBuild)other.build).link != tile.pos());
    }

    public boolean positionsValid(int x1, int y1, int x2, int y2){
        if(x1 == x2){
            return Math.abs(y1 - y2) <= range;
        }else if(y1 == y2){
            return Math.abs(x1 - x2) <= range;
        }else{
            return false;
        }
    }

    public Tile findLink(int x, int y){
        Tile tile = world.tile(x, y);
        if(tile != null && lastBuild != null && linkValid(tile, lastBuild.tile) && lastBuild.tile != tile){
            return lastBuild.tile;
        }
        return null;
    }

    @Override
    public void onNewPlan(BuildPlan plan){
        if(lastPlan != null && lastPlan.config == null && positionsValid(lastPlan.x, lastPlan.y, plan.x, plan.y)){
            lastPlan.config = new Point2(plan.x - lastPlan.x, plan.y - lastPlan.y);
        }

        lastPlan = plan;
    }

    @Override
    public void changePlacementPath(Seq<Point2> points, int rotation){
        Placement.calculateNodes(points, this, rotation, (point, other) -> Math.max(Math.abs(point.x - other.x), Math.abs(point.y - other.y)) <= range);
    }

    public class ItemBridgeBuild extends Building{
        public int link = -1;
        public IntSet incoming = new IntSet();
        public float uptime;
        public float time;
        public float time2;
        public float cycleSpeed = 1f;

        @Override
        public void playerPlaced(Object config){
            super.playerPlaced(config);

            if(config != null) return;

            Tile link = findLink(tile.x, tile.y);
            if(linkValid(tile, link) && !proximity.contains(link.build)){
                link.build.configure(tile.pos());
            }

            lastBuild = this;
        }

        @Override
        public void drawSelect(){
            if(linkValid(tile, world.tile(link))){
                drawInput(world.tile(link));
            }

            incoming.each(pos -> drawInput(world.tile(pos)));

            Draw.reset();
        }

        private void drawInput(Tile other){
            if(!linkValid(tile, other, false)) return;
            boolean linked = other.pos() == link;

            Tmp.v2.trns(tile.angleTo(other), 2f);
            float tx = tile.drawx(), ty = tile.drawy();
            float ox = other.drawx(), oy = other.drawy();
            float alpha = Math.abs((linked ? 100 : 0)-(Time.time * 2f) % 100f) / 100f;
            float x = Mathf.lerp(ox, tx, alpha);
            float y = Mathf.lerp(oy, ty, alpha);

            Tile otherLink = linked ? other : tile;
            int rel = (linked ? tile : other).absoluteRelativeTo(otherLink.x, otherLink.y);

            //draw "background"
            Draw.color(Pal.gray);
            Lines.stroke(2.5f);
            Lines.square(ox, oy, 2f, 45f);
            Lines.stroke(2.5f);
            Lines.line(tx + Tmp.v2.x, ty + Tmp.v2.y, ox - Tmp.v2.x, oy - Tmp.v2.y);

            //draw foreground colors
            Draw.color(linked ? Pal.place : Pal.accent);
            Lines.stroke(1f);
            Lines.line(tx + Tmp.v2.x, ty + Tmp.v2.y, ox - Tmp.v2.x, oy - Tmp.v2.y);

            Lines.square(ox, oy, 2f, 45f);
            Draw.mixcol(Draw.getColor(), 1f);
            Draw.color();
            Draw.rect(arrowRegion, x, y, rel * 90);
            Draw.mixcol();
        }

        @Override
        public void drawConfigure(){
            Drawf.select(x, y, tile.block().size * tilesize / 2f + 2f, Pal.accent);

            for(int i = 1; i <= range; i++){
                for(int j = 0; j < 4; j++){
                    Tile other = tile.nearby(Geometry.d4[j].x * i, Geometry.d4[j].y * i);
                    if(linkValid(tile, other)){
                        boolean linked = other.pos() == link;

                        Drawf.select(other.drawx(), other.drawy(),
                            other.block().size * tilesize / 2f + 2f + (linked ? 0f : Mathf.absin(Time.time, 4f, 1f)), linked ? Pal.place : Pal.breakInvalid);
                    }
                }
            }
        }

        @Override
        public boolean onConfigureTileTapped(Building other){
            //reverse connection
            if(other instanceof ItemBridgeBuild && ((ItemBridgeBuild)other).link == pos()){
                configure(other.pos());
                other.configure(-1);
                return true;
            }

            if(linkValid(tile, other.tile)){
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
                if(!linkValid(tile, other, false) || ((ItemBridgeBuild)other.build).link != tile.pos()){
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
                ((ItemBridgeBuild)other.build).incoming.add(tile.pos());

                if(consValid() && Mathf.zero(1f - efficiency())){
                    uptime = Mathf.lerpDelta(uptime, 1f, 0.04f);
                }else{
                    uptime = Mathf.lerpDelta(uptime, 0f, 0.02f);
                }

                updateTransport(other.build);
            }
        }

        public void updateTransport(Building other){
            if(uptime >= 0.5f && timer(timerTransport, transportTime)){
                Item item = items.take();
                if(item != null && other.acceptItem(this, item)){
                    other.handleItem(this, item);
                    cycleSpeed = Mathf.lerpDelta(cycleSpeed, 4f, 0.05f); //TODO this is kinda broken, because lerping only happens on a timer
                }else{
                    cycleSpeed = Mathf.lerpDelta(cycleSpeed, 1f, 0.01f);
                    if(item != null){
                        items.add(item, 1);
                        items.undoFlow(item);
                    }
                }
            }
        }

        @Override
        public void draw(){
            super.draw();

            Draw.z(Layer.power);

            Tile other = world.tile(link);
            if(!linkValid(tile, other)) return;

            if(Mathf.zero(Renderer.bridgeOpacity)) return;

            int i = relativeTo(other.x, other.y);

            Draw.color(Color.white, Color.black, Mathf.absin(Time.time, 6f, 0.07f));
            Draw.alpha(Math.max(uptime, 0.25f) * Renderer.bridgeOpacity);

            Draw.rect(endRegion, x, y, i * 90 + 90);
            Draw.rect(endRegion, other.drawx(), other.drawy(), i * 90 + 270);

            Lines.stroke(8f);

            Tmp.v1.set(x, y).sub(other.worldx(), other.worldy()).setLength(tilesize/2f).scl(-1f);

            Lines.line(bridgeRegion,
            x + Tmp.v1.x,
            y + Tmp.v1.y,
            other.worldx() - Tmp.v1.x,
            other.worldy() - Tmp.v1.y, false);

            int dist = Math.max(Math.abs(other.x - tile.x), Math.abs(other.y - tile.y));

            float time = time2 / 1.7f;
            int arrows = (dist) * tilesize / 4 - 2;

            Draw.color();

            for(int a = 0; a < arrows; a++){
                Draw.alpha(Mathf.absin(a / (float)arrows - time / 100f, 0.1f, 1f) * uptime * Renderer.bridgeOpacity);
                Draw.rect(arrowRegion,
                x + Geometry.d4(i).x * (tilesize / 2f + a * 4f + time % 4f),
                y + Geometry.d4(i).y * (tilesize / 2f + a * 4f + time % 4f), i * 90f);
            }
            Draw.reset();
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            if(team != source.team) return false;

            Tile other = world.tile(link);

            if(items.total() >= itemCapacity) return false;

            if(linked(source)) return true;

            if(linkValid(tile, other)){
                int rel = relativeTo(other);
                int rel2 = relativeTo(Edges.getFacingEdge(source, this));

                return rel != rel2;
            }

            return false;
        }

        @Override
        public boolean canDumpLiquid(Building to, Liquid liquid){
            return checkDump(to);
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            if(team != source.team || !hasLiquids) return false;

            Tile other = world.tile(link);

            if(!(liquids.current() == liquid || liquids.get(liquids.current()) < 0.2f)) return false;

            if(linked(source)) return true;

            if(linkValid(tile, other)){
                int rel = relativeTo(other.x, other.y);
                int rel2 = relativeTo(Edges.getFacingEdge(source, this));

                return rel != rel2;
            }

            return false;
        }

        protected boolean linked(Building source){
            return source instanceof ItemBridgeBuild && linkValid(source.tile, tile) && ((ItemBridgeBuild)source).link == pos();
        }

        @Override
        public boolean canDump(Building to, Item item){
            return checkDump(to);
        }

        protected boolean checkDump(Building to){
            Tile other = world.tile(link);
            if(!linkValid(tile, other)){
                Tile edge = Edges.getFacingEdge(to.tile, tile);
                int i = relativeTo(edge.x, edge.y);

                IntSetIterator it = incoming.iterator();

                while(it.hasNext){
                    int v = it.next();
                    if(relativeTo(Point2.x(v), Point2.y(v)) == i){
                        return false;
                    }
                }
                return true;
            }

            int rel = relativeTo(other.x, other.y);
            int rel2 = relativeTo(to.tileX(), to.tileY());

            return rel != rel2;
        }

        @Override
        public boolean shouldConsume(){
            return linkValid(tile, world.tile(link)) && enabled;
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
