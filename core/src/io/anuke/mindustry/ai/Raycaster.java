package io.anuke.mindustry.ai;

import com.badlogic.gdx.ai.utils.Collision;
import com.badlogic.gdx.ai.utils.Ray;
import com.badlogic.gdx.ai.utils.RaycastCollisionDetector;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class Raycaster implements RaycastCollisionDetector<Vector2>{
	private boolean found = false;

	@Override
	public boolean collides(Ray<Vector2> ray){
		found = false;
		
		Geometry.iterateLine(0f, ray.start.x, ray.start.y, ray.end.x, ray.end.y, tilesize, (x, y)->{
			if(solid(x, y)){
				found = true;
				return;
			}
		});
		
		return found;
	}

	@Override
	public boolean findCollision(Collision<Vector2> collision, Ray<Vector2> ray){
		Vector2 v = vectorCast(ray.start.x, ray.start.y, ray.end.x, ray.end.y);
		if(v == null) return false;
		collision.point = v;
		collision.normal = v.nor();
		return true;
	}
	
	Vector2 vectorCast(float x0f, float y0f, float x1f, float y1f){
		int x0 = (int)x0f;
		int y0 = (int)y0f;
		int x1 = (int)x1f;
		int y1 = (int)y1f;
		int dx = Math.abs(x1 - x0);
		int dy = Math.abs(y1 - y0);

		int sx = x0 < x1 ? 1 : -1;
		int sy = y0 < y1 ? 1 : -1;

		int err = dx - dy;
		int e2;
		while(true){

			if(solid(x0, y0)){
				return new Vector2(x0, y0);
			}
			if(x0 == x1 && y0 == y1) break;

			e2 = 2 * err;
			if(e2 > -dy){
				err = err - dy;
				x0 = x0 + sx;
			}

			if(e2 < dx){
				err = err + dx;
				y0 = y0 + sy;
			}
		}
		return null;
	}
	
	private boolean solid(float x, float y){
		Tile tile = world.tile(Mathf.scl2(x, tilesize), Mathf.scl2(y, tilesize));
		
		if(tile == null || tile.solid()) return true;
		
		for(int i = 0; i < 4; i ++){
			Tile near = tile.getNearby(i);
			if(near == null || near.solid()) return true;
		}
		
		return false;
	}

}
