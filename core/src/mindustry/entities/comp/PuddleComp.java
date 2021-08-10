package mindustry.entities.comp;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;
import static mindustry.entities.Puddles.*;

@EntityDef(value = {Puddlec.class}, pooled = true)
@Component(base = true)
abstract class PuddleComp implements Posc, Puddlec, Drawc{
    private static final Rect rect = new Rect(), rect2 = new Rect();
    private static int seeds;

    private static Puddle paramPuddle;
    private static Cons<Unit> unitCons = unit -> {
        if(unit.isGrounded() && !unit.hovering){
            unit.hitbox(rect2);
            if(rect.overlaps(rect2)){
                unit.apply(paramPuddle.liquid.effect, 60 * 2);

                if(unit.vel.len2() > 0.1f * 0.1f){
                    Fx.ripple.at(unit.x, unit.y, unit.type.rippleScale, paramPuddle.liquid.color);
                }
            }
        }
    };

    @Import int id;
    @Import float x, y;

    transient private boolean mismatch = false;
    transient float accepting, updateTime, lastRipple = Time.time + Mathf.random(40f);
    float amount;
    Tile tile;
    Liquid liquid;

    public float getFlammability(){
        return liquid.flammability * amount;
    }

    @Override
    public void update(){
        float addSpeed = accepting > 0 ? 3f : 0f;

        amount -= Time.delta * (1f - liquid.viscosity) / (5f + addSpeed);
        amount += accepting;
        accepting = 0f;

        if(amount >= maxLiquid / 1.5f){
            float deposited = Math.min((amount - maxLiquid / 1.5f) / 4f, 0.3f) * Time.delta;
            int targets = 0;
            for(Point2 point : Geometry.d4){
                Tile other = world.tile(tile.x + point.x, tile.y + point.y);
                if(other != null && other.block() == Blocks.air){
                    targets ++;
                    Puddles.deposit(other, tile, liquid, deposited, false);
                }
            }
            amount -= deposited * targets;
        }

        amount = Mathf.clamp(amount, 0, maxLiquid);

        if(amount <= 0f){
            remove();
            return;
        }

        if(Puddles.get(tile) != self()){
            mismatch = true;
            remove();
            return;
        }

        //effects-only code
        if(amount >= maxLiquid / 2f && updateTime <= 0f){
            paramPuddle = self();

            Units.nearby(rect.setSize(Mathf.clamp(amount / (maxLiquid / 1.5f)) * 10f).setCenter(x, y), unitCons);

            if(liquid.temperature > 0.7f && tile.build != null && Mathf.chance(0.5)){
                Fires.create(tile);
            }

            updateTime = 40f;
        }

        updateTime -= Time.delta;
    }

    @Override
    public void draw(){
        Draw.z(Layer.debris - 1);

        seeds = id;
        boolean onLiquid = tile.floor().isLiquid;
        float f = Mathf.clamp(amount / (maxLiquid / 1.5f));
        float smag = onLiquid ? 0.8f : 0f;
        float sscl = 25f;

        Draw.color(Tmp.c1.set(liquid.color).shiftValue(-0.05f));
        Fill.circle(x + Mathf.sin(Time.time + seeds * 532, sscl, smag), y + Mathf.sin(Time.time + seeds * 53, sscl, smag), f * 8f);
        Angles.randLenVectors(id(), 3, f * 6f, (ex, ey) -> {
            Fill.circle(x + ex + Mathf.sin(Time.time + seeds * 532, sscl, smag),
            y + ey + Mathf.sin(Time.time + seeds * 53, sscl, smag), f * 5f);
            seeds++;
        });
        Draw.color();

        if(liquid.lightColor.a > 0.001f && f > 0){
            Color color = liquid.lightColor;
            float opacity = color.a * f;
            Drawf.light(Team.derelict, tile.drawx(), tile.drawy(),  30f * f, color, opacity * 0.8f);
        }
    }

    @Replace
    public float clipSize(){
        return 20;
    }

    @Override
    public void remove(){
        if(!mismatch){
            Puddles.remove(tile);
        }
    }

    @Override
    public void afterRead(){
        Puddles.register(self());
    }
}
