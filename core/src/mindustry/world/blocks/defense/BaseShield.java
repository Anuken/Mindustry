package mindustry.world.blocks.defense;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class BaseShield extends Block{
    //TODO game rule? or field? should vary by base.
    //public float radius = 400f;

    protected static BaseShieldBuild paramBuild;
    //protected static Effect paramEffect;
    protected static final Cons<Bullet> bulletConsumer = bullet -> {
        if(bullet.team != paramBuild.team && bullet.type.absorbable && bullet.within(paramBuild, paramBuild.radius())){
            bullet.absorb();
            //paramEffect.at(bullet);

            //TODO effect, shield health go down?
            //paramBuild.hit = 1f;
            //paramBuild.buildup += bullet.damage;
        }
    };

    protected static final Cons<Unit> unitConsumer = unit -> {
        //if this is positive, repel the unit; if it exceeds the unit radius * 2, it's inside the forcefield and must be killed
        float overlapDst = (unit.hitSize/2f + paramBuild.radius()) - unit.dst(paramBuild);

        if(overlapDst > 0){
            if(overlapDst > unit.hitSize){
                //instakill units that are stuck inside the shield (TODO or maybe damage them instead?)
                unit.kill();
            }else{
                unit.move(Tmp.v1.set(unit).sub(paramBuild).setLength(overlapDst));
            }
        }
    };


    public BaseShield(String name){
        super(name);

        hasPower = true;
    }

    public class BaseShieldBuild extends Building{
        public boolean broken = false; //TODO
        public float hit = 0f;

        @Override
        public void updateTile(){
            //TODO smooth radius
            float radius = radius();

            if(radius > 0 && !broken){
                paramBuild = this;
                //paramEffect = absorbEffect;
                Groups.bullet.intersect(x - radius, y - radius, radius * 2f, radius * 2f, bulletConsumer);
                Units.nearbyEnemies(team,x ,y, radius, unitConsumer);
            }
        }

        public float radius(){
            //TODO bad rule?
            return Vars.state.rules.enemyCoreBuildRadius;
        }

        @Override
        public void draw(){
            super.draw();

            drawShield();
        }

        public void drawShield(){
            if(!broken){
                float radius = radius();

                Draw.z(Layer.shields);

                Draw.color(team.color, Color.white, Mathf.clamp(hit));

                if(renderer.animateShields){
                    Fill.circle(x, y, radius);
                }else{
                    Lines.stroke(1.5f);
                    Draw.alpha(0.09f + Mathf.clamp(0.08f * hit));
                    Fill.circle(x, y, radius);
                    Draw.alpha(1f);
                    //TODO
                    Lines.poly(x, y, 60, radius);
                    Draw.reset();
                }
            }

            Draw.reset();
        }
    }
}
