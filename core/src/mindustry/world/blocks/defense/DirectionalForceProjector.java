package mindustry.world.blocks.defense;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;

import static mindustry.Vars.*;

public class DirectionalForceProjector extends ForceProjector{
    protected static final Vec2 intersectOut = new Vec2(), p1 = new Vec2(), p2 = new Vec2();
    protected static final Cons<Bullet> dirShieldConsumer = b -> {
        if(b.team != paramEntity.team && b.type.absorbable){
            //just in case
            float deltaAdd = 1.1f;

            if(Intersector.intersectSegments(b.x, b.y, b.x + b.vel.x * (Time.delta + deltaAdd), b.y + b.vel.y * (Time.delta + deltaAdd), p1.x, p1.y, p2.x, p2.y, intersectOut)){
                b.set(intersectOut);
                b.absorb();
                paramEffect.at(b);
                paramEntity.hit = 1f;
                paramEntity.buildup += b.damage();
            }
        }
    };

    //TODO proper length?
    public float length = 40f;
    public float padSize = 40f;

    public DirectionalForceProjector(String name){
        super(name);

        radius = 30f;
        consumeCoolant = false;
        rotate = true;
        rotateDraw = false;
    }

    @Override
    public void init(){
        super.init();
        if(length < 0){
            length = size * tilesize/2f;
        }
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        drawPotentialLinks(x, y);

        //TODO
    }

    public class DirectionalForceProjectorBuild extends ForceBuild{

        @Override
        public void deflectBullets(){
            float realRadius = realRadius();

            if(realRadius > 0 && !broken){
                paramEntity = this;
                paramEffect = absorbEffect;

                //top
                p1.set(length, realRadius).rotate(rotdeg());
                //bot
                p2.set(length, -realRadius).rotate(rotdeg());

                //"check" radius is grown to catch bullets moving at high velocity
                Tmp.r1.set(p2.x, p2.y, p1.x - p2.x, p1.y - p2.y).normalize().grow(padSize);

                p1.add(x, y);
                p2.add(x, y);

                Groups.bullet.intersect(x + Tmp.r1.x, y + Tmp.r1.y, Tmp.r1.width, Tmp.r1.height, dirShieldConsumer);
            }
        }

        @Override
        public void drawShield(){
            if(!broken && realRadius() > 0){
                float realRadius = realRadius(), rot = rotdeg();

                p1.set(length, realRadius).rotate(rot).add(this);
                p2.set(length, -realRadius).rotate(rot).add(this);
                float size = 3f;
                Tmp.r1.set(p2.x, p2.y, p1.x - p2.x, p1.y - p2.y).normalize().grow(size);

                Draw.z(Layer.shields);

                Draw.color(team.color, Color.white, Mathf.clamp(hit));

                if(renderer.animateShields){
                    Fill.rect(Tmp.r1);

                    Tmp.v1.set(length - size/2f - size * 2, (realRadius + size/2f)).rotate(rot).add(this);
                    Tmp.v2.set(length - size/2f - size * 2, -(realRadius + size/2f)).rotate(rot).add(this);

                    Fill.tri(x, y, Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y);

                    for(int i : Mathf.signs){
                        Tmp.v1.set(length - size/2f, (realRadius + size/2f) * i).rotate(rot).add(this);
                        Tmp.v3.set(length + size/2f, (realRadius + size/2f) * i).rotate(rot).add(this);
                        Tmp.v2.set(length, (realRadius + size) * i).rotate(rot).add(this);
                        Fill.tri(Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y, Tmp.v3.x, Tmp.v3.y);
                    }
                }else{
                    Lines.stroke(1.5f);
                    Draw.alpha(0.09f + Mathf.clamp(0.08f * hit));
                    Fill.rect(Tmp.r1);
                    Draw.alpha(1f);
                    Lines.rect(Tmp.r1);
                    Draw.reset();
                }

                Draw.reset();
            }
        }
    }
}
