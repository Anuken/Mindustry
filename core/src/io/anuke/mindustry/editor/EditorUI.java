package io.anuke.mindustry.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Scaling;

import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.graphics.Atlas;
import io.anuke.ucore.modules.SceneModule;
import io.anuke.ucore.scene.Skin;
import io.anuke.ucore.scene.builders.*;
import io.anuke.ucore.scene.style.TextureRegionDrawable;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.TextField;

public class EditorUI extends SceneModule{
	
	@Override
	public void init(){
		
		build.begin();
		new table(){{
			Image image = new Image();
			image.update(()-> image.setDrawable(new TextureRegionDrawable(new TextureRegion(Editor.control.texture))));
			image.setScaling(Scaling.fit);
			add(image).size(256 * 3);
			
			new table("button"){{
				new field(Editor.control.map, text->{
					if(Gdx.files.internal("maps/" + text + ".png").exists()){
						Editor.control.map = text;
						Editor.control.reload();
					}
				});
				row();
				for(String key : Editor.control.prefs.keys()){
					new checkbox(key, Editor.control.prefs.get(key), b->{
						 Editor.control.prefs.put(key, b);
						 Editor.control.reload();
					}).left();
					row();
				}
				get().pad(16);
			}}.end();
		}}.end();
		build.end();
	}
	
	@Override
	public void update(){
		super.update();
		if(Inputs.buttonUp(Buttons.LEFT)){
			if(!hasMouse() || !(scene.hit(Graphics.mouse().x, Graphics.mouse().y, true) instanceof TextField)){
				scene.setKeyboardFocus(null);
			}
		}
	}
	
	@Override
	protected void loadSkin(){
		skin = new Skin(Gdx.files.internal("ui/uiskin.json"), new Atlas(Gdx.files.internal("sprites/sprites.atlas")));
	}
}
