package mindustry.world.blocks.production;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.ArcAnnotate.*;
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
            ((accept.tileX() + Geometry.d4(accept.rotation()).x * size == tile.tileX() && accept.tileY() + Geometry.d4(accept.rotation()).y * size == tile.tileY()) || !accept.block().rotate);
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
        }

        /** @return true if the payload is in position. */
        public boolean moveInPayload(){
            if(payload == null) return false;

            payRotation = Mathf.slerpDelta(payRotation, rotate ? rotdeg() : 90f, 0.3f);
            payVector.approachDelta(Vec2.ZERO, payloadSpeed);

            return hasArrived();
        }

        public boolean hasArrived(){
            return payVector.isZero(0.01f);
        }

        public void drawPayload(){
            if(payload != null){
                Draw.z(Layer.blockOver);
                payload.draw(x + payVector.x, y + payVector.y, payRotation);
            }
        }
    }
}
