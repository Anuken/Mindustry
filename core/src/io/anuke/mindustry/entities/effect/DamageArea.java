package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.entities.DestructibleEntity;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.SolidEntity;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Physics;
import io.anuke.ucore.util.Translator;

import static io.anuke.mindustry.Vars.*;

public class DamageArea{
	private static Rectangle rect = new Rectangle();
	private static Translator tr = new Translator();

	//only for entities, not tiles (yet!)
	public static void damageLine(Entity owner, Effect effect, float x, float y, float angle, float length, int damage){
		tr.trns(angle, length);
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
		rect.width += expand*2;
		rect.height += expand*2;

        Consumer<SolidEntity> cons = e -> {
            if(e == owner || (e instanceof  Player && ((Player)e).isAndroid)) return;
            DestructibleEntity enemy = (DestructibleEntity) e;
            Rectangle other = enemy.hitbox.getRect(enemy.x, enemy.y);
            other.y -= expand;
            other.x -= expand;
            other.width += expand * 2;
            other.height += expand * 2;

            Vector2 vec = Physics.raycastRect(x, y, x2, y2, other);

            if (vec != null) {
                Effects.effect(effect, vec.x, vec.y);
                enemy.damage(damage);
            }
        };

		Entities.getNearby(enemyGroup, rect, cons);
        if(state.friendlyFire) Entities.getNearby(playerGroup, rect, cons);
	}
	
	public static void damageEntities(float x, float y, float radius, int damage){
		damage(true, x, y, radius, damage);

		for(Player player : playerGroup.all()){
			if(player.isAndroid) continue;
			int amount = calculateDamage(x, y, player.x, player.y, radius, damage);
			player.damage(amount);
		}
	}
	
	public static void damage(boolean enemies, float x, float y, float radius, int damage){
		Consumer<SolidEntity> cons = entity -> {
			DestructibleEntity enemy = (DestructibleEntity)entity;
			if(enemy.distanceTo(x, y) > radius || (entity instanceof Player && ((Player)entity).isAndroid)){
				return;
			}
			int amount = calculateDamage(x, y, enemy.x, enemy.y, radius, damage);
			enemy.damage(amount);
		};
		
		if(enemies){
			Entities.getNearby(enemyGroup, x, y, radius*2, cons);
		}else{
			int trad = (int)(radius / tilesize);
			for(int dx = -trad; dx <= trad; dx ++){
				for(int dy= -trad; dy <= trad; dy ++){
					Tile tile = world.tile(Mathf.scl2(x, tilesize) + dx, Mathf.scl2(y, tilesize) + dy);
					if(tile != null && tile.entity != null && Vector2.dst(dx, dy, 0, 0) <= trad){
						int amount = calculateDamage(x, y, tile.worldx(), tile.worldy(), radius, damage);
						tile.entity.damage(amount);
					}
				}
			}

			Entities.getNearby(playerGroup, x, y, radius*2, cons);
		}
	}
	
	static int calculateDamage(float x, float y, float tx, float ty, float radius, int damage){
		float dist = Vector2.dst(x, y, tx, ty);
		float scaled = 1f - dist/radius;
		return (int)(damage * scaled);
	}
}
