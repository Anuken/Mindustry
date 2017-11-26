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
import io.anuke.ucore.util.Tmp;

public class Pathfind{
	private static final long ms = 1000000;
	
	MHueristic heuristic = new MHueristic();
	PassTileGraph graph = new PassTileGraph();
	PathSmoother<Tile, Vector2> smoother = new PathSmoother<Tile, Vector2>(new Raycaster());
	Vector2 vector = new Vector2();
	
	public Vector2 find(Enemy enemy){
		if(enemy.node == -1){
			findNode(enemy);
		}
		
		if(enemy.path == null){
			return vector.set(enemy.x, enemy.y);
		}
		
		//-1 is only possible here if both pathfindings failed, which should NOT happen
		//check graph code
		
		Tile[] path = enemy.path;
		
		//REPRODUCE BUG: load in test map, then load save 1?
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
			
		if(dst < 8){
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
				if(point.finder.search(point.request, ms*2)){
					smoother.smoothPath(point.path);
					point.pathTiles = point.path.nodes.toArray(Tile.class);
				}
			}
		}
	}
	
	public void updatePath(){
		for(SpawnPoint point : Vars.control.getSpawnPoints()){
			if(point.finder == null){
				point.finder = new IndexedAStarPathFinder<Tile>(graph);
			}
			
			point.path.clear();
			
			point.pathTiles = null;
			
			point.request = new PathFinderRequest<Tile>(point.start, Vars.control.getCore(), heuristic, point.path);
			point.request.statusChanged = true; //IMPORTANT!
		}
		
		/*
		if(paths.size == 0 || paths.size != World.spawnpoints.size){
			paths.clear();
			finders.clear();
			pathSequences = new Tile[World.spawnpoints.size][0];
			for(int i = 0; i < World.spawnpoints.size; i ++){
				SmoothGraphPath path = new SmoothGraphPath();
				paths.add(path);
				finders.add(new IndexedAStarPathFinder(graph));
			}
		}
		
		for(int i = 0; i < paths.size; i ++){
			SmoothGraphPath path = paths.get(i);
			
			path.clear();
			finders.get(i).searchNodePath(
					World.spawnpoints.get(i), 
					World.core, heuristic, path);
			
			smoother.smoothPath(path);
			
			pathSequences[i] = new Tile[path.getCount()];
			
			for(int node = 0; node < path.getCount(); node ++){
				Tile tile = path.get(node);
				
				pathSequences[i][node] = tile;
			}
			
			
			if(Vars.debug && Vars.showPaths)
			for(Tile tile : path){
				Effects.effect(Fx.ind, tile.worldx(), tile.worldy());
			}
			
		}*/
	}
	
	void findNode(Enemy enemy){
		if(Vars.control.getSpawnPoints().get(enemy.spawn).pathTiles == null){
			return;
		}
		
		enemy.path = Vars.control.getSpawnPoints().get(enemy.spawn).pathTiles;
		Tile[] path = enemy.path;
		Tile closest = null;
		float ldst = 0f;
		int cindex = -1;
		
		for(int i = 0; i < path.length; i ++){
			Tile tile = path[i];
			float dst = Vector2.dst(tile.worldx(), tile.worldy(), enemy.x, enemy.y);
			
			if(closest == null || dst < ldst){
				ldst = dst;
				closest = tile;
				cindex = i;
			}
		}
		enemy.node = Math.max(cindex, 1);
	}
	
	private static boolean onLine(Vector2 vector, float x1, float y1, float x2, float y2){
		return MathUtils.isEqual(vector.dst(x1, y1) + vector.dst(x2, y2), Vector2.dst(x1, y1, x2, y2), 0.01f);
	}
	
	private static Vector2 projectPoint(float x1, float y1, float x2, float y2, float pointx, float pointy){
	    float px = x2-x1, py = y2-y1, dAB = px*px + py*py;
	    float u = ((pointx - x1) * px + (pointy - y1) * py) / dAB;
	    float x = x1 + u * px, y = y1 + u * py;
	    return Tmp.v3.set(x, y); //this is D
	}
}
