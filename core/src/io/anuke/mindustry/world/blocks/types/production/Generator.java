package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.effect.Fx;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerAcceptor;
import io.anuke.mindustry.world.blocks.types.PowerBlock;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.util.*;

public class Generator extends PowerBlock{
	public static final int powerTime = 2;

	public int laserRange = 6;
	public int laserDirections = 4;
	public float powerSpeed = 0.06f;
	public boolean explosive = true;
	public boolean hasLasers = true;
	public boolean outputOnly = false;

	public Generator(String name) {
		super(name);
	}
	
	@Override
	public void getStats(Array<String> list){
		super.getStats(list);
		
		if(hasLasers){
			list.add("[powerinfo]Laser range: " + laserRange + " blocks");
			list.add("[powerinfo]Max power transfer/second: " + Strings.toFixed(powerSpeed*2, 2));
		}
		
		if(explosive){
			list.add("[orange]Highly explosive!");
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
					tile.damageNearby(4, 40, 0f);
				});

				Effects.sound(explosionSound, x, y);
			});

		}else{
			super.onDestroyed(tile);
		}
	}
	
	@Override
	public void drawOver(Tile tile){
		PowerEntity entity = tile.entity();

		for(int i = 0; i < laserDirections; i++){
			if(entity.power > powerSpeed){
				Draw.alpha(1f);
			}else{
				Draw.alpha(0.5f);
			}
			drawLaserTo(tile, (tile.getRotation() + i) - laserDirections/2);
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
			int rot = (tile.getRotation() + i) - laserDirections/2;
			Tile target = laserTarget(tile, rot);
			
			if(target == null || isInterfering(target, rot)) continue;
			
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
			
			Tmp.v1.set(Angles.translation(rotation * 90, target.block().width * Vars.tilesize/2 + 2f + 
					(interfering ? 
							Vector2.dst(tile.worldx(), tile.worldy(), target.worldx(), target.worldy()) / 2f - Vars.tilesize/2f * target.block().width - 1 : 0)));
			
			Angles.translation(rotation * 90, width * Vars.tilesize/2 + 2f);

			if(!interfering){
				Draw.tint(Hue.mix(Color.GRAY, Color.WHITE, 0.904f + Mathf.sin(Timers.time(), 1.7f, 0.06f)));
			}else{
				Draw.tint(Hue.mix(Color.SCARLET, Color.WHITE, 0.902f + Mathf.sin(Timers.time(), 1.7f, 0.08f)));
				if(Mathf.chance(Timers.delta() * 0.033)){
					Effects.effect(Fx.laserspark, target.worldx() - Tmp.v1.x, target.worldy() - Tmp.v1.y);
				}
			}
			
			float r = interfering ? 0.8f : 0f;

			Draw.laser("laser", "laserend", tile.worldx() + Angles.x(), tile.worldy() + Angles.y(), 
					target.worldx() - Tmp.v1.x + Mathf.range(r), target.worldy() - Tmp.v1.y + Mathf.range(r), 0.7f + Mathf.sin(Timers.time(), 2f, 0.1f * 0));

			Draw.color();
		}
	}
	
	protected boolean isInterfering(Tile target, int rotation){
		if(target.block() instanceof Generator){
			Generator other = (Generator)target.block();
			int relrot = (rotation + 2) % 4;
			if(other.hasLasers && Math.abs(target.getRotation() - relrot) <= other.laserDirections/2){
				return true;
			}
		}
		return false;
	}

	protected Tile laserTarget(Tile tile, int rotation){
		if(rotation < 0)
			rotation += 4;
		rotation %= 4;
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
