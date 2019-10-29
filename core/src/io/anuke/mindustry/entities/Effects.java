package io.anuke.mindustry.entities;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.func.Cons;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Position;
import io.anuke.arc.util.pooling.Pools;
import io.anuke.mindustry.entities.type.EffectEntity;
import io.anuke.mindustry.entities.traits.ScaleTrait;

public class Effects{
    private static final EffectContainer container = new EffectContainer();
    private static Array<Effect> effects = new Array<>();
    private static ScreenshakeProvider shakeProvider;
    private static float shakeFalloff = 10000f;
    private static EffectProvider provider = (effect, color, x, y, rotation, data) -> {
        EffectEntity entity = Pools.obtain(EffectEntity.class, EffectEntity::new);
        entity.effect = effect;
        entity.color = color;
        entity.rotation = rotation;
        entity.data = data;
        entity.set(x, y);
        entity.add();
    };

    public static void setEffectProvider(EffectProvider prov){
        provider = prov;
    }

    public static void setScreenShakeProvider(ScreenshakeProvider provider){
        shakeProvider = provider;
    }

    public static void renderEffect(int id, Effect render, Color color, float life, float rotation, float x, float y, Object data){
        container.set(id, color, life, render.lifetime, rotation, x, y, data);
        render.draw.render(container);
    }

    public static Effect getEffect(int id){
        if(id >= effects.size || id < 0)
            throw new IllegalArgumentException("The effect with ID \"" + id + "\" does not exist!");
        return effects.get(id);
    }

    public static Array<Effect> all(){
        return effects;
    }

    public static void effect(Effect effect, float x, float y, float rotation){
        provider.createEffect(effect, Color.white, x, y, rotation, null);
    }

    public static void effect(Effect effect, float x, float y){
        effect(effect, x, y, 0);
    }

    public static void effect(Effect effect, Color color, float x, float y){
        provider.createEffect(effect, color, x, y, 0f, null);
    }

    public static void effect(Effect effect, Position loc){
        provider.createEffect(effect, Color.white, loc.getX(), loc.getY(), 0f, null);
    }

    public static void effect(Effect effect, Color color, float x, float y, float rotation){
        provider.createEffect(effect, color, x, y, rotation, null);
    }

    public static void effect(Effect effect, Color color, float x, float y, float rotation, Object data){
        provider.createEffect(effect, color, x, y, rotation, data);
    }

    public static void effect(Effect effect, float x, float y, float rotation, Object data){
        provider.createEffect(effect, Color.white, x, y, rotation, data);
    }

    /** Default value is 1000. Higher numbers mean more powerful shake (less falloff). */
    public static void setShakeFalloff(float falloff){
        shakeFalloff = falloff;
    }

    private static void shake(float intensity, float duration){
        if(shakeProvider == null) throw new RuntimeException("Screenshake provider is null! Set it first.");
        shakeProvider.accept(intensity, duration);
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

    public interface ScreenshakeProvider{
        void accept(float intensity, float duration);
    }

    public static class Effect{
        private static int lastid = 0;
        public final int id;
        public final EffectRenderer draw;
        public final float lifetime;
        /** Clip size. */
        public float size;

        public Effect(float life, float clipsize, EffectRenderer draw){
            this.id = lastid++;
            this.lifetime = life;
            this.draw = draw;
            this.size = clipsize;
            effects.add(this);
        }

        public Effect(float life, EffectRenderer draw){
            this(life, 28f, draw);
        }
    }

    public static class EffectContainer implements ScaleTrait{
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

    public interface EffectProvider{
        void createEffect(Effect effect, Color color, float x, float y, float rotation, Object data);
    }

    public interface EffectRenderer{
        void render(EffectContainer effect);
    }
}
