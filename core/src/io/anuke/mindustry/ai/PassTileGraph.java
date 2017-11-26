package io.anuke.mindustry.ai;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.world.Tile;
/**Tilegraph that ignores player-made tiles.*/
public class PassTileGraph implements IndexedGraph<Tile>{
	private Array<Connection<Tile>> tempConnections = new Array<Connection<Tile>>();

	@Override
	public Array<Connection<Tile>> getConnections(Tile fromNode){
		tempConnections.clear();
		
		if(!fromNode.passable())
			return tempConnections;
		
		for(Tile tile : fromNode.getNearby()){
			if(tile != null && (tile.passable()))
				tempConnections.add(new TileConnection(fromNode, tile));
		}
		
		return tempConnections;
	}

	@Override
	public int getIndex(Tile node){
		return node.id();
	}

	@Override
	public int getNodeCount(){
		return Vars.world.width() * Vars.world.height();
	}
}
