package mindustry.entities.comp;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

@EntityDef(value = {Firec.class}, pooled = true)
@Component(base = true)
abstract class FireComp implements Timedc, Posc, Syncc, Drawc{
    public static final int frames = 40, duration = 90;

    private static final float spreadDelay = 22f, fireballDelay = 40f,
        ticksPerFrame = (float)duration / frames, warmupDuration = 20f, damageDelay = 40f, tileDamage = 1.8f, unitDamage = 3f;

    public static final TextureRegion[] regions = new TextureRegion[frames];

    @Import float time, lifetime, x, y;

    Tile tile;
    private transient float
        puddleFlammability, damageTimer = Mathf.random(40f),
        spreadTimer = Mathf.random(spreadDelay), fireballTimer = Mathf.random(fireballDelay),
        warmup = 0f,
        animation = Mathf.random(frames - 1);

    @Override
    public void update(){

        animation += Time.delta / ticksPerFrame;
        warmup += Time.delta;
        animation %= frames;

        if(!headless){
            control.sound.loop(Sounds.fire, this, 0.07f);
        }

        //faster updates -> disappears more quickly
        float speedMultiplier = 1f + Math.max(state.envAttrs.get(Attribute.water) * 10f, 0);
        time = Mathf.clamp(time + Time.delta * speedMultiplier, 0, lifetime);

        if(Vars.net.client()){
            return;
        }

        if(time >= lifetime || tile == null || Float.isNaN(lifetime)){
            remove();
            return;
        }

        Building entity = tile.build;
        boolean damage = entity != null;

        float flammability = tile.getFlammability() + puddleFlammability;

        if(!damage && flammability <= 0){
            time += Time.delta * 8;
        }

        if(damage){
            lifetime += Mathf.clamp(flammability / 8f, 0f, 0.6f) * Time.delta;
        }

        if(flammability > 1f && (spreadTimer += Time.delta * Mathf.clamp(flammability / 5f, 0.3f, 2f)) >= spreadDelay){
            spreadTimer = 0f;
            Point2 p = Geometry.d4[Mathf.random(3)];
            Tile other = world.tile(tile.x + p.x, tile.y + p.y);
            Fires.create(other);
        }

        if(flammability > 0 && (fireballTimer += Time.delta * Mathf.clamp(flammability / 10f, 0f, 0.5f)) >= fireballDelay){
            fireballTimer = 0f;
            Bullets.fireball.createNet(Team.derelict, x, y, Mathf.random(360f), -1f, 1, 1);
        }

        //apply damage to nearby units & building
        if((damageTimer += Time.delta) >= damageDelay){
            damageTimer = 0f;
            Puddlec p = Puddles.get(tile);
            puddleFlammability = p != null ? p.getFlammability() / 3f : 0;

            if(damage){
                entity.damage(tileDamage);
            }
            Damage.damageUnits(null, tile.worldx(), tile.worldy(), tilesize, unitDamage,
            unit -> !unit.isFlying() && !unit.isImmune(StatusEffects.burning),
            unit -> unit.apply(StatusEffects.burning, 60 * 5));
        }
    }

    @Override
    public void draw(){
        if(regions[0] == null){
            for(int i = 0; i < frames; i++){
                regions[i] = Core.atlas.find("fire" + i);
            }
        }

        Draw.color(1f, 1f, 1f, Mathf.clamp(warmup / warmupDuration));
        Draw.z(Layer.effect);
        Draw.rect(regions[Math.min((int)animation, regions.length - 1)], x + Mathf.randomSeedRange((int)y, 2), y + Mathf.randomSeedRange((int)x, 2));
        Draw.reset();

        Drawf.light(x, y, 50f + Mathf.absin(5f, 5f), Pal.lightFlame, 0.6f  * Mathf.clamp(warmup / warmupDuration));
    }

    @Replace
    @Override
    public float clipSize(){
        return 25;
    }

    @Override
    public void remove(){
        Fx.fireRemove.at(x, y, animation);
        Fires.remove(tile);
    }

    @Override
    public void afterRead(){
        Fires.register(self());
    }

    @Override
    public void afterSync(){
        Fires.register(self());
    }
}
