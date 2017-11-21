package io.anuke.mindustry.ai;

import com.badlogic.gdx.ai.pfa.PathFinder;
import com.badlogic.gdx.ai.pfa.PathFinderRequest;
import com.badlogic.gdx.ai.pfa.PathSmoother;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.Fx;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.World;
import io.anuke.ucore.core.Effects;
public class Pathfind{
	static MHueristic heuristic = new MHueristic();
	static PassTileGraph passgraph = new PassTileGraph();
	static PathFinder<Tile> passpathfinder;
	static Array<SmoothGraphPath> paths = new Array<>();
	static Tile[][] pathSequences;
	static PathSmoother<Tile, Vector2> smoother = new PathSmoother<Tile, Vector2>(new Raycaster());
	static Vector2 vector = new Vector2();
	
	static public Vector2 find(Enemy enemy){
		if(enemy.node == -1){
			findNode(enemy);
		}
		
		//-1 is only possible here if both pathfindings failed, which should NOT happen
		//check graph code
		
		Tile[] path = enemy.path;

		Tile target = path[enemy.node];
			
		float dst = Vector2.dst(enemy.x, enemy.y, target.worldx(), target.worldy());
			
		if(dst < 2){
			if(enemy.node <= path.length-2)
				enemy.node ++;
				
			target = path[enemy.node];
		}
		
		//near the core, stop
		if(enemy.node == path.length - 1){
			vector.set(target.worldx(), target.worldy());
		}
			
		return vector.set(target.worldx(), target.worldy());
		
	}
	
	static public void reset(){
		paths.clear();
		pathSequences = null;
		passpathfinder = new IndexedAStarPathFinder<Tile>(passgraph);
	}
	
	static public void updatePath(){
		if(paths.size == 0 || paths.size != World.spawnpoints.size){
			paths.clear();
			pathSequences = new Tile[World.spawnpoints.size][0];
			for(int i = 0; i < World.spawnpoints.size; i ++){
				SmoothGraphPath path = new SmoothGraphPath();
				paths.add(path);
			}
		}
		
		//TODO
		PathFinderRequest<Tile> request = new PathFinderRequest<Tile>();
		request.startNode = World.spawnpoints.get(0);
		request.endNode = World.core;
		passpathfinder.search(request, 1000);
		
		for(int i = 0; i < paths.size; i ++){
			SmoothGraphPath path = paths.get(i);
			
			path.clear();
			passpathfinder.searchNodePath(
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
			
		}
	}
	
	static void findNode(Enemy enemy){
		enemy.path = pathSequences[enemy.spawn];
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
		enemy.node = cindex;
	}
}
