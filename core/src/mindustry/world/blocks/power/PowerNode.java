package mindustry.world.blocks.power;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.core.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import mindustry.world.modules.*;

import static mindustry.Vars.*;

public class PowerNode extends PowerBlock{
    protected static boolean returnValue = false;
    protected static BuildPlan otherReq;

    protected final static ObjectSet<PowerGraph> graphs = new ObjectSet<>();
    protected static int returnInt = 0;

    public @Load("laser") TextureRegion laser;
    public @Load("laser-end") TextureRegion laserEnd;
    public float laserRange = 6;
    public int maxNodes = 3;
    public Color laserColor1 = Color.white;
    public Color laserColor2 = Pal.powerLight;

    public PowerNode(String name){
        super(name);
        configurable = true;
        consumesPower = false;
        outputsPower = false;
        canOverdrive = false;
        swapDiagonalPlacement = true;
        schematicPriority = -10;
        drawDisabled = false;

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

                if(other.team == entity.team){

                    if(!other.power.links.contains(entity.pos())){
                        other.power.links.add(entity.pos());
                    }
                }

                power.graph.addGraph(other.power.graph);
            }
        });

        config(Point2[].class, (tile, value) -> {
            tile.power.links.clear();

            IntSeq old = new IntSeq(tile.power.links);

            //clear old
            for(int i = 0; i < old.size; i++){
                int cur = old.get(i);
                configurations.get(Integer.class).get(tile, cur);
            }

            //set new
            for(Point2 p : value){
                int newPos = Point2.pack(p.x + tile.tileX(), p.y + tile.tileY());
                configurations.get(Integer.class).get(tile, newPos);
            }
        });
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.add("power", entity -> new Bar(() ->
        Core.bundle.format("bar.powerbalance",
            ((entity.power.graph.getPowerBalance() >= 0 ? "+" : "") + UI.formatAmount((long)(entity.power.graph.getPowerBalance() * 60)))),
            () -> Pal.powerBar,
            () -> Mathf.clamp(entity.power.graph.getLastPowerProduced() / entity.power.graph.getLastPowerNeeded())));

        bars.add("batteries", entity -> new Bar(() ->
        Core.bundle.format("bar.powerstored",
            (UI.formatAmount((long)entity.power.graph.getLastPowerStored())), UI.formatAmount((long)entity.power.graph.getLastCapacity())),
            () -> Pal.powerBar,
            () -> Mathf.clamp(entity.power.graph.getLastPowerStored() / entity.power.graph.getLastCapacity())));

        bars.add("connections", entity -> new Bar(() ->
        Core.bundle.format("bar.powerlines", entity.power.links.size, maxNodes),
            () -> Pal.items,
            () -> (float)entity.power.links.size / (float)maxNodes
        ));
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.powerRange, laserRange, StatUnit.blocks);
        stats.add(Stat.powerConnections, maxNodes, StatUnit.none);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Tile tile = world.tile(x, y);

        if(tile == null) return;

        Lines.stroke(1f);
        Draw.color(Pal.placing);
        Drawf.circles(x * tilesize + offset, y * tilesize + offset, laserRange * tilesize);

        getPotentialLinks(tile, player.team(), other -> {
            Draw.color(laserColor1, Renderer.laserOpacity * 0.5f);
            drawLaser(tile.team(), x * tilesize + offset, y * tilesize + offset, other.x, other.y, size, other.block.size);

            Drawf.square(other.x, other.y, other.block.size * tilesize / 2f + 2f, Pal.place);
        });

        Draw.reset();
    }

    @Override
    public void changePlacementPath(Seq<Point2> points, int rotation){
        Placement.calculateNodes(points, this, rotation, (point, other) -> overlaps(world.tile(point.x, point.y), world.tile(other.x, other.y)));
    }

    protected void setupColor(float satisfaction){
        Draw.color(laserColor1, laserColor2, (1f - satisfaction) * 0.86f + Mathf.absin(3f, 0.1f));
        Draw.alpha(Renderer.laserOpacity);
    }

    public void drawLaser(Team team, float x1, float y1, float x2, float y2, int size1, int size2){
        float angle1 = Angles.angle(x1, y1, x2, y2),
            vx = Mathf.cosDeg(angle1), vy = Mathf.sinDeg(angle1),
            len1 = size1 * tilesize / 2f - 1.5f, len2 = size2 * tilesize / 2f - 1.5f;

        Drawf.laser(team, laser, laserEnd, x1 + vx*len1, y1 + vy*len1, x2 - vx*len2, y2 - vy*len2, 0.25f);
    }

    protected boolean overlaps(float srcx, float srcy, Tile other, Block otherBlock, float range){
        return Intersector.overlaps(Tmp.cr1.set(srcx, srcy, range), Tmp.r1.setCentered(other.worldx() + otherBlock.offset, other.worldy() + otherBlock.offset,
            otherBlock.size * tilesize, otherBlock.size * tilesize));
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

    protected void getPotentialLinks(Tile tile, Team team, Cons<Building> others){
        Boolf<Building> valid = other -> other != null && other.tile() != tile && other.power != null &&
            (other.block.outputsPower || other.block.consumesPower || other.block instanceof PowerNode) &&
            overlaps(tile.x * tilesize + offset, tile.y * tilesize + offset, other.tile(), laserRange * tilesize) && other.team == team &&
            !graphs.contains(other.power.graph) &&
            !PowerNode.insulated(tile, other.tile) &&
            !(other instanceof PowerNodeBuild obuild && obuild.power.links.size >= ((PowerNode)obuild.block).maxNodes) &&
            !Structs.contains(Edges.getEdges(size), p -> { //do not link to adjacent buildings
                var t = world.tile(tile.x + p.x, tile.y + p.y);
                return t != null && t.build == other;
            });

        tempTileEnts.clear();
        graphs.clear();

        //add conducting graphs to prevent double link
        for(var p : Edges.getEdges(size)){
            Tile other = tile.nearby(p);
            if(other != null && other.team() == team && other.build != null && other.build.power != null){
                graphs.add(other.build.power.graph);
            }
        }

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
            int type = -Boolean.compare(a.block instanceof PowerNode, b.block instanceof PowerNode);
            if(type != 0) return type;
            return Float.compare(a.dst2(tile), b.dst2(tile));
        });

        returnInt = 0;

        tempTileEnts.each(valid, t -> {
            if(returnInt ++ < maxNodes){
                graphs.add(t.power.graph);
                others.get(t);
            }
        });
    }

    //TODO code duplication w/ method above?
    /** Iterates through linked nodes of a block at a tile. All returned buildings are power nodes. */
    public static void getNodeLinks(Tile tile, Block block, Team team, Cons<Building> others){
        Boolf<Building> valid = other -> other != null && other.tile() != tile && other.block instanceof PowerNode node &&
        other.power.links.size < node.maxNodes &&
        node.overlaps(other.x, other.y, tile, block, node.laserRange * tilesize) && other.team == team
        && !graphs.contains(other.power.graph) &&
        !PowerNode.insulated(tile, other.tile) &&
        !Structs.contains(Edges.getEdges(block.size), p -> { //do not link to adjacent buildings
            var t = world.tile(tile.x + p.x, tile.y + p.y);
            return t != null && t.build == other;
        });

        tempTileEnts.clear();
        graphs.clear();

        //add conducting graphs to prevent double link
        for(var p : Edges.getEdges(block.size)){
            Tile other = tile.nearby(p);
            if(other != null && other.team() == team && other.build != null && other.build.power != null
                && !(block.consumesPower && other.block().consumesPower && !block.outputsPower && !other.block().outputsPower)){
                graphs.add(other.build.power.graph);
            }
        }

        if(tile.build != null && tile.build.power != null){
            graphs.add(tile.build.power.graph);
        }

        Geometry.circle(tile.x, tile.y, 13, (x, y) -> {
            Building other = world.build(x, y);
            if(valid.get(other) && !tempTileEnts.contains(other)){
                tempTileEnts.add(other);
            }
        });

        tempTileEnts.sort((a, b) -> {
            int type = -Boolean.compare(a.block instanceof PowerNode, b.block instanceof PowerNode);
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
        if(req.config instanceof Point2[] ps){
            setupColor(1f);
            for(Point2 point : ps){
                int px = req.x + point.x, py = req.y + point.y;
                otherReq = null;
                list.each(other -> {
                    if(other.block != null
                        && (px >= other.x - ((other.block.size-1)/2) && py >= other.y - ((other.block.size-1)/2) && px <= other.x + other.block.size/2 && py <= other.y + other.block.size/2)
                        && other != req && other.block.hasPower){
                        otherReq = other;
                    }
                });

                if(otherReq == null || otherReq.block == null) continue;

                drawLaser(player == null ? Team.sharded : player.team(), req.drawx(), req.drawy(), otherReq.drawx(), otherReq.drawy(), size, otherReq.block.size);
            }
            Draw.color();
        }
    }

    public boolean linkValid(Building tile, Building link){
        return linkValid(tile, link, true);
    }

    public boolean linkValid(Building tile, Building link, boolean checkMaxNodes){
        if(tile == link || link == null || !link.block.hasPower || tile.team != link.team) return false;

        if(overlaps(tile, link, laserRange * tilesize) || (link.block instanceof PowerNode node && overlaps(link, tile, node.laserRange * tilesize))){
            if(checkMaxNodes && link.block instanceof PowerNode node){
                return link.power.links.size < node.maxNodes || link.power.links.contains(tile.pos());
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
        return world.raycast(x, y, x2, y2, (wx, wy) -> {
            Building tile = world.build(wx, wy);
            return tile != null && tile.block.insulated;
        });
    }

    public class PowerNodeBuild extends Building{

        @Override
        public void placed(){
            if(net.client()) return;

            getPotentialLinks(tile, team, other -> {
                if(!power.links.contains(other.pos())){
                    configureAny(other.pos());
                }
            });

            super.placed();
        }

        @Override
        public void dropped(){
            power.links.clear();
            updatePowerGraph();
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
                    getPotentialLinks(tile, team, link -> {
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

            Drawf.circles(x, y, tile.block().size * tilesize / 2f + 1f + Mathf.absin(Time.time, 4f, 1f));
            Drawf.circles(x, y, laserRange * tilesize);

            for(int x = (int)(tile.x - laserRange - 2); x <= tile.x + laserRange + 2; x++){
                for(int y = (int)(tile.y - laserRange - 2); y <= tile.y + laserRange + 2; y++){
                    Building link = world.build(x, y);

                    if(link != this && linkValid(this, link, false)){
                        boolean linked = linked(link);

                        if(linked){
                            Drawf.square(link.x, link.y, link.block.size * tilesize / 2f + 1f, Pal.place);
                        }
                    }
                }
            }

            Draw.reset();
        }

        @Override
        public void draw(){
            super.draw();

            if(Mathf.zero(Renderer.laserOpacity)) return;

            Draw.z(Layer.power);
            setupColor(power.graph.getSatisfaction());

            for(int i = 0; i < power.links.size; i++){
                Building link = world.build(power.links.get(i));

                if(!linkValid(this, link)) continue;

                if(link.block instanceof PowerNode && link.id >= id) continue;

                drawLaser(team, x, y, link.x, link.y, size, link.block.size);
            }

            Draw.reset();
        }

        protected boolean linked(Building other){
            return power.links.contains(other.pos());
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
