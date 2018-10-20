package io.anuke.mindustry.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.content.bullets.TurretBullets;
import io.anuke.mindustry.content.fx.ExplosionFx;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.effect.Fire;
import io.anuke.mindustry.entities.effect.Lightning;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.function.Predicate;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Physics;
import io.anuke.ucore.util.Translator;

import static io.anuke.mindustry.Vars.*;

/**Utility class for damaging in an area.*/
public class Damage{
    private static Rectangle rect = new Rectangle();
    private static Rectangle hitrect = new Rectangle();
    private static Translator tr = new Translator();

    /**Creates a dynamic explosion based on specified parameters.*/
    public static void dynamicExplosion(float x, float y, float flammability, float explosiveness, float power, float radius, Color color){
        for(int i = 0; i < Mathf.clamp(power / 20, 0, 6); i++){
            int branches = 5 + Mathf.clamp((int) (power / 30), 1, 20);
            Timers.run(i * 2f + Mathf.random(4f), () -> Lightning.create(Team.none, Palette.power, 3,
                    x, y, Mathf.random(360f), branches + Mathf.range(2)));
        }

        for(int i = 0; i < Mathf.clamp(flammability / 4, 0, 30); i++){
            Timers.run(i / 2f, () -> Call.createBullet(TurretBullets.fireball, x, y, Mathf.random(360f)));
        }

        int waves = Mathf.clamp((int) (explosiveness / 4), 0, 30);

        for(int i = 0; i < waves; i++){
            int f = i;
            Timers.run(i * 2f, () -> {
                threads.run(() -> Damage.damage(x, y, Mathf.clamp(radius + explosiveness, 0, 50f) * ((f + 1f) / waves), explosiveness / 2f));
                Effects.effect(ExplosionFx.blockExplosionSmoke, x + Mathf.range(radius), y + Mathf.range(radius));
            });
        }

        if(explosiveness > 15f){
            Effects.effect(ExplosionFx.shockwave, x, y);
        }

        if(explosiveness > 30f){
            Effects.effect(ExplosionFx.bigShockwave, x, y);
        }

        float shake = Math.min(explosiveness / 4f + 3f, 9f);
        Effects.shake(shake, shake, x, y);
        Effects.effect(ExplosionFx.blockExplosion, x, y);
    }

    public static void createIncend(float x, float y, float range, int amount){
        for(int i = 0; i < amount; i++){
            float cx = x + Mathf.range(range);
            float cy = y + Mathf.range(range);
            Tile tile = world.tileWorld(cx, cy);
            if(tile != null){
                Fire.create(tile);
            }
        }
    }

    /**
     * Damages entities in a line.
     * Only enemies of the specified team are damaged.
     */
    public static void collideLine(Bullet hitter, Team team, Effect effect, float x, float y, float angle, float length){
        tr.trns(angle, length);
        world.raycastEachWorld(x, y, x + tr.x, y + tr.y, (cx, cy) -> {
            Tile tile = world.tile(cx, cy);
            if(tile != null && tile.entity != null && tile.target().getTeamID() != team.ordinal() && tile.entity.collide(hitter)){
                tile.entity.collision(hitter);
                hitter.getBulletType().hit(hitter, tile.worldx(), tile.worldy());
            }
            return false;
        });

        rect.setPosition(x, y).setSize(tr.x, tr.y);
        float x2 = tr.x + x, y2 = tr.y + y;

        if(rect.width < 0){
            rect.x += rect.width;
            rect.width *= -1;
        }

        if(rect.height < 0){
            rect.y += rect.height;
            rect.height *= -1;
        }

        float expand = 3f;

        rect.y -= expand;
        rect.x -= expand;
        rect.width += expand * 2;
        rect.height += expand * 2;

        Consumer<Unit> cons = e -> {
            e.getHitbox(hitrect);
            Rectangle other = hitrect;
            other.y -= expand;
            other.x -= expand;
            other.width += expand * 2;
            other.height += expand * 2;

            Vector2 vec = Physics.raycastRect(x, y, x2, y2, other);

            if(vec != null){
                Effects.effect(effect, vec.x, vec.y);
                e.collision(hitter, vec.x, vec.y);
                hitter.collision(e, vec.x, vec.y);
            }
        };

        Units.getNearbyEnemies(team, rect, cons);
    }

    /**Damages all entities and blocks in a radius that are enemies of the team.*/
    public static void damageUnits(Team team, float x, float y, float size, float damage, Predicate<Unit> predicate, Consumer<Unit> acceptor){
        Consumer<Unit> cons = entity -> {
            if(!predicate.test(entity)) return;

            entity.getHitbox(hitrect);
            if(!hitrect.overlaps(rect)){
                return;
            }
            entity.damage(damage);
            acceptor.accept(entity);
        };

        rect.setSize(size * 2).setCenter(x, y);
        if(team != null){
            Units.getNearbyEnemies(team, rect, cons);
        }else{
            Units.getNearby(rect, cons);
        }
    }

    /**Damages everything in a radius.*/
    public static void damage(float x, float y, float radius, float damage){
        damage(null, x, y, radius, damage);
    }

    /**Damages all entities and blocks in a radius that are enemies of the team.*/
    public static void damage(Team team, float x, float y, float radius, float damage){
        Consumer<Unit> cons = entity -> {
            if(entity.team == team || entity.distanceTo(x, y) > radius){
                return;
            }
            float amount = calculateDamage(x, y, entity.x, entity.y, radius, damage);
            entity.damage(amount);
            //TODO better velocity displacement
            float dst = tr.set(entity.x - x, entity.y - y).len();
            entity.getVelocity().add(tr.setLength((1f - dst / radius) * 2f));
        };

        rect.setSize(radius * 2).setCenter(x, y);
        if(team != null){
            Units.getNearbyEnemies(team, rect, cons);
        }else{
            Units.getNearby(rect, cons);
        }

        int trad = (int) (radius / tilesize);
        for(int dx = -trad; dx <= trad; dx++){
            for(int dy = -trad; dy <= trad; dy++){
                Tile tile = world.tile(Mathf.scl2(x, tilesize) + dx, Mathf.scl2(y, tilesize) + dy);
                if(tile != null && tile.entity != null && (team == null || state.teams.areEnemies(team, tile.getTeam())) && Vector2.dst(dx, dy, 0, 0) <= trad){
                    float amount = calculateDamage(x, y, tile.worldx(), tile.worldy(), radius, damage);
                    tile.entity.damage(amount);
                }
            }
        }

    }

    private static float calculateDamage(float x, float y, float tx, float ty, float radius, float damage){
        float dist = Vector2.dst(x, y, tx, ty);
        float falloff = 0.4f;
        float scaled = Mathf.lerp(1f - dist / radius, 1f, falloff);
        return damage * scaled;
    }
}
