package io.anuke.mindustry.ai;

import com.badlogic.gdx.ai.pfa.Heuristic;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.world.Tile;

public class MHueristic implements Heuristic<Tile>{
	//so this means that the cost of going through solids is 10x going through non solids
	float multiplier = 10f;

	@Override
	public float estimate(Tile node, Tile other){
		float cost = Math.abs(node.worldx() - other.worldx()) + Math.abs(node.worldy() - other.worldy());
		
		//TODO balance multiplier
		if(node.breakable() && node.block().solid) cost += Vars.tilesize*multiplier;
		if(other.breakable() && other.block().solid) cost += Vars.tilesize*multiplier;
		for(Tile tile : node.getNearby()){
			if(tile != null && tile.solid()){
				//don't go near solid tiles!
				cost += Vars.tilesize*3;
			}
		}
		return cost;
	}

}
