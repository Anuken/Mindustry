package io.anuke.mindustry.ai;

import io.anuke.mindustry.world.Tile;

/**Tilegraph that ignores player-made tiles.*/
public class TileGraph implements OptimizedGraph<Tile> {
	private Tile[] tiles = new Tile[4];

	/**Used for the OptimizedPathFinder implementation.*/
	@Override
    public Tile[] connectionsOf(Tile node){
	    Tile[] nodes = node.getNearby(tiles);
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
