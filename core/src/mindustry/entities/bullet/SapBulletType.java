package mindustry.entities.bullet;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class SapBulletType extends BulletType{
    public float length = 100f;
    public float sapStrength = 0.5f;
    public Color color = Color.white.cpy();
    public float width = 0.4f;

    public SapBulletType(){
        speed = 0.0001f;
        despawnEffect = Fx.none;
        pierce = true;
        hitSize = 0f;
        hittable = false;
        hitEffect = Fx.hitLiquid;
        status = StatusEffects.sapped;
        statusDuration = 60f * 3f;
    }

    @Override
    public void draw(Bullet b){
        if(b.data instanceof Position){
            Position data = (Position)b.data;
            Tmp.v1.set(data).lerp(b, b.fin());

            Draw.color(color);
            Drawf.laser(b.team, Core.atlas.find("laser"), Core.atlas.find("laser-end"),
                b.x, b.y, Tmp.v1.x, Tmp.v1.y, width * b.fout());

            Draw.reset();

            Drawf.light(b.team, b.x, b.y, b.x + Tmp.v1.x, b.y + Tmp.v1.y, 15f * b.fout(), lightColor, 0.6f);
        }
    }

    @Override
    public float range(){
        return length;
    }

    @Override
    public void init(Bullet b){
        super.init(b);

        Healthc target = Damage.linecast(b, b.x, b.y, b.rotation(), length);
        b.data = target;

        if(target != null){
            float result = Math.min(target.health(), damage);

            if(b.owner instanceof Healthc){
                ((Healthc)b.owner).heal(result * sapStrength);
            }
        }

        if(target instanceof Hitboxc){
            Hitboxc hit = (Hitboxc)target;

            hit.collision(b, hit.x(), hit.y());
            b.collision(hit, hit.x(), hit.y());
        }else if(target instanceof Building){
            Building tile = (Building)target;

            if(tile.collide(b)){
                tile.collision(b);
                hit(b, tile.x, tile.y);
            }
        }else{
            b.data = new Vec2().trns(b.rotation(), length).add(b.x, b.y);
        }
    }
}
