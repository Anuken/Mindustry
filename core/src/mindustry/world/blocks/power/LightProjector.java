package mindustry.world.blocks.power;

import arc.util.*;
import mindustry.graphics.*;

public class LightProjector extends LightBlock{
	public float lightCone = 30f;

	public LightProjector(String name){
		super(name);
		rotate = true;
	}

	public class LightProjectorBuild extends LightBuild{
		@Override
        public void drawLight(){
            Drawf.light(team, x, y, radius * Math.min(smoothTime, 2f), Tmp.c1.set(color), brightness * efficiency(), rotation * 90 - lightCone - 90, rotation * 90 + lightCone - 90);
        }
	}
}