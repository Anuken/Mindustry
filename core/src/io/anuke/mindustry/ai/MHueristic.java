package io.anuke.mindustry.ai;

import com.badlogic.gdx.ai.pfa.Heuristic;

import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.TileType;

public class MHueristic implements Heuristic<Tile>{
	//so this means that the cost of going through solids is 10x going through non solids
	float multiplier = 10f;

	@Override
	public float estimate(Tile node, Tile other){
		float cost = Math.abs(node.worldx() - other.worldx()) + Math.abs(node.worldy() - other.worldy());
		
		//TODO balance multiplier
		if(node.artifical() && node.block().solid) cost += TileType.tilesize*multiplier;
		if(other.artifical() && other.block().solid) cost += TileType.tilesize*multiplier;
		return cost;
	}

}
