package io.anuke.mindustry.ai;

import com.badlogic.gdx.ai.pfa.PathFinderRequest;
import com.badlogic.gdx.ai.pfa.PathSmoother;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.world.SpawnPoint;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

public class Pathfind{
	private static final long ms = 1000000;
	
	MHueristic heuristic = new MHueristic();
	PassTileGraph graph = new PassTileGraph();
	PathSmoother<Tile, Vector2> smoother = new PathSmoother<Tile, Vector2>(new Raycaster());
	Vector2 vector = new Vector2();
	
	public Vector2 find(Enemy enemy){
		if(enemy.node == -1 || enemy.node == -2){
			findNode(enemy);
		}
		
		if(enemy.path == null){
			return vector.set(enemy.x, enemy.y);
		}else if(enemy.node == -2){
			enemy.node = -1;
		}
		
		Tile[] path = enemy.path;
		
		
		if(enemy.idletime > Enemy.maxIdle){
			//TODO reverse
			Tile target = path[enemy.node];
			if(Vars.world.raycastWorld(enemy.x, enemy.y, target.worldx(), target.worldy()) != null){
				if(enemy.node > 1)
					enemy.node = enemy.node - 1;
			}else{
				//must be blocked by a playermade block
			}
			
			enemy.idletime = 0;
		}
		
		//-1 is only possible here if both pathfindings failed, which should NOT happen
		//check graph code
		
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
	
	public void update(){
		for(SpawnPoint point : Vars.control.getSpawnPoints()){
			if(!point.request.pathFound){
				if(point.finder.search(point.request, ms * 2)){
					smoother.smoothPath(point.path);
					point.pathTiles = point.path.nodes.toArray(Tile.class);
					point.tempTiles = point.path.nodes.toArray(Tile.class);
				}
			}
		}
	}
	
	public void updatePath(){
		for(SpawnPoint point : Vars.control.getSpawnPoints()){
			point.finder = new IndexedAStarPathFinder<Tile>(graph);
			
			point.path.clear();
			
			point.pathTiles = null;
			point.tempTiles = null;
			
			point.request = new PathFinderRequest<Tile>(point.start, Vars.control.getCore(), heuristic, point.path);
			point.request.statusChanged = true; //IMPORTANT!
		}
	}
	
	void findNode(Enemy enemy){
		if(enemy.spawn >= Vars.control.getSpawnPoints().size){
			enemy.spawn = 0;
		}
		
		if(Vars.control.getSpawnPoints().get(enemy.spawn).pathTiles == null){
			return;
		}
		
		enemy.path = Vars.control.getSpawnPoints().get(enemy.spawn).pathTiles;
		
		int closest = findClosest(enemy.path, enemy.x, enemy.y);
		
		closest = Mathf.clamp(closest, 1, enemy.path.length-1);
		Tile end = enemy.path[closest];
		enemy.node = closest;
		
		//if the enemy can't get to this node, teleport to it
		if(enemy.node < enemy.path.length - 2 && Vars.world.raycastWorld(enemy.x, enemy.y, end.worldx(), end.worldy()) != null){
		//	Timers.run(Mathf.random(20f), () -> enemy.set(end.worldx(), end.worldy()));
		}
	}
	
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
	
	private static int indexOf(Tile tile, Tile[] tiles){
		int i = -1;
		for(int j = 0; j < tiles.length; j ++){
			if(tiles[j] == tile){
				return j;
			}
		}
		return i;
	}
	
	private static boolean onLine(Vector2 vector, float x1, float y1, float x2, float y2){
		return MathUtils.isEqual(vector.dst(x1, y1) + vector.dst(x2, y2), Vector2.dst(x1, y1, x2, y2), 0.01f);
	}
	
	private static float pointLineDist(float x, float y, float x2, float y2, float px, float py){
		float l2 = Vector2.dst2(x, y, x2, y2);
		float t = Math.max(0, Math.min(1, Vector2.dot(px - x, py - y, x2 - x, y2 - y) / l2));
		Vector2 projection = Tmp.v1.set(x, y).add(Tmp.v2.set(x2, y2).sub(x, y).scl(t)); // Projection falls on the segment
		return projection.dst(px, py);
	}
	
	private static Vector2 projectPoint(float x1, float y1, float x2, float y2, float pointx, float pointy){
	    float px = x2-x1, py = y2-y1, dAB = px*px + py*py;
	    float u = ((pointx - x1) * px + (pointy - y1) * py) / dAB;
	    float x = x1 + u * px, y = y1 + u * py;
	    return Tmp.v3.set(x, y); //this is D
	}
}
