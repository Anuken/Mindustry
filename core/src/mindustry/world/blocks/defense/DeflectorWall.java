package mindustry.world.blocks.defense;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.gen.*;

import static mindustry.Vars.tilesize;

public class DeflectorWall extends Wall{
    public static final float hitTime = 10f;

    protected float maxDamageDeflect = 10f;
    protected Rect rect = new Rect();
    protected Rect rect2 = new Rect();

    public DeflectorWall(String name){
        super(name);
    }

    public class DeflectorEntity extends Building{
        public float hit;

        @Override
        public void draw(){
            super.draw();

            if(hit < 0.0001f) return;

            Draw.color(Color.white);
            Draw.alpha(hit * 0.5f);
            Draw.blend(Blending.additive);
            Fill.rect(x, y, tilesize * size, tilesize * size);
            Draw.blend();
            Draw.reset();

            hit = Mathf.clamp(hit - Time.delta() / hitTime);
        }

        @Override
        public boolean collision(Bullet bullet){
            super.collision(bullet);

            //doesn't reflect powerful bullets
            if(bullet.damage() > maxDamageDeflect) return true;

            //translate bullet back to where it was upon collision
            bullet.trns(-bullet.vel().x, -bullet.vel().y);

            float penX = Math.abs(x - bullet.x()), penY = Math.abs(y - bullet.y());

            if(penX > penY){
                bullet.vel().x *= -1;
            }else{
                bullet.vel().y *= -1;
            }

            bullet.owner(this);
            bullet.team(team);
            bullet.time(bullet.time() + 1f);

            hit = 1f;

            return false;
        }
    }
}
