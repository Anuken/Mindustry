package mindustry.world.blocks.distribution;

import arc.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;

import static mindustry.Vars.*;

public class PayloadConveyor extends Block{
    public float moveTime = 70f;
    public @Load("@-top") TextureRegion topRegion;
    public @Load("@-edge") TextureRegion edgeRegion;
    public Interpolation interp = Interpolation.pow5;

    public PayloadConveyor(String name){
        super(name);

        size = 3;
        rotate = true;
        update = true;
        outputsPayload = true;
    }

    @Override
    protected TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name + "-icon")};
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        for(int i = 0; i < 4; i++){
            Tilec other = world.ent(x + Geometry.d4x[i] * size, y + Geometry.d4y[i] * size);
            if(other != null && other.block().outputsPayload && other.block().size == size){
                Drawf.selected(other.tileX(), other.tileY(), other.block(), Pal.accent);
            }
        }
    }

    public class PayloadConveyorEntity extends TileEntity{
        public @Nullable Payload item;
        public float progress, itemRotation, animation;
        public @Nullable Tilec next;
        public boolean blocked;
        public int step = -1, stepAccepted = -1;

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            Tilec accept = nearby(Geometry.d4(rotation()).x * size, Geometry.d4(rotation()).y * size);
            //next block must be aligned and of the same size
            if(accept != null && accept.block().size == size &&
                tileX() + Geometry.d4(rotation()).x * size == accept.tileX() && tileY() + Geometry.d4(rotation()).y * size == accept.tileY()){
                next = accept;
            }else{
                next = null;
            }

            int ntrns = 1 + size/2;
            Tile next = tile.getNearby(Geometry.d4(rotation()).x * ntrns, Geometry.d4(rotation()).y * ntrns);
            blocked = (next != null && next.solid()) || (this.next != null && (this.next.rotation() + 2)%4 == rotation());
        }

        @Override
        public void updateTile(){
            progress = Time.time() % moveTime;

            //TODO DEBUG
            if(Core.input.keyTap(KeyCode.g) && world.entWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y) == this){
                item = new UnitPayload((Mathf.chance(0.5) ? UnitTypes.wraith : UnitTypes.dagger).create(Team.sharded));
                itemRotation = rotation() * 90;
                animation = 0f;
            }

            //TODO nondeterministic input priority
            int curStep = curStep();
            if(curStep > step){
                boolean valid = step != -1;
                step = curStep;

                if(valid && stepAccepted != curStep && item != null){
                    if(next != null){
                        //trigger update forward
                        next.updateTile();

                        //TODO add self to queue of next conveyor, then check if this conveyor was selected next frame - selection happens deterministically
                        if(next.acceptPayload(this, item)){
                            //move forward.
                            next.handlePayload(this, item);
                            item = null;
                        }
                    }else if(!blocked){
                        //dump item forward
                        float trnext = size * tilesize / 2f, cx = Geometry.d4(rotation()).x, cy = Geometry.d4(rotation()).y;

                        if(item.dump(x + cx * trnext, y + cy * trnext, rotation() * 90)){
                            item = null;
                        }
                    }
                }
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

            for(int i = 0; i < 4; i++){
                if(blends(i) && i != rotation()){
                    Draw.alpha(1f - Interpolation.pow5In.apply(fract()));
                    //prev from back
                    Tmp.v1.set(- s/2f + clipped.getWidth()/2f*Draw.scl,  - s/2f + clipped.getHeight()/2f*Draw.scl).rotate(i * 90 + 180);
                    Draw.rect(clipped, x + Tmp.v1.x, y + Tmp.v1.y, i * 90 + 180);
                }
            }

            Draw.reset();

            for(int i = 0; i < 4; i++){
                if(!blends(i)){
                    Draw.rect(edgeRegion, x, y, i * 90);
                }
            }

            Draw.z(Layer.blockOver);

            if(animation > fract()){
                animation = Mathf.lerp(animation, 0.8f, 0.15f);
            }

            animation = Math.max(animation, fract());

            float fract = animation;
            rot = Mathf.slerp(itemRotation, rotation() * 90, fract);

            if(fract < 0.5f){
                Tmp.v1.trns(itemRotation + 180, (0.5f - fract) * tilesize * size);
            }else{
                Tmp.v1.trns(rotation() * 90, (fract - 0.5f) * tilesize * size);
            }

            float vx = Tmp.v1.x, vy = Tmp.v1.y;

            if(item != null){
                item.draw(x + vx, y + vy, rot);
            }
        }

        @Override
        public boolean acceptPayload(Tilec source, Payload payload){
            return this.item == null && progress <= 5f;
        }

        @Override
        public void handlePayload(Tilec source, Payload payload){
            this.item = payload;
            this.stepAccepted = curStep();
            this.itemRotation = source.angleTo(this);
            this.animation = 0;
        }

        boolean blends(int direction){
            if(direction == rotation()){
                return !blocked || next != null;
            }else{
                Tilec accept = nearby(Geometry.d4(direction).x * size, Geometry.d4(direction).y * size);
                return accept != null && accept.block().size == size && accept.block().outputsPayload &&
                    //block must either be facing this one, or not be rotating
                    ((accept.tileX() + Geometry.d4(accept.rotation()).x * size == tileX() && accept.tileY() + Geometry.d4(accept.rotation()).y * size == tileY()) || !accept.block().rotate);
            }
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
            return (int)((Time.time()) / moveTime);
        }

        float fract(){
            return interp.apply(progress / moveTime);
        }
    }

}
