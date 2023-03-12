package mindustry.entities;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.effect.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class Effect{
    private static final float shakeFalloff = 10000f;
    private static final EffectContainer container = new EffectContainer();

    public static final Seq<Effect> all = new Seq<>();

    private boolean initialized;

    public final int id;

    public Cons<EffectContainer> renderer = e -> {};
    public float lifetime = 50f;
    /** Clip size. */
    public float clip;
    /** Time delay before the effect starts */
    public float startDelay;
    /** Amount added to rotation */
    public float baseRotation;
    /** If true, parent unit is data are followed. */
    public boolean followParent = true;
    /** If this and followParent are true, the effect will offset and rotate with the parent's rotation. */
    public boolean rotWithParent;

    public float layer = Layer.effect;
    public float layerDuration;

    public Effect(float life, float clipsize, Cons<EffectContainer> renderer){
        this.id = all.size;
        this.lifetime = life;
        this.renderer = renderer;
        this.clip = clipsize;
        all.add(this);
    }

    public Effect(float life, Cons<EffectContainer> renderer){
        this(life, 50f, renderer);
    }

    //for custom implementations
    public Effect(){
        this.id = all.size;
        all.add(this);
    }

    public Effect startDelay(float d){
        startDelay = d;
        return this;
    }

    public void init(){}

    public Effect followParent(boolean follow){
        followParent = follow;
        return this;
    }

    public Effect rotWithParent(boolean follow){
        rotWithParent = follow;
        return this;
    }

    public Effect layer(float l){
        layer = l;
        return this;
    }

    public Effect baseRotation(float d){
        baseRotation = d;
        return this;
    }

    public Effect layer(float l, float duration){
        layer = l;
        this.layerDuration = duration;
        return this;
    }

    public WrapEffect wrap(Color color){
        return new WrapEffect(this, color);
    }

    public WrapEffect wrap(Color color, float rotation){
        return new WrapEffect(this, color, rotation);
    }

    public void at(Position pos){
        create(pos.getX(), pos.getY(), 0, Color.white, null);
    }

    public void at(Position pos, boolean parentize){
        create(pos.getX(), pos.getY(), 0, Color.white, parentize ? pos : null);
    }

    public void at(Position pos, float rotation){
        create(pos.getX(), pos.getY(), rotation, Color.white, null);
    }

    public void at(float x, float y){
        create(x, y, 0, Color.white, null);
    }

    public void at(float x, float y, float rotation){
        create(x, y, rotation, Color.white, null);
    }

    public void at(float x, float y, float rotation, Color color){
        create(x, y, rotation, color, null);
    }

    public void at(float x, float y, Color color){
        create(x, y, 0, color, null);
    }

    public void at(float x, float y, float rotation, Color color, Object data){
        create(x, y, rotation, color, data);
    }

    public void at(float x, float y, float rotation, Object data){
        create(x, y, rotation, Color.white, data);
    }

    public boolean shouldCreate(){
        return !headless && this != Fx.none && Vars.renderer.enableEffects;
    }

    public void create(float x, float y, float rotation, Color color, Object data){
        if(!shouldCreate()) return;

        if(Core.camera.bounds(Tmp.r1).overlaps(Tmp.r2.setCentered(x, y, clip))){
            if(!initialized){
                initialized = true;
                init();
            }

            if(startDelay <= 0f){
                add(x, y, rotation, color, data);
            }else{
                Time.run(startDelay, () -> add(x, y, rotation, color, data));
            }
        }
    }

    protected void add(float x, float y, float rotation, Color color, Object data){
        var entity = EffectState.create();
        entity.effect = this;
        entity.rotation = baseRotation + rotation;
        entity.data = data;
        entity.lifetime = lifetime;
        entity.set(x, y);
        entity.color.set(color);
        if(followParent && data instanceof Posc p){
            entity.parent = p;
            entity.rotWithParent = rotWithParent;
        }
        entity.add();
    }

    public float render(int id, Color color, float life, float lifetime, float rotation, float x, float y, Object data){
        container.set(id, color, life, lifetime, rotation, x, y, data);
        Draw.z(layer);
        Draw.reset();
        render(container);
        Draw.reset();

        return container.lifetime;
    }

    public void render(EffectContainer e){
        renderer.get(e);
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

    public static void floorDust(float x, float y, float size){
        Tile tile = world.tileWorld(x, y);
        if(tile != null){
            Color color = tile.floor().mapColor;
            Fx.unitLand.at(x, y, size, color);
        }
    }

    public static void floorDustAngle(Effect effect, float x, float y, float angle){
        Tile tile = world.tileWorld(x, y);
        if(tile != null){
            Color color = tile.floor().mapColor;
            effect.at(x, y, angle, color);
        }
    }

    public static void decal(TextureRegion region, float x, float y, float rotation){
        decal(region, x, y, rotation, 3600f, Pal.rubble);
    }

    public static void decal(TextureRegion region, float x, float y, float rotation, float lifetime, Color color){
        if(headless || region == null || !Core.atlas.isFound(region)) return;

        Tile tile = world.tileWorld(x, y);
        if(tile == null || !tile.floor().hasSurface()) return;

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

        public EffectContainer inner(){
            return innerContainer == null ? (innerContainer = new EffectContainer()) : innerContainer;
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
