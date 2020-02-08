package mindustry.entities;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

public class Effects{
    private static final float shakeFalloff = 10000f;

    private static void shake(float intensity, float duration){
        if(!headless){
            renderer.shake(intensity, duration);
        }
    }

    public static void shake(float intensity, float duration, float x, float y){
        if(Core.camera == null) return;

        float distance = Core.camera.position.dst(x, y);
        if(distance < 1) distance = 1;

        shake(Mathf.clamp(1f / (distance * distance / shakeFalloff)) * intensity, duration);
    }

    public static void shake(float intensity, float duration, Position loc){
        shake(intensity, duration, loc.getX(), loc.getY());
    }

    public static void createEffect(Effect effect, float x, float y, float rotation, Color color, Object data){
        if(headless || effect == Fx.none) return;
        if(Core.settings.getBool("effects")){
            Rect view = Core.camera.bounds(Tmp.r1);
            Rect pos = Tmp.r2.setSize(effect.size).setCenter(x, y);

            if(view.overlaps(pos)){
                Effectc entity = effect.ground ? GroundEffectEntity.create() : EffectEntity.create();
                entity.effect(effect);
                entity.rotation(rotation);
                entity.data(data);
                entity.lifetime(effect.lifetime);
                entity.set(x, y);
                entity.color().set(color);
                if(data instanceof Posc) entity.parent((Posc)data);
                entity.add();
            }
        }
    }
}
