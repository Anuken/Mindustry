package io.anuke.mindustry.ui;

import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.g2d.Batch;

import io.anuke.ucore.core.Draw;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.layout.Unit;

public class BorderImage extends Image{
	
	@Override
	public void draw(Batch batch, float alpha){
		super.draw(batch, alpha);
		
		float scaleX = getScaleX();
		float scaleY = getScaleY();
		
		Draw.color(Colors.get("accent"));
		Draw.thick(Unit.dp.inPixels(3f));
		Draw.linerect(x + imageX, y + imageY, imageWidth * scaleX, imageHeight * scaleY);
		Draw.reset();
	}
}
