package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Physics;
import io.anuke.ucore.util.Translator;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class DamageArea{
	private static Rectangle rect = new Rectangle();
	private static Translator tr = new Translator();

	/**Damages entities in a line.
	 * Only enemies of the specified team are damaged.*/
	public static void damageLine(Team team, Effect effect, float x, float y, float angle, float length, int damage){
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

        Consumer<Unit> cons = e -> {
            Rectangle other = e.hitbox.getRect(e.x, e.y);
            other.y -= expand;
            other.x -= expand;
            other.width += expand * 2;
            other.height += expand * 2;

            Vector2 vec = Physics.raycastRect(x, y, x2, y2, other);

            if (vec != null) {
                Effects.effect(effect, vec.x, vec.y);
                e.damage(damage);
            }
        };

		Units.getNearbyEnemies(team, rect, cons);
	}

	/**Damages everything in a radius.*/
	public static void damage(float x, float y, float radius, int damage){
		damage(null, x, y, radius, damage);
	}

	/**Damages all entities and blocks in a radius that are enemies of the team.*/
	public static void damage(Team team, float x, float y, float radius, int damage){
		Consumer<Unit> cons = entity -> {
			if(entity.distanceTo(x, y) > radius){
				return;
			}
			int amount = calculateDamage(x, y, entity.x, entity.y, radius, damage);
			entity.damage(amount);
		};

		rect.setSize(radius *2).setCenter(x, y);
		if(team != null) {
			Units.getNearbyEnemies(team, rect, cons);
		}else{
			Units.getNearby(rect, cons);
		}

		int trad = (int)(radius / tilesize);
		for(int dx = -trad; dx <= trad; dx ++){
			for(int dy= -trad; dy <= trad; dy ++){
				Tile tile = world.tile(Mathf.scl2(x, tilesize) + dx, Mathf.scl2(y, tilesize) + dy);
				if(tile != null && tile.entity != null && (team == null || Units.areEnemies(team, tile.getTeam())) && Vector2.dst(dx, dy, 0, 0) <= trad){
					int amount = calculateDamage(x, y, tile.worldx(), tile.worldy(), radius, damage);
					tile.entity.damage(amount);
				}
			}
		}

	}
	
	private static int calculateDamage(float x, float y, float tx, float ty, float radius, int damage){
		float dist = Vector2.dst(x, y, tx, ty);
		float scaled = 1f - dist/radius;
		return (int)(damage * scaled);
	}
}
