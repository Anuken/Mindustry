package mindustry.entities.comp;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

@EntityDef(value = {Firec.class}, pooled = true)
@Component(base = true)
abstract class FireComp implements Timedc, Posc, Firec, Syncc{
    private static final float spreadChance = 0.04f, fireballChance = 0.06f;

    @Import float time, lifetime, x, y;

    Tile tile;
    private transient Block block;
    private transient float baseFlammability = -1, puddleFlammability;

    @Override
    public void update(){
        if(Mathf.chance(0.09 * Time.delta)){
            Fx.fire.at(x + Mathf.range(4f), y + Mathf.range(4f));
        }

        if(Mathf.chance(0.05 * Time.delta)){
            Fx.fireSmoke.at(x + Mathf.range(4f), y + Mathf.range(4f));
        }

        if(!headless){
            control.sound.loop(Sounds.fire, this, 0.07f);
        }

        //faster updates -> disappears more quickly
        float speedMultiplier = 1f + Math.max(state.envAttrs.get(Attribute.water) * 10f, 0);
        time = Mathf.clamp(time + Time.delta * speedMultiplier, 0, lifetime);

        if(Vars.net.client()){
            return;
        }

        if(time >= lifetime || tile == null){
            remove();
            return;
        }

        Building entity = tile.build;
        boolean damage = entity != null;

        float flammability = baseFlammability + puddleFlammability;

        if(!damage && flammability <= 0){
            time += Time.delta * 8;
        }

        if(baseFlammability < 0 || block != tile.block()){
            baseFlammability = tile.build == null ? 0 : tile.getFlammability();
            block = tile.block();
        }

        if(damage){
            lifetime += Mathf.clamp(flammability / 8f, 0f, 0.6f) * Time.delta;
        }

        if(flammability > 1f && Mathf.chance(spreadChance * Time.delta * Mathf.clamp(flammability / 5f, 0.3f, 2f))){
            Point2 p = Geometry.d4[Mathf.random(3)];
            Tile other = world.tile(tile.x + p.x, tile.y + p.y);
            Fires.create(other);

            if(Mathf.chance(fireballChance * Time.delta * Mathf.clamp(flammability / 10f))){
                Bullets.fireball.createNet(Team.derelict, x, y, Mathf.random(360f), -1f, 1, 1);
            }
        }

        if(Mathf.chance(0.025 * Time.delta)){
            Puddlec p = Puddles.get(tile);
            puddleFlammability = p != null ? p.getFlammability() / 3f : 0;

            if(damage){
                entity.damage(1.6f);
            }
            Damage.damageUnits(null, tile.worldx(), tile.worldy(), tilesize, 3f,
            unit -> !unit.isFlying() && !unit.isImmune(StatusEffects.burning),
            unit -> unit.apply(StatusEffects.burning, 60 * 5));
        }
    }

    @Override
    public void remove(){
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
