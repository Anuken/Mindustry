package mindustry.entities;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.ArcAnnotate.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class Effect{
    private static final float shakeFalloff = 10000f;
    private static final EffectContainer container = new EffectContainer();
    private static final Seq<Effect> all = new Seq<>();

    public final int id;
    public final Cons<EffectContainer> renderer;
    public final float lifetime;
    /** Clip size. */
    public float size;

    public boolean ground;
    public float groundDuration;

    public Effect(float life, float clipsize, Cons<EffectContainer> renderer){
        this.id = all.size;
        this.lifetime = life;
        this.renderer = renderer;
        this.size = clipsize;
        all.add(this);
    }

    public Effect(float life, Cons<EffectContainer> renderer){
        this(life, 32f, renderer);
    }

    public Effect ground(){
        ground = true;
        return this;
    }

    public Effect ground(float duration){
        ground = true;
        this.groundDuration = duration;
        return this;
    }

    public void at(Position pos){
        create(this, pos.getX(), pos.getY(), 0, Color.white, null);
    }

    public void at(Position pos, float rotation){
        create(this, pos.getX(), pos.getY(), rotation, Color.white, null);
    }

    public void at(float x, float y){
        create(this, x, y, 0, Color.white, null);
    }

    public void at(float x, float y, float rotation){
        create(this, x, y, rotation, Color.white, null);
    }

    public void at(float x, float y, float rotation, Color color){
        create(this, x, y, rotation, color, null);
    }

    public void at(float x, float y, Color color){
        create(this, x, y, 0, color, null);
    }

    public void at(float x, float y, float rotation, Color color, Object data){
        create(this, x, y, rotation, color, data);
    }

    public void at(float x, float y, float rotation, Object data){
        create(this, x, y, rotation, Color.white, data);
    }

    public float render(int id, Color color, float life, float lifetime, float rotation, float x, float y, Object data){
        container.set(id, color, life, lifetime, rotation, x, y, data);
        Draw.z(ground ? Layer.debris : Layer.effect);
        Draw.reset();
        renderer.get(container);
        Draw.reset();

        return container.lifetime;
    }

    public static @Nullable Effect get(int id){
        return id >= all.size || id < 0 ? null : all.get(id);
    }

    private static void shake(float intensity, float duration){
        if(!headless){
            Vars.renderer.shake(intensity, duration);
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
                entity.effect = effect;
                entity.rotation = rotation;
                entity.data = (data);
                entity.lifetime = (effect.lifetime);
                entity.set(x, y);
                entity.color.set(color);
                if(data instanceof Posc) entity.parent = ((Posc)data);
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

    public static class EffectContainer implements Scaled{
        public float x, y, time, lifetime, rotation;
        public Color color;
        public int id;
        public Object data;
        private EffectContainer innerContainer;

        public void set(int id, Color color, float life, float lifetime, float rotation, float x, float y, Object data){
            this.x = x;
            this.y = y;
            this.color = color;
            this.time = life;
            this.lifetime = lifetime;
            this.id = id;
            this.rotation = rotation;
            this.data = data;
        }

        public <T> T data(){
            return (T)data;
        }

        public void scaled(float lifetime, Cons<EffectContainer> cons){
            if(innerContainer == null) innerContainer = new EffectContainer();
            if(time <= lifetime){
                innerContainer.set(id, color, time, lifetime, rotation, x, y, data);
                cons.get(innerContainer);
            }
        }

        @Override
        public float fin(){
            return time / lifetime;
        }
    }

}