package io.anuke.mindustry.ai;

import com.badlogic.gdx.ai.pfa.PathSmoother;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.game.SpawnPoint;
import io.anuke.mindustry.world.Tile;

public class Pathfind{
	/**Maximum time taken per frame on pathfinding for a single path.*/
	private static final long maxTime = 1000000 * 5;

	/**Tile graph, for determining conenctions between two tiles*/
	TileGraph graph = new TileGraph();
	/**Smoother that removes extra nodes from a path.*/
	PathSmoother<Tile, Vector2> smoother = new PathSmoother<Tile, Vector2>(new Raycaster());
	/**temporary vector2 for calculations*/
	Vector2 vector = new Vector2();

	Vector2 v1 = new Vector2();
	Vector2 v2 = new Vector2();
	Vector2 v3 = new Vector2();

	/**Finds the position on the path an enemy should move to.
	 * If the path is not yet calculated, this returns the enemy's position (i. e. "don't move")
	 * @param enemy The enemy to find a path for
	 * @return The position the enemy should move to.*/
	public Vector2 find(BaseUnit enemy){
		//TODO!
		return v1.set(enemy.x, enemy.y);
	}

	/**Update the pathfinders and continue calculating the path if it hasn't been calculated yet.
	 *  This method is run each frame.*/
	public void update(){
		/*

		//go through each spawnpoint, and if it's not found a path yet, update it
		for(int i = 0; i < world.getSpawns().size; i ++){
			SpawnPoint point = world.getSpawns().get(i);
			if(point.request == null || point.finder == null){
				continue;
			}

			if(!point.request.pathFound){
				try{
					if(point.finder.search(point.request, maxTime)){
						smoother.smoothPath(point.path);
						point.pathTiles = point.path.nodes.toArray(Tile.class);
						point.finder = null;
					}
				}catch (ArrayIndexOutOfBoundsException e){
					//no path
					point.request.pathFound = true;
				}
			}
		}*/

	}

	private void resetPathFor(SpawnPoint point){
		/*
		point.finder = new OptimizedPathFinder<>(graph);

		point.path.clear();

		point.pathTiles = null;
		//TODO
		point.request = new PathFinderRequest<>(point.start, world.getCore(), state.difficulty.heuristic, point.path);
		point.request.statusChanged = true; //IMPORTANT!*/
	}

	/**Finds the closest tile to a position, in an array of tiles.*/
	private int findClosest(Tile[] tiles, float x, float y){
		int cindex = -2;
		float dst = Float.MAX_VALUE;

		for(int i = 0; i < tiles.length - 1; i ++){
			Tile tile = tiles[i];
			Tile next = tiles[i + 1];
			float d = pointLineDist(tile.worldx(), tile.worldy(), next.worldx(), next.worldy(), x, y);
			if(d < dst){
				dst = d;
				cindex = i;
			}
		}
		
		return cindex + 1;
	}

	/**Returns whether a point is on a line.*/
	private boolean onLine(Vector2 vector, float x1, float y1, float x2, float y2){
		return MathUtils.isEqual(vector.dst(x1, y1) + vector.dst(x2, y2), Vector2.dst(x1, y1, x2, y2), 0.01f);
	}

	/**Returns distance from a point to a line segment.*/
	private float pointLineDist(float x, float y, float x2, float y2, float px, float py){
		float l2 = Vector2.dst2(x, y, x2, y2);
		float t = Math.max(0, Math.min(1, Vector2.dot(px - x, py - y, x2 - x, y2 - y) / l2));
		Vector2 projection = v1.set(x, y).add(v2.set(x2, y2).sub(x, y).scl(t)); // Projection falls on the segment
		return projection.dst(px, py);
	}

	//TODO documentation
	private Vector2 projectPoint(float x1, float y1, float x2, float y2, float pointx, float pointy){
	    float px = x2-x1, py = y2-y1, dAB = px*px + py*py;
	    float u = ((pointx - x1) * px + (pointy - y1) * py) / dAB;
	    float x = x1 + u * px, y = y1 + u * py;
	    return v3.set(x, y); //this is D
	}
}
