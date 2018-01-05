package io.anuke.mindustry.ai;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.world.Tile;

/**Tilegraph that ignores player-made tiles.*/
public class TileGraph implements OptimizedGraph<Tile> {
	private Array<Connection<Tile>> tempConnections = new Array<Connection<Tile>>(4);

	/**Used for the default Graph implementation. Returns a result similar to connectionsOf()*/
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

	/**Used for the OptimizedPathFinder implementation.*/
	@Override
    public Tile[] connectionsOf(Tile node){
	    Tile[] nodes = node.getNearby();
	    for(int i = 0; i < 4; i ++){
	        if(nodes[i] != null && !nodes[i].passable()){
                nodes[i] = null;
            }
        }
        return nodes;
    }

	@Override
	public int getIndex(Tile node){
		return node.packedPosition();
	}

	@Override
	public int getNodeCount(){
		return Vars.world.width() * Vars.world.height();
	}
}
