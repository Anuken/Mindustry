package io.anuke.ucore.scene.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;

public class ColorImage extends Image{
	private Color set;
	
	public ColorImage(Color set){
		super("white");
		this.set = set;
	}
	
	@Override
	public void draw(Batch batch, float alpha){
		setColor(set);
		super.draw(batch, alpha);
	}
}
