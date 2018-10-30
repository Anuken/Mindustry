package io.anuke.ucore.scene.ui;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;

import io.anuke.ucore.core.Core;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.function.ColorListenable;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.graphics.Pixmaps;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.ui.layout.Cell;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Mathf;

public class ColorPicker extends Table{
	private static Texture hue;
	
	private Bar hbar, sbar, bbar;
	private ColorListenable changed;
	private TextField field;
	private Color color = Color.CORAL.cpy(), tmp = new Color(1, 1, 1, 1);
	
	public ColorPicker(){
		setup();
	}
	
	private void setup(){
		int w = 300, h = 50;
		float space = 20;
		
		if(hue == null)
			hue = Pixmaps.hueTexture(w/2, 1);
		
		hbar = new Bar(hue);
		sbar = new Bar();
		bbar = new Bar();
		
		add(hbar).size(w, h).padBottom(space);
		row();
		add(sbar).size(w, h).padBottom(space);
		row();
		add(bbar).size(w, h);
		row();
		
		Image image = new Image("white");
		image.update(()->{
			image.setColor(color);
		});
		
		Table c = new Table();
		c.background("button");
		c.margin(6);
		c.add(image).size(50);
		add(c).pad(10);
		
		row();
		
		Cell<TextField> cell = addField("", f->{
			if(f.length() != 6) return;
			
			try{
				Color color = Color.valueOf(f);
				this.color.set(color);
				setColor(this.color);
			}catch (Exception e){}
		});
		
		field = cell.getElement();
		
		setColor(color);
	}
	
	public void setColor(Color color){
		this.color = color;
		float[] f = Hue.RGBtoHSB(color);
		hbar.selection = f[0];
		sbar.selection = f[1];
		bbar.selection = f[2];
		updateColor();
	}
	
	private void updateColor(){
		float hue = hbar.selection;
		float sat = sbar.selection;
		float bri = bbar.selection;
		
		hbar.tint = Hue.lightness(bri);
		
		Hue.fromHSB(hue, 1f, bri, tmp);
		sbar.to.set(tmp);
		sbar.from.set(Hue.lightness(bri));
		
		Hue.fromHSB(hue, sat, 1f, tmp);
		bbar.from.set(Color.BLACK);
		bbar.to.set(tmp);
		
		Hue.fromHSB(hue, sat, bri, color);
		
		String hex = Integer.toHexString((0xFFFFFF & Color.rgb888(color)));
		int l = 6 - hex.length();
		for(int i = 0; i < l; i ++){
			hex = "0" + hex;
		}
		
		//if(DrawContext.scene.getKeyboardFocus() != field)
		field.setText(hex);
	}
	
	public void colorChanged(ColorListenable cons){
		changed = cons;
	}
	
	public Color getColor(){
		return color;
	}
	
	private class Bar extends Element{
		Texture texture;
		Color tint = Color.WHITE.cpy();
		Color from = new Color();
		Color to = new Color();
		float selection = 0.5f;
		
		public Bar(Texture texture){
			this.texture = texture;
			
			addListener(new InputListener(){
				public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {return true;}
				
				public void touchDragged (InputEvent event, float x, float y, int pointer) {
					selection = x/width;
					selection = Mathf.clamp(selection);
					updateColor();
				}
			});
		}
		
		public Bar(){
			this(null);
		}
		
		public void draw(){
			Draw.alpha(alpha);
			
			patch("button", -6);
			
			if(texture == null){
				Fill.gradient(from, to, alpha, x, y, width, height);
			}else{
				Draw.tint(tint);
				Core.batch.draw(texture, x, y, width, height);
			}
			
			float nw = 20;
			float nh = 62;
			Draw.tint(Color.WHITE);
			Draw.patch("slider-knob", x-nw/2+selection*width, y-(nh-height)/2f, nw, nh);
			Draw.color();
		}
	}
}
