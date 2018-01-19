package io.anuke.mindustry.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.function.Listenable;
import io.anuke.ucore.scene.ui.Button;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Bundles;

public class MenuButton extends Button{
	private static boolean hasInvalid = false;
	private String text;
	private boolean added = false;
	
	public MenuButton(String text, PressGroup group, Listenable clicked){
		super("menu");
		this.text = text;
		BitmapFont font = Core.skin.getFont("title");
		for(char c : Bundles.get(text.substring(1)).toCharArray()){
			if(!font.getData().hasGlyph(c)){
				hasInvalid = true;
				break;
			}
		}
		clicked(clicked);
		group.add(this);
	}

	@Override
	public void layout() {
		super.layout();
		if(added)
			return;
		added = true;
		String style = "title";
		float scale = 4f;
		BitmapFont font = Core.skin.getFont("title");
		if(hasInvalid){
			style = "default";
			scale = Unit.dp.scl(1f);
		}
		add(text, style, scale);
	}
}
