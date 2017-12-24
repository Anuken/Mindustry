package io.anuke.mindustry.world;

public enum GameMode{
	waves,
	sandbox{
		{
			infiniteResources = true;
			toggleWaves = true;
		}
	},
    freebuild{
        {
            toggleWaves = true;
        }
    };
	public boolean infiniteResources;
	public boolean toggleWaves;
}
