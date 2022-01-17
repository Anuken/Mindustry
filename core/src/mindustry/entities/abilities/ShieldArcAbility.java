package mindustry.entities.abilities;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;

public class ShieldArcAbility extends Ability{
    private static Unit paramUnit;
    private static ShieldArcAbility paramField;
    private static Vec2 paramPos = new Vec2();
    private static final Cons<Bullet> shieldConsumer = b -> {
        if(b.team != paramUnit.team && b.type.absorbable && paramField.data > 0 &&
            !paramPos.within(b, paramField.radius + paramField.width/2f) &&
            Tmp.v1.set(b).add(b.vel).within(paramPos, paramField.radius + paramField.width/2f) &&
            Angles.within(paramPos.angleTo(b), paramUnit.rotation + paramField.angleOffset, paramField.angle / 2f)){

            b.absorb();
            Fx.absorb.at(b);

            //break shield
            if(paramField.data <= b.damage()){
                paramField.data -= paramField.cooldown * paramField.regen;

                //TODO fx
            }

            paramField.data -= b.damage();
            paramField.alpha = 1f;
        }
    };

    /** Shield radius. */
    public float radius = 60f;
    /** Shield regen speed in damage/tick. */
    public float regen = 0.1f;
    /** Maximum shield. */
    public float max = 200f;
    /** Cooldown after the shield is broken, in ticks. */
    public float cooldown = 60f * 5;
    /** Angle of shield arc. */
    public float angle = 80f;
    /** Offset parameters for shield. */
    public float angleOffset = 0f, x = 0f, y = 0f;
    /** If true, only activates when shooting. */
    public boolean whenShooting = true;
    /** Width of shield line. */
    public float width = 6f;

    /** Whether to draw the arc line. */
    public boolean drawArc = true;
    /** If not null, will be drawn on top. */
    public @Nullable String region;
    /** If true, sprite position will be influenced by x/y. */
    public boolean offsetRegion = false;

    /** State. */
    protected float widthScale, alpha;

    @Override
    public void update(Unit unit){
        if(data < max){
            data += Time.delta * regen;
        }

        boolean active = data > 0 && (unit.isShooting || !whenShooting);
        alpha = Math.max(alpha - Time.delta/10f, 0f);

        if(active){
            widthScale = Mathf.lerpDelta(widthScale, 1f, 0.06f);
            paramUnit = unit;
            paramField = this;
            paramPos.set(x, y).rotate(unit.rotation - 90f).add(unit);

            Groups.bullet.intersect(unit.x - radius, unit.y - radius, radius * 2f, radius * 2f, shieldConsumer);
        }else{
            widthScale = Mathf.lerpDelta(widthScale, 0f, 0.11f);
        }
    }

    @Override
    public void init(UnitType type){
        data = max;
    }

    @Override
    public void draw(Unit unit){

        if(widthScale > 0.001f){
            Draw.z(Layer.shields);

            Draw.color(unit.team.color, Color.white, Mathf.clamp(alpha));
            var pos = paramPos.set(x, y).rotate(unit.rotation - 90f).add(unit);

            if(Vars.renderer.animateShields){
                if(region != null){
                    Vec2 rp = offsetRegion ? pos : Tmp.v1.set(unit);
                    Draw.yscl = widthScale;
                    Draw.rect(region, rp.x, rp.y, unit.rotation - 90);
                    Draw.yscl = 1f;
                }

                if(drawArc){
                    Lines.stroke(width * widthScale);
                    Lines.arc(pos.x, pos.y, radius, angle / 360f, unit.rotation + angleOffset - angle / 2f);
                }
            }else{
                //TODO
                Lines.stroke(1.5f);
                Draw.alpha(0.09f);
                Fill.poly(pos.x, pos.y, 6, radius);
                Draw.alpha(1f);
                Lines.poly(pos.x, pos.y, 6, radius);
            }
            Draw.reset();
        }
    }

    @Override
    public void displayBars(Unit unit, Table bars){
        bars.add(new Bar("stat.shieldhealth", Pal.accent, () -> data / max)).row();
    }
}
