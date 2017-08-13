package io.anuke.mindustry.ai;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.SmoothableGraphPath;
import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.util.Tmp;

public class SmoothGraphPath extends DefaultGraphPath<Tile> implements SmoothableGraphPath<Tile, Vector2>{

	@Override
	public Vector2 getNodePosition(int index){
		Tile tile = nodes.get(index);
		return Tmp.v1.set(tile.worldx(), tile.worldy());
	}

	@Override
	public void swapNodes(int index1, int index2){
		nodes.swap(index1, index2);
	}

	@Override
	public void truncatePath(int newLength){
		nodes.truncate(newLength);
	}

}
