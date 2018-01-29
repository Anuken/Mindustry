package io.anuke.mindustry.mapeditor;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Disposable;
import io.anuke.ucore.graphics.Pixmaps;

public class DrawOperation implements Disposable{
	Pixmap from, to, pixmap;

	public DrawOperation(Pixmap pixmap){
		this.pixmap = pixmap;
	}

	public void add(Pixmap from,  Pixmap to) {
		this.from = from;
		this.to = to;
	}

	public void undo() {
		if(from != null) pixmap.drawPixmap(from, 0, 0);
	}

	public void redo() {
		if(to != null) pixmap.drawPixmap(to, 0, 0);
	}

	@Override
	public void dispose() {
		if(!Pixmaps.isDisposed(from))
			from.dispose();

		if(!Pixmaps.isDisposed(to))
			to.dispose();
	}

	public void disposeFrom(){
		if(from != null) from.dispose();
	}

}
