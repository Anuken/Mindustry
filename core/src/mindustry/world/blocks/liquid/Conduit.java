package mindustry.world.blocks.liquid;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

public class Conduit extends LiquidBlock implements Autotiler{
    public final int timerFlow = timers++;

    public TextureRegion[] topRegions = new TextureRegion[7];
    public TextureRegion[] botRegions = new TextureRegion[7];

    public float leakResistance = 1.5f;

    public Conduit(String name){
        super(name);
        rotate = true;
        solid = false;
        floating = true;
        conveyorPlacement = true;
    }

    @Override
    public void load(){
        super.load();

        liquidRegion = Core.atlas.find("conduit-liquid");
        for(int i = 0; i < topRegions.length; i++){
            topRegions[i] = Core.atlas.find(name + "-top-" + i);
            botRegions[i] = Core.atlas.find("conduit-bottom-" + i);
        }
    }

    @Override
    public void drawRequestRegion(BuildRequest req, Eachable<BuildRequest> list){
        int[] bits = getTiling(req, list);

        if(bits == null) return;

        Draw.colorl(0.34f);
        Draw.alpha(0.5f);
        Draw.rect(botRegions[bits[0]], req.drawx(), req.drawy(),
            botRegions[bits[0]].getWidth() * Draw.scl * req.animScale, botRegions[bits[0]].getHeight() * Draw.scl * req.animScale,
            req.rotation * 90);
        Draw.color();


        Draw.rect(topRegions[bits[0]], req.drawx(), req.drawy(), topRegions[bits[0]].getWidth() * Draw.scl * req.animScale, topRegions[bits[0]].getHeight() * Draw.scl * req.animScale, req.rotation * 90);
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

        protected void drawer(int blendbits2, float x2, float y2, float rotation2){
            Draw.colorl(0.34f);
            Draw.rect(botRegions[blendbits2], x2, y2, rotation2);

            Draw.color(liquids.current().color);
            Draw.alpha(smoothLiquid);
            Draw.rect(botRegions[blendbits2], x2, y2, rotation2);
            Draw.color();

            Draw.rect(topRegions[blendbits2], x2, y2, rotation2);
        }

        @Override
        public void draw(){
            drawer(blendbits, x, y, rotation() * 90);

            Draw.z(Layer.lawn);

            if(front() != null && front().block().hasLiquids && !(front().block() instanceof Conduit)){

                float x2 = 0;
                float y2 = 0;

                if(rotation() == 0) x2 += 6;
                if(rotation() == 1) y2 += 6;
                if(rotation() == 2) x2 -= 6;
                if(rotation() == 3) y2 -= 6;

                float finalX = x2;
                float finalY = y2;
                top50(botRegions[blendbits], () -> top50(topRegions[blendbits], () -> {
                    drawer(0, x + finalX, y + finalY, rotation() * 90);
                }));
            }

            upstream(t -> {
                if(t != null && t.block().hasLiquids && !(t.block() instanceof Conduit)){
                    float x2 = 0;
                    float y2 = 0;

                    int rotation2 = 0;

                    if(t == back()){
                        if(rotation() == 0) x2 -= 6;
                        if(rotation() == 1) y2 -= 6;
                        if(rotation() == 2) x2 += 6;
                        if(rotation() == 3) y2 += 6;
                        rotation2 = 2;
                    }

                    if(t == left()){
                        if(rotation() == 0) y2 += 6;
                        if(rotation() == 1) x2 -= 6;
                        if(rotation() == 2) y2 -= 6;
                        if(rotation() == 3) x2 += 6;
                        rotation2 = 1;
                    }

                    if(t == right()){
                        if(rotation() == 0) y2 -= 6;
                        if(rotation() == 1) x2 += 6;
                        if(rotation() == 2) y2 += 6;
                        if(rotation() == 3) x2 -= 6;
                        rotation2 = 3;
                    }

                    float finalX = x2;
                    float finalY = y2;
                    int finalRotation = rotation2;
                    bot50(botRegions[blendbits], () -> bot50(topRegions[blendbits], () -> {
                        drawer(0, x + finalX, y + finalY, (rotation() + finalRotation) * 90);
                    }));
                }
            });
        }

        private void upstream(Cons<Tilec> cons){
            if(blendbits == 0 || blendbits == 2 || blendbits == 4) cons.get(back());
            if(blendbits == 1 || blendbits == 3 || blendbits == 4 || blendbits == 6) cons.get(left());
            if(blendbits == 2 || blendbits == 3 || blendbits == 5 || blendbits == 6) cons.get(right());
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
                && ((source.absoluteRelativeTo(tile.x, tile.y) + 2) % 4 != tile.rotation());
        }

        @Override
        public void updateTile(){
            smoothLiquid = Mathf.lerpDelta(smoothLiquid, liquids.currentAmount() / liquidCapacity, 0.05f);

            if(liquids.total() > 0.001f && timer(timerFlow, 1)){
                moveLiquid(tile.getNearbyEntity(rotation()), leakResistance, liquids.current());
                noSleep();
            }else{
                sleep();
            }
        }
    }
}
