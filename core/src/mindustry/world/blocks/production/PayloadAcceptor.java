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

    public PayloadAcceptor(String name){
        super(name);

        update = true;
    }

    public class PayloadAcceptorEntity extends TileEntity{
        public @Nullable Payload payload;
        public Vec2 inputVector = new Vec2();
        public float inputRotation;

        @Override
        public boolean acceptPayload(Tilec source, Payload payload){
            return this.payload == null;
        }

        @Override
        public void handlePayload(Tilec source, Payload payload){
            this.payload = payload;
            this.inputVector.set(source).sub(this).clamp(-size * tilesize / 2f, size * tilesize / 2f, -size * tilesize / 2f, size * tilesize / 2f);
            this.inputRotation = source.angleTo(this);
        }

        /** @return true if the payload is in position. */
        public boolean updatePayload(){
            if(payload == null) return false;

            inputRotation = Mathf.slerpDelta(inputRotation, 90f, 0.3f);
            inputVector.lerpDelta(Vec2.ZERO, 0.2f);

            return inputVector.isZero(0.5f);
        }

        public void drawPayload(){
            if(payload != null){
                Draw.z(Layer.blockOver);
                payload.draw(x + inputVector.x, y + inputVector.y, inputRotation);
            }
        }
    }
}
