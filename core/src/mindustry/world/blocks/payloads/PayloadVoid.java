package mindustry.world.blocks.payloads;

import arc.audio.*;
import arc.graphics.g2d.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class PayloadVoid extends PayloadBlock{
    public Effect incinerateEffect = Fx.blastExplosion;
    public Sound incinerateSound = Sounds.bang;

    public PayloadVoid(String name){
        super(name);

        outputsPayload = false;
        acceptsPayload = true;
        update = true;
        rotate = false;
        size = 3;
        payloadSpeed = 1.2f;
        //make sure to display large units.
        clipSize = 120;
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, topRegion};
    }

    public class PayloadVoidBuild extends PayloadBlockBuild<Payload>{

        @Override
        public void draw(){
            Draw.rect(region, x, y);

            //draw input
            for(int i = 0; i < 4; i++){
                if(blends(i)){
                    Draw.rect(inRegion, x, y, (i * 90) - 180);
                }
            }

            Draw.rect(topRegion, x, y);

            Draw.z(Layer.blockOver);
            drawPayload();
        }

        @Override
        public boolean acceptUnitPayload(Unit unit){
            return true;
        }

        @Override
        public void updateTile(){
            if(moveInPayload(false) && cons.valid()){
                payload = null;
                incinerateEffect.at(this);
                incinerateSound.at(this);
            }
        }
    }
}
