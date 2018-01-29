package io.anuke.mindustry.ai;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.utils.Array;
import static io.anuke.mindustry.Vars.*;
import io.anuke.mindustry.world.Tile;

/**Tilegraph that ignores player-made tiles.*/
public class TileGraph implements OptimizedGraph<Tile> {

	/**return nothing, as this isn't used*/
	@Override
	public Array<Connection<Tile>> getConnections(Tile fromNode){ return null; }

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
		return world.width() * world.height();
	}
}
