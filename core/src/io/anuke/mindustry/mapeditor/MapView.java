package io.anuke.mindustry.mapeditor;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Bresenham2;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;

import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.util.Tmp;

public class MapView extends Element{
	private MapEditor editor;
	private Bresenham2 br = new Bresenham2();
	
	public MapView(MapEditor editor){
		this.editor = editor;
		addListener(new InputListener(){
			int lastx, lasty;
			boolean drawing;
			
			@Override
			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				GridPoint2 p = project(x, y);
				lastx = p.x;
				lasty = p.y;
				editor.draw(p.x, p.y);
				return true;
			}
			
			@Override
			public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
				drawing = false;
			}
			
			@Override
			public void touchDragged (InputEvent event, float x, float y, int pointer) {
				GridPoint2 p = project(x, y);
				
				if(drawing){
					Array<GridPoint2> points = br.line(lastx, lasty, p.x, p.y);
					for(GridPoint2 point : points){
						editor.draw(point.x, point.y);
					}
				}
				drawing = true;
				lastx = p.x;
				lasty = p.y;
			}
		});
	}
	
	private GridPoint2 project(float x, float y){
		float size = Math.min(width, height);
		x = (x - getWidth()/2 + size/2) / size * editor.texture().getWidth();
		y = (y - getHeight()/2 + size/2) / size * editor.texture().getHeight();
		return Tmp.g1.set((int)x, editor.texture().getHeight() - 1 - (int)y);
	}
	
	@Override
	public void draw(Batch batch, float alpha){
		float size = Math.min(width, height);
		batch.draw(editor.texture(), x + width/2 - size/2, y + height/2 - size/2, size, size);
	}
}
