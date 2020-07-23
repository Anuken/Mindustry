package mindustry.entities;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

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

    public static void create(Effect effect, float x, float y, float rotation, Color color, Object data){
        if(headless || effect == Fx.none) return;
        if(Core.settings.getBool("effects")){
            Rect view = Core.camera.bounds(Tmp.r1);
            Rect pos = Tmp.r2.setSize(effect.size).setCenter(x, y);

            if(view.overlaps(pos)){
                EffectState entity = EffectState.create();
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

    public static void decal(TextureRegion region, float x, float y, float rotation){
        decal(region, x, y, rotation, 3600f, Pal.rubble);
    }

    public static void decal(TextureRegion region, float x, float y, float rotation, float lifetime, Color color){
        if(headless || region == null || !Core.atlas.isFound(region)) return;

        Tile tile = world.tileWorld(x, y);
        if(tile == null || tile.floor().isLiquid) return;

        Decal decal = Decal.create();
        decal.set(x, y);
        decal.rotation(rotation);
        decal.lifetime(lifetime);
        decal.color().set(color);
        decal.region(region);
        decal.add();
    }

    public static void scorch(float x, float y, int size){
        if(headless) return;

        size = Mathf.clamp(size, 0, 9);

        TextureRegion region = Core.atlas.find("scorch-" + size + "-" + Mathf.random(2));
        decal(region, x, y, Mathf.random(4) * 90, 3600, Pal.rubble);
    }

    public static void rubble(float x, float y, int blockSize){
        if(headless) return;

        TextureRegion region = Core.atlas.find("rubble-" + blockSize + "-" + (Core.atlas.has("rubble-" + blockSize + "-1") ? Mathf.random(0, 1) : "0"));
        decal(region, x, y, Mathf.random(4) * 90, 3600, Pal.rubble);
    }
}
