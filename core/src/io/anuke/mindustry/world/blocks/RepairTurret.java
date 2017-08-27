package io.anuke.mindustry.world.blocks;

import com.badlogic.gdx.math.MathUtils;

import io.anuke.mindustry.World;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Timers;

public class RepairTurret extends Turret{

	public RepairTurret(String name) {
		super(name);
	}
	
	@Override
	public void update(Tile tile){
		TurretEntity entity = tile.entity();
		
		entity.target = World.findTileTarget(tile.worldx(), tile.worldy(), tile, range, true);

		if(entity.target != null){
			float target = entity.angleTo(entity.target);
			entity.rotation = Mathf.slerp(entity.rotation, target, 0.16f*Mathf.delta());

			if(Timers.get(tile, reload) && Angles.angleDist(target, entity.rotation) < 10){
				entity.target.health++;
			}
		}
	}
	
	@Override
	public void drawOver(Tile tile){
		TurretEntity entity = tile.entity();
		
		if(entity.target != null){
			float x = tile.worldx(), y = tile.worldy();
			float x2 = entity.target.x, y2 = entity.target.y;

			Draw.color(Hue.rgb(138, 244, 138, (MathUtils.sin(Timers.time()) + 1f) / 14f));
			Draw.alpha(0.3f);
			Draw.thickness(4f);
			Draw.line(x, y, x2, y2);
			Draw.thickness(2f);
			Draw.rect("circle", x2, y2, 7f, 7f);
			Draw.alpha(1f);
			Draw.thickness(2f);
			Draw.line(x, y, x2, y2);
			Draw.thickness(1f);
			Draw.rect("circle", x2, y2, 5f, 5f);
			Draw.reset();
		}
		
		Draw.rect(name(), tile.worldx(), tile.worldy(), entity.rotation - 90);
	}
	
	@Override
	public String description(){
		return "[green]Range: " + (int)range + "\n[orange]Heals nearby tiles.";
	}
}
