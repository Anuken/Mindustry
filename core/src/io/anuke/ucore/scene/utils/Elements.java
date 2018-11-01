package io.anuke.ucore.scene.utils;

import static io.anuke.ucore.core.Core.skin;

import com.badlogic.gdx.graphics.Color;

import io.anuke.ucore.function.CheckListenable;
import io.anuke.ucore.function.FieldListenable;
import io.anuke.ucore.function.Listenable;
import io.anuke.ucore.scene.ui.*;

public class Elements{
	
	public static CheckBox newCheck(String text, CheckListenable listener){
		CheckBox button = new CheckBox(text);
		if(listener != null)
		button.changed(()->{
			listener.listen(button.isChecked());
		});
		return button;
	}
	
	public static TextButton newButton(String text, Listenable listener){
		TextButton button = new TextButton(text);
		if(listener != null)
			button.changed(listener);
		
		return button;
	}
	
	public static TextButton newButton(String text, String style, Listenable listener){
		TextButton button = new TextButton(text, style);
		if(listener != null)
			button.changed(listener);
		
		return button;
	}
	
	public static ImageButton newImageButton(String icon, Listenable listener){
		ImageButton button = new ImageButton(skin.getDrawable(icon));
		if(listener != null)
			button.changed(listener);
		return button;
	}
	
	public static ImageButton newImageButton(String icon, float size, Listenable listener){
		ImageButton button = new ImageButton(skin.getDrawable(icon));
		button.resizeImage(size);
		if(listener != null)
			button.changed(listener);
		return button;
	}
	
	public static ImageButton newImageButton(String style, String icon, float size, Listenable listener){
		ImageButton button = new ImageButton(icon, style);
		button.resizeImage(size);
		if(listener != null)
			button.changed(listener);
		return button;
	}
	
	public static ImageButton newImageButton(String icon, float size, Color color, Listenable listener){
		ImageButton button = new ImageButton(skin.getDrawable(icon));
		button.resizeImage(size);
		button.getImage().setColor(color);
		if(listener != null)
			button.changed(listener);
		return button;
	}
	
	public static ImageButton newToggleImageButton(String icon, float size, boolean on, CheckListenable listener){
		ImageButton button = new ImageButton(icon, "toggle");
		button.setChecked(on);
		button.resizeImage(size);
		button.clicked(()->{
			listener.listen(button.isChecked());
		});
		return button;
	}
	
	public static TextField newField(String text, FieldListenable listener){
		TextField field = new TextField(text);
		if(listener != null)
			field.changed(()->{
				listener.listen(field.getText());
			});
		
		return field;
	}
}
