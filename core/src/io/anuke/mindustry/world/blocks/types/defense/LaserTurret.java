package io.anuke.mindustry.world.blocks.types.defense;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.world.Layer;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

public class LaserTurret extends PowerTurret{
	protected Color beamColor = Color.WHITE.cpy();
	protected Effect hiteffect = Fx.laserhit;
	protected int damage = 4;
	protected float cone = 15f;

	public LaserTurret(String name) {
		super(name);
		shootsound = null;
		layer2 = Layer.laser;
		soundReload = 20;
	}
	
	@Override
	public void shoot(Tile tile){
		TurretEntity entity = tile.entity();
		Enemy enemy = entity.target;
		
		if(Angles.angleDist(entity.rotation, Angles.angle(tile.drawx(), tile.drawy(), enemy.x, enemy.y)) < cone){
			enemy.damage(damage);
			Effects.effect(hiteffect, enemy.x + Mathf.range(3), enemy.y + Mathf.range(3));
		}
	}
	
	@Override
	public void drawLayer2(Tile tile){
		TurretEntity entity = tile.entity();
		Enemy enemy = entity.target;
		
		if(enemy != null &&
				Angles.angleDist(entity.rotation, Angles.angle(tile.drawx(), tile.drawy(), enemy.x, enemy.y)) <= cone){
			float len = 4f;
			
			float x = tile.drawx() + Angles.trnsx(entity.rotation, len), y = tile.drawy() + Angles.trnsy(entity.rotation, len);
			float x2 = enemy.x, y2 = enemy.y;

			float lighten = (MathUtils.sin(Timers.time()/1.2f) + 1f) / 10f;
			
			Draw.color(Tmp.c1.set(beamColor).mul(1f + lighten, 1f + lighten, 1f + lighten, 1f));
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
			
		}
		
		Draw.reset();
	}
}
