package mindustry.world.blocks.production;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.graphics.*;

public class PayloadVoid extends PayloadAcceptor{
    public @Load("@-light") TextureRegion lightRegion;
    public Color lightColor = Color.valueOf("#ffb380");

    public PayloadVoid(String name){
        super(name);
        update = true;
        size = 3;
    }

    public class PayloadVoidBuild extends PayloadAcceptor<Payload>{
        public float warmup;

        @Override
        public void updateTile(){
            if(power.status >= 1f){
                if(payload != null){
                    Fx.smokeEffect.at(x + Mathf.range(4f), y + Mathf.range(4f));
                }

                payload = null;
                warmup = Mathf.lerp(warmup, 1, 0.02f);
            }else{
                warmup = Mathf.lerp(warmup, 0, 0.01f);
            }
        }

        @Override
        public void draw(){
            super.draw();
            if(warmup > 0.001){
                float g = 0.3f;
                float r = 0.06f;
                float cr = Mathf.random(0.1f);

                Draw.alpha(((1f - g) + Mathf.absin(Time.time(), 8f, g) + Mathf.random(r) - r) * warmup);
                Draw.tint(lightColor);
                Fill.circle(x, y, 3f + Mathf.absin(Time.time(), 5f, 2f) + cr);
                Draw.color(1f, 1f, 1f, warmup);
                Draw.rect(lightRegion, x, y);
                Fill.circle(x, y, 1.9f + Mathf.absin(Time.time(), 5f, 1f) + cr);

                Draw.color();
            }
        }

        @Override
        public void moveOutPayload(){}
    }
}
