package io.anuke.ucore.core;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.I18NBundle;
import io.anuke.ucore.scene.Scene;
import io.anuke.ucore.scene.Skin;
import io.anuke.ucore.scene.utils.Cursors;
import io.anuke.ucore.util.Atlas;

public class Core{
	public static OrthographicCamera camera = new OrthographicCamera();
	public static Batch batch;
	public static Atlas atlas;
	public static BitmapFont font;
	public static int cameraScale = 1;

	public static I18NBundle bundle;
	public static Scene scene;
	public static Skin skin;
	
	public static void setScene(Scene ascene, Skin askin){
		if(ascene != null) scene = ascene;
		if(askin != null) skin = askin;
	}

	/* Disposes of all resources, as well as internal resources and skin.*/
	public static void dispose(){
		Graphics.dispose();
		Inputs.dispose();
		Sounds.dispose();
		Musics.dispose();
		Timers.dispose();
		Cursors.dispose();
		
		if(batch != null){
			batch.dispose();
			batch = null;
		}
		
		if(scene != null){
			scene.dispose();
			scene = null;
		}
	
		if(atlas != null){
			atlas.dispose();
			atlas = null;
		}
	
		if(skin != null){
			skin.dispose();
			skin = null;
		}
		
		if(font != null){
			font.dispose();
			font = null;
		}
	}
}
