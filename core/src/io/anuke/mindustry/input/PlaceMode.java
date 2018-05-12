package io.anuke.mindustry.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.ui.fragments.ToolFragment;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Translator;

import static io.anuke.mindustry.Vars.*;

public enum PlaceMode{
	cursor{
		{
			shown = true;
			lockCamera = true;
			pan = true;
		}
		
		public void draw(InputHandler input, int tilex, int tiley, int endx, int endy){
			float x = tilex * tilesize;
			float y = tiley * tilesize;
			
			boolean valid = input.validPlace(tilex, tiley, input.recipe.result) && (mobile || input.cursorNear());
			
			Vector2 offset = input.recipe.result.getPlaceOffset();

			float si = MathUtils.sin(Timers.time() / 6f) + 1.5f;

			Draw.color(valid ? Colors.get("place") : Colors.get("placeInvalid"));
			Lines.stroke(2f);
			Lines.crect(x + offset.x, y + offset.y, tilesize * input.recipe.result.size + si,
					tilesize * input.recipe.result.size + si);

			input.recipe.result.drawPlace(tilex, tiley, input.rotation, valid);

			if(input.recipe.result.rotate){

				Draw.color(Colors.get("placeRotate"));
				tr.trns(input.rotation * 90, 7, 0);
				Lines.line(x, y, x + tr.x, y + tr.y);
			}
		}
		
		public void tapped(InputHandler input, int tilex, int tiley){
			input.tryPlaceBlock(tilex, tiley, true);
		}
	},
	touch{
		{
			shown = true;
			lockCamera = false;
			showRotate = true;
			showCancel = true;
		}
		
		public void tapped(InputHandler input, int x, int y){
			input.tryPlaceBlock(x, y, true);
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
		
		public void draw(InputHandler input, int tilex, int tiley, int endx, int endy){
			Tile tile = world.tile(tilex, tiley);
			
			if(tile != null && input.validBreak(tilex, tiley)){
				if(tile.isLinked())
					tile = tile.getLinked();
				float fin = input.breaktime / tile.getBreakTime();
				
				if(mobile && input.breaktime > 0){
					Draw.color(Colors.get("breakStart"), Colors.get("break"), fin);
					Lines.poly(tile.drawx(), tile.drawy(), 25, 4 + (1f - fin) * 26);
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
		
		public void tapped(InputHandler input, int x, int y){
			input.tryDeleteBlock(x, y, true);
		}
	},
	areaDelete{
		int maxlen = debug ? 999999: 20;
		int tilex;
		int tiley;
		int endx;
		int endy;
		
		{
			shown = true;
			lockCamera = true;
			delete = true;
		}
		
		public void draw(InputHandler input, int tilex, int tiley, int endx, int endy){
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
			Lines.stroke(1f);
			for(int cx = tilex; cx <= endx; cx ++){
				for(int cy = tiley; cy <= endy; cy ++){
					Tile tile = world.tile(cx, cy);
					if(tile != null && tile.getLinked() != null)
						tile = tile.getLinked();
					if(tile != null && input.validBreak(tile.x, tile.y)){
						Lines.crect(tile.drawx(), tile.drawy(),
								tile.block().size * t, tile.block().size * t);
					}
				}
			}
			
			Lines.stroke(2f);
			Draw.color(input.cursorNear() ? Colors.get("break") : Colors.get("breakInvalid"));
			Lines.rect(x, y, x2 - x, y2 - y);
			Draw.alpha(0.3f);
			Draw.crect("blank", x, y, x2 - x, y2 - y);
			Draw.reset();
		}
		
		public void released(InputHandler input, int tilex, int tiley, int endx, int endy){
			process(tilex, tiley, endx, endy);
			tilex = this.tilex; tiley = this.tiley;
			endx = this.endx; endy = this.endy;
			
			if(mobile){
				ToolFragment t = input.frag.tool;
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
					if(input.tryDeleteBlock(cx, cy, first)){
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
		
		public void draw(InputHandler input, int tilex, int tiley, int endx, int endy){
			if(mobile && !Gdx.input.isTouched(0) && !input.isCursorVisible()){
				return;
			}

			float t = tilesize;
			Block block = input.recipe.result;
			Vector2 offset = block.getPlaceOffset();
			
			process(input, tilex, tiley, endx, endy);
			int tx = tilex, ty = tiley, ex = endx, ey = endy;
			tilex = this.tilex; tiley = this.tiley;
			endx = this.endx; endy = this.endy;
			float x = this.tilex * t, y = this.tiley * t,
					x2 = this.endx * t, y2 = this.endy * t;
			
			if(x2 >= x){
				x -= block.size * t/2;
				x2 += block.size * t/2;
			}
			
			if(y2 >= y){
				y -= block.size * t/2;
				y2 += block.size * t/2;
			}
			
			x += offset.x;
			y += offset.y;
			x2 += offset.x;
			y2 += offset.y;
			
			if(tilex == endx && tiley == endy){
				cursor.draw(input, tilex, tiley, endx, endy);
			}else{
				Lines.stroke(2f);
				Draw.color(input.cursorNear() ? "place" : "placeInvalid");
				Lines.rect(x, y, x2 - x, y2 - y);
				Draw.alpha(0.3f);
				Draw.crect("blank", x, y, x2 - x, y2 - y);

				int amount = 1;
				boolean isX = Math.abs(endx - tilex) >= Math.abs(endy - tiley);

				for(int cx = 0; cx <= Math.abs(endx - tilex); cx += (isX ? 0 : 1)){
					for(int cy = 0; cy <= Math.abs(endy - tiley); cy += (isX ? 1 : 0)){

						int px = tx + cx * Mathf.sign(ex - tx),
								py = ty + cy * Mathf.sign(ey - ty);

						//step by the block size if it's valid
						if(input.validPlace(px, py, input.recipe.result) && state.inventory.hasItems(input.recipe.requirements, amount)){

							if(isX){
								cx += block.size;
							}else{
								cy += block.size;
							}
							amount ++;
						}else{ //otherwise, step by 1 until it is valid
							if(input.cursorNear()){
								Lines.stroke(2f);
								Draw.color("placeInvalid");
								Lines.crect(
										px * t + (isX ? 0 : offset.x) + (ex < tx && isX && block.size > 1 ? t : 0) - (block.size == 3 && ex > tx && isX ? t : 0),
										py * t + (isX ? offset.y : 0) + (ey < ty && !isX && block.size > 1 ? t : 0) - (block.size == 3 && ey > ty && !isX ? t : 0),
										t*(isX ? 1 : block.size),
										t*(isX ? block.size : 1));
								Draw.color("place");
							}

							if(isX){
								cx += 1;
							}else{
								cy += 1;
							}
						}
					}
				}

				if(input.recipe.result.rotate){
					float cx = tx * t, cy = ty * t;
					Lines.stroke(2f);
					Draw.color(Colors.get("placeRotate"));
					tr.trns(rotation * 90, 7, 0);
					Lines.line(cx, cy, cx + tr.x, cy + tr.y);
				}
				Draw.reset();
			}
		}
		
		public void released(InputHandler input, int tilex, int tiley, int endx, int endy){
			process(input, tilex, tiley, endx, endy);
			
			input.rotation = this.rotation;
			
			boolean first = true;
			for(int x = 0; x <= Math.abs(this.endx - this.tilex); x ++){
				for(int y = 0; y <= Math.abs(this.endy - this.tiley); y ++){
					if(input.tryPlaceBlock(
							tilex + x * Mathf.sign(endx - tilex),
							tiley + y * Mathf.sign(endy - tiley), first)){
						first = false;
					}
					
				}
			}
		}
		
		void process(InputHandler input, int tilex, int tiley, int endx, int endy){
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
				rotation = input.rotation;
			
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

	private static final Translator tr = new Translator();
	
	public void draw(InputHandler input, int tilex, int tiley, int endx, int endy){}
	public void released(InputHandler input, int tilex, int tiley, int endx, int endy){}
	public void tapped(InputHandler input, int x, int y){}

	@Override
	public String toString(){
		return Bundles.get("placemode."+name().toLowerCase()+".name");
	}
}