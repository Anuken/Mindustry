package io.anuke.moment.ai;

import com.badlogic.gdx.ai.pfa.Connection;

import io.anuke.moment.world.Tile;

public class TileConnection implements Connection{
	Tile a, b;
	
	public TileConnection(Tile a, Tile b){
		this.a = a;
		this.b = b;
	}

	@Override
	public float getCost(){
		return Math.abs(a.worldx() - b.worldx()) + Math.abs(a.worldy() - b.worldy());
	}

	@Override
	public Object getFromNode(){
		return a;
	}

	@Override
	public Object getToNode(){
		return b;
	}

}
