package io.anuke.ucore.scene.style;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.NinePatch;

import io.anuke.ucore.scene.ui.layout.Unit;

public class ScaledNinePatchDrawable extends NinePatchDrawable{
	private static float scale = Unit.dp.scl(1f);
	
	public ScaledNinePatchDrawable(NinePatch patch) {
		super(patch);
	}
	
	@Override
	public void draw (Batch batch, float x, float y, float width, float height) {
		getPatch().draw(batch, x, y, 0, 0, width/scale, height/scale, scale, scale, 0);
	}
	
	@Override
	public void setPatch(NinePatch patch){
		super.setPatch(patch);
		
		setMinWidth(patch.getTotalWidth()*scale);
		setMinHeight(patch.getTotalHeight()*scale);
	}
	
	public float getLeftWidth (){
		return patch.getPadLeft()*scale;
	}

	public float getRightWidth (){
		return patch.getPadRight()*scale;
	}

	public float getTopHeight (){
		return patch.getPadTop()*scale;
	}

	public float getBottomHeight (){
		return patch.getPadBottom()*scale;
	}
	
}
