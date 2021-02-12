package mindustry.world.blocks.production;

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

import static mindustry.Vars.*;

public class PayloadAcceptor extends Block{
    public float payloadSpeed = 0.5f, payloadRotateSpeed = 5f;

    public @Load(value = "@-top", fallback = "factory-top-@size") TextureRegion topRegion;
    public @Load(value = "@-out", fallback = "factory-out-@size") TextureRegion outRegion;
    public @Load(value = "@-in", fallback = "factory-in-@size") TextureRegion inRegion;

    public PayloadAcceptor(String name){
        super(name);

        update = true;
        sync = true;
    }

    public static boolean blends(Building build, int direction){
        int size = build.block.size;
        int trns = build.block.size/2 + 1;
        Building accept = build.nearby(Geometry.d4(direction).x * trns, Geometry.d4(direction).y * trns);
        return accept != null &&
            accept.block.outputsPayload &&

            //if size is the same, block must either be facing this one, or not be rotating
            ((accept.block.size == size
            && Math.abs(accept.tileX() - build.tileX()) % size == 0 //check alignment
            && Math.abs(accept.tileY() - build.tileY()) % size == 0
            && ((accept.block.rotate && accept.tileX() + Geometry.d4(accept.rotation).x * size == build.tileX() && accept.tileY() + Geometry.d4(accept.rotation).y * size == build.tileY())
            || !accept.block.rotate
            || !accept.block.outputFacing)) ||

            //if the other block is smaller, check alignment
            (accept.block.size < size &&
            (accept.rotation % 2 == 0 ? //check orientation; make sure it's aligned properly with this block.
                Math.abs(accept.y - build.y) <= (size * tilesize - accept.block.size * tilesize)/2f : //check Y alignment
                Math.abs(accept.x - build.x) <= (size * tilesize - accept.block.size * tilesize)/2f   //check X alignment
                )) && (!accept.block.rotate || accept.front() == build || !accept.block.outputFacing) //make sure it's facing this block
            );
    }

    public class PayloadAcceptorBuild<T extends Payload> extends Building{
        public @Nullable T payload;
        public Vec2 payVector = new Vec2();
        public float payRotation;
        public boolean carried;

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            return this.payload == null;
        }

        @Override
        public void handlePayload(Building source, Payload payload){
            this.payload = (T)payload;
            this.payVector.set(source).sub(this).clamp(-size * tilesize / 2f, -size * tilesize / 2f, size * tilesize / 2f, size * tilesize / 2f);
            this.payRotation = payload.rotation();

            updatePayload();
        }

        @Override
        public Payload getPayload(){
            return payload;
        }

        @Override
        public void pickedUp(){
            carried = true;
        }

        @Override
        public void drawTeamTop(){
            carried = false;
        }

        @Override
        public Payload takePayload(){
            T t = payload;
            payload = null;
            return t;
        }

        @Override
        public void onRemoved(){
            super.onRemoved();
            if(payload != null && !carried) payload.dump();
        }

        public boolean blends(int direction){
            return PayloadAcceptor.blends(this, direction);
        }

        public void updatePayload(){
            if(payload != null){
                payload.set(x + payVector.x, y + payVector.y, payRotation);
            }
        }

        /** @return true if the payload is in position. */
        public boolean moveInPayload(){
            if(payload == null) return false;

            updatePayload();

            payRotation = Angles.moveToward(payRotation, rotate ? rotdeg() : 90f, payloadRotateSpeed * edelta());
            payVector.approach(Vec2.ZERO, payloadSpeed * delta());

            return hasArrived();
        }

        public void moveOutPayload(){
            if(payload == null) return;

            updatePayload();

            Vec2 dest = Tmp.v1.trns(rotdeg(), size* tilesize/2f);

            payRotation = Angles.moveToward(payRotation, rotdeg(), payloadRotateSpeed * edelta());
            payVector.approach(dest, payloadSpeed * delta());

            if(payVector.within(dest, 0.001f)){
                payVector.clamp(-size * tilesize / 2f, -size * tilesize / 2f, size * tilesize / 2f, size * tilesize / 2f);

                Building front = front();
                if(front != null && front.block.outputsPayload){
                    if(movePayload(payload)){
                        payload = null;
                    }
                }else if(front == null || !front.tile().solid()){
                    dumpPayload();
                }
            }
        }

        public void dumpPayload(){
            if(payload.dump()){
                payload = null;
            }
        }

        public boolean hasArrived(){
            return payVector.isZero(0.01f);
        }

        public void drawPayload(){
            if(payload != null){
                updatePayload();

                Draw.z(Layer.blockOver);
                payload.draw();
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.f(payVector.x);
            write.f(payVector.y);
            write.f(payRotation);
            Payload.write(payload, write);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            payVector.set(read.f(), read.f());
            payRotation = read.f();
            payload = Payload.read(read);
        }
    }
}
