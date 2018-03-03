package io.anuke.mindustry.world.blocks.types.defense;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Layer;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

import static io.anuke.mindustry.Vars.world;

public class RepairTurret extends PowerTurret{
	protected float repairFrac = 1f / 135f;

	public RepairTurret(String name) {
		super(name);
		powerUsed = 0.1f;
		layer2 = Layer.laser;
	}
	
	@Override
	public void getStats(Array<String> list){
		list.add("[health]health: " + health);
		list.add("[powerinfo]Power Capacity: " + (int)powerCapacity);
		list.add("[powerinfo]Power/shot: " + Strings.toFixed(powerUsed, 1));
		list.add("[turretinfo]Range: " + (int)range);
		list.add("[turretinfo]Repairs/Second: " + Strings.toFixed(60f/reload * repairFrac * 100, 1) + "%");
	}
	
	@Override
	public void update(Tile tile){
		PowerTurretEntity entity = tile.entity();
		
		if(entity.power < powerUsed){
			return;
		}
		
		if(entity.blockTarget != null && entity.blockTarget.dead){
			entity.blockTarget = null;
		}
		
		if(entity.timer.get(timerTarget, targetInterval)){
			entity.blockTarget = world.findTileTarget(tile.worldx(), tile.worldy(), tile, range, true);
		}

		if(entity.blockTarget != null){
			if(Float.isNaN(entity.rotation)){
				entity.rotation = 0;
			}

			float target = entity.angleTo(entity.blockTarget);
			entity.rotation = Mathf.slerpDelta(entity.rotation, target, 0.16f);

			int maxhealth = entity.blockTarget.tile.block().health;

			if(entity.timer.get(timerReload, reload) && Angles.angleDist(target, entity.rotation) < shootCone){
				entity.blockTarget.health += maxhealth * repairFrac;
				
				if(entity.blockTarget.health > maxhealth)
					entity.blockTarget.health = maxhealth;
				
				entity.power -= powerUsed;
			}
		}
	}
	
	@Override
	public void drawSelect(Tile tile){
		Draw.color(Color.GREEN);
		Lines.dashCircle(tile.drawx(), tile.drawy(), range);
		Draw.reset();
	}
	
	@Override
	public void drawLayer2(Tile tile){
		PowerTurretEntity entity = tile.entity();
		TileEntity target = entity.blockTarget;
		
		if(entity.power >= powerUsed && target != null && Angles.angleDist(entity.angleTo(target), entity.rotation) < 10){
			Tile targetTile = target.tile;
			float len = 4f;

			float x = tile.drawx() + Angles.trnsx(entity.rotation, len), y = tile.drawy() + Angles.trnsy(entity.rotation, len);
			float x2 = targetTile.drawx(), y2 = targetTile.drawy();

			Draw.color(Hue.rgb(138, 244, 138, (MathUtils.sin(Timers.time()) + 1f) / 14f));
			Draw.alpha(0.3f);
			Lines.stroke(4f);
			Lines.line(x, y, x2, y2);
			Lines.stroke(2f);
			Draw.rect("circle", x2, y2, 7f, 7f);
			Draw.alpha(1f);
			Lines.stroke(2f);
			Lines.line(x, y, x2, y2);
			Lines.stroke(1f);
			Draw.rect("circle", x2, y2, 5f, 5f);
			Draw.reset();
		}
	}
}
