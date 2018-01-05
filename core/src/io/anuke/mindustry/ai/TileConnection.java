package io.anuke.mindustry.ai;

import com.badlogic.gdx.ai.pfa.Connection;
import io.anuke.mindustry.world.Tile;

public class TileConnection implements Connection<Tile>{
	Tile a, b;
	
	public TileConnection(Tile a, Tile b){
		this.a = a;
		this.b = b;
	}

	@Override
	public float getCost(){
		return HueristicImpl.estimateStatic(a, b);
	}

	@Override
	public Tile getFromNode(){
		return a;
	}

	@Override
	public Tile getToNode(){
		return b;
	}

}
