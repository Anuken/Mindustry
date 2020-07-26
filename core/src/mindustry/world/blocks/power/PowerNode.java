package mindustry.world.blocks.power;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.core.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import mindustry.world.modules.*;

import static mindustry.Vars.*;

public class PowerNode extends PowerBlock{
    protected static boolean returnValue = false;
    protected static BuildPlan otherReq;

    protected final ObjectSet<PowerGraph> graphs = new ObjectSet<>();
    protected final Vec2 t1 = new Vec2(), t2 = new Vec2();

    public @Load("laser") TextureRegion laser;
    public @Load("laser-end") TextureRegion laserEnd;
    public float laserRange = 6;
    public int maxNodes = 3;

    public PowerNode(String name){
        super(name);
        expanded = true;
        configurable = true;
        consumesPower = false;
        outputsPower = false;
        config(Integer.class, (entity, value) -> {
            PowerModule power = entity.power;
            Building other = world.build(value);
            boolean contains = power.links.contains(value), valid = other != null && other.power != null;

            if(contains){
                //unlink
                power.links.removeValue(value);
                if(valid) other.power.links.removeValue(entity.pos());

                PowerGraph newgraph = new PowerGraph();

                //reflow from this point, covering all tiles on this side
                newgraph.reflow(entity);

                if(valid && other.power.graph != newgraph){
                    //create new graph for other end
                    PowerGraph og = new PowerGraph();
                    //reflow from other end
                    og.reflow(other);
                }
            }else if(linkValid(entity, other) && valid && power.links.size < maxNodes){

                if(!power.links.contains(other.pos())){
                    power.links.add(other.pos());
                }

                if(other.team() == entity.team()){

                    if(!other.power.links.contains(entity.pos())){
                        other.power.links.add(entity.pos());
                    }
                }

                power.graph.addGraph(other.power.graph);
            }
        });

        config(Point2[].class, (tile, value) -> {
            tile.power.links.clear();
            for(Point2 p : value){
                if(tile.power.links.size < maxNodes){
                    tile.power.links.add(Point2.pack(p.x + tile.tileX(), p.y + tile.tileY()));
                }
            }
            tile.updatePowerGraph();
        });
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.add("power", entity -> new Bar(() ->
        Core.bundle.format("bar.powerbalance",
            ((entity.power.graph.getPowerBalance() >= 0 ? "+" : "") + Strings.fixed(entity.power.graph.getPowerBalance() * 60, 1))),
            () -> Pal.powerBar,
            () -> Mathf.clamp(entity.power.graph.getLastPowerProduced() / entity.power.graph.getLastPowerNeeded())));

        bars.add("batteries", entity -> new Bar(() ->
        Core.bundle.format("bar.powerstored",
            (UI.formatAmount((int)entity.power.graph.getBatteryStored())), UI.formatAmount((int)entity.power.graph.getTotalBatteryCapacity())),
            () -> Pal.powerBar,
            () -> Mathf.clamp(entity.power.graph.getBatteryStored() / entity.power.graph.getTotalBatteryCapacity())));
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.powerRange, laserRange, StatUnit.blocks);
        stats.add(BlockStat.powerConnections, maxNodes, StatUnit.none);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Tile tile = world.tile(x, y);

        if(tile == null) return;

        Lines.stroke(1f);
        Draw.color(Pal.placing);
        Drawf.circles(x * tilesize + offset, y * tilesize + offset, laserRange * tilesize);

        getPotentialLinks(tile, other -> {
            Drawf.square(other.x, other.y, other.block().size * tilesize / 2f + 2f, Pal.place);

            insulators(tile.x, tile.y, other.tileX(), other.tileY(), cause -> {
                Drawf.square(cause.x, cause.y, cause.block().size * tilesize / 2f + 2f, Pal.plastanium);
            });
        });

        Draw.reset();
    }

    protected void drawLaser(Team team, float x1, float y1, float x2, float y2, float satisfaction, int size1, int size2){
        float opacity = Core.settings.getInt("lasersopacity") / 100f;
        if(Mathf.zero(opacity)) return;

        float angle1 = Angles.angle(x1, y1, x2, y2);
        t1.trns(angle1, size1 * tilesize / 2f - 1.5f);
        t2.trns(angle1 + 180f, size2 * tilesize / 2f - 1.5f);

        x1 += t1.x;
        y1 += t1.y;
        x2 += t2.x;
        y2 += t2.y;

        float fract = 1f - satisfaction;

        Draw.color(Color.white, Pal.powerLight, fract * 0.86f + Mathf.absin(3f, 0.1f));
        Draw.alpha(opacity);
        Drawf.laser(team, laser, laserEnd, x1, y1, x2, y2, 0.25f);
        Draw.color();
    }

    protected boolean overlaps(float srcx, float srcy, Tile other, float range){
        return Intersector.overlaps(Tmp.cr1.set(srcx, srcy, range), other.getHitbox(Tmp.r1));
    }

    protected boolean overlaps(Building src, Building other, float range){
        return overlaps(src.x, src.y, other.tile(), range);
    }

    protected boolean overlaps(Tile src, Tile other, float range){
        return overlaps(src.drawx(), src.drawy(), other, range);
    }

    public boolean overlaps(@Nullable Tile src, @Nullable Tile other){
        if(src == null || other == null) return true;
        return Intersector.overlaps(Tmp.cr1.set(src.worldx() + offset, src.worldy() + offset, laserRange * tilesize), Tmp.r1.setSize(size * tilesize).setCenter(other.worldx() + offset, other.worldy() + offset));
    }

    protected void getPotentialLinks(Tile tile, Cons<Building> others){
        Boolf<Building> valid = other -> other != null && other.tile() != tile && other.power != null &&
            ((!other.block().outputsPower && other.block().consumesPower) || (other.block().outputsPower && !other.block().consumesPower) || other.block() instanceof PowerNode) &&
            overlaps(tile.x * tilesize + offset, tile.y * tilesize + offset, other.tile(), laserRange * tilesize) && other.team() == player.team()
            && !other.proximity().contains(e -> e.tile() == tile) && !graphs.contains(other.power.graph);

        tempTileEnts.clear();
        graphs.clear();
        if(tile.build != null && tile.build.power != null){
            graphs.add(tile.build.power.graph);
        }

        Geometry.circle(tile.x, tile.y, (int)(laserRange + 2), (x, y) -> {
            Building other = world.build(x, y);
            if(valid.get(other) && !tempTileEnts.contains(other)){
                tempTileEnts.add(other);
            }
        });

        tempTileEnts.sort((a, b) -> {
            int type = -Boolean.compare(a.block() instanceof PowerNode, b.block() instanceof PowerNode);
            if(type != 0) return type;
            return Float.compare(a.dst2(tile), b.dst2(tile));
        });

        tempTileEnts.each(valid, t -> {
            graphs.add(t.power.graph);
            others.get(t);
        });
    }

    @Override
    public void drawRequestConfigTop(BuildPlan req, Eachable<BuildPlan> list){
        if(req.config instanceof Point2[]){
            for(Point2 point : (Point2[])req.config){
                otherReq = null;
                list.each(other -> {
                    if((other.x == req.x + point.x && other.y == req.y + point.y) && other != req){
                        otherReq = other;
                    }
                });

                if(otherReq == null || otherReq.block == null) return;

                drawLaser(player.team(), req.drawx(), req.drawy(), otherReq.drawx(), otherReq.drawy(), 1f, size, otherReq.block.size);
            }
        }
    }

    public boolean linkValid(Building tile, Building link){
        return linkValid(tile, link, true);
    }

    public boolean linkValid(Building tile, Building link, boolean checkMaxNodes){
        if(tile == link || link == null || !link.block().hasPower || tile.team() != link.team()) return false;

        if(overlaps(tile, link, laserRange * tilesize) || (link.block() instanceof PowerNode && overlaps(link, tile, ((PowerNode)link.block()).laserRange * tilesize))){
            if(checkMaxNodes && link.block() instanceof PowerNode){
                return link.power.links.size < ((PowerNode)link.block()).maxNodes || link.power.links.contains(tile.pos());
            }
            return true;
        }
        return false;
    }

    public static boolean insulated(Tile tile, Tile other){
        return insulated(tile.x, tile.y, other.x, other.y);
    }

    public static boolean insulated(Building tile, Building other){
        return insulated(tile.tileX(), tile.tileY(), other.tileX(), other.tileY());
    }

    public static boolean insulated(int x, int y, int x2, int y2){
        returnValue = false;
        insulators(x, y, x2, y2, cause -> returnValue = true);
        return returnValue;
    }

    public static void insulators(int x, int y, int x2, int y2, Cons<Building> iterator){
        world.raycastEach(x, y, x2, y2, (wx, wy) -> {

            Building tile = world.build(wx, wy);
            if(tile != null && tile.block().insulated){
                iterator.get(tile);
            }

            return false;
        });
    }

    public class PowerNodeEntity extends Building{

        @Override
        public void placed(){
            if(net.client()) return;

            Boolf<Building> valid = other -> other != null && other != this && ((!other.block().outputsPower && other.block().consumesPower) ||
                (other.block().outputsPower && !other.block().consumesPower) || other.block() instanceof PowerNode) && linkValid(this, other)
                && !other.proximity().contains(this) && other.power.graph != power.graph;

            tempTileEnts.clear();
            Geometry.circle(tile.x, tile.y, (int)(laserRange + 2), (x, y) -> {
                Building other = world.build(x, y);
                if(valid.get(other)){
                    if(!insulated(this, other)){
                        tempTileEnts.add(other);
                    }
                }
            });

            tempTileEnts.sort((a, b) -> {
                int type = -Boolean.compare(a.block() instanceof PowerNode, b.block() instanceof PowerNode);
                if(type != 0) return type;
                return Float.compare(a.dst2(tile), b.dst2(tile));
            });
            tempTileEnts.each(valid, other -> {
                if(!power.links.contains(other.pos())){
                    configureAny(other.pos());
                }
            });

            super.placed();
        }

        @Override
        public void updateTile(){
            power.graph.update();
        }

        @Override
        public boolean onConfigureTileTapped(Building other){
            if(linkValid(this, other)){
                configure(other.pos());
                return false;
            }

            if(this == other){
                if(other.power.links.size == 0){
                    int[] total = {0};
                    getPotentialLinks(tile, link -> {
                        if(!insulated(this, link) && total[0]++ < maxNodes){
                            configure(link.pos());
                        }
                    });
                }else{
                    while(power.links.size > 0){
                        configure(power.links.get(0));
                    }
                }
                deselect();
                return false;
            }

            return true;
        }

        @Override
        public void drawSelect(){
            super.drawSelect();

            Lines.stroke(1f);

            Draw.color(Pal.accent);
            Drawf.circles(x, y, laserRange * tilesize);
            Draw.reset();
        }

        @Override
        public void drawConfigure(){

            Drawf.circles(x, y, tile.block().size * tilesize / 2f + 1f + Mathf.absin(Time.time(), 4f, 1f));
            Drawf.circles(x, y, laserRange * tilesize);

            for(int x = (int)(tile.x - laserRange - 2); x <= tile.x + laserRange + 2; x++){
                for(int y = (int)(tile.y - laserRange - 2); y <= tile.y + laserRange + 2; y++){
                    Building link = world.build(x, y);

                    if(link != this && linkValid(this, link, false)){
                        boolean linked = linked(link);

                        if(linked){
                            Drawf.square(link.x, link.y, link.block().size * tilesize / 2f + 1f, Pal.place);
                        }
                    }
                }
            }

            Draw.reset();
        }

        @Override
        public void draw(){
            super.draw();

            if(Core.settings.getInt("lasersopacity") == 0) return;

            Draw.z(Layer.power);

            for(int i = 0; i < power.links.size; i++){
                Building link = world.build(power.links.get(i));

                if(!linkValid(this, link)) continue;

                if(link.block() instanceof PowerNode && !(link.pos() < tile.pos())) continue;

                drawLaserTo(link);
            }

            Draw.reset();
        }

        protected boolean linked(Building other){
            return power.links.contains(other.pos());
        }

        protected void drawLaserTo(Building target){
            drawLaser(team, x, y, target.x, target.y, power.graph.getSatisfaction(), size, target.block().size);
        }

        @Override
        public Point2[] config(){
            Point2[] out = new Point2[power.links.size];
            for(int i = 0; i < out.length; i++){
                out[i] = Point2.unpack(power.links.get(i)).sub(tile.x, tile.y);
            }
            return out;
        }
    }
}
