package mindustry.world.blocks.liquid;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.modules.*;

import static mindustry.Vars.*;
import static mindustry.type.Liquid.*;

public class Conduit extends LiquidBlock implements Autotiler{
    static final boolean debugGraphs = true;
    static final float mergeThreshold = 0.2f;
    static final float rotatePad = 6, hpad = rotatePad / 2f / 4f;
    static final float[][] rotateOffsets = {{hpad, hpad}, {-hpad, hpad}, {-hpad, -hpad}, {hpad, -hpad}};
    static final LiquidModule tempLiquids = new LiquidModule();

    public final int timerFlow = timers++;

    public Color botColor = Color.valueOf("565656");

    public @Load(value = "@-top-#", length = 5) TextureRegion[] topRegions;
    public @Load(value = "@-bottom-#", length = 5, fallback = "conduit-bottom-#") TextureRegion[] botRegions;
    public @Load("@-cap") TextureRegion capRegion;

    /** indices: [rotation] [fluid type] [frame] */
    public TextureRegion[][][] rotateRegions;

    public boolean leaks = true;
    public @Nullable Block junctionReplacement, bridgeReplacement, rotBridgeReplacement;

    public Conduit(String name){
        super(name);
        rotate = true;
        solid = false;
        floating = true;
        underBullets = true;
        conveyorPlacement = true;
        noUpdateDisabled = true;
        canOverdrive = false;
        priority = TargetPriority.transport;

        //conduits don't need to update
        update = false;
        destructible = true;
    }

    @Override
    public void init(){
        super.init();

        if(junctionReplacement == null) junctionReplacement = Blocks.liquidJunction;
        if(bridgeReplacement == null || !(bridgeReplacement instanceof ItemBridge)) bridgeReplacement = Blocks.bridgeConduit;
    }

    @Override
    public void load(){
        super.load();

        rotateRegions = new TextureRegion[4][2][animationFrames];

        if(renderer != null){
            float pad = rotatePad;
            var frames = renderer.getFluidFrames();

            for(int rot = 0; rot < 4; rot++){
                for(int fluid = 0; fluid < 2; fluid++){
                    for(int frame = 0; frame < animationFrames; frame++){
                        TextureRegion base = frames[fluid][frame];
                        TextureRegion result = new TextureRegion();
                        result.set(base);

                        if(rot == 0){
                            result.setX(result.getX() + pad);
                            result.setHeight(result.height - pad);
                        }else if(rot == 1){
                            result.setWidth(result.width - pad);
                            result.setHeight(result.height - pad);
                        }else if(rot == 2){
                            result.setWidth(result.width - pad);
                            result.setY(result.getY() + pad);
                        }else{
                            result.setX(result.getX() + pad);
                            result.setY(result.getY() + pad);
                        }

                        rotateRegions[rot][fluid][frame] = result;
                    }
                }
            }
        }
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        int[] bits = getTiling(plan, list);

        if(bits == null) return;

        Draw.scl(bits[1], bits[2]);
        Draw.color(botColor);
        Draw.alpha(0.5f);
        Draw.rect(botRegions[bits[0]], plan.drawx(), plan.drawy(), plan.rotation * 90);
        Draw.color();
        Draw.rect(topRegions[bits[0]], plan.drawx(), plan.drawy(), plan.rotation * 90);
        Draw.scl();
    }

    @Override
    public Block getReplacement(BuildPlan req, Seq<BuildPlan> plans){
        if(junctionReplacement == null) return this;

        Boolf<Point2> cont = p -> plans.contains(o -> o.x == req.x + p.x && o.y == req.y + p.y && o.rotation == req.rotation && (req.block instanceof Conduit || req.block instanceof LiquidJunction));
        return cont.get(Geometry.d4(req.rotation)) &&
            cont.get(Geometry.d4(req.rotation - 2)) &&
            req.tile() != null &&
            req.tile().block() instanceof Conduit &&
            Mathf.mod(req.build().rotation - req.rotation, 2) == 1 ? junctionReplacement : this;
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return otherblock.hasLiquids && (otherblock.outputsLiquid || (lookingAt(tile, rotation, otherx, othery, otherblock))) && lookingAtEither(tile, rotation, otherx, othery, otherrot, otherblock);
    }

    @Override
    public void handlePlacementLine(Seq<BuildPlan> plans){
        if(bridgeReplacement == null) return;

        if(rotBridgeReplacement instanceof DirectionBridge duct){
            Placement.calculateBridges(plans, duct, true, b -> b instanceof Conduit);
        }else{
            Placement.calculateBridges(plans, (ItemBridge)bridgeReplacement);
        }
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{Core.atlas.find("conduit-bottom"), topRegions[0]};
    }

    public class ConduitBuild extends LiquidBuild implements ChainedBuilding{
        public @Nullable ConduitGraph graph;

        public float smoothLiquid;
        public int blendbits, xscl = 1, yscl = 1, blending;
        public boolean capped, backCapped = false;

        protected void addGraphs(){
            //connect self to every nearby graph
            getConnections(other -> {
                if(other.graph != null){
                    other.graph.merge(this);
                }
            });

            //nothing to connect to
            if(graph == null){
                new ConduitGraph().merge(this);
            }
        }

        protected void removeGraphs(){
            //graph is getting recalculated, no longer valid
            if(graph != null){
                graph.checkRemove();
                graph.remove(this);
                graph = null; //TODO ?????
            }

            getConnections(other -> new ConduitGraph().reflow(this, other));
        }

        @Override
        public void control(LAccess type, double p1, double p2, double p3, double p4){
            if(type == LAccess.enabled){
                boolean shouldEnable = !Mathf.zero((float)p1);
                if(enabled != shouldEnable){

                    if(graph != null){
                        //keep track of how many conduits are disabled, so the graph can stop working
                        if(shouldEnable){
                            graph.disabledConduits --;
                        }else{
                            graph.disabledConduits ++;
                        }
                    }

                    enabled = shouldEnable;
                }
            }
        }

        @Override
        public void onAdded(){
            addGraphs();
        }

        @Override
        public void onProximityRemoved(){
            super.onProximityRemoved();

            removeGraphs();
        }

        @Override
        public void rotated(int prevRot, int newRot){
            //essentially simulates the conduit being removed and re-placed - hacky, but it works
            rotation = prevRot;
            removeGraphs();
            rotation = newRot;
            addGraphs();
        }

        @Override
        public void draw(){
            int r = this.rotation;

            //draw extra conduits facing this one for tiling purposes
            Draw.z(Layer.blockUnder);
            for(int i = 0; i < 4; i++){
                if((blending & (1 << i)) != 0){
                    int dir = r - i;
                    drawAt(x + Geometry.d4x(dir) * tilesize*0.75f, y + Geometry.d4y(dir) * tilesize*0.75f, 0, i == 0 ? r : dir, i != 0 ? SliceMode.bottom : SliceMode.top);
                }
            }

            Draw.z(Layer.block);

            Draw.scl(xscl, yscl);
            drawAt(x, y, blendbits, r, SliceMode.none);
            Draw.reset();

            if(capped && capRegion.found()) Draw.rect(capRegion, x, y, rotdeg());
            if(backCapped && capRegion.found()) Draw.rect(capRegion, x, y, rotdeg() + 180);

            if(debugGraphs){
                //simple visualization that assigns random color to each graph
                Mathf.rand.setSeed(graph == null ? -1 : graph.id);
                Draw.color(Tmp.c1.rand());

                Drawf.selected(tileX(), tileY(), block, Tmp.c1);
                Draw.color(Pal.accent);

                if(this == graph.head){
                    Fill.poly(x, y, 3, 2f, rotdeg());
                }

                Draw.color();
            }
        }

        protected void drawAt(float x, float y, int bits, int rotation, SliceMode slice){
            float angle = rotation * 90f;
            Draw.color(botColor);
            Draw.rect(sliced(botRegions[bits], slice), x, y, angle);

            int offset = yscl == -1 ? 3 : 0;

            int frame = liquids.current().getAnimationFrame();
            int gas = liquids.current().gas ? 1 : 0;
            float ox = 0f, oy = 0f;
            int wrapRot = (rotation + offset) % 4;
            TextureRegion liquidr = bits == 1 ? rotateRegions[wrapRot][gas][frame] : renderer.fluidFrames[gas][frame];

            if(bits == 1){
                ox = rotateOffsets[wrapRot][0];
                oy = rotateOffsets[wrapRot][1];
            }

            //the drawing state machine sure was a great design choice with no downsides or hidden behavior!!!
            float xscl = Draw.xscl, yscl = Draw.yscl;
            Draw.scl(1f, 1f);
            Drawf.liquid(sliced(liquidr, slice), x + ox, y + oy, graph == null ? smoothLiquid : graph.smoothLiquid, liquids.current().color.write(Tmp.c1).a(1f));
            Draw.scl(xscl, yscl);

            Draw.rect(sliced(topRegions[bits], slice), x, y, angle);
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            int[] bits = buildBlending(tile, rotation, null, true);
            blendbits = bits[0];
            xscl = bits[1];
            yscl = bits[2];
            blending = bits[4];

            Building next = front(), prev = back();
            capped = next == null || next.team != team || !next.block.hasLiquids;
            backCapped = blendbits == 0 && (prev == null || prev.team != team || !prev.block.hasLiquids);
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            noSleep();
            return (liquids.current() == liquid || liquids.currentAmount() < 0.2f)
                && (tile == null || (source.relativeTo(tile.x, tile.y) + 2) % 4 != rotation);
        }

        @Override
        public LiquidModule writeLiquids(){
            //"saved" liquids are based on a fraction, essentially splitting apart and re-joining
            tempLiquids.set(liquids, graph == null ? 1f : block.liquidCapacity / graph.totalCapacity);
            return tempLiquids;
        }

        @Override
        public float liquidCapacity(){
            return graph == null ? block.liquidCapacity : graph.totalCapacity;
        }

        @Nullable
        @Override
        public Building next(){
            Tile next = tile.nearby(rotation);
            if(next != null && next.build instanceof ConduitBuild){
                return next.build;
            }
            return null;
        }

        @Override
        public void writeAll(Writes write){
            super.writeAll(write);
        }

        /** Calls callback with every conduit that transfers fluids to this one. */
        public void getConnections(Cons<ConduitBuild> cons){
            for(var other : proximity){
                if(canMerge(other)){
                    cons.get((ConduitBuild)other);
                }
            }
        }

        public boolean canMerge(Building other){
            return
                other instanceof ConduitBuild conduit && other.team == team &&
                (front() == conduit || other.front() == this) &&
                (other.liquids.current() == liquids.current() || other.liquids.currentAmount() <= mergeThreshold || liquids.currentAmount() <= mergeThreshold);
        }
    }

    /*
    TODO:
    - [X] liquids shared as one inventory
    - [X] liquids merged when placing
    - [X] liquids split when breaking
    - [X] liquids saved
    - [X] liquids accept input
    - [X] liquids transfer forward
    - [X] liquids leak
    - [X] liquids display properly (including flow rate)
    - [X] liquids merge different types correctly
    - [X] conduits can (or can't) be disabled
     */
    public static class ConduitGraph{
        private static final IntSet closedSet = new IntSet(), headSet = new IntSet();
        private static final Queue<ConduitBuild> queue = new Queue<>();

        static int lastId = 0;

        public final int id = lastId ++;
        public float smoothLiquid;

        /** if any are disabled, does not update */
        private int disabledConduits;
        private Seq<ConduitBuild> conduits = new Seq<>();
        private final @Nullable ConduitGraphUpdater entity;
        private LiquidModule liquids = new LiquidModule();
        private float totalCapacity;

        public @Nullable ConduitBuild head;

        public ConduitGraph(){
            entity = ConduitGraphUpdater.create();
            entity.graph = this;
        }

        public void update(){
            smoothLiquid = Mathf.lerpDelta(smoothLiquid, liquids.currentAmount() / totalCapacity, 0.05f);

            if(disabledConduits > 0) return;

            if(head != null){

                //move forward as the head
                if(liquids.currentAmount() > 0.0001f && head.timer(((Conduit)head.block).timerFlow, 1)){
                    head.moveLiquidForward(((Conduit)head.block).leaks, liquids.current());
                }

                //merge with front if one of the conduits becomes empty
                if(head.front() instanceof ConduitBuild build && build.graph != this && head.canMerge(build)){
                    merge(build);
                }
            }
        }

        public void checkAdd(){
            if(entity != null) entity.add();
        }

        public void checkRemove(){
            if(entity != null) entity.remove();
        }

        public void remove(ConduitBuild build){
            float fraction = build.block.liquidCapacity / totalCapacity;
            //remove fraction of liquids based on what part this conduit constituted
            //e.g. 70% of capacity was made up by this conduit = multiply liquids by 0.3 (remove 70%)
            liquids.mul(1f - fraction);

            totalCapacity -= build.block.liquidCapacity;
        }

        public void reflow(@Nullable ConduitBuild ignore, ConduitBuild conduit){
            closedSet.clear();
            queue.clear();

            //ignore the starting point and don't add it, as it is being removed
            if(ignore != null) closedSet.add(ignore.id);

            closedSet.add(conduit.id);
            queue.add(conduit);


            while(queue.size > 0){
                var parent = queue.removeFirst();
                assign(parent, ignore);

                parent.getConnections(child -> {
                    if(closedSet.add(child.id)){
                        queue.addLast(child);
                    }
                });
            }

            closedSet.clear();
            queue.clear();
        }

        public void merge(ConduitBuild other){
            if(other.graph == this) return;

            if(other.graph != null){

                //merge graphs - TODO - flip if it is larger, like power graphs?
                for(var cond : other.graph.conduits){
                    assign(cond);
                }
            }else{
                assign(other);
            }
        }

        protected void assign(ConduitBuild build){
            assign(build, null);
        }

        protected void assign(ConduitBuild build, @Nullable Building ignore){
            if(build.graph != this){

                //merge graph liquids - TODO - how does this react to different types
                if(build.graph != null){
                    build.graph.checkRemove();

                    //add liquids based on what fraction it made up
                    liquids.add(build.liquids, build.block.liquidCapacity / build.graph.totalCapacity);
                }else{
                    //simple direct liquid merge
                    liquids.add(build.liquids);
                }

                totalCapacity += build.block.liquidCapacity;
                build.graph = this;
                build.liquids = liquids;
                conduits.add(build);
                checkAdd();

                //re-validate head
                if(head == null){
                    head = build;
                }

                //find the best head block
                headSet.clear();
                headSet.add(head.id);

                while(true){
                    var next = head.front();

                    if(next instanceof ConduitBuild cond && cond.team == head.team && next != ignore && cond.graph == this){
                        if(!headSet.add(next.id)){
                            //there's a loop, which means a head does not exist
                            head = null;
                            break;
                        }else{
                            head = cond;
                        }
                    }else{
                        //found the end
                        break;
                    }
                }

                //snap smoothLiquid so it doesn't start at 0
                smoothLiquid = liquids.currentAmount() / totalCapacity;
            }
        }

    }
}
