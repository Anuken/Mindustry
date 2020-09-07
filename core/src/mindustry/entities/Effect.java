package mindustry.entities;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import mindustry.graphics.*;

public class Effect{
    private static final EffectContainer container = new EffectContainer();
    private static int lastid = 0;

    public final int id;
    public final Cons<EffectContainer> renderer;
    public final float lifetime;
    /** Clip size. */
    public float size;

    public boolean ground;
    public float groundDuration;

    public Effect(float life, float clipsize, Cons<EffectContainer> renderer){
        this.id = lastid++;
        this.lifetime = life;
        this.renderer = renderer;
        this.size = clipsize;
    }

    public Effect(float life, Cons<EffectContainer> renderer){
        this(life, 28f, renderer);
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
        Effects.create(this, pos.getX(), pos.getY(), 0, Color.white, null);
    }

    public void at(Position pos, float rotation){
        Effects.create(this, pos.getX(), pos.getY(), rotation, Color.white, null);
    }

    public void at(float x, float y){
        Effects.create(this, x, y, 0, Color.white, null);
    }

    public void at(float x, float y, float rotation){
        Effects.create(this, x, y, rotation, Color.white, null);
    }

    public void at(float x, float y, float rotation, Color color){
        Effects.create(this, x, y, rotation, color, null);
    }

    public void at(float x, float y, Color color){
        Effects.create(this, x, y, 0, color, null);
    }

    public void at(float x, float y, float rotation, Color color, Object data){
        Effects.create(this, x, y, rotation, color, data);
    }

    public void at(float x, float y, float rotation, Object data){
        Effects.create(this, x, y, rotation, Color.white, data);
    }

    public void render(int id, Color color, float life, float rotation, float x, float y, Object data){
        container.set(id, color, life, lifetime, rotation, x, y, data);
        Draw.z(ground ? Layer.debris : Layer.effect);
        renderer.get(container);
        Draw.reset();
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