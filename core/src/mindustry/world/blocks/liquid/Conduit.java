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
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

public class Conduit extends LiquidBlock implements Autotiler{
    public final int timerFlow = timers++;
    
    public Color botColor = Color.valueOf("565656");

    public @Load(value = "@-top-#", length = 7) TextureRegion[] topRegions;
    public @Load(value = "@-bottom-#", length = 7, fallback = "conduit-bottom-#") TextureRegion[] botRegions;

    public float leakResistance = 1.5f;

    public Conduit(String name){
        super(name);
        rotate = true;
        solid = false;
        floating = true;
        conveyorPlacement = true;
    }

    @Override
    public void drawRequestRegion(BuildRequest req, Eachable<BuildRequest> list){
        int[] bits = getTiling(req, list);

        if(bits == null) return;

        Draw.color(botColor);
        Draw.alpha(0.5f);
        Draw.rect(botRegions[bits[0]], req.drawx(), req.drawy(), req.rotation * 90);
        Draw.color();
        Draw.rect(topRegions[bits[0]], req.drawx(), req.drawy(), req.rotation * 90);
    }

    @Override
    public Block getReplacement(BuildRequest req, Array<BuildRequest> requests){
        Boolf<Point2> cont = p -> requests.contains(o -> o.x == req.x + p.x && o.y == req.y + p.y && o.rotation == req.rotation && (req.block instanceof Conduit || req.block instanceof LiquidJunction));
        return cont.get(Geometry.d4(req.rotation)) &&
            cont.get(Geometry.d4(req.rotation - 2)) &&
            req.tile() != null &&
            req.tile().block() instanceof Conduit &&
            Mathf.mod(req.tile().rotation() - req.rotation, 2) == 1 ? Blocks.liquidJunction : this;
    }

    @Override
    public Block upgrade(Tile tile){
        return tile.block() != null && tile.block() instanceof Conduit ? this : null;
    }
    @Override
    public void transformCase(int num, int[] bits){
        bits[0] = num == 0 ? 3 : num == 1 ? 6 : num == 2 ? 2 : num == 3 ? 4 : num == 4 ? 5 : num == 5 ? 1 : 0;
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return otherblock.hasLiquids && otherblock.outputsLiquid && lookingAt(tile, rotation, otherx, othery, otherrot, otherblock);
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find("conduit-bottom"), Core.atlas.find(name + "-top-0")};
    }

    public class ConduitEntity extends LiquidBlockEntity{
        public float smoothLiquid;
        int blendbits;

        @Override
        public void draw(){
            float rotation = rotdeg();

            Draw.color(botColor);
            Draw.rect(botRegions[blendbits], x, y, rotation);

            Draw.color(liquids.current().color);
            Draw.alpha(smoothLiquid);
            Draw.rect(botRegions[blendbits], x, y, rotation);
            Draw.color();

            Draw.rect(topRegions[blendbits], x, y, rotation);
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            blendbits = buildBlending(tile, rotation(), null, true)[0];
        }

        @Override
        public boolean acceptLiquid(Tilec source, Liquid liquid, float amount){
            noSleep();
            return liquids.get(liquid) + amount < liquidCapacity && (liquids.current() == liquid || liquids.currentAmount() < 0.2f)
                && ((source.relativeTo(tile.x, tile.y) + 2) % 4 != tile.rotation());
        }

        @Override
        public void updateTile(){
            smoothLiquid = Mathf.lerpDelta(smoothLiquid, liquids.currentAmount() / liquidCapacity, 0.05f);

            if(liquids.total() > 0.001f && timer(timerFlow, 1)){
                moveLiquidForward(leakResistance, liquids.current());
                noSleep();
            }else{
                sleep();
            }
        }
    }
}
