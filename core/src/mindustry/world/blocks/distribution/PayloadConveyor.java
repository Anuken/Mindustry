package mindustry.world.blocks.distribution;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class PayloadConveyor extends Block{
    public float moveTime = 40f, moveForce = 201f;
    public @Load("@-top") TextureRegion topRegion;
    public @Load("@-edge") TextureRegion edgeRegion;
    public Interp interp = Interp.pow5;
    public float payloadLimit = 3f;

    public PayloadConveyor(String name){
        super(name);
        group = BlockGroup.transportation;
        size = 3;
        rotate = true;
        update = true;
        outputsPayload = true;
        noUpdateDisabled = true;
        sync = true;
    }

    @Override
    protected TextureRegion[] icons(){
        return new TextureRegion[]{Core.atlas.find(name + "-icon")};
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        for(int i = 0; i < 4; i++){
            Building other = world.build(x + Geometry.d4x[i] * size, y + Geometry.d4y[i] * size);
            if(other != null && other.block.outputsPayload && other.block.size == size){
                Drawf.selected(other.tileX(), other.tileY(), other.block, other.team.color);
            }
        }
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.payloadCapacity, (payloadLimit), StatUnit.blocksSquared);
    }

    @Override
    public void init(){
        super.init();

        //increase clip size for oversize loads
        clipSize = Math.max(clipSize, size * tilesize * 2.1f);
    }

    public class PayloadConveyorBuild extends Building{
        public @Nullable Payload item;
        public float progress, itemRotation, animation;
        public float curInterp, lastInterp;
        public @Nullable Building next;
        public boolean blocked;
        public int step = -1, stepAccepted = -1;

        @Override
        public boolean canControlSelect(Player player){
            return this.item == null && !player.unit().spawnedByCore && player.unit().hitSize / tilesize <= payloadLimit && player.tileOn() != null && player.tileOn().build == this;
        }

        @Override
        public void onControlSelect(Player player){
            acceptPlayerPayload(player, p -> item = p);
        }

        @Override
        public Payload takePayload(){
            Payload t = item;
            item = null;
            return t;
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            Building accept = nearby(Geometry.d4(rotation).x * size, Geometry.d4(rotation).y * size);
            //next block must be aligned and of the same size
            if(accept != null && (
                //same size
                (accept.block.size == size && tileX() + Geometry.d4(rotation).x * size == accept.tileX() && tileY() + Geometry.d4(rotation).y * size == accept.tileY()) ||

                //differing sizes
                (accept.block.size > size &&
                    (rotation % 2 == 0 ? //check orientation
                    Math.abs(accept.y - y) <= (accept.block.size * tilesize - size * tilesize)/2f : //check Y alignment
                    Math.abs(accept.x - x) <= (accept.block.size * tilesize - size * tilesize)/2f   //check X alignment
                )))){
                next = accept;
            }else{
                next = null;
            }

            int ntrns = 1 + size/2;
            Tile next = tile.nearby(Geometry.d4(rotation).x * ntrns, Geometry.d4(rotation).y * ntrns);
            blocked = (next != null && next.solid() && !(next.block().outputsPayload || next.block().acceptsPayload)) || (this.next != null && this.next.block.rotate && (this.next.rotation + 2) % 4 == rotation);
        }

        @Override
        public Payload getPayload(){
            return item;
        }

        @Override
        public void updateTile(){
            if(!enabled) return;

            lastInterp = curInterp;
            curInterp = fract();
            //rollover skip
            if(lastInterp > curInterp) lastInterp = 0f;
            progress = time() % moveTime;

            updatePayload();
            if(item != null && next == null){
                PayloadBlock.pushOutput(item, progress / moveTime);
            }

            //TODO nondeterministic input priority
            int curStep = curStep();
            if(curStep > step){
                boolean valid = step != -1;
                step = curStep;
                boolean had = item != null;

                if(valid && stepAccepted != curStep && item != null){
                    if(next != null){
                        //trigger update forward
                        next.updateTile();

                        //TODO add self to queue of next conveyor, then check if this conveyor was selected next frame - selection happens deterministically
                        if(next.acceptPayload(this, item)){
                            //move forward.
                            next.handlePayload(this, item);
                            item = null;
                            moved();
                        }
                    }else if(!blocked){
                        //dump item forward
                        if(item.dump()){
                            item = null;
                            moved();
                        }
                    }
                }

                if(had && item != null){
                    moveFailed();
                }
            }
        }

        public void moveFailed(){

        }

        public void moved(){

        }

        public void drawBottom(){
            super.draw();
        }

        @Override
        public void draw(){
            super.draw();

            float dst = 0.8f;

            float glow = Math.max((dst - (Math.abs(fract() - 0.5f) * 2)) / dst, 0);
            Draw.mixcol(team.color, glow);

            float trnext = fract() * size * tilesize, trprev = size * tilesize * (fract() - 1), rot = rotdeg();

            TextureRegion clipped = clipRegion(tile.getHitbox(Tmp.r1), tile.getHitbox(Tmp.r2).move(trnext, 0), topRegion);
            float s = tilesize * size;

            //next
            Tmp.v1.set((s- clipped.width *Draw.scl) + clipped.width /2f*Draw.scl - s/2f, s- clipped.height *Draw.scl + clipped.height /2f*Draw.scl - s/2f).rotate(rot);
            Draw.rect(clipped, x + Tmp.v1.x, y + Tmp.v1.y, rot);

            clipped = clipRegion(tile.getHitbox(Tmp.r1), tile.getHitbox(Tmp.r2).move(trprev, 0), topRegion);

            //prev
            Tmp.v1.set(- s/2f + clipped.width /2f*Draw.scl,  - s/2f + clipped.height /2f*Draw.scl).rotate(rot);
            Draw.rect(clipped, x + Tmp.v1.x, y + Tmp.v1.y, rot);

            for(int i = 0; i < 4; i++){
                if(blends(i) && i != rotation){
                    Draw.alpha(1f - Interp.pow5In.apply(fract()));
                    //prev from back
                    Tmp.v1.set(- s/2f + clipped.width /2f*Draw.scl,  - s/2f + clipped.height /2f*Draw.scl).rotate(i * 90 + 180);
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

            if(item != null){
                item.draw();
            }
        }

        public float time(){
            return Time.time;
        }

        @Override
        public void unitOn(Unit unit){
            //calculate derivative of units moved last frame
            float delta = (curInterp - lastInterp) * size * tilesize;
            Tmp.v1.trns(rotdeg(), delta * moveForce).scl(1f / Math.max(unit.mass(), 201f));
            unit.move(Tmp.v1.x, Tmp.v1.y);
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            return this.item == null
                && payload.fits(payloadLimit)
                && (source == this || this.enabled && progress <= 5f);
        }

        @Override
        public void handlePayload(Building source, Payload payload){
            this.item = payload;
            this.stepAccepted = curStep();
            this.itemRotation = source == this ? rotdeg() : source.angleTo(this);
            this.animation = 0;

            updatePayload();
        }

        @Override
        public void onRemoved(){
            super.onRemoved();
            if(item != null) item.dump();
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.f(progress);
            write.f(itemRotation);
            Payload.write(item, write);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            read.f(); //why is progress written?
            itemRotation = read.f();
            item = Payload.read(read);
        }

        public void updatePayload(){
            if(item != null){
                if(animation > fract()){
                    animation = Mathf.lerp(animation, 0.8f, 0.15f);
                }

                animation = Math.max(animation, fract());

                float fract = animation;
                float rot = Mathf.slerp(itemRotation, rotdeg(), fract);

                if(fract < 0.5f){
                    Tmp.v1.trns(itemRotation + 180, (0.5f - fract) * tilesize * size);
                }else{
                    Tmp.v1.trns(rotdeg(), (fract - 0.5f) * tilesize * size);
                }

                float vx = Tmp.v1.x, vy = Tmp.v1.y;

                item.set(x + vx, y + vy, rot);
            }
        }

        protected boolean blends(int direction){
            if(direction == rotation){
                return !blocked || next != null;
            }
            return PayloadBlock.blends(this, direction);
        }

        protected TextureRegion clipRegion(Rect bounds, Rect sprite, TextureRegion region){
            Rect over = Tmp.r3;

            boolean overlaps = Intersector.intersectRectangles(bounds, sprite, over);

            TextureRegion out = Tmp.tr1;
            out.set(region.texture);

            if(overlaps){
                float w = region.u2 - region.u;
                float h = region.v2 - region.v;
                float x = region.u, y = region.v;
                float newX = (over.x - sprite.x) / sprite.width * w + x;
                float newY = (over.y - sprite.y) / sprite.height * h + y;
                float newW = (over.width / sprite.width) * w, newH = (over.height / sprite.height) * h;

                out.set(newX, newY, newX + newW, newY + newH);
            }else{
                out.set(0f, 0f, 0f, 0f);
            }

            return out;
        }

        public int curStep(){
            return (int)((time()) / moveTime);
        }

        public float fract(){
            return interp.apply(progress / moveTime);
        }
    }

}
