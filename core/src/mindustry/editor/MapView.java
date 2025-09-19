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
import mindustry.*;
import mindustry.game.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

public class MapView extends Element implements GestureListener{
    EditorTool tool = Vars.mobile ? EditorTool.zoom : EditorTool.pencil;
    private float offsetx, offsety;
    private float zoom = 1f;
    private boolean grid = false;
    private GridImage image = new GridImage(0, 0);
    private Vec2 vec = new Vec2();
    private Rect rect = new Rect();
    private Vec2[][] brushPolygons = new Vec2[MapEditor.brushSizes.length][0];
    public CopySelection selection = new CopySelection();

    boolean drawing;
    int lastx, lasty;
    int startx, starty;
    int copyStartX, copyStartY;
    float mousex, mousey;
    EditorTool lastTool;

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

                if(tool == EditorTool.copy && tool.mode == 0) {
                    startSelection();
                }

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
                    tool.touchedLine(startx, starty, p.x, p.y);
                }

                if(tool == EditorTool.copy && tool.mode == 0) {
                    endSelection();
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

        if(Core.scene.getScrollFocus() != this) return;

        zoom += Core.input.axis(Binding.zoom) / 10f * zoom;
        clampZoom();
    }

    private void clampZoom(){
        zoom = Mathf.clamp(zoom, 0.2f, 20f);
    }

    public Point2 project(float x, float y){
        float ratio = 1f / ((float)editor.width() / editor.height());
        float size = Math.min(width, height);
        float sclwidth = size * zoom;
        float sclheight = size * zoom * ratio;
        x = (x - getWidth() / 2 + sclwidth / 2 - offsetx * zoom) / sclwidth * editor.width();
        y = (y - getHeight() / 2 + sclheight / 2 - offsety * zoom) / sclheight * editor.height();

        if(editor.drawBlock.size % 2 == 0 && tool != EditorTool.eraser){
            return Tmp.p1.set((int)(x - 0.5f), (int)(y - 0.5f));
        }else{
            return Tmp.p1.set((int)x, (int)y);
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

        float scaling = zoom * Math.min(width, height) / editor.width();

        Draw.color(Pal.accent);
        Lines.stroke(Scl.scl(2f));

        if(tool == EditorTool.copy && ((selection.width > 0 && selection.height > 0) || tool.mode == 0)){
            if(tool.mode == 0){
                Point2 p = project(mousex, mousey);
                int copyEndX = p.x, copyEndY = p.y;

                Vec2 v1 = unproject(Math.min(copyStartX, copyEndX) + 1, Math.min(copyStartY, copyEndY) + 1).add(x, y);
                float sx = v1.x, sy = v1.y;
                Vec2 v2 = unproject(Math.max(copyStartX, copyEndX), Math.max(copyStartY, copyEndY)).add(x, y);
                Lines.rect(sx, sy, v2.x - sx, v2.y - sy, scaling);
            }else{
                {
                    Vec2 v1 = unproject(selection.sx + 1, selection.sy + 1).add(x, y);
                    float sx = v1.x, sy = v1.y;
                    Vec2 v2 = unproject(selection.sx + selection.width - 1, selection.sy + selection.height - 1).add(x, y);

                    Draw.color(Color.white);
                    Lines.rect(sx, sy, v2.x - sx, v2.y - sy, scaling);

                    selection.corners[0].set(sx, sy);
                    selection.corners[1].set(v2.x, sy);
                    selection.corners[2].set(v2.x, v2.y);
                    selection.corners[3].set(sx, v2.y);

                    selection.drawTransformHints(scaling);
                }

                {
                    Point2 p = project(mousex, mousey).sub(selection.width / 2, selection.height / 2);

                    Vec2 v1 = unproject(p.x + 1, p.y + 1).add(x, y);
                    float sx = v1.x, sy = v1.y;
                    int w = selection.width, h = selection.height;
                    if(editor.rotation % 2 == 1){
                        w = selection.height;
                        h = selection.width;
                    }
                    Vec2 v2 = unproject(p.x + w - 1, p.y + h - 1).add(x, y);

                    Draw.color(Pal.accent);
                    Lines.rect(sx, sy, v2.x - sx, v2.y - sy, scaling);

                    selection.corners[(0 + editor.rotation) % 4].set(sx, sy);
                    selection.corners[(1 + editor.rotation) % 4].set(v2.x, sy);
                    selection.corners[(2 + editor.rotation) % 4].set(v2.x, v2.y);
                    selection.corners[(3 + editor.rotation) % 4].set(sx, v2.y);

                    if(selection.flipY){
                        Vec2 tmp = selection.corners[0];
                        selection.corners[0] = selection.corners[3];
                        selection.corners[3] = tmp;
                        tmp = selection.corners[1];
                        selection.corners[1] = selection.corners[2];
                        selection.corners[2] = tmp;
                    }

                    if(selection.flipX){
                        Vec2 tmp = selection.corners[0];
                        selection.corners[0] = selection.corners[1];
                        selection.corners[1] = tmp;
                        tmp = selection.corners[2];
                        selection.corners[2] = selection.corners[3];
                        selection.corners[3] = tmp;
                    }

                    selection.drawTransformHints(scaling);
                }
            }
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
        Lines.stroke(Scl.scl(3f));
        Lines.rect(x, y, width, height);
        Draw.reset();

        ScissorStack.pop();
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

    public void startSelection(){
        if(tool != EditorTool.copy) return;

        tool.mode = 0;
        Point2 p = project(mousex, mousey);
        copyStartX = p.x;
        copyStartY = p.y;
    }

    public void endSelection(){
        if(tool != EditorTool.copy) return;

        tool.mode = -1;
        Point2 p = project(mousex, mousey);
        int copyEndX = p.x, copyEndY = p.y;

        selection.sx = Math.min(copyStartX, copyEndX);
        selection.sy = Math.min(copyStartY, copyEndY);
        selection.width = Math.abs(copyStartX - copyEndX) + 1;
        selection.height = Math.abs(copyStartY - copyEndY) + 1;
    }

    public void pasteSelection(){
        if(tool != EditorTool.copy) return;

        Point2 p = project(mousex, mousey).sub(selection.width / 2, selection.height / 2);
        int bdx = p.x, bdy = p.y;

        PhoneyTile[] tiles = new PhoneyTile[selection.width * selection.height];
        for(int x = 0; x < selection.width; x++){
            for(int y = 0; y < selection.height; y++){
                int sx = selection.sx + x;
                int sy = selection.sy + y;
                if (sx < 0 || sy < 0 || sx >= editor.width() || sy >= editor.height()) continue;
                tiles[x + y * selection.width] = new PhoneyTile(editor.tile(selection.sx + x, selection.sy + y));
            }
        }

        if(selection.flipX){
            for(int x = 0; x < selection.width / 2; x++){
                for(int y = 0; y < selection.height; y++){
                    PhoneyTile tmp = tiles[x + y * selection.width];
                    tiles[x + y * selection.width] = tiles[(selection.width - x - 1) + y * selection.width];
                    tiles[(selection.width - x - 1) + y * selection.width] = tmp;
                }
            }
        }

        if(selection.flipY){
            for(int x = 0; x < selection.width; x++){
                for(int y = 0; y < selection.height / 2; y++){
                    PhoneyTile tmp = tiles[x + y * selection.width];
                    tiles[x + y * selection.width] = tiles[x + (selection.height - y - 1) * selection.width];
                    tiles[x + (selection.height - y - 1) * selection.width] = tmp;
                }
            }
        }

        // apply rotation
        PhoneyTile[] rotationTmp = new PhoneyTile[selection.width * selection.height];
        int width = selection.width, height = selection.height;
        for(int round = 0; round < editor.rotation; round++){
            for(int x = 0; x < width; x++){
                for(int y = 0; y < height; y++){
                    rotationTmp[x * height + y] = tiles[y * width + x];
                }
            }

            int tmpw = width;
            width = height;
            height = tmpw;

            for(int x = 0; x < width; x++){
                for(int y = 0; y < height / 2; y++){
                    PhoneyTile tmp = rotationTmp[y * width + x];
                    rotationTmp[y * width + x] = rotationTmp[(height - 1 - y) * width + x];
                    rotationTmp[(height - 1 - y) * width + x] = tmp;
                }
            }


            PhoneyTile[] tmp = rotationTmp;
            rotationTmp = tiles;
            tiles = tmp;
        }

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                int dx = bdx + x;
                int dy = bdy + y;

                if(dx < 0 || dy < 0 || dx >= editor.width() || dy >= editor.height()) continue;

                PhoneyTile stile = tiles[x + y * width];
                if(stile == null) continue;

                Tile dtile = editor.tile(dx, dy);

                editor.addTileOp(TileOp.get(dtile.x, dtile.y, DrawOperation.opData, TileOpData.get(dtile.data, dtile.floorData, dtile.overlayData)));
                editor.addTileOp(TileOp.get(dtile.x, dtile.y, DrawOperation.opDataExtra, dtile.extraData));

                dtile.setFloor(stile.floor);
                dtile.setOverlay(stile.overlay);
                if(stile.block != null) {
                    dtile.setBlock(stile.block, stile.team, stile.rotation);
                }
            }
        }

        editor.flushOp();
    };

    class PhoneyTile{
        Floor floor;
        Floor overlay;
        Team team;
        @Nullable Block block;
        int rotation;

        public PhoneyTile(Tile tile){
            floor = tile.floor();
            overlay = tile.overlay();
            block = tile.isCenter() ? tile.block() : null;
            team = tile.team();
            rotation = tile.build == null ? 0 : tile.build.rotation;
        }
    }

    public class CopySelection{
        public int sx, sy, width, height;
        public boolean flipX, flipY;

        Vec2[] corners = {new Vec2(), new Vec2(), new Vec2(), new Vec2()};

        public void drawTransformHints(float scaling) {
            Draw.color(Color.red);
            Lines.square(selection.corners[0].x, selection.corners[0].y, 0.5f * scaling);

            Draw.color(Color.green);
            Lines.square(selection.corners[1].x, selection.corners[1].y, 0.5f * scaling);

            Draw.color(Color.blue);
            Lines.square(selection.corners[3].x, selection.corners[3].y, 0.5f * scaling);
        }
    }

}
