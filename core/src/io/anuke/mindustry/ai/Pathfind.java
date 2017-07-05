package io.anuke.mindustry.ai;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.PathFinder;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.entities.Enemy;
import io.anuke.mindustry.world.Tile;
public class Pathfind{
	static MHueristic heuristic = new MHueristic();
	static PassTileGraph passgraph = new PassTileGraph();
	static PathFinder<Tile> passpathfinder;
	static Array<DefaultGraphPath<Tile>> paths = new Array<>();
	static Tile[][] pathSequences;
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
			
			
		return vector.set(target.worldx(), target.worldy());
		
	}
	
	static public void reset(){
		paths.clear();
		pathSequences = null;
		passpathfinder = new IndexedAStarPathFinder<Tile>(passgraph);
	}
	
	static public void updatePath(){
		if(paths.size == 0){
			pathSequences = new Tile[spawnpoints.size][0];
			for(int i = 0; i < spawnpoints.size; i ++){
				DefaultGraphPath<Tile> path = new DefaultGraphPath<>();
				paths.add(path);
			}
		}
		
		for(int i = 0; i < paths.size; i ++){
			DefaultGraphPath<Tile> path = paths.get(i);
			
			path.clear();
			passpathfinder.searchNodePath(
					spawnpoints.get(i), 
					core, heuristic, path);
			
			pathSequences[i] = new Tile[path.getCount()];
			
			for(int node = 0; node < path.getCount(); node ++){
				Tile tile = path.get(node);
				
				pathSequences[i][node] = tile;
			}
			
			
			//if(debug)
			//for(Tile tile : path){
			//	Effects.effect("ind", tile.worldx(), tile.worldy());
			//}
			
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
