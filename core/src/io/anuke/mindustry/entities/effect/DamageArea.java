package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.util.Mathf;

//TODO
public class DamageArea{
	
	public static void damage(boolean enemies, float x, float y, float radius, int damage){
		
		if(enemies){
			Entities.getNearby(x, y, radius*2, entity->{
				if(entity instanceof Enemy){
					Enemy enemy = (Enemy)entity;
					if(enemy.distanceTo(x, y) > radius){
						return;
					}
					int amount = calculateDamage(x, y, enemy.x, enemy.y, radius, damage);
					enemy.damage(amount);
				}
			});
		}else{
			int trad = (int)(radius / Vars.tilesize);
			for(int dx = -trad; dx <= trad; dx ++){
				for(int dy= -trad; dy <= trad; dy ++){
					Tile tile = Vars.world.tile(Mathf.scl2(x, Vars.tilesize) + dx, Mathf.scl2(y, Vars.tilesize) + dy);
					if(tile != null && tile.entity != null && Vector2.dst(dx, dy, 0, 0) <= trad){
						int amount = calculateDamage(x, y, tile.worldx(), tile.worldy(), radius, damage);
						tile.entity.damage(amount);
					}
				}
			}
		}
	}
	
	static int calculateDamage(float x, float y, float tx, float ty, float radius, int damage){
		float dist = Vector2.dst(x, y, tx, ty);
		float scaled = 1f - dist/radius;
		return (int)(damage * scaled);
	}
}
