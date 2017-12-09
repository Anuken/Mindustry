package io.anuke.mindustry.world;

import com.badlogic.gdx.ai.pfa.PathFinder;
import com.badlogic.gdx.ai.pfa.PathFinderRequest;

import io.anuke.mindustry.ai.SmoothGraphPath;

public class SpawnPoint{
	public Tile start;
	public Tile[] pathTiles;
	public Tile[] tempTiles;
	public PathFinder<Tile> finder;
	public SmoothGraphPath path = new SmoothGraphPath();
	public PathFinderRequest<Tile> request;
}
