package mindustry.editor;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.input.GestureDetector;
import arc.input.GestureDetector.GestureListener;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.*;
import arc.scene.Element;
import arc.scene.event.*;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Scl;
import arc.util.*;
import mindustry.graphics.Pal;
import mindustry.input.Binding;
import mindustry.ui.GridImage;

import static mindustry.Vars.mobile;
import static mindustry.Vars.ui;

public class MapView extends Element implements GestureListener{
    private MapEditor editor;
    private EditorTool tool = EditorTool.pencil;
    private float offsetx, offsety;
    private float zoom = 1f;
    private boolean grid = false;
    private GridImage image = new GridImage(0, 0);
    private Vec2 vec = new Vec2();
    private Rect rect = new Rect();
    private Vec2[][] brushPolygons = new Vec2[MapEditor.brushSizes.length][0];

    private boolean drawing;
    private int lastx, lasty;
    private int startx, starty;
    private float mousex, mousey;
    private EditorTool lastTool;

    public MapView(MapEditor editor){
        this.editor = editor;

        for(int i = 0; i < MapEditor.brushSizes.length; i++){
            float size = MapEditor.brushSizes[i];
            brushPolygons[i] = Geometry.pixelCircle(size, (index, x, y) -> Mathf.dst(x, y, index, index) <= index - 0.5f);
        }

        Core.input.getInputProcessors().insert(0, new GestureDetector(20, 0.5f, 2, 0.15f, this));
        this.touchable = Touchable.enabled;

        Point2 firstTouch = new Point2();

        addListener(new InputListener(){

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y){
                mousex = x;
                mousey = y;
                requestScroll();

                return false;
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Element fromActor){
                requestScroll();
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(pointer != 0){
                    return false;
                }

                if(!mobile && button != KeyCode.mouseLeft && button != KeyCode.mouseMiddle && button != KeyCode.mouseRight){
                    return true;
                }
                
                if(button == KeyCode.mouseRight){
                    lastTool = tool;
                    tool = EditorTool.eraser;
                }

                if(button == KeyCode.mouseMiddle){
                    lastTool = tool;
                    tool = EditorTool.zoom;
                }

                mousex = x;
                mousey = y;

                Point2 p = project(x, y);
                lastx = p.x;
                lasty = p.y;
                startx = p.x;
                starty = p.y;
                tool.touched(editor, p.x, p.y);
                firstTouch.set(p);

                if(tool.edit){
                    ui.editor.resetSaved();
                }

                drawing = true;
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(!mobile && button != KeyCode.mouseLeft && button != KeyCode.mouseMiddle && button != KeyCode.mouseRight){
                    return;
                }

                drawing = false;

                Point2 p = project(x, y);

                if(tool == EditorTool.line){
                    ui.editor.resetSaved();
                    tool.touchedLine(editor, startx, starty, p.x, p.y);
                }

                editor.flushOp();

                if((button == KeyCode.mouseMiddle || button == KeyCode.mouseRight) && lastTool != null){
                    tool = lastTool;
                    lastTool = null;
                }

            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                mousex = x;
                mousey = y;

                Point2 p = project(x, y);

                if(drawing && tool.draggable && !(p.x == lastx && p.y == lasty)){
                    ui.editor.resetSaved();
                    Bresenham2.line(lastx, lasty, p.x, p.y, (cx, cy) -> tool.touched(editor, cx, cy));
                }

                if(tool == EditorTool.line && tool.mode == 1){
                    if(Math.abs(p.x - firstTouch.x) > Math.abs(p.y - firstTouch.y)){
                        lastx = p.x;
                        lasty = firstTouch.y;
                    }else{
                        lastx = firstTouch.x;
                        lasty = p.y;
                    }
                }else{
                    lastx = p.x;
                    lasty = p.y;
                }
            }
        });
    }

    public EditorTool getTool(){
        return tool;
    }

    public void setTool(EditorTool tool){
        this.tool = tool;
    }

    public boolean isGrid(){
        return grid;
    }

    public void setGrid(boolean grid){
        this.grid = grid;
    }

    @Override
    public void act(float delta){
        super.act(delta);

        if(Core.scene.getKeyboardFocus() == null || !(Core.scene.getKeyboardFocus() instanceof TextField) && !Core.input.keyDown(KeyCode.controlLeft)){
            float ax = Core.input.axis(Binding.move_x);
            float ay = Core.input.axis(Binding.move_y);
            offsetx -= ax * 15f / zoom;
            offsety -= ay * 15f / zoom;
        }

        if(Core.input.keyTap(KeyCode.shiftLeft)){
            lastTool = tool;
            tool = EditorTool.pick;
        }

        if(Core.input.keyRelease(KeyCode.shiftLeft) && lastTool != null){
            tool = lastTool;
            lastTool = null;
        }

        if(Core.scene.getScrollFocus() != this) return;

        zoom += Core.input.axis(KeyCode.scroll) / 10f * zoom;
        clampZoom();
    }

    private void clampZoom(){
        zoom = Mathf.clamp(zoom, 0.2f, 20f);
    }

    private Point2 project(float x, float y){
        float ratio = 1f / ((float)editor.width() / editor.height());
        float size = Math.min(width, height);
        float sclwidth = size * zoom;
        float sclheight = size * zoom * ratio;
        x = (x - getWidth() / 2 + sclwidth / 2 - offsetx * zoom) / sclwidth * editor.width();
        y = (y - getHeight() / 2 + sclheight / 2 - offsety * zoom) / sclheight * editor.height();

        if(editor.drawBlock.size % 2 == 0 && tool != EditorTool.eraser){
            return Tmp.g1.set((int)(x - 0.5f), (int)(y - 0.5f));
        }else{
            return Tmp.g1.set((int)x, (int)y);
        }
    }

    private Vec2 unproject(int x, int y){
        float ratio = 1f / ((float)editor.width() / editor.height());
        float size = Math.min(width, height);
        float sclwidth = size * zoom;
        float sclheight = size * zoom * ratio;
        float px = ((float)x / editor.width()) * sclwidth + offsetx * zoom - sclwidth / 2 + getWidth() / 2;
        float py = ((float)(y) / editor.height()) * sclheight
        + offsety * zoom - sclheight / 2 + getHeight() / 2;
        return vec.set(px, py);
    }

    @Override
    public void draw(){
        float ratio = 1f / ((float)editor.width() / editor.height());
        float size = Math.min(width, height);
        float sclwidth = size * zoom;
        float sclheight = size * zoom * ratio;
        float centerx = x + width / 2 + offsetx * zoom;
        float centery = y + height / 2 + offsety * zoom;

        image.setImageSize(editor.width(), editor.height());

        if(!ScissorStack.push(rect.set(x, y, width, height))){
            return;
        }

        Draw.color(Pal.remove);
        Lines.stroke(2f);
        Lines.rect(centerx - sclwidth / 2 - 1, centery - sclheight / 2 - 1, sclwidth + 2, sclheight + 2);
        editor.renderer().draw(centerx - sclwidth / 2, centery - sclheight / 2, sclwidth, sclheight);
        Draw.reset();

        if(grid){
            Draw.color(Color.gray);
            image.setBounds(centerx - sclwidth / 2, centery - sclheight / 2, sclwidth, sclheight);
            image.draw();
            Draw.color();
        }

        int index = 0;
        for(int i = 0; i < MapEditor.brushSizes.length; i++){
            if(editor.brushSize == MapEditor.brushSizes[i]){
                index = i;
                break;
            }
        }

        float scaling = zoom * Math.min(width, height) / editor.width();

        Draw.color(Pal.accent);
        Lines.stroke(Scl.scl(2f));

        if((!editor.drawBlock.isMultiblock() || tool == EditorTool.eraser) && tool != EditorTool.fill){
            if(tool == EditorTool.line && drawing){
                Vec2 v1 = unproject(startx, starty).add(x, y);
                float sx = v1.x, sy = v1.y;
                Vec2 v2 = unproject(lastx, lasty).add(x, y);

                Lines.poly(brushPolygons[index], sx, sy, scaling);
                Lines.poly(brushPolygons[index], v2.x, v2.y, scaling);
            }

            if((tool.edit || (tool == EditorTool.line && !drawing)) && (!mobile || drawing)){
                Point2 p = project(mousex, mousey);
                Vec2 v = unproject(p.x, p.y).add(x, y);

                //pencil square outline
                if(tool == EditorTool.pencil && tool.mode == 1){
                    Lines.square(v.x + scaling/2f, v.y + scaling/2f, scaling * (editor.brushSize + 0.5f));
                }else{
                    Lines.poly(brushPolygons[index], v.x, v.y, scaling);
                }
            }
        }else{
            if((tool.edit || tool == EditorTool.line) && (!mobile || drawing)){
                Point2 p = project(mousex, mousey);
                Vec2 v = unproject(p.x, p.y).add(x, y);
                float offset = (editor.drawBlock.size % 2 == 0 ? scaling / 2f : 0f);
                Lines.square(
                v.x + scaling / 2f + offset,
                v.y + scaling / 2f + offset,
                scaling * editor.drawBlock.size / 2f);
            }
        }

        Draw.color(Pal.accent);
        Lines.stroke(Scl.scl(3f));
        Lines.rect(x, y, width, height);
        Draw.reset();

        ScissorStack.pop();
    }

    private boolean active(){
        return Core.scene.getKeyboardFocus() != null
        && Core.scene.getKeyboardFocus().isDescendantOf(ui.editor)
        && ui.editor.isShown() && tool == EditorTool.zoom &&
        Core.scene.hit(Core.input.mouse().x, Core.input.mouse().y, true) == this;
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY){
        if(!active()) return false;
        offsetx += deltaX / zoom;
        offsety += deltaY / zoom;
        return false;
    }

    @Override
    public boolean zoom(float initialDistance, float distance){
        if(!active()) return false;
        float nzoom = distance - initialDistance;
        zoom += nzoom / 10000f / Scl.scl(1f) * zoom;
        clampZoom();
        return false;
    }

    @Override
    public boolean pinch(Vec2 initialPointer1, Vec2 initialPointer2, Vec2 pointer1, Vec2 pointer2){
        return false;
    }

    @Override
    public void pinchStop(){

    }
}
