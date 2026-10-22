package mindustry.editor;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.input.GestureDetector.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class MapView extends Element implements GestureListener{
    EditorTool tool = EditorTool.zoom;
    private float offsetx, offsety;
    private float zoom = 1f;
    private boolean grid = false;
    private GridImage image = new GridImage(0, 0);
    private Vec2 vec = new Vec2();
    private Rect rect = new Rect();
    private Vec2[][] brushPolygons = new Vec2[MapEditor.brushSizes.length][0];

    boolean drawing;
    int lastx, lasty;
    int startx, starty;
    float mousex, mousey;
    EditorTool lastTool;

    @Nullable EditorClipboard clipboard;
    int pasteX, pasteY;
    boolean selecting, holdSelect;
    int selX, selY;
    private float pasteFX, pasteFY;
    private float dragTileX, dragTileY;
    private int lastWorldW, lastWorldH;
    private final Vec2 rawVec = new Vec2();
    private final Point2 rawPoint = new Point2();

    public MapView(){

        for(int i = 0; i < MapEditor.brushSizes.length; i++){
            float size = MapEditor.brushSizes[i];
            float mod = size % 1f;
            brushPolygons[i] = Geometry.pixelCircle(size, (index, x, y) -> Mathf.dst(x, y, index - mod, index - mod) <= size - 0.5f);
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

                if(selecting) return true;

                if(clipboard != null){
                    mousex = x;
                    mousey = y;

                    if(button == KeyCode.mouseRight){
                        cancelPaste();
                    }else if(mobile){
                        Vec2 t = rawTile(x, y);
                        dragTileX = t.x;
                        dragTileY = t.y;
                    }else if(button == KeyCode.mouseLeft){
                        updatePasteOrigin();
                        pasteNow();
                    }
                    return true;
                }

                if(tool == EditorTool.copy && (mobile || button == KeyCode.mouseLeft)){
                    mousex = x;
                    mousey = y;
                    Point2 rp = rawProject(x, y);
                    selX = rp.x;
                    selY = rp.y;
                    selecting = true;
                    holdSelect = false;
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
                tool.touched(p.x, p.y);
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

                if(selecting){
                    if(!holdSelect){
                        mousex = x;
                        mousey = y;
                        finishSelection();
                    }
                    return;
                }

                if(clipboard != null) return;

                drawing = false;

                Point2 p = project(x, y);

                if(tool == EditorTool.line){
                    ui.editor.resetSaved();
                    tool.touchedLine(startx, starty, p.x, p.y);
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

                if(selecting) return;

                if(clipboard != null){
                    if(mobile){
                        Vec2 t = rawTile(x, y);
                        pasteFX += t.x - dragTileX;
                        pasteFY += t.y - dragTileY;
                        dragTileX = t.x;
                        dragTileY = t.y;
                        pasteX = Mathf.round(pasteFX);
                        pasteY = Mathf.round(pasteFY);
                    }
                    return;
                }

                Point2 p = project(x, y);

                if(drawing && tool.draggable && !(p.x == lastx && p.y == lasty)){
                    ui.editor.resetSaved();
                    Bresenham2.line(lastx, lasty, p.x, p.y, (cx, cy) -> tool.touched(cx, cy));
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

        if(tool != EditorTool.copy){
            cancelPaste();
            if(selecting && !holdSelect) selecting = false;
        }
    }

    public boolean isPasting(){
        return clipboard != null;
    }

    public void pasteNow(){
        if(clipboard != null) clipboard.paste(pasteX, pasteY);
    }

    public void cancelPaste(){
        clipboard = null;
        editor.renderer.pasteData = null;
        editor.renderer.clipboard.dispose();
    }

    public void flipClipboard(boolean x){
        if(clipboard != null) clipboard.flip(x);
    }

    public void rotateClipboard(){
        if(clipboard != null) clipboard.rotate(1);
    }

    private void finishSelection(){
        selecting = false;
        holdSelect = false;

        Point2 p = rawProject(mousex, mousey);
        EditorClipboard c = new EditorClipboard();
        if(c.capture(selX, selY, p.x, p.y)) beginPaste(c);
    }

    private void beginPaste(EditorClipboard c){
        clipboard = c;

        if(mobile){
            Vec2 t = rawTile(getWidth() / 2f, getHeight() / 2f);
            pasteFX = t.x - c.width / 2f;
            pasteFY = t.y - c.height / 2f;
            pasteX = Mathf.round(pasteFX);
            pasteY = Mathf.round(pasteFY);
        }else{
            updatePasteOrigin();
        }
    }

    private void updatePasteOrigin(){
        Point2 p = rawProject(mousex, mousey);
        pasteX = p.x - clipboard.width / 2;
        pasteY = p.y - clipboard.height / 2;
    }

    public boolean isGrid(){
        return grid;
    }

    public void setGrid(boolean grid){
        this.grid = grid;
    }

    public void center(){
        offsetx = offsety = 0;
    }

    @Override
    public void act(float delta){
        super.act(delta);

        if(Core.scene.getKeyboardFocus() == null || !Core.scene.hasField() && !Core.input.keyDown(KeyCode.controlLeft)){
            float ax = Core.input.axis(Binding.moveX);
            float ay = Core.input.axis(Binding.moveY);
            offsetx -= ax * 15 * Time.delta / zoom;
            offsety -= ay * 15 * Time.delta / zoom;
        }

        if(Core.input.keyTap(KeyCode.shiftLeft) || Core.input.keyTap(KeyCode.altLeft)){
            lastTool = tool;
            tool = EditorTool.pick;
        }

        if((Core.input.keyRelease(KeyCode.shiftLeft) || Core.input.keyRelease(KeyCode.altLeft)) && lastTool != null){
            tool = lastTool;
            lastTool = null;
        }

        if(lastWorldW != state.world.width || lastWorldH != state.world.height){
            lastWorldW = state.world.width;
            lastWorldH = state.world.height;
            cancelPaste();
            selecting = false;
        }

        if(!selecting && clipboard == null && !drawing && Core.input.keyTap(Binding.schematicSelect)
        && !Core.scene.hasField() && Core.scene.getHoverElement() == this){
            Point2 rp = rawProject(mousex, mousey);
            selX = rp.x;
            selY = rp.y;
            selecting = true;
            holdSelect = true;
        }

        if(selecting && holdSelect && Core.input.keyRelease(Binding.schematicSelect)){
            finishSelection();
        }

        if(clipboard != null){
            if(!mobile) updatePasteOrigin();

            if(!Core.scene.hasField()){
                //holding the diagonal placement key (ctrl) zooms instead of rotating
                if(Core.scene.getScrollFocus() == this && !Core.input.keyDown(Binding.diagonalPlacement)){
                    int rot = (int)Core.input.axisTap(Binding.rotate);
                    if(rot != 0) clipboard.rotate(Mathf.sign(rot));
                }
                if(Core.input.keyTap(Binding.schematicFlipX)) clipboard.flip(true);
                if(Core.input.keyTap(Binding.schematicFlipY)) clipboard.flip(false);
            }

            editor.renderer.pasteData = clipboard;
            editor.renderer.pasteX = pasteX * tilesize;
            editor.renderer.pasteY = pasteY * tilesize;
        }else{
            editor.renderer.pasteData = null;
        }

        if(Core.scene.getScrollFocus() != this) return;

        boolean rotatingClipboard = clipboard != null && !Core.input.keyDown(Binding.diagonalPlacement) && Binding.zoom.value.equals(Binding.rotate.value);
        if(!rotatingClipboard && !ui.consolefrag.shown()) zoom += Core.input.axis(Binding.zoom) / 10f * zoom;
        clampZoom();
    }

    private void clampZoom(){
        zoom = Mathf.clamp(zoom, 0.2f, 20f);
    }

    private Vec2 rawTile(float x, float y){
        float ratio = 1f / ((float)state.world.width / state.world.height);
        float size = Math.min(width, height);
        float sclwidth = size * zoom;
        float sclheight = size * zoom * ratio;
        return rawVec.set(
        (x - getWidth() / 2 + sclwidth / 2 - offsetx * zoom) / sclwidth * state.world.width,
        (y - getHeight() / 2 + sclheight / 2 - offsety * zoom) / sclheight * state.world.height
        );
    }

    /** Projects to tile coordinates without the even-size brush shift. */
    private Point2 rawProject(float x, float y){
        Vec2 t = rawTile(x, y);
        return rawPoint.set(Mathf.floor(t.x), Mathf.floor(t.y));
    }

    public Point2 project(float x, float y){
        float ratio = 1f / ((float)state.world.width / state.world.height);
        float size = Math.min(width, height);
        float sclwidth = size * zoom;
        float sclheight = size * zoom * ratio;
        x = (x - getWidth() / 2 + sclwidth / 2 - offsetx * zoom) / sclwidth * state.world.width;
        y = (y - getHeight() / 2 + sclheight / 2 - offsety * zoom) / sclheight * state.world.height;

        if(editor.drawBlock.size % 2 == 0 && tool != EditorTool.eraser){
            return Tmp.p1.set((int)(x - 0.5f), (int)(y - 0.5f));
        }else{
            return Tmp.p1.set((int)x, (int)y);
        }
    }

    private Vec2 unproject(int x, int y){
        float ratio = 1f / ((float)state.world.width / state.world.height);
        float size = Math.min(width, height);
        float sclwidth = size * zoom;
        float sclheight = size * zoom * ratio;
        float px = ((float)x / state.world.width) * sclwidth + offsetx * zoom - sclwidth / 2 + getWidth() / 2;
        float py = ((float)(y) / state.world.height) * sclheight
        + offsety * zoom - sclheight / 2 + getHeight() / 2;
        return vec.set(px, py);
    }

    @Override
    public void draw(){
        //can cause NaN
        if(state.world.width == 0 || state.world.height == 0) return;

        float ratio = 1f / ((float)state.world.width / state.world.height);
        float size = Math.min(width, height);
        float sclwidth = size * zoom;
        float sclheight = size * zoom * ratio;
        float centerx = x + width / 2 + offsetx * zoom;
        float centery = y + height / 2 + offsety * zoom;

        image.setImageSize(state.world.width, state.world.height);

        if(!ScissorStack.push(rect.set(x + Core.scene.marginLeft, y + Core.scene.marginBottom, width, height))){
            return;
        }

        Draw.color(Pal.remove);
        Lines.stroke(2f);
        Lines.rect(centerx - sclwidth / 2 - 1, centery - sclheight / 2 - 1, sclwidth + 2, sclheight + 2);
        editor.renderer.draw(centerx - sclwidth / 2 + Core.scene.marginLeft, centery - sclheight / 2 + Core.scene.marginBottom, sclwidth, sclheight);
        Draw.reset();

        if(grid){
            Draw.color(Color.gray);
            image.setBounds(centerx - sclwidth / 2, centery - sclheight / 2, sclwidth, sclheight);
            image.draw();

            Lines.stroke(2f);
            Draw.color(Pal.bulletYellowBack);
            Lines.line(centerx - sclwidth/2f, centery - sclheight/4f, centerx + sclwidth/2f, centery - sclheight/4f);
            Lines.line(centerx - sclwidth/4f, centery - sclheight/2f, centerx - sclwidth/4f, centery + sclheight/2f);
            Lines.line(centerx - sclwidth/2f, centery + sclheight/4f, centerx + sclwidth/2f, centery + sclheight/4f);
            Lines.line(centerx + sclwidth/4f, centery - sclheight/2f, centerx + sclwidth/4f, centery + sclheight/2f);

            Lines.stroke(3f);
            Draw.color(Pal.accent);
            Lines.line(centerx - sclwidth/2f, centery, centerx + sclwidth/2f, centery);
            Lines.line(centerx, centery - sclheight/2f, centerx, centery + sclheight/2f);

            Draw.reset();
        }

        int index = 0;
        for(int i = 0; i < MapEditor.brushSizes.length; i++){
            if(editor.brushSize == MapEditor.brushSizes[i]){
                index = i;
                break;
            }
        }

        float scaling = zoom * Math.min(width, height) / state.world.width;

        Draw.color(Pal.accent);
        Lines.stroke(Scl.scl(2f));

        if(selecting || clipboard != null){
            //no brush outline while selecting or pasting
        }else if((!editor.drawBlock.isMultiblock() || tool == EditorTool.eraser) && tool != EditorTool.fill){
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
                    Lines.square(v.x + scaling/2f, v.y + scaling/2f, scaling * ((editor.brushSize == 1.5f ? 1f : editor.brushSize) + 0.5f));
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
        Lines.stroke(Scl.scl(2f));

        if(selecting){
            Point2 p = rawProject(mousex, mousey);
            dashRect(Math.min(selX, p.x), Math.min(selY, p.y), Math.max(selX, p.x) + 1, Math.max(selY, p.y) + 1);
        }

        if(clipboard != null){
            dashRect(pasteX, pasteY, pasteX + clipboard.width, pasteY + clipboard.height);
        }

        Draw.color(Pal.accent);
        Lines.stroke(Scl.scl(3f));
        Lines.rect(x, y, width, height);
        Draw.reset();

        ScissorStack.pop();
    }

    private void dashRect(int tx1, int ty1, int tx2, int ty2){
        Vec2 a = unproject(tx1, ty1);
        float x1 = a.x + x, y1 = a.y + y;
        Vec2 b = unproject(tx2, ty2);
        float x2 = b.x + x, y2 = b.y + y;

        dashEdge(x1, y1, x2, y1);
        dashEdge(x2, y1, x2, y2);
        dashEdge(x2, y2, x1, y2);
        dashEdge(x1, y2, x1, y1);
    }

    private void dashEdge(float x1, float y1, float x2, float y2){
        int dashes = Math.max(1, (int)(Mathf.dst(x1, y1, x2, y2) / Scl.scl(8f)));
        Draw.color(Pal.gray);
        Lines.stroke(Scl.scl(4f));
        Lines.dashLine(x1, y1, x2, y2, dashes);
        Draw.color(Pal.accent);
        Lines.stroke(Scl.scl(2f));
        Lines.dashLine(x1, y1, x2, y2, dashes);
    }

    private boolean active(){
        return Core.scene != null && Core.scene.getKeyboardFocus() != null
        && Core.scene.getKeyboardFocus().isDescendantOf(ui.editor)
        && ui.editor.isShown() && tool == EditorTool.zoom &&
        Core.scene.getHoverElement() == this;
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
