package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.world.Layer;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerAcceptor;
import io.anuke.mindustry.world.blocks.types.PowerBlock;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.util.*;

public class Generator extends PowerBlock{
	public static final int powerTime = 2;
	public static boolean drawRangeOverlay = false;

	public int laserRange = 6;
	public int laserDirections = 4;
	public float powerSpeed = 0.1f;
	public boolean explosive = true;
	public boolean hasLasers = true;
	public boolean outputOnly = false;

	public Generator(String name){
		super(name);
		expanded = true;
		layer = Layer.power;
	}

	@Override
	public void getStats(Array<String> list){
		super.getStats(list);

		if(hasLasers){
			list.add("[powerinfo]Laser range: " + laserRange + " blocks");
			list.add("[powerinfo]Max power transfer/second: " + Strings.toFixed(powerSpeed * 2, 2));
		}

		if(explosive){
			list.add("[orange]Highly explosive!");
		}
	}

	@Override
	public void drawSelect(Tile tile){
		super.drawSelect(tile);

		if(drawRangeOverlay){
			int rotation = tile.getRotation();
			if(hasLasers){
				Draw.color(Color.YELLOW);
				Draw.thick(2f);

				for(int i = 0; i < laserDirections; i++){
					int dir = Mathf.mod(i + rotation - laserDirections / 2, 4);
					float lx = Geometry.getD4Points()[dir].x, ly = Geometry.getD4Points()[dir].y;
					float dx = lx * laserRange * Vars.tilesize;
					float dy = ly * laserRange * Vars.tilesize;
					
					Draw.dashLine(
							tile.worldx() + lx * Vars.tilesize / 2, 
							tile.worldy() + ly * Vars.tilesize / 2, 
							tile.worldx() + dx - lx * Vars.tilesize, 
							tile.worldy() + dy - ly * Vars.tilesize, 9);
				}

				Draw.reset();
			}
		}
	}

	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		if(hasLasers){
			Draw.color(Color.PURPLE);
			Draw.thick(2f);

			for(int i = 0; i < laserDirections; i++){
				int dir = Mathf.mod(i + rotation - laserDirections / 2, 4);
				float lx = Geometry.getD4Points()[dir].x, ly = Geometry.getD4Points()[dir].y;
				float dx = lx * laserRange * Vars.tilesize;
				float dy = ly * laserRange * Vars.tilesize;
				Draw.dashLine(
						x * Vars.tilesize + lx * Vars.tilesize / 2,
						y * Vars.tilesize + ly * Vars.tilesize / 2, 
						x * Vars.tilesize + dx - lx * Vars.tilesize, 
						y * Vars.tilesize + dy - ly * Vars.tilesize, 9);
			}

			Draw.reset();
		}
	}

	@Override
	public void onDestroyed(Tile tile){
		if(explosive){
			float x = tile.worldx(), y = tile.worldy();

			Effects.effect(Fx.shellsmoke, x, y);
			Effects.effect(Fx.blastsmoke, x, y);

			Timers.run(Mathf.random(8f + Mathf.random(6f)), () -> {
				Effects.shake(6f, 8f, x, y);
				Effects.effect(Fx.generatorexplosion, x, y);
				Effects.effect(Fx.shockwave, x, y);

				Timers.run(12f + Mathf.random(20f), () -> {
					tile.damageNearby(4, 60, 0f);
				});

				Effects.sound(explosionSound, x, y);
			});

		}else{
			super.onDestroyed(tile);
		}
	}

	@Override
	public void drawLayer(Tile tile){
		if(!Settings.getBool("lasers")) return;

		PowerEntity entity = tile.entity();

		for(int i = 0; i < laserDirections; i++){
			if(entity.power > powerSpeed){
				Draw.alpha(1f);
			}else{
				Draw.alpha(0.5f);
			}
			drawLaserTo(tile, (tile.getRotation() + i) - laserDirections / 2);
		}

		Draw.color();
	}

	@Override
	public boolean acceptsPower(Tile tile){
		return false;
	}

	protected void distributeLaserPower(Tile tile){
		PowerEntity entity = tile.entity();

		for(int i = 0; i < laserDirections; i++){
			int rot = (tile.getRotation() + i) - laserDirections / 2;
			Tile target = laserTarget(tile, rot);

			if(target == null || isInterfering(target, rot))
				continue;

			PowerAcceptor p = (PowerAcceptor) target.block();
			if(p.acceptsPower(target) && entity.power >= powerSpeed){
				float accepted = p.addPower(target, powerSpeed);
				entity.power -= (accepted);
			}

		}
	}

	protected void drawLaserTo(Tile tile, int rotation){

		Tile target = laserTarget(tile, rotation);

		if(target != null){
			boolean interfering = isInterfering(target, rotation);

			Tmp.v1.set(Angles.translation(rotation * 90, target.block().width * Vars.tilesize / 2 + 2f + (interfering ? Vector2.dst(tile.worldx(), tile.worldy(), target.worldx(), target.worldy()) / 2f - Vars.tilesize / 2f * target.block().width - 1 : 0)));

			Angles.translation(rotation * 90, width * Vars.tilesize / 2 + 2f);

			if(!interfering){
				Draw.tint(Hue.mix(Color.GRAY, Color.WHITE, 0.904f + Mathf.sin(Timers.time(), 1.7f, 0.06f)));
			}else{
				Draw.tint(Hue.mix(Color.SCARLET, Color.WHITE, 0.902f + Mathf.sin(Timers.time(), 1.7f, 0.08f)));
				if(GameState.is(State.playing) && Mathf.chance(Timers.delta() * 0.033)){
					Effects.effect(Fx.laserspark, target.worldx() - Tmp.v1.x, target.worldy() - Tmp.v1.y);
				}
			}

			float r = interfering ? 0.8f : 0f;
			
			int relative = tile.relativeTo(target.x, target.y);
			
			if(relative == -1){
				Draw.laser("laser", "laserend", tile.worldx() + Angles.x(), tile.worldy() + Angles.y(), 
						target.worldx() - Tmp.v1.x + Mathf.range(r), 
						target.worldy() - Tmp.v1.y + Mathf.range(r), 0.7f);
			}else{
				Draw.rect("laserfull", 
						tile.worldx() + Geometry.getD4Points()[relative].x * width * Vars.tilesize / 2f, 
						tile.worldy() + Geometry.getD4Points()[relative].y * width * Vars.tilesize / 2f);
			}
			
			Draw.color();
		}
	}

	protected boolean isInterfering(Tile target, int rotation){
		if(target.block() instanceof Generator){
			Generator other = (Generator) target.block();
			int relrot = (rotation + 2) % 4;
			if(other.hasLasers){
				for(int i = 0; i < other.laserDirections; i ++){
					if(Mathf.mod(target.getRotation() + i - other.laserDirections/2, 4) == relrot){
						return true;
					}
				}
			}
		}
		return false;
	}

	protected Tile laserTarget(Tile tile, int rotation){
		rotation = Mathf.mod(rotation, 4);
		GridPoint2 point = Geometry.getD4Points()[rotation];

		int i = 0;

		for(i = 1; i < laserRange; i++){
			Tile other = Vars.world.tile(tile.x + i * point.x, tile.y + i * point.y);

			if(other != null && other.block() instanceof PowerAcceptor){
				Tile linked = other.getLinked();
				if(linked == null || linked instanceof PowerAcceptor){
					return other;
				}
			}
		}
		return null;
	}

}
