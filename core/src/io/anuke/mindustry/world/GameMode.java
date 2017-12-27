package io.anuke.mindustry.world;

import io.anuke.ucore.util.Bundles;

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

	@Override
	public String toString(){
		return Bundles.get("mode."+name()+".name");
	}

}
