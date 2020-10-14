package mindustry.world.blocks.liquid;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.distribution.*;

import static mindustry.Vars.*;

public class Conduit extends LiquidBlock implements Autotiler{
    public final int timerFlow = timers++;
    
    public Color botColor = Color.valueOf("565656");

    public @Load(value = "@-top-#", length = 5) TextureRegion[] topRegions;
    public @Load(value = "@-bottom-#", length = 5, fallback = "conduit-bottom-#") TextureRegion[] botRegions;

    public boolean leaks = true;

    public Conduit(String name){
        super(name);
        rotate = true;
        solid = false;
        floating = true;
        conveyorPlacement = true;
        noUpdateDisabled = true;
    }

    @Override
    public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        int[] bits = getTiling(req, list);

        if(bits == null) return;

        Draw.scl(bits[1], bits[2]);
        Draw.color(botColor);
        Draw.alpha(0.5f);
        Draw.rect(botRegions[bits[0]], req.drawx(), req.drawy(), req.rotation * 90);
        Draw.color();
        Draw.rect(topRegions[bits[0]], req.drawx(), req.drawy(), req.rotation * 90);
        Draw.scl();
    }

    @Override
    public Block getReplacement(BuildPlan req, Seq<BuildPlan> requests){
        Boolf<Point2> cont = p -> requests.contains(o -> o.x == req.x + p.x && o.y == req.y + p.y && o.rotation == req.rotation && (req.block instanceof Conduit || req.block instanceof LiquidJunction));
        if(cont.get(Geometry.d4(req.rotation)) &&
            cont.get(Geometry.d4(req.rotation - 2)) &&
            req.tile() != null &&
            req.tile().block() instanceof Conduit &&
            Mathf.mod(req.tile().build.rotation - req.rotation, 2) == 1) {
            return Blocks.liquidJunction;
        }

        int ogRot = req.rotation;
        for(int i = 0;i < 2;i ++) {
            //TODO: automatically generate bridges?
            Block[] bridges = {Blocks.bridgeConduit, Blocks.phaseConduit};
            for(int j = 0;j < bridges.length;j ++) {
                final int distance = ((ItemBridge)bridges[j]).range;
                if(req.block instanceof Conduit && !thisPlaceableOn(frontTile(req.x, req.y, req.rotation)) && requests.contains(o -> 
                    (o.block instanceof Conduit || o.block instanceof ItemBridge) && 
                    thisPlaceableOn(world.tile(req.x, req.y)) &&
                    thisPlaceableOn(world.tile(o.x, o.y)) &&
                    !thisPlaceableOn(frontTile(o.x, o.y, (req.rotation + 2) % 4)) && 
                    inFront(req.x, req.y, req.rotation, o) && 
                    Mathf.dstm(req.x, req.y, o.x, o.y) <= distance)) {
                    return bridges[j];
                }
            }
            req.rotation = (req.rotation + 2) % 4;
        }
        req.rotation = ogRot;

        return this;
    }

    /** Whether the second build plan is "in front" of the first. */
    public boolean inFront(int x, int y, int rotation, BuildPlan other) {
        return !(other.x == x && other.y == y) && (other.x - x) == Geometry.d4x(rotation) * Math.abs(other.x - x) && (other.y - y) == Geometry.d4y(rotation) * Math.abs(other.y - y); 
    }

    /** Returns the tile in front of this one. */
    public Tile frontTile(int x, int y, int rotation) {
        return world.tile(x + Geometry.d4x(rotation), y + Geometry.d4y(rotation));
    }

    /** Whether this block can be placed on this tile. */
    public boolean thisPlaceableOn(Tile tile) {
        boolean sidePlacableOn = false;
        for(int i = 0;i < 4;i ++) {
            sidePlacableOn = !frontTile(tile.x, tile.y, i).floor().isDeep();
            if(sidePlacableOn) break;
        }
        return (tile.block() instanceof Conduit || tile.block() == Blocks.air) && (!tile.floor().isDeep() || (sidePlacableOn && floating));
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return otherblock.hasLiquids && (otherblock.outputsLiquid || (lookingAt(tile, rotation, otherx, othery, otherblock))) && lookingAtEither(tile, rotation, otherx, othery, otherrot, otherblock);
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{Core.atlas.find("conduit-bottom"), topRegions[0]};
    }

    public class ConduitBuild extends LiquidBuild{
        public float smoothLiquid;
        public int blendbits, xscl, yscl, blending;

        @Override
        public void draw(){
            float rotation = rotdeg();
            int r = this.rotation;

            //draw extra conduits facing this one for tiling purposes
            Draw.z(Layer.blockUnder);
            for(int i = 0; i < 4; i++){
                if((blending & (1 << i)) != 0){
                    int dir = r - i;
                    float rot = i == 0 ? rotation : (dir)*90;
                    drawAt(x + Geometry.d4x(dir) * tilesize*0.75f, y + Geometry.d4y(dir) * tilesize*0.75f, 0, rot, i != 0 ? SliceMode.bottom : SliceMode.top);
                }
            }

            Draw.z(Layer.block);

            Draw.scl(xscl, yscl);
            drawAt(x, y, blendbits, rotation, SliceMode.none);
            Draw.reset();
        }

        protected void drawAt(float x, float y, int bits, float rotation, SliceMode slice){
            Draw.color(botColor);
            Draw.rect(sliced(botRegions[bits], slice), x, y, rotation);

            Drawf.liquid(sliced(botRegions[bits], slice), x, y, smoothLiquid, liquids.current().color, rotation);

            Draw.rect(sliced(topRegions[bits], slice), x, y, rotation);
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            int[] bits = buildBlending(tile, rotation, null, true);
            blendbits = bits[0];
            xscl = bits[1];
            yscl = bits[2];
            blending = bits[4];
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            noSleep();
            return (liquids.current() == liquid || liquids.currentAmount() < 0.2f)
                && ((source.relativeTo(tile.x, tile.y) + 2) % 4 != rotation);
        }

        @Override
        public void updateTile(){
            smoothLiquid = Mathf.lerpDelta(smoothLiquid, liquids.currentAmount() / liquidCapacity, 0.05f);

            if(liquids.total() > 0.001f && timer(timerFlow, 1)){
                moveLiquidForward(leaks, liquids.current());
                noSleep();
            }else{
                sleep();
            }
        }
    }
}
