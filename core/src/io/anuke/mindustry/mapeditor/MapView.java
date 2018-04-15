package io.anuke.mindustry.mapeditor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.input.GestureDetector.GestureListener;
import com.badlogic.gdx.math.Bresenham2;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.ui.GridImage;
import io.anuke.mindustry.world.ColorMapper;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.graphics.Pixmaps;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.TextField;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Input;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

import static io.anuke.mindustry.Vars.ui;

public class MapView extends Element implements GestureListener{
	private MapEditor editor;
	private EditorTool tool = EditorTool.pencil;
	private OperationStack stack = new OperationStack();
	private DrawOperation op;
	private Pixmap current;
	private Bresenham2 br = new Bresenham2();
	private boolean updated = false;
	private float offsetx, offsety;
	private float zoom = 1f;
	private boolean grid = false;
	private GridImage image = new GridImage(0, 0);
	private Vector2 vec = new Vector2();
	private Rectangle rect = new Rectangle();

	private boolean drawing;
	private int lastx, lasty;
	private int startx, starty;

	public void setTool(EditorTool tool){
		this.tool = tool;
	}

	public EditorTool getTool() {
		return tool;
	}

	public void clearStack(){
		stack.clear();
		current = null;
	}

	public OperationStack getStack() {
		return stack;
	}

	public void setGrid(boolean grid) {
		this.grid = grid;
	}

	public boolean isGrid() {
		return grid;
	}

	public void push(Pixmap previous, Pixmap add){
		DrawOperation op = new DrawOperation(editor.pixmap());
		op.add(previous, add);
		stack.add(op);
		this.current = add;
	}

	public void undo(){
		if(stack.canUndo()){
			stack.undo();
			editor.updateTexture();
		}
	}

	public void redo(){
		if(stack.canRedo()){
			stack.redo();
			editor.updateTexture();
		}
	}

	public MapView(MapEditor editor){
		this.editor = editor;
		
		Inputs.addProcessor(0, new GestureDetector(20, 0.5f, 2, 0.15f, this));
		setTouchable(Touchable.enabled);
		
		addListener(new InputListener(){
			
			@Override
			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				if(pointer != 0){
					return false;
				}

				if(current == null){
					current = Pixmaps.copy(editor.pixmap());
				}
				updated = false;

				GridPoint2 p = project(x, y);
				lastx = p.x;
				lasty = p.y;
				startx = p.x;
				starty = p.y;
				tool.touched(editor, p.x, p.y);
				
				if(tool.edit){
					updated = true;
					ui.editor.resetSaved();
				}

				op = new DrawOperation(editor.pixmap());

				drawing = true;
				return true;
			}
			
			@Override
			public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
				drawing = false;

				GridPoint2 p = project(x, y);

				if(tool == EditorTool.line){
					ui.editor.resetSaved();
					Array<GridPoint2> points = br.line(startx, starty, p.x, p.y);
					for(GridPoint2 point : points){
						editor.draw(point.x, point.y);
					}
					updated = true;
				}

				if(updated){
					if(op == null) op = new DrawOperation(editor.pixmap());
					Pixmap next = Pixmaps.copy(editor.pixmap());
					op.add(current, next);
					current = null;
					stack.add(op);
					op = null;
				}
			}
			
			@Override
			public void touchDragged (InputEvent event, float x, float y, int pointer) {
				GridPoint2 p = project(x, y);
				
				if(drawing && tool == EditorTool.pencil){
					ui.editor.resetSaved();
					Array<GridPoint2> points = br.line(lastx, lasty, p.x, p.y);
					for(GridPoint2 point : points){
						editor.draw(point.x, point.y);
					}
					updated = true;
				}
				lastx = p.x;
				lasty = p.y;
			}
		});
	}
	
	@Override
	public void act(float delta){
		super.act(delta);

		if(Core.scene.getKeyboardFocus() == null || !(Core.scene.getKeyboardFocus() instanceof TextField) &&
				!Inputs.keyDown(Input.CONTROL_LEFT)) {
			float ax = Inputs.getAxis("move_x");
			float ay = Inputs.getAxis("move_y");
			offsetx -= ax * 15f / zoom;
			offsety -= ay * 15f / zoom;
		}

		if(ui.editor.hasPane()) return;
		
		zoom += Inputs.scroll()/10f * zoom;
		clampZoom();
	}
	
	private void clampZoom(){
		zoom = Mathf.clamp(zoom, 0.2f, 12f);
	}
	
	private GridPoint2 project(float x, float y){
		float ratio = 1f / ((float)editor.pixmap().getWidth() / editor.pixmap().getHeight());
		float size = Math.min(width, height);
		float sclwidth = size * zoom;
		float sclheight = size * zoom * ratio;
		x = (x - getWidth()/2 + sclwidth/2 - offsetx*zoom) / sclwidth * editor.texture().getWidth();
		y = (y - getHeight()/2 + sclheight/2 - offsety*zoom) / sclheight * editor.texture().getHeight();
		return Tmp.g1.set((int)x, editor.texture().getHeight() - 1 - (int)y);
	}

	private Vector2 unproject(int x, int y){
		float ratio = 1f / ((float)editor.pixmap().getWidth() / editor.pixmap().getHeight());
		float size = Math.min(width, height);
		float sclwidth = size * zoom;
		float sclheight = size * zoom * ratio;
		float px = ((float)x / editor.texture().getWidth()) * sclwidth + offsetx*zoom - sclwidth/2 + getWidth()/2;
		float py = (float)((float)(editor.texture().getHeight() - 1 -  y) / editor.texture().getHeight()) * sclheight
				+ offsety*zoom - sclheight/2 + getHeight()/2;
		return vec.set(px, py);
	}

	@Override
	public void draw(Batch batch, float alpha){
		float ratio = 1f / ((float)editor.pixmap().getWidth() / editor.pixmap().getHeight());
		float size = Math.min(width, height);
		float sclwidth = size * zoom;
		float sclheight = size * zoom * ratio;
		float centerx = x + width/2 + offsetx * zoom;
		float centery = y + height/2 + offsety * zoom;

		image.setImageSize(editor.pixmap().getWidth(), editor.pixmap().getHeight());
		
		batch.flush();
		boolean pop = ScissorStack.pushScissors(rect.set(x + width/2 - size/2, y + height/2 - size/2, size, size));
		
		batch.draw(editor.texture(), centerx - sclwidth/2, centery - sclheight/2, sclwidth, sclheight);

		if(grid){
			Draw.color(Color.GRAY);
			image.setBounds(centerx - sclwidth/2, centery - sclheight/2, sclwidth, sclheight);
			image.draw(batch, alpha);
			Draw.color();
		}

		if(tool == EditorTool.line && drawing){
			Vector2 v1 = unproject(startx, starty).add(x, y);
			float sx = v1.x, sy = v1.y;
			Vector2 v2 = unproject(lastx, lasty).add(x, y);

			Draw.color(Tmp.c1.set(ColorMapper.getColor(editor.getDrawBlock())));
			Lines.stroke(Unit.dp.scl(3f * zoom));
			Lines.line(sx, sy, v2.x, v2.y);

			Lines.poly(sx, sy, 40, editor.getBrushSize() * zoom * 3);

            Lines.poly(v2.x, v2.y, 40, editor.getBrushSize() * zoom * 3);
		}

		batch.flush();
		
		if(pop) ScissorStack.popScissors();
		
		Draw.color(Colors.get("accent"));
		Lines.stroke(Unit.dp.scl(3f));
		Lines.rect(x + width/2 - size/2, y + height/2 - size/2, size, size);
		Draw.reset();
	}
	
	private boolean active(){
		return Core.scene.getKeyboardFocus() != null 
				&& Core.scene.getKeyboardFocus().isDescendantOf(ui.editor)
				&& ui.editor.isShown() && tool == EditorTool.zoom &&
				Core.scene.hit(Graphics.mouse().x, Graphics.mouse().y, true) == this;
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
		zoom += nzoom / 10000f / Unit.dp.scl(1f) * zoom;
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
