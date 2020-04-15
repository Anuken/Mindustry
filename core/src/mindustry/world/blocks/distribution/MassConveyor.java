package mindustry.world.blocks.distribution;

import arc.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;

import static mindustry.Vars.*;

//TODO rename
public class MassConveyor extends Block{
    public float moveTime = 70f;
    public TextureRegion topRegion, edgeRegion;

    public MassConveyor(String name){
        super(name);

        layer = Layer.overlay;
        size = 3;
        rotate = true;
        update = true;
    }

    @Override
    public void load(){
        super.load();

        topRegion = Core.atlas.find(name + "-top");
        edgeRegion = Core.atlas.find(name + "-edge");
    }

    public class MassConveyorEntity extends TileEntity implements MassAcceptor{
        public @Nullable Payload item;
        public float progress, itemRotation;
        public @Nullable MassAcceptor next;
        public boolean blocked;
        public int step = -1, stepAccepted = -1;

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            Tilec accept = nearby(Geometry.d4[rotation()].x * size, Geometry.d4[rotation()].y * size);
            //next block must be aligned and of the same size
            if(accept instanceof MassAcceptor && accept.block().size == size &&
                tileX() + Geometry.d4[rotation()].x * size == accept.tileX() && tileY() + Geometry.d4[rotation()].y * size == accept.tileY()){
                next = (MassAcceptor)accept;
            }

            int ntrns = 1 + size/2;
            Tile next = tile.getNearby(Geometry.d4[rotation()].x * ntrns, Geometry.d4[rotation()].y * ntrns);
            blocked = next != null && next.solid();
        }

        @Override
        public void updateTile(){
            progress = Time.time() % moveTime;

            //TODO DEBUG
            if(Core.input.keyTap(KeyCode.G) && world.tileWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y) == tile){
                item = new UnitPayload(UnitTypes.dagger.create(Team.sharded));
                itemRotation = rotation() * 90;
            }

            int curStep = curStep();
            if(curStep > step){
                if(step != -1 && stepAccepted != curStep){
                    if(canMove()){
                        //move forward.
                        next.handleMass(item, this);
                        item = null;
                    }
                }

                step = curStep;
            }


            //dumping item forward.
            if(fract() >= 0.5f && item != null && !blocked && next == null){
                float trnext = fract() * size * tilesize, cx = Geometry.d4[rotation()].x, cy = Geometry.d4[rotation()].y, rot = rotation() * 90;

                item.dump(x + cx * trnext, y + cy * trnext, rotation() * 90);
                item = null;
            }
        }

        @Override 
        public void draw(){
            super.draw();

            float dst = 0.8f;

            float glow = Math.max((dst - (Math.abs(fract() - 0.5f) * 2)) / dst, 0);
            Draw.mixcol(Pal.accent, glow);

            float trnext = fract() * size * tilesize, trprev = size * tilesize * (fract() - 1), rot = rotation() * 90;

            TextureRegion clipped = clipRegion(tile.getHitbox(Tmp.r1), tile.getHitbox(Tmp.r2).move(trnext, 0), topRegion);
            float s = tilesize * size;

            //next
            Tmp.v1.set((s-clipped.getWidth()*Draw.scl) + clipped.getWidth()/2f*Draw.scl - s/2f, s-clipped.getHeight()*Draw.scl + clipped.getHeight()/2f*Draw.scl - s/2f).rotate(rot);
            Draw.rect(clipped, x + Tmp.v1.x, y + Tmp.v1.y, rot);

            clipped = clipRegion(tile.getHitbox(Tmp.r1), tile.getHitbox(Tmp.r2).move(trprev, 0), topRegion);

            //prev
            Tmp.v1.set(- s/2f + clipped.getWidth()/2f*Draw.scl,  - s/2f + clipped.getHeight()/2f*Draw.scl).rotate(rot);
            Draw.rect(clipped, x + Tmp.v1.x, y + Tmp.v1.y, rot);

            Draw.reset();

            for(int i = 0; i < 4; i++){
                if(!blends(i)){
                    Draw.rect(edgeRegion, x, y, i * 90);
                }
            }
        }

        @Override
        public void drawLayer(){
            float fract = !blocked ? fract() : fract() > 0.5f ? 1f - fract() : fract();
            float trnext = fract * size * tilesize, cx = Geometry.d4[rotation()].x, cy = Geometry.d4[rotation()].y, rot = Mathf.slerp(itemRotation, rotation() * 90, fract);

            if(item != null){
                Draw.color(0, 0, 0, 0.4f);
                float size = 21;
                Draw.rect("circle-shadow", x + cx * trnext, y + cy * trnext, size, size);
                Draw.color();

                item.draw(x + cx * trnext, y + cy * trnext, rot);
            }
        }

        @Override
        public boolean acceptMass(Payload item, Tilec source){
            return this.item == null;
        }

        @Override
        public void handleMass(Payload item, Tilec source){
            this.item = item;
            this.stepAccepted = curStep();
            this.itemRotation = source.rotation() * 90;
        }

        boolean blends(int direction){
            if(direction == rotation()){
                return !blocked;
            }else{
                Tilec accept = nearby(Geometry.d4[direction].x * size, Geometry.d4[direction].y * size);
                return accept instanceof MassAcceptor && accept.block().size == size &&
                    accept.tileX() + Geometry.d4[accept.rotation()].x * size == tileX() && accept.tileY() + Geometry.d4[accept.rotation()].y * size == tileY();
            }
        }

        boolean canMove(){
            return item != null && next != null && next.acceptMass(item, this);
        }

        TextureRegion clipRegion(Rect bounds, Rect sprite, TextureRegion region){
            Rect over = Tmp.r3;

            boolean overlaps = Intersector.intersectRectangles(bounds, sprite, over);

            TextureRegion out = Tmp.tr1;
            out.set(region.getTexture());

            if(overlaps){
                float w = region.getU2() - region.getU();
                float h = region.getV2() - region.getV();
                float x = region.getU(), y = region.getV();
                float newX = (over.x - sprite.x) / sprite.width * w + x;
                float newY = (over.y - sprite.y) / sprite.height * h + y;
                float newW = (over.width / sprite.width) * w, newH = (over.height / sprite.height) * h;

                out.set(newX, newY, newX + newW, newY + newH);
            }else{
                out.set(0f, 0f, 0f, 0f);
            }

            return out;
        }

        int curStep(){
            return (int)(Time.time() / moveTime);
        }

        float fract(){
            return Interpolation.pow5.apply(progress / moveTime);
        }
    }

    public interface MassAcceptor extends Tilec{
        boolean acceptMass(Payload item, Tilec source);
        void handleMass(Payload item, Tilec source);
    }

    public interface Payload{
        void draw(float x, float y, float rotation);
        void dump(float x, float y, float rotation);
    }

    public static class UnitPayload implements Payload{
        Unitc unit;

        public UnitPayload(Unitc unit){
            this.unit = unit;
        }

        @Override
        public void dump(float x, float y, float rotation){
            unit.set(x, y);
            unit.rotation(rotation);
            unit.add();
        }

        @Override
        public void draw(float x, float y, float rotation){
            Draw.rect(unit.type().icon(Cicon.full), x, y, rotation - 90);
        }
    }
}
