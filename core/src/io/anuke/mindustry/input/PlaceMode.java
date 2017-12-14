package io.anuke.mindustry.input;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

public enum PlaceMode{
	cursor{
		public void draw(int tilex, int tiley, int endx, int endy){
			float x = tilex * Vars.tilesize;
			float y = tiley * Vars.tilesize;
			
			boolean valid = world.validPlace(tilex, tiley, player.recipe.result) && (android || control.getInput().cursorNear());

			Vector2 offset = player.recipe.result.getPlaceOffset();

			float si = MathUtils.sin(Timers.time() / 6f) + 1;

			Draw.color(valid ? Color.PURPLE : Color.SCARLET);
			Draw.thickness(2f);
			Draw.linecrect(x + offset.x, y + offset.y, tilesize * player.recipe.result.width + si, tilesize * player.recipe.result.height + si);

			player.recipe.result.drawPlace(tilex, tiley, player.rotation, valid);
			Draw.thickness(2f);

			if(player.recipe.result.rotate){
				Draw.color(Color.ORANGE);
				Tmp.v1.set(7, 0).rotate(player.rotation * 90);
				Draw.line(x, y, x + Tmp.v1.x, y + Tmp.v1.y);
			}
			
			if(valid)
				Cursors.setHand();
			else
				Cursors.restoreCursor();
		}
	}, 
	touch{
		int maxlen = 10;
		int tilex;
		int tiley;
		int endx;
		int endy;
		int rotation;
		
		{
			lockCamera = true;
		}
		
		public void draw(int tilex, int tiley, int endx, int endy){
			float t = Vars.tilesize;
			
			process(tilex, tiley, endx, endy);
			int tx = tilex, ty = tiley, ex = endx, ey = endy;
			tilex = this.tilex; tiley = this.tiley; 
			endx = this.endx; endy = this.endy;
			float x = this.tilex * t, y = this.tiley * t, 
					x2 = this.endx * t, y2 = this.endy * t;
			
			if(x2 >= x){
				x -= t/2;
				x2 += t/2;
			}
			
			if(y2 >= y){
				y -= t/2;
				y2 += t/2;
			}
			
			if(tilex == endx && tiley == endy){
				cursor.draw(tilex, tiley, endx, endy);
			}else{
				Draw.thick(2f);
				Draw.color(control.getInput().cursorNear() ? Color.PURPLE : Color.RED);
				Draw.linerect(x, y, x2 - x, y2 - y);
				Draw.alpha(0.3f);
				Draw.crect("blank", x, y, x2 - x, y2 - y);
				
				Draw.color(Color.RED);
				
				int amount = 1;
				for(int cx = 0; cx <= Math.abs(endx - tilex); cx ++){
					for(int cy = 0; cy <= Math.abs(endy - tiley); cy ++){
						int px = tx + cx * Mathf.sign(ex - tx), 
						py = ty + cy * Mathf.sign(ey - ty);
						
						if(!world.validPlace(px, py, player.recipe.result) 
								|| !control.hasItems(player.recipe.requirements, amount)){
							Draw.square(px * t, py * t, t/2);
						}
						amount ++;
					}
				}
				
				if(player.recipe.result.rotate){
					float cx = tilex * t, cy = tiley * t;
					Draw.color(Color.ORANGE);
					Tmp.v1.set(7, 0).rotate(rotation * 90);
					Draw.line(cx, cy, cx + Tmp.v1.x, cy + Tmp.v1.y);
				}
				Draw.reset();
			}
		}
		
		public void tapped(int tilex, int tiley, int endx, int endy){
			int prev = player.rotation;
			process(tilex, tiley, endx, endy);
			//tilex = this.tilex; tiley = this.tiley; 
			//endx = this.endx; endy = this.endy;
			
			player.rotation = this.rotation;
			
			for(int x = 0; x <= Math.abs(this.endx - this.tilex); x ++){
				for(int y = 0; y <= Math.abs(this.endy - this.tiley); y ++){
					control.getInput().tryPlaceBlock(
							tilex + x * Mathf.sign(endx - tilex), 
							tiley + y * Mathf.sign(endy - tiley));
				}
			}
			
			player.rotation = prev;
			
		}
		
		void process(int tilex, int tiley, int endx, int endy){
			if(Math.abs(tilex - endx) > Math.abs(tiley - endy)){
				endy = tiley;
			}else{
				endx = tilex;
			}
			
			if(Math.abs(endx - tilex) > maxlen){
				endx = Mathf.sign(endx - tilex) * maxlen + tilex;
			}
			
			if(Math.abs(endy - tiley) > maxlen){
				endy = Mathf.sign(endy - tiley) * maxlen + tiley;
			}
			
			if(endx > tilex)
				rotation = 0;
			else if(endx < tilex)
				rotation = 2;
			else if(endy > tiley)
				rotation = 1;
			else if(endy < tiley)
				rotation = 3;
			else 
				rotation = player.rotation;
			
			if(endx < tilex){
				int t = endx;
				endx = tilex;
				tilex = t;
			}
			if(endy < tiley){
				int t = endy;
				endy = tiley;
				tiley = t;
			}
			
			this.endx = endx;
			this.endy = endy;
			this.tilex = tilex;
			this.tiley = tiley;
		}
	},
	breaker{
		public void draw(int tilex, int tiley, int endx, int endy){
			Tile tile = world.tile(tilex, tiley);
			
			if(tile != null && world.validBreak(tilex, tiley)){
				if(tile.isLinked())
					tile = tile.getLinked();
				Vector2 offset = tile.block().getPlaceOffset();
				float fract = player.breaktime / tile.getBreakTime();
				
				if(Inputs.buttonDown(Buttons.RIGHT)){
					Draw.color(Color.YELLOW, Color.SCARLET, fract);
					Draw.linecrect(tile.worldx() + offset.x, tile.worldy() + offset.y, tile.block().width * Vars.tilesize, tile.block().height * Vars.tilesize);
				}else if(android && player.breaktime > 0){
					Draw.color(Color.YELLOW, Color.SCARLET, fract);
					Draw.circle(tile.worldx(), tile.worldy(), 4 + (1f - fract) * 26);
				}
				Draw.reset();
			}
		}
	};
	public boolean lockCamera;
	
	public void draw(int tilex, int tiley, int endx, int endy){
		
	}
	
	public void tapped(int tilex, int tiley, int endx, int endy){
		
	}
}
