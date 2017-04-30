package io.anuke.moment.ai;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import io.anuke.moment.Moment;
import io.anuke.moment.entities.Enemy;
import io.anuke.moment.world.Tile;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.Entity;

public class Pathfind{
	static MHueristic heuristic = new MHueristic();
	static TileGraph graph = new TileGraph();
	static PassTileGraph passgraph = new PassTileGraph();
	static IndexedAStarPathFinder<Tile> pathfinder = new IndexedAStarPathFinder<Tile>(graph);
	static IndexedAStarPathFinder<Tile> passpathfinder = new IndexedAStarPathFinder<Tile>(passgraph);
	static Array<DefaultGraphPath<Tile>> paths = new Array<>();
	static Vector2 vector = new Vector2();
	
	static public Vector2 find(Enemy enemy){
		if(enemy.node == -1){
			findNode(enemy);
		}
		
		//-1 is only possible here if both pathfindings failed, which should NOT happen
		//check graph code
		
		DefaultGraphPath<Tile> path = paths.get(enemy.spawn);

		Tile target = path.get(enemy.node);
			
		float dst = Vector2.dst(enemy.x, enemy.y, target.worldx(), target.worldy());
			
		if(dst < 2){
			if(enemy.node <= path.getCount()-2)
				enemy.node ++;
				
			target = path.get(enemy.node);
		}
			
			
		return vector.set(target.worldx(), target.worldy());
		
	}
	
	static public void updatePath(){
		if(paths.size == 0){
			for(int i = 0; i < Moment.i.spawnpoints.size; i ++){
				DefaultGraphPath<Tile> path = new DefaultGraphPath<>();
				paths.add(path);
			}
		}
		
		int i = 0;
		for(DefaultGraphPath<Tile> path : paths){
			path.clear();
			passpathfinder.searchNodePath(
					Moment.i.spawnpoints.get(i), 
					Moment.i.core, heuristic, path);
			
			//for(Tile tile : path){
			//	Effects.effect("ind", tile.worldx(), tile.worldy());
			///}
			i++;
		}
		
		for(Entity e : Entities.all()){
			if(e instanceof Enemy){
				findNode((Enemy)e);
			}
		}
	}
	
	static void findNode(Enemy enemy){
		DefaultGraphPath<Tile> path = paths.get(enemy.spawn);
		
		Tile closest = null;
		float ldst = 0f;
		int cindex = -1;
		
		for(int i = 0; i < path.getCount(); i ++){
			Tile tile = path.get(i);
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
