package io.anuke.mindustry.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.ui.fragments.ToolFragment;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

import static io.anuke.mindustry.Vars.*;

public enum PlaceMode{
	cursor{
		{
			shown = true;
			lockCamera = true;
			pan = true;
		}
		
		public void draw(int tilex, int tiley, int endx, int endy){
			float x = tilex * Vars.tilesize;
			float y = tiley * Vars.tilesize;
			
			boolean valid = control.getInput().validPlace(tilex, tiley, control.getInput().recipe.result) && (android || control.getInput().cursorNear());

			Vector2 offset = control.getInput().recipe.result.getPlaceOffset();

			float si = MathUtils.sin(Timers.time() / 6f) + 1.5f;

			Draw.color(valid ? Colors.get("place") : Colors.get("placeInvalid"));
			Draw.thickness(2f);
			Draw.linecrect(x + offset.x, y + offset.y, tilesize * control.getInput().recipe.result.width + si, 
					tilesize * control.getInput().recipe.result.height + si);

			control.getInput().recipe.result.drawPlace(tilex, tiley, control.getInput().rotation, valid);
			Draw.thickness(2f);

			if(control.getInput().recipe.result.rotate){
				Draw.color(Colors.get("placeRotate"));
				Tmp.v1.set(7, 0).rotate(control.getInput().rotation * 90);
				Draw.line(x, y, x + Tmp.v1.x, y + Tmp.v1.y);
			}
			
			if(valid)
				Cursors.setHand();
			else
				Cursors.restoreCursor();
		}
		
		public void tapped(int tilex, int tiley){
			control.getInput().tryPlaceBlock(tilex, tiley, true);
		}
	},
	touch{
		{
			shown = true;
			lockCamera = false;
			showRotate = true;
			showCancel = true;
		}
		
		public void tapped(int x, int y){
			control.getInput().tryPlaceBlock(x, y, true);
		}
	},
	none{
		{
			delete = true;
			shown = true;
			both = true;
		}
	},
	holdDelete{
		{
			delete = true;
			shown = true;
			both = true;
		}
		
		public void draw(int tilex, int tiley, int endx, int endy){
			Tile tile = world.tile(tilex, tiley);
			
			if(tile != null && control.getInput().validBreak(tilex, tiley)){
				if(tile.isLinked())
					tile = tile.getLinked();
				Vector2 offset = tile.block().getPlaceOffset();
				float fract = control.getInput().breaktime / tile.getBreakTime();
				
				if(android && control.getInput().breaktime > 0){
					Draw.color(Colors.get("breakStart"), Colors.get("break"), fract);
					Draw.polygon(tile.worldx() + offset.x, tile.worldy() + offset.y, 25, 4 + (1f - fract) * 26);
				}
				Draw.reset();
			}
		}
	},
	touchDelete{
		{
			shown = true;
			lockCamera = false;
			showRotate = true;
			showCancel = true;
			delete = true;
		}
		
		public void tapped(int x, int y){
			control.getInput().tryDeleteBlock(x, y, true);
		}
	},
	areaDelete{
		int maxlen = 20;
		int tilex;
		int tiley;
		int endx;
		int endy;
		
		{
			shown = true;
			lockCamera = true;
			delete = true;
		}
		
		public void draw(int tilex, int tiley, int endx, int endy){
			float t = tilesize;
			
			process(tilex, tiley, endx, endy);
			
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
			
			Draw.color(Colors.get("break"));
			Draw.thick(1f);
			for(int cx = tilex; cx <= endx; cx ++){
				for(int cy = tiley; cy <= endy; cy ++){
					Tile tile = Vars.world.tile(cx, cy);
					if(tile != null && tile.getLinked() != null)
						tile = tile.getLinked();
					if(tile != null && control.getInput().validBreak(tile.x, tile.y)){
						Vector2 offset = tile.block().getPlaceOffset();
						Draw.linecrect(tile.worldx() + offset.x, tile.worldy() + offset.y, 
								tile.block().width * t, tile.block().height * t);
					}
				}
			}
			
			Draw.thick(2f);
			Draw.color(control.getInput().cursorNear() ? Colors.get("break") : Colors.get("breakInvalid"));
			Draw.linerect(x, y, x2 - x, y2 - y);
			Draw.alpha(0.3f);
			Draw.crect("blank", x, y, x2 - x, y2 - y);
			Draw.reset();
		}
		
		public void released(int tilex, int tiley, int endx, int endy){

			process(tilex, tiley, endx, endy);
			tilex = this.tilex; tiley = this.tiley; 
			endx = this.endx; endy = this.endy;

			if(Vars.android){
				ToolFragment t = Vars.ui.toolfrag;
				if(!t.confirming || t.px != tilex || t.py != tiley || t.px2 != endx || t.py2 != endy) {
					t.confirming = true;
					t.px = tilex;
					t.py = tiley;
					t.px2 = endx;
					t.py2 = endy;
					return;
				}
			}
			
			boolean first = true;
			
			for(int cx = tilex; cx <= endx; cx ++){
				for(int cy = tiley; cy <= endy; cy ++){
					if(control.getInput().tryDeleteBlock(cx, cy, first)){
						first = false;
					}
				}
			}
		}
		
		void process(int tilex, int tiley, int endx, int endy){
			
			if(Math.abs(endx - tilex) > maxlen){
				endx = Mathf.sign(endx - tilex) * maxlen + tilex;
			}
			
			if(Math.abs(endy - tiley) > maxlen){
				endy = Mathf.sign(endy - tiley) * maxlen + tiley;
			}
			
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
	hold{
		int maxlen = 20;
		int tilex;
		int tiley;
		int endx;
		int endy;
		int rotation;
		
		{
			lockCamera = true;
			shown = true;
			showCancel = true;
			showRotate = true;
		}
		
		public void draw(int tilex, int tiley, int endx, int endy){
			if(Vars.android && !Gdx.input.isTouched(0) && !Vars.control.showCursor()){
				return;
			}
			
			float t = Vars.tilesize;
			Block block = control.getInput().recipe.result;
			Vector2 offset = block.getPlaceOffset();
			
			process(tilex, tiley, endx, endy);
			int tx = tilex, ty = tiley, ex = endx, ey = endy;
			tilex = this.tilex; tiley = this.tiley; 
			endx = this.endx; endy = this.endy;
			float x = this.tilex * t, y = this.tiley * t, 
					x2 = this.endx * t, y2 = this.endy * t;
			
			if(x2 >= x){
				x -= block.width * t/2;
				x2 += block.width * t/2;
			}
			
			if(y2 >= y){
				y -= block.height * t/2;
				y2 += block.height * t/2;
			}
			
			x += offset.x;
			y += offset.y;
			x2 += offset.x;
			y2 += offset.y;
			
			if(tilex == endx && tiley == endy){
				cursor.draw(tilex, tiley, endx, endy);
			}else{
				Draw.thick(2f);
				Draw.color(control.getInput().cursorNear() ? Colors.get("place") : Colors.get("placeInvalid"));
				Draw.linerect(x, y, x2 - x, y2 - y);
				Draw.alpha(0.3f);
				Draw.crect("blank", x, y, x2 - x, y2 - y);

				Draw.color(Colors.get("placeInvalid"));
				
				int amount = 1;
				for(int cx = 0; cx <= Math.abs(endx - tilex); cx ++){
					for(int cy = 0; cy <= Math.abs(endy - tiley); cy ++){
						int px = tx + cx * Mathf.sign(ex - tx), 
						py = ty + cy * Mathf.sign(ey - ty);
						
						if(!control.getInput().validPlace(px, py, control.getInput().recipe.result) 
								|| !control.hasItems(control.getInput().recipe.requirements, amount)){
							Draw.linecrect(px * t + offset.x, py * t + offset.y, t*block.width, t*block.height);
						}
						amount ++;
					}
				}
				
				if(control.getInput().recipe.result.rotate){
					float cx = tx * t, cy = ty * t;
					Draw.color(Colors.get("placeRotate"));
					Tmp.v1.set(7, 0).rotate(rotation * 90);
					Draw.line(cx, cy, cx + Tmp.v1.x, cy + Tmp.v1.y);
				}
				Draw.reset();
			}
		}
		
		public void released(int tilex, int tiley, int endx, int endy){
			process(tilex, tiley, endx, endy);
			
			control.getInput().rotation = this.rotation;
			
			boolean first = true;
			for(int x = 0; x <= Math.abs(this.endx - this.tilex); x ++){
				for(int y = 0; y <= Math.abs(this.endy - this.tiley); y ++){
					if(control.getInput().tryPlaceBlock(
							tilex + x * Mathf.sign(endx - tilex), 
							tiley + y * Mathf.sign(endy - tiley), first)){
						first = false;
					}
					
				}
			}
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
				rotation = control.getInput().rotation;
			
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
	};
	public boolean lockCamera;
	public boolean pan = false;
	public boolean shown = false;
	public boolean showRotate;
	public boolean showCancel;
	public boolean delete = false;
	public boolean both = false;
	
	public void draw(int tilex, int tiley, int endx, int endy){
		
	}
	
	public void released(int tilex, int tiley, int endx, int endy){
		
	}
	
	public void tapped(int x, int y){
		
	}

	@Override
	public String toString(){
		return Bundles.get("placemode."+name().toLowerCase()+".name");
	}
}
