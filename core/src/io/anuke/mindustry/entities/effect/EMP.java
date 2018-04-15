package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerAcceptor;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.entities.TimedEntity;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Translator;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class EMP extends TimedEntity{
	static final int maxTargets = 8;
	static Array<Tile> array = new Array<>();
	static Translator tr = new Translator();
	
	int radius = 4;
	int damage = 6;
	Array<Tile> targets = new Array<>(maxTargets);
	
	public EMP(float x, float y, int damage){
		this.damage = damage;
		set(x, y);
		
		lifetime = 30f;
		
		int worldx = Mathf.scl2(x, tilesize);
		int worldy = Mathf.scl2(y, tilesize);
		
		array.clear();
		
		for(int dx = -radius; dx <= radius; dx ++){
			for(int dy = -radius; dy <= radius; dy ++){
				if(Vector2.dst(dx, dy, 0, 0) < radius){
					Tile tile = world.tile(worldx + dx, worldy + dy);
					
					if(tile != null && tile.block().destructible){
						array.add(tile);
					}
				}
			}
		}
		
		array.shuffle();
		
		for(int i = 0; i < array.size && i < maxTargets; i ++){
			Tile tile = array.get(i);
			targets.add(tile);
			
			if(tile != null && tile.block() instanceof PowerAcceptor){
				PowerAcceptor p = (PowerAcceptor)tile.block();
				p.setPower(tile, 0f);
				tile.entity.damage((int)(damage*2f)); //extra damage
			}

			if(tile == null) continue;

			//entity may be null here, after the block is dead!
			Effects.effect(Fx.empspark, tile.worldx(), tile.worldy());
			if(tile.entity != null) tile.entity.damage(damage);
		}
	}
	
	@Override
	public void drawOver(){
		Draw.color(Color.SKY);
		
		for(int i = 0; i < targets.size; i ++){
			Tile target = targets.get(i);
			
			drawLine(target.worldx(), target.worldy());
			
			float rad = 5f*fout();
			Draw.rect("circle", target.worldx(), target.worldy(), rad, rad);
		}
		
		for(int i = 0; i < 14 - targets.size; i ++){
			tr.trns(Mathf.randomSeed(i + id*77)*360f, radius * tilesize);
			drawLine(x + tr.x, y + tr.y);
		}
	
		Lines.stroke(fout()*2f);
		Lines.poly(x, y, 34, radius * tilesize);
		
		Draw.reset();
	}
	
	private void drawLine(float targetx, float targety){
		int joints = 3;
		float r = 3f;
		float lastx = x, lasty = y;
		
		for(int seg = 0; seg < joints; seg ++){
			float dx = Mathf.range(r), 
					dy = Mathf.range(r);
			
			float frac = (seg+1f)/joints;
			
			float tx = (targetx - x)*frac + x + dx, 
					ty = (targety - y)*frac + y + dy;
			
			drawLaser(lastx, lasty, tx, ty);
			
			lastx = tx;
			lasty = ty;
		}
	}
	
	private void drawLaser(float x, float y, float x2, float y2){
		Lines.stroke(fout() * 2f);
		Lines.line(x, y, x2, y2);
	}
}
