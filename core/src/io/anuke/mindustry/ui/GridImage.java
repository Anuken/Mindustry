package io.anuke.mindustry.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.Element;

public class GridImage extends Element{
	private int imageWidth, imageHeight;
	
	public GridImage(int w, int h){
		this.imageWidth = w;
		this.imageHeight = h;
	}

	public void draw(Batch batch, float alpha){
		TextureRegion blank = Draw.region("white");
		
		float xspace = (getWidth() / imageWidth);
		float yspace = (getHeight() / imageHeight);
		float s = 1f;
		
		for(int x = 0; x <= imageWidth; x ++){
			batch.draw(blank, (int)(getX() + xspace * x - s), getY() - s, 2, getHeight()+ (x == imageWidth ? 1: 0));
		}
		
		for(int y = 0; y <= imageHeight; y ++){
			batch.draw(blank, getX() - s, (int)(getY() + y * yspace - s), getWidth(), 2);
		}
	}
	
	public void setImageSize(int w, int h){
		this.imageWidth = w;
		this.imageHeight = h;
	}
}
