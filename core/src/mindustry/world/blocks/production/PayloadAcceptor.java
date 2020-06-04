package mindustry.world.blocks.production;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.ArcAnnotate.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;

import static mindustry.Vars.tilesize;

public class PayloadAcceptor extends Block{
    public float payloadSpeed = 0.5f;

    public PayloadAcceptor(String name){
        super(name);

        update = true;
    }

    public static boolean blends(Tilec tile, int direction){
        int size = tile.block().size;
        Tilec accept = tile.nearby(Geometry.d4(direction).x * size, Geometry.d4(direction).y * size);
        return accept != null &&
            accept.block().size == size &&
            accept.block().outputsPayload &&
            //block must either be facing this one, or not be rotating
            ((accept.tileX() + Geometry.d4(accept.rotation()).x * size == tile.tileX() && accept.tileY() + Geometry.d4(accept.rotation()).y * size == tile.tileY())
            || !accept.block().rotate  || (accept.block().rotate && !accept.block().outputFacing));
    }

    public class PayloadAcceptorEntity<T extends Payload> extends TileEntity{
        public @Nullable T payload;
        public Vec2 payVector = new Vec2();
        public float payRotation;

        @Override
        public boolean acceptPayload(Tilec source, Payload payload){
            return this.payload == null;
        }

        @Override
        public void handlePayload(Tilec source, Payload payload){
            this.payload = (T)payload;
            this.payVector.set(source).sub(this).clamp(-size * tilesize / 2f, size * tilesize / 2f, -size * tilesize / 2f, size * tilesize / 2f);
            this.payRotation = source.angleTo(this);

            updatePayload();
        }

        @Override
        public Payload takePayload(){
            T t = payload;
            payload = null;
            return t;
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

            payRotation = Mathf.slerpDelta(payRotation, rotate ? rotdeg() : 90f, 0.3f);
            payVector.approachDelta(Vec2.ZERO, payloadSpeed);

            return hasArrived();
        }

        public void moveOutPayload(){
            if(payload == null) return;

            updatePayload();

            payVector.trns(rotdeg(), payVector.len() + edelta() * payloadSpeed);
            payRotation = rotdeg();

            if(payVector.len() >= size * tilesize/2f){
                payVector.clamp(-size * tilesize / 2f, size * tilesize / 2f, -size * tilesize / 2f, size * tilesize / 2f);

                Tilec front = front();
                if(front != null && front.block().outputsPayload){
                    if(movePayload(payload)){
                        payload = null;
                    }
                }else if(front != null && !front.tile().solid()){
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
                payload.set(x + payVector.x, y + payVector.y, payRotation);

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
