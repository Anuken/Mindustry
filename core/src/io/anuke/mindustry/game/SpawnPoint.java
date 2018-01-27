package io.anuke.mindustry.game;

import com.badlogic.gdx.ai.pfa.PathFinder;
import com.badlogic.gdx.ai.pfa.PathFinderRequest;

import io.anuke.mindustry.ai.SmoothGraphPath;
import io.anuke.mindustry.world.Tile;

public class SpawnPoint{
	public Tile start;
	public Tile[] pathTiles;
	public PathFinder<Tile> finder;
	public SmoothGraphPath path = new SmoothGraphPath();
	public PathFinderRequest<Tile> request;

	public SpawnPoint(Tile start){
		this.start = start;
	}
}
