package io.anuke.mindustry.ai;

import com.badlogic.gdx.ai.pfa.Heuristic;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.world.Tile;

public class HeuristicImpl implements Heuristic<Tile>{
	/**How many times more it costs to go through a destructible block than an empty block.*/
	static final float solidMultiplier = 5f;
	/**How many times more it costs to go through a tile that touches a solid block.*/
	static final float occludedMultiplier = 5f;

	@Override
	public float estimate(Tile node, Tile other){
		return estimateStatic(node, other);
	}

	/**Estimate the cost of walking between two tiles.*/
	public static float estimateStatic(Tile node, Tile other){
		//Get Manhattan distance cost
		float cost = Math.abs(node.worldx() - other.worldx()) + Math.abs(node.worldy() - other.worldy());
		
		//If either one of the tiles is a breakable solid block (that is, it's player-made),
		//increase the cost by the tilesize times the solid block multiplier
		//Also add the block health, so blocks with more health cost more to traverse
		if(node.breakable() && node.block().solid) cost += Vars.tilesize* solidMultiplier + node.block().health;
		if(other.breakable() && other.block().solid) cost += Vars.tilesize* solidMultiplier + other.block().health;

		//if this block has solid blocks near it, increase the cost, as we don't want enemies hugging walls
		if(node.occluded) cost += Vars.tilesize*occludedMultiplier;

		return cost;
	}

}
