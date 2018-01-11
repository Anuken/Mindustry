package io.anuke.mindustry.ai;

import com.badlogic.gdx.ai.pfa.PathFinderRequest;
import com.badlogic.gdx.ai.pfa.PathSmoother;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.entities.enemies.EnemyType;
import io.anuke.mindustry.game.SpawnPoint;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

public class Pathfind{
	/**Maximum time taken per frame on pathfinding for a single path.*/
	private static final long maxTime = 1000000 * 5;

	/**Heuristic for determining cost between two tiles*/
	HeuristicImpl heuristic = new HeuristicImpl();
	/**Tile graph, for determining conenctions between two tiles*/
	TileGraph graph = new TileGraph();
	/**Smoother that removes extra nodes from a path.*/
	PathSmoother<Tile, Vector2> smoother = new PathSmoother<Tile, Vector2>(new Raycaster());
	/**temporary vector2 for calculations*/
	Vector2 vector = new Vector2();

	/**Finds the position on the path an enemy should move to.
	 * If the path is not yet calculated, this returns the enemy's position (i. e. "don't move")
	 * @param enemy The enemy to find a path for
	 * @return The position the enemy should move to.*/
	public Vector2 find(Enemy enemy){
		//TODO fix -1/-2 node usage
		if(enemy.node == -1 || enemy.node == -2){
			findNode(enemy);
		}
		
		if(enemy.node == -2){
			enemy.node = -1;
		}

		if(enemy.node < 0 || Vars.control.getSpawnPoints().get(enemy.lane).pathTiles == null){
			return vector.set(enemy.x, enemy.y);
		}

		Tile[] path = Vars.control.getSpawnPoints().get(enemy.lane).pathTiles;

		//if an enemy is idle for a while, it's probably stuck
		if(enemy.idletime > EnemyType.maxIdle){

			Tile target = path[enemy.node];
			if(Vars.world.raycastWorld(enemy.x, enemy.y, target.worldx(), target.worldy()) != null) {
				if (enemy.node > 1)
					enemy.node = enemy.node - 1;
				enemy.idletime = 0;
			}

			//else, must be blocked by a playermade block, do nothing
		}
		
		//-1 is only possible here if both pathfindings failed, which should NOT happen
		//check graph code
		
		if(enemy.node <= -1){
			return vector.set(enemy.x, enemy.y);
		}

		if(enemy.node >= path.length){
			enemy.node = -1;
			return vector.set(enemy.x, enemy.y);
		}

		//TODO documentation on what this does
		Tile prev = path[enemy.node - 1];

		Tile target = path[enemy.node];
		
		float projectLen = Vector2.dst(prev.worldx(), prev.worldy(), target.worldx(), target.worldy()) / 6f;
		
		Vector2 projection = projectPoint(prev.worldx(), prev.worldy(), 
				target.worldx(), target.worldy(), enemy.x, enemy.y);
		
		boolean canProject = true;
		
		if(projectLen < 8 || !onLine(projection, prev.worldx(), prev.worldy(), target.worldx(), target.worldy())){
			canProject = false;
		}else{
			projection.add(Angles.translation(Angles.angle(prev.worldx(), prev.worldy(), 
					target.worldx(), target.worldy()), projectLen));
		}
			
		float dst = Vector2.dst(enemy.x, enemy.y, target.worldx(), target.worldy());
		float nlinedist = enemy.node >= path.length - 1 ? 9999 :
			pointLineDist(path[enemy.node].worldx(), path[enemy.node].worldy(), 
					path[enemy.node + 1].worldx(), path[enemy.node + 1].worldy(), enemy.x, enemy.y);
			
		if(dst < 8 || nlinedist < 8){
			if(enemy.node <= path.length-2)
				enemy.node ++;
				
			target = path[enemy.node];
		}
		
		if(canProject && projection.dst(enemy.x, enemy.y) < Vector2.dst(target.x, target.y, enemy.x, enemy.y)){
			vector.set(projection);
		}else{
			vector.set(target.worldx(), target.worldy());
		}
		
		//near the core, stop
		if(enemy.node == path.length - 1){
			vector.set(target.worldx(), target.worldy());
		}
			
		return vector;
		
	}

	/**Update the pathfinders and continue calculating the path if it hasn't been calculated yet.
	 *  This method is run each frame.*/
	public void update(){

		//go through each spawnpoint, and if it's not found a path yet, update it
		for(SpawnPoint point : Vars.control.getSpawnPoints()){
			if(!point.request.pathFound){
				try{
					if(point.finder.search(point.request, maxTime)){
						smoother.smoothPath(point.path);
						point.pathTiles = point.path.nodes.toArray(Tile.class);
					}
				}catch (ArrayIndexOutOfBoundsException e){
					//no path
					point.request.pathFound = true;
				}
			}
		}

	}

	//1300-1500ms, usually 1400 unoptimized on Caldera
	/**Benchmark pathfinding speed. Debugging stuff.*/
	public void benchmark(){
		SpawnPoint point = Vars.control.getSpawnPoints().first();
		int amount = 100;

		//warmup
		for(int i = 0; i < 100; i ++){
			point.finder.searchNodePath(point.start, Vars.control.getCore(), heuristic, point.path);
			point.path.clear();
		}

		Timers.mark();
		for(int i = 0; i < amount; i ++){
			point.finder.searchNodePath(point.start, Vars.control.getCore(), heuristic, point.path);
			point.path.clear();
		}
		UCore.log("Time elapsed: " + Timers.elapsed() + "ms\nAverage MS per path: " + Timers.elapsed()/amount);
	}

	/**Reset and clear the paths.*/
	public void resetPaths(){
		for(SpawnPoint point : Vars.control.getSpawnPoints()){
			point.finder = new OptimizedPathFinder<>(graph);
			
			point.path.clear();
			
			point.pathTiles = null;
			
			point.request = new PathFinderRequest<>(point.start, Vars.control.getCore(), heuristic, point.path);
			point.request.statusChanged = true; //IMPORTANT!
		}
	}

	/**For an enemy that was just loaded from a save, find the node in the path it should be following.*/
	void findNode(Enemy enemy){
		if(enemy.lane >= Vars.control.getSpawnPoints().size){
			enemy.lane = 0;
		}
		
		if(Vars.control.getSpawnPoints().get(enemy.lane).pathTiles == null){
			return;
		}
		
		Tile[] path = Vars.control.getSpawnPoints().get(enemy.lane).pathTiles;
		
		int closest = findClosest(path, enemy.x, enemy.y);
		
		closest = Mathf.clamp(closest, 1, path.length-1);
		if(closest == -1){
			return;
		}

		enemy.node = closest;
	}

	/**Finds the closest tile to a position, in an array of tiles.*/
	private static int findClosest(Tile[] tiles, float x, float y){
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
	private static boolean onLine(Vector2 vector, float x1, float y1, float x2, float y2){
		return MathUtils.isEqual(vector.dst(x1, y1) + vector.dst(x2, y2), Vector2.dst(x1, y1, x2, y2), 0.01f);
	}

	/**Returns distance from a point to a line segment.*/
	private static float pointLineDist(float x, float y, float x2, float y2, float px, float py){
		float l2 = Vector2.dst2(x, y, x2, y2);
		float t = Math.max(0, Math.min(1, Vector2.dot(px - x, py - y, x2 - x, y2 - y) / l2));
		Vector2 projection = Tmp.v1.set(x, y).add(Tmp.v2.set(x2, y2).sub(x, y).scl(t)); // Projection falls on the segment
		return projection.dst(px, py);
	}

	//TODO documentation
	private static Vector2 projectPoint(float x1, float y1, float x2, float y2, float pointx, float pointy){
	    float px = x2-x1, py = y2-y1, dAB = px*px + py*py;
	    float u = ((pointx - x1) * px + (pointy - y1) * py) / dAB;
	    float x = x1 + u * px, y = y1 + u * py;
	    return Tmp.v3.set(x, y); //this is D
	}
}
