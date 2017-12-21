package io.anuke.mindustry.mapeditor;

import com.badlogic.gdx.graphics.Pixmap;

public abstract class DrawOperation{
	protected Pixmap pixmap;
	
	public DrawOperation(Pixmap pixmap){
		this.pixmap = pixmap;
	}
	
	public abstract void undo();
	public abstract void redo();
}
