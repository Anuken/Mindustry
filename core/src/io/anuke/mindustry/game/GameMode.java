package io.anuke.mindustry.game;

import io.anuke.ucore.util.Bundles;

public enum GameMode{
	waves,
	sandbox{
		{
			infiniteResources = true;
			disableWaveTimer = true;
		}
	},
    freebuild{
        {
            disableWaveTimer = true;
        }
    };
	public boolean infiniteResources;
	public boolean disableWaveTimer;

	public String description(){
		return Bundles.get("mode."+name()+".description");
	}

	@Override
	public String toString(){
		return Bundles.get("mode."+name()+".name");
	}

}
