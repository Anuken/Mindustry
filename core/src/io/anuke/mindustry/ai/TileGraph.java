package io.anuke.mindustry.ai;

import io.anuke.mindustry.world.Tile;

/**Tilegraph that ignores player-made tiles.*/
public class TileGraph implements OptimizedGraph<Tile> {

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
}
