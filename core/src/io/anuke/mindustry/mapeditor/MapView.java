package io.anuke.mindustry.mapeditor;

import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.input.GestureDetector.GestureListener;
import com.badlogic.gdx.math.Bresenham2;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.Vars;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

public class MapView extends Element implements GestureListener{
	private MapEditor editor;
	private EditorTool tool = EditorTool.pencil;
	private Bresenham2 br = new Bresenham2();
	private float offsetx, offsety;
	private float zoom = 1f;
	
	public void setTool(EditorTool tool){
		this.tool = tool;
	}
	
	public MapView(MapEditor editor){
		this.editor = editor;
		
		Inputs.addProcessor(0, new GestureDetector(20, 0.5f, 2, 0.15f, this));
		
		addListener(new InputListener(){
			int lastx, lasty;
			boolean drawing;
			
			@Override
			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				GridPoint2 p = project(x, y);
				lastx = p.x;
				lasty = p.y;
				tool.touched(editor, p.x, p.y);
				
				drawing = true;
				return true;
			}
			
			@Override
			public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
				drawing = false;
			}
			
			@Override
			public void touchDragged (InputEvent event, float x, float y, int pointer) {
				GridPoint2 p = project(x, y);
				
				if(drawing && tool == EditorTool.pencil){
					Array<GridPoint2> points = br.line(lastx, lasty, p.x, p.y);
					for(GridPoint2 point : points){
						editor.draw(point.x, point.y);
					}
				}
				lastx = p.x;
				lasty = p.y;
			}
		});
		
		addListener(new InputListener(){
			@Override
			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				return tool == EditorTool.zoom;
			}
			

			@Override
			public void touchDragged (InputEvent event, float x, float y, int pointer) {
				//offsetx += Gdx.input.getDeltaX(pointer) / zoom;
				//offsety -= Gdx.input.getDeltaY(pointer) / zoom;
			}
		});
	}
	
	@Override
	public void act(float delta){
		super.act(delta);
		
		float size = Math.min(width, height);
		offsetx = Mathf.clamp(offsetx, -size, size);
		offsety = Mathf.clamp(offsety, -size, size);
		
		if(tool != EditorTool.zoom) return;
		
		zoom += Inputs.scroll()/10f * zoom;
		clampZoom();
	}
	
	private void clampZoom(){
		zoom = Mathf.clamp(zoom, 0.4f, 6f);
	}
	
	private GridPoint2 project(float x, float y){
		float size = Math.min(width, height)*zoom;
		x = (x - getWidth()/2 + size/2 - offsetx*zoom) / size * editor.texture().getWidth();
		y = (y - getHeight()/2 + size/2 - offsety*zoom) / size * editor.texture().getHeight();
		return Tmp.g1.set((int)x, editor.texture().getHeight() - 1 - (int)y);
	}
	
	@Override
	public void draw(Batch batch, float alpha){
		float size = Math.min(width, height);
		float sclsize = size * zoom;
		float centerx = x + width/2 + offsetx * zoom;
		float centery = y + height/2 + offsety * zoom;
		
		batch.flush();
		ScissorStack.pushScissors(Tmp.r1.set(x + width/2 - size/2, y + height/2 - size/2, size, size));
		
		batch.draw(editor.texture(), centerx - sclsize/2, centery - sclsize/2, sclsize, sclsize);
		batch.flush();
		
		ScissorStack.popScissors();
		
		Draw.color(Colors.get("accent"));
		Draw.thick(Unit.dp.inPixels(3f));
		Draw.linerect(x + width/2 - size/2, y + height/2 - size/2, size, size);
		Draw.reset();
	}
	
	private boolean active(){
		return Core.scene.getKeyboardFocus().isDescendantOf(Vars.ui.getEditor()) && Vars.ui.isEditing() && tool == EditorTool.zoom;
	}

	@Override
	public boolean touchDown(float x, float y, int pointer, int button){
		return false;
	}

	@Override
	public boolean tap(float x, float y, int count, int button){
		return false;
	}

	@Override
	public boolean longPress(float x, float y){
		return false;
	}

	@Override
	public boolean fling(float velocityX, float velocityY, int button){
		return false;
	}

	@Override
	public boolean pan(float x, float y, float deltaX, float deltaY){
		if(!active()) return false;
		offsetx += deltaX / zoom;
		offsety -= deltaY / zoom;
		return false;
	}

	@Override
	public boolean panStop(float x, float y, int pointer, int button){
		return false;
	}

	@Override
	public boolean zoom(float initialDistance, float distance){
		if(!active()) return false;
		float nzoom = distance - initialDistance;
		zoom += nzoom / 2000f / Unit.dp.inPixels(1f) * zoom;
		clampZoom();
		return false;
	}

	@Override
	public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2){
		return false;
	}

	@Override
	public void pinchStop(){
		
	}
}
