package io.anuke.mindustry.ui;

import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.layout.Unit;

public class BorderImage extends Image{
	private float thickness = 3f;
	
	public BorderImage(){}
	
	public BorderImage(Texture texture){
		super(texture);
	}
	
	public BorderImage(Texture texture, float thick){
		super(texture);
		thickness = thick;
	}

	public BorderImage(TextureRegion region, float thick){
		super(region);
		thickness = thick;
	}
	
	@Override
	public void draw(Batch batch, float alpha){
		super.draw(batch, alpha);
		
		float scaleX = getScaleX();
		float scaleY = getScaleY();
		
		Draw.color(Colors.get("accent"));
		Lines.stroke(Unit.dp.scl(thickness));
		Lines.rect(x + imageX, y + imageY, imageWidth * scaleX, imageHeight * scaleY);
		Draw.reset();
	}
}
