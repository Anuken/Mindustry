package mindustry.entities.def;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.*;

@Component
abstract class FireComp implements Timedc, Posc, Firec{
    private static final IntMap<Firec> map = new IntMap<>();
    private static final float baseLifetime = 1000f, spreadChance = 0.05f, fireballChance = 0.07f;

    transient float time, lifetime, x, y;

    Tile tile;
    private Block block;
    private float baseFlammability = -1, puddleFlammability;

    //TODO move these somewhere else.
    /** Start a fire on the tile. If there already is a file there, refreshes its lifetime. */
    public static void create(Tile tile){
        if(net.client() || tile == null) return; //not clientside.

        Firec fire = map.get(tile.pos());

        if(fire == null){
            fire = FireEntity.create();
            fire.tile(tile);
            fire.lifetime(baseLifetime);
            fire.set(tile.worldx(), tile.worldy());
            fire.add();
            map.put(tile.pos(), fire);
        }else{
            fire.lifetime(baseLifetime);
            fire.time(0f);
        }
    }

    public static boolean has(int x, int y){
        if(!Structs.inBounds(x, y, world.width(), world.height()) || !map.containsKey(Pos.get(x, y))){
            return false;
        }
        Firec fire = map.get(Pos.get(x, y));
        return fire.isAdded() && fire.fin() < 1f && fire.tile() != null && fire.tile().x == x && fire.tile().y == y;
    }

    /**
     * Attempts to extinguish a fire by shortening its life. If there is no fire here, does nothing.
     */
    public static void extinguish(Tile tile, float intensity){
        if(tile != null && map.containsKey(tile.pos())){
            Firec fire = map.get(tile.pos());
            fire.time(fire.time() + intensity * Time.delta());
            if(fire.time() >= fire.lifetime()){
                Events.fire(Trigger.fireExtinguish);
            }
        }
    }

    @Override
    public void update(){
        if(Mathf.chance(0.1 * Time.delta())){
            Fx.fire.at(x + Mathf.range(4f), y + Mathf.range(4f));
        }

        if(Mathf.chance(0.05 * Time.delta())){
            Fx.fireSmoke.at(x + Mathf.range(4f), y + Mathf.range(4f));
        }

        if(Mathf.chance(0.001 * Time.delta())){
            Sounds.fire.at(this);
        }

        time = Mathf.clamp(time + Time.delta(), 0, lifetime());
        map.put(tile.pos(), this);

        if(Vars.net.client()){
            return;
        }

        if(time >= lifetime() || tile == null){
            remove();
            return;
        }

        Tilec entity = tile.link().entity;
        boolean damage = entity != null;

        float flammability = baseFlammability + puddleFlammability;

        if(!damage && flammability <= 0){
            time += Time.delta() * 8;
        }

        if(baseFlammability < 0 || block != tile.block()){
            baseFlammability = tile.block().getFlammability(tile);
            block = tile.block();
        }

        if(damage){
            lifetime += Mathf.clamp(flammability / 8f, 0f, 0.6f) * Time.delta();
        }

        if(flammability > 1f && Mathf.chance(spreadChance * Time.delta() * Mathf.clamp(flammability / 5f, 0.3f, 2f))){
            Point2 p = Geometry.d4[Mathf.random(3)];
            Tile other = world.tile(tile.x + p.x, tile.y + p.y);
            create(other);

            if(Mathf.chance(fireballChance * Time.delta() * Mathf.clamp(flammability / 10f))){
                Bullets.fireball.createNet(Team.derelict, x, y, Mathf.random(360f), -1f, 1, 1);
            }
        }

        if(Mathf.chance(0.1 * Time.delta())){
            //TODO implement
            //Puddle p = Puddle.getPuddle(tile);
            //if(p != null){
             //   puddleFlammability = p.getFlammability() / 3f;
            //}else{
                puddleFlammability = 0;
            //}

            if(damage){
                entity.damage(0.4f);
            }
            Damage.damageUnits(null, tile.worldx(), tile.worldy(), tilesize, 3f,
            unit -> !unit.isFlying() && !unit.isImmune(StatusEffects.burning),
            unit -> unit.apply(StatusEffects.burning, 60 * 5));
        }
    }
}
