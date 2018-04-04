package io.anuke.mindustry.game;

import io.anuke.ucore.util.Bundles;

public enum GameMode{
	waves,
	sandbox{
		{
			infiniteResources = true;
			disableWaveTimer = true;
			infinityReach=true;
			noclip=true;
		}
	},
    freebuild{
        {
            disableWaveTimer = true;
        }
    };
	public boolean infiniteResources;
	public boolean disableWaveTimer;
	public boolean infinityReach;
	public boolean noclip;

	public String description(){
		return Bundles.get("mode."+name()+".description");
	}

	@Override
	public String toString(){
		return Bundles.get("mode."+name()+".name");
	}

}
