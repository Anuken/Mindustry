package mindustry.entities.comp;

import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;
import static mindustry.entities.Puddles.*;

@EntityDef(value = {Puddlec.class}, pooled = true)
@Component(base = true)
abstract class PuddleComp implements Posc, Puddlec, Drawc, Syncc{
    private static final Rect rect = new Rect(), rect2 = new Rect();

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
    @Import boolean added;

    transient float accepting, updateTime, lastRipple = Time.time + Mathf.random(40f), effectTime = Mathf.random(50f);
    float amount;
    Tile tile;
    Liquid liquid;

    public float getFlammability(){
        return liquid.flammability * amount;
    }

    @Override
    public void update(){
        if(liquid == null || tile == null){
            remove();
            return;
        }

        float addSpeed = accepting > 0 ? 3f : 0f;

        amount -= Time.delta * (1f - liquid.viscosity) / (5f + addSpeed);
        amount += accepting;
        accepting = 0f;

        if(amount >= maxLiquid / 1.5f){
            float deposited = Math.min((amount - maxLiquid / 1.5f) / 4f, 0.3f * Time.delta);
            int targets = 0;
            for(Point2 point : Geometry.d4){
                Tile other = world.tile(tile.x + point.x, tile.y + point.y);
                if(other != null && (other.block() == Blocks.air || liquid.moveThroughBlocks)){
                    targets ++;
                    Puddles.deposit(other, tile, liquid, deposited, false);
                }
            }
            amount -= deposited * targets;
        }

        if(liquid.capPuddles){
            amount = Mathf.clamp(amount, 0, maxLiquid);
        }

        if(amount <= 0f){
            remove();
            return;
        }

        if(Puddles.get(tile) != self() && added){
            //force removal without pool free
            Groups.all.remove(self());
            Groups.draw.remove(self());
            Groups.puddle.remove(self());
            added = false;
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

        if(!headless && liquid.particleEffect != Fx.none){
            if((effectTime += Time.delta) >= liquid.particleSpacing){
                float size = Mathf.clamp(amount / (maxLiquid / 1.5f)) * 4f;
                liquid.particleEffect.at(x + Mathf.range(size), y + Mathf.range(size));
                effectTime = 0f;
            }
        }

        updateTime -= Time.delta;

        liquid.update(self());
    }

    @Override
    public void draw(){
        Draw.z(Layer.debris - 1);

        liquid.drawPuddle(self());
    }

    @Replace
    public float clipSize(){
        return 50; //high for light drawing
    }

    @Override
    public void remove(){
        Puddles.remove(tile);
    }

    @Override
    public void afterRead(){
        Puddles.register(self());
    }

    @Override
    public void afterSync(){
        if(liquid != null){
            Puddles.register(self());
        }
    }
}
