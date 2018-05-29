package io.anuke.mindustry.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Build;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
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

			float offset = input.recipe.result.offset();

			float si = MathUtils.sin(Timers.time() / 6f) + 1.5f;

			Draw.color(valid ? Palette.place : Palette.remove);
			Lines.stroke(2f);
			Lines.crect(x + offset, y + offset, tilesize * input.recipe.result.size + si,
					tilesize * input.recipe.result.size + si);

			input.recipe.result.drawPlace(tilex, tiley, input.rotation, valid);

			if(input.recipe.result.rotate){

				Draw.color(Palette.placeRotate);
				tr.trns(input.rotation * 90, 7, 0);
				Lines.line(x, y, x + tr.x, y + tr.y);
			}
		}
		
		public void tapped(InputHandler input, int tilex, int tiley){
			input.tryPlaceBlock(tilex, tiley);
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
			input.tryPlaceBlock(x, y);
		}
	},
	none{
		{
			delete = true;
			shown = true;
			both = true;
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
			input.tryDeleteBlock(x, y);
		}
	},
	areaDelete{
		int rtilex;
		int rtiley;
		int rendx;
		int rendy;
		
		{
			shown = true;
			lockCamera = true;
			delete = true;
		}
		
		public void draw(InputHandler input, int tilex, int tiley, int endx, int endy){
			float t = tilesize;
			
			process(tilex, tiley, endx, endy);
			
			tilex = this.rtilex; tiley = this.rtiley;
			endx = this.rendx; endy = this.rendy;
			float x = this.rtilex * t, y = this.rtiley * t,
					x2 = this.rendx * t, y2 = this.rendy * t;
			
			if(x2 >= x){
				x -= t/2;
				x2 += t/2;
			}
			
			if(y2 >= y){
				y -= t/2;
				y2 += t/2;
			}
			
			Draw.color(Palette.remove);
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
			Draw.color(Palette.remove);
			Lines.rect(x, y, x2 - x, y2 - y);
			Draw.alpha(0.3f);
			Draw.crect("blank", x, y, x2 - x, y2 - y);
			Draw.reset();
		}
		
		public void released(InputHandler input, int tilex, int tiley, int endx, int endy){
			process(tilex, tiley, endx, endy);
			tilex = this.rtilex; tiley = this.rtiley;
			endx = this.rendx; endy = this.rendy;

			input.player.clearBuilding();
			
			for(int cx = tilex; cx <= endx; cx ++){
				for(int cy = tiley; cy <= endy; cy ++){
					input.tryDeleteBlock(cx, cy);
				}
			}
		}
		
		void process(int tilex, int tiley, int endx, int endy){
			/*
			if(Math.abs(endx - tilex) > maxlen){
				endx = Mathf.sign(endx - tilex) * maxlen + tilex;
			}
			
			if(Math.abs(endy - tiley) > maxlen){
				endy = Mathf.sign(endy - tiley) * maxlen + tiley;
			}*/
			
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
			
			this.rendx = endx;
			this.rendy = endy;
			this.rtilex = tilex;
			this.rtiley = tiley;
		}
	},
	hold{
		int rtilex;
		int rtiley;
		int rendx;
		int rendy;
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
			float offset = block.offset();
			
			process(input, tilex, tiley, endx, endy);
			float x = rtilex * t, y = rtiley * t,
					x2 = rendx * t, y2 = rendy * t;

			if(x2 >= x){
				x -= block.size * t/2;
				x2 += block.size * t/2;
			}

			if(y2 >= y){
				y -= block.size * t/2;
				y2 += block.size * t/2;
			}

			x += offset;
			y += offset;
			x2 += offset;
			y2 += offset;
			
			if(tilex == endx && tiley == endy){
				cursor.draw(input, tilex, tiley, endx, endy);
			}else{
			    Draw.color(Palette.place);
				Lines.stroke(1f);
				Lines.rect(x, y, x2 - x, y2 - y);
				Draw.alpha(0.3f);
				Fill.crect(x, y, x2 - x, y2 - y);
				Draw.alpha(0f);

                Graphics.shader(Shaders.blockpreview, false);

                for(int py = 0; py <= Math.abs(this.rendy - this.rtiley); py += block.size){
                    for(int px = 0; px <= Math.abs(this.rendx - this.rtilex); px += block.size){

                        int wx = tilex + px * Mathf.sign(endx - tilex),
                                wy = tiley + py * Mathf.sign(endy - tiley);
                        if(!Build.validPlace(input.player.team, wx, wy, block, rotation)){
                            Draw.color(Palette.remove);
                        }else{
                            Draw.color(Palette.accent);
                        }

                        drawPreview(block, wx * t + offset, wy * t + offset);
                    }
                }

                Graphics.shader();
				Draw.reset();
			}
		}

		public void drawPreview(Block block, float x, float y){
		    for(TextureRegion region : block.getBlockIcon()){
                Shaders.blockpreview.region = region;
                Shaders.blockpreview.color.set(Palette.accent);
                Shaders.blockpreview.apply();

		        Draw.rect(region, x, y);

		        Graphics.flush();
            }
        }
		
		public void released(InputHandler input, int tilex, int tiley, int endx, int endy){
			process(input, tilex, tiley, endx, endy);
			
			input.rotation = this.rotation;
			input.player.clearBuilding();
			
			boolean first = true;
			for(int x = 0; x <= Math.abs(this.rendx - this.rtilex); x += input.recipe.result.size){
				for(int y = 0; y <= Math.abs(this.rendy - this.rtiley); y += input.recipe.result.size){
					input.tryPlaceBlock(
							tilex + x * Mathf.sign(endx - tilex),
							tiley + y * Mathf.sign(endy - tiley));
				}
			}
		}
		
		void process(InputHandler input, int tilex, int tiley, int endx, int endy){

			//todo hold shift to snap
		    /*
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
			}*/

		    int dx = endx - tilex, dy = endy - tiley;

		    if(Math.abs(dx) > Math.abs(dy)){
				if(dx >= 0){
					rotation = 0;
				}else{
					rotation = 2;
				}
			}else if(Math.abs(dx) < Math.abs(dy)){
				if(dy >= 0){
					rotation = 1;
				}else{
					rotation = 3;
				}
			}else{
				rotation = input.rotation;
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
			
			this.rendx = endx;
			this.rendy = endy;
			this.rtilex = tilex;
			this.rtiley = tiley;
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