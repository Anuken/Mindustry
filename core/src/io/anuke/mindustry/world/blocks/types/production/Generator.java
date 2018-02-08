package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.world.Layer;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerAcceptor;
import io.anuke.mindustry.world.blocks.types.PowerBlock;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.graphics.Shapes;
import io.anuke.ucore.util.*;

import static io.anuke.mindustry.Vars.*;

public class Generator extends PowerBlock{
	public static final int powerTime = 2;
	public static boolean drawRangeOverlay = false;

	protected Translator t1 = new Translator();
	protected Translator t2 = new Translator();

	public int laserRange = 6;
	public int laserDirections = 4;
	public float powerSpeed = 0.5f;
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
			list.add("[powerinfo]Laser tile range: " + laserRange);
			list.add("[powerinfo]Max power transfer/second: " + Strings.toFixed(powerSpeed * 60, 2));
		}

		if(explosive){
			list.add("[orange]" + Bundles.get("text.blocks.explosive"));
		}
	}

	@Override
	public void drawSelect(Tile tile){
		super.drawSelect(tile);

		if(drawRangeOverlay){
			int rotation = tile.getRotation();
			if(hasLasers){
				Draw.color(Color.YELLOW);
				Lines.stroke(2f);

				for(int i = 0; i < laserDirections; i++){
					int dir = Mathf.mod(i + rotation - laserDirections / 2, 4);
					float lx = Geometry.d4[dir].x, ly = Geometry.d4[dir].y;
					float dx = lx * laserRange * tilesize;
					float dy = ly * laserRange * tilesize;
					
					Lines.dashLine(
							tile.worldx() + lx * tilesize / 2,
							tile.worldy() + ly * tilesize / 2,
							tile.worldx() + dx - lx * tilesize,
							tile.worldy() + dy - ly * tilesize, 9);
				}

				Draw.reset();
			}
		}
	}

	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		if(hasLasers){
			Draw.color(Color.PURPLE);
			Lines.stroke(2f);

			for(int i = 0; i < laserDirections; i++){
				int dir = Mathf.mod(i + rotation - laserDirections / 2, 4);
				float lx = Geometry.d4[dir].x, ly = Geometry.d4[dir].y;
				float dx = lx * laserRange * tilesize;
				float dy = ly * laserRange * tilesize;
				Lines.dashLine(
						x * tilesize + lx * tilesize / 2,
						y * tilesize + ly * tilesize / 2,
						x * tilesize + dx - lx * tilesize,
						y * tilesize + dy - ly * tilesize, 9);
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
			float transmit = Math.min(powerSpeed * Timers.delta(), entity.power);
			if(p.acceptsPower(target)){
				float accepted = p.addPower(target, transmit);
				entity.power -= accepted;
			}

		}
	}

	protected void drawLaserTo(Tile tile, int rotation){

		Tile target = laserTarget(tile, rotation);

		if(target != null){
			boolean interfering = isInterfering(target, rotation);

			t1.trns(rotation * 90, target.block().width * tilesize / 2 + 2f +
					(interfering ? Vector2.dst(tile.worldx(), tile.worldy(), target.worldx(), target.worldy()) / 2f - tilesize / 2f * target.block().width + 1 : 0));

			t2.trns(rotation * 90, width * tilesize / 2 + 2f);

			if(!interfering){
				Draw.tint(Hue.mix(Color.GRAY, Color.WHITE, 0.904f + Mathf.sin(Timers.time(), 1.7f, 0.06f)));
			}else{
				Draw.tint(Hue.mix(Color.SCARLET, Color.WHITE, 0.902f + Mathf.sin(Timers.time(), 1.7f, 0.08f)));

				if(state.is(State.playing) && Mathf.chance(Timers.delta() * 0.033)){
					Effects.effect(Fx.laserspark, target.worldx() - t1.x, target.worldy() - t1.y);
				}
			}

			float r = interfering ? 0f : 0f;
			
			int relative = tile.relativeTo(target.x, target.y);
			
			if(relative == -1){
				Shapes.laser("laser", "laserend", tile.worldx() + t2.x, tile.worldy() + t2.y,
						target.worldx() - t1.x + Mathf.range(r),
						target.worldy() - t1.y + Mathf.range(r), 0.7f);
			}else{
				Draw.rect("laserfull", 
						tile.worldx() + Geometry.d4[relative].x * width * tilesize / 2f,
						tile.worldy() + Geometry.d4[relative].y * width * tilesize / 2f);
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
		GridPoint2 point = Geometry.d4[rotation];

		for(int i = 1; i < laserRange; i++){
			Tile other = world.tile(tile.x + i * point.x, tile.y + i * point.y);

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
