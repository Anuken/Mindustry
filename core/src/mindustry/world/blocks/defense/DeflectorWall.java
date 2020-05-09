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

    public class DeflectorEntity extends TileEntity{
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
        public void collision(Bulletc bullet){
            super.collision(bullet);

            //TODO fix and test
            //doesn't reflect powerful bullets
            if(bullet.damage() > maxDamageDeflect) return;

            float penX = Math.abs(getX() - bullet.x()), penY = Math.abs(getY() - bullet.y());

            bullet.hitbox(rect2);

            Vec2 position = Geometry.raycastRect(bullet.x() - bullet.vel().x*Time.delta(), bullet.y() - bullet.vel().y*Time.delta(), bullet.x() + bullet.vel().x*Time.delta(), bullet.y() + bullet.vel().y*Time.delta(),
            rect.setSize(size * tilesize + rect2.width*2 + rect2.height*2).setCenter(getX(), getY()));

            if(position != null){
                bullet.set(position.x, position.y);
            }

            if(penX > penY){
                bullet.vel().x *= -1;
            }else{
                bullet.vel().y *= -1;
            }

            //bullet.updateVelocity();
            bullet.owner(this);
            bullet.team(team());
            bullet.time(bullet.time() + 1f);
            //TODO deflect
            //bullet.deflect();

            hit = 1f;
        }
    }
}
