package io.anuke.mindustry.input;

import io.anuke.arc.Core;
import io.anuke.arc.Graphics.Cursor;
import io.anuke.arc.Graphics.Cursor.SystemCursor;
import io.anuke.arc.collection.Array;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Geometry;
import io.anuke.arc.math.geom.Point2;
import io.anuke.arc.util.Tmp;
import io.anuke.arc.util.pooling.Pool;
import io.anuke.arc.util.pooling.Pools;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.input.PlaceUtils.NormalizeDrawResult;
import io.anuke.mindustry.input.PlaceUtils.NormalizeResult;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.mindustry.input.PlaceMode.*;

public class DesktopInput extends InputHandler{
    /**Current cursor type.*/
    private Cursor cursorType = SystemCursor.arrow;

    /**Position where the player started dragging a line.*/
    private int selectX, selectY;
    /**Whether selecting mode is active.*/
    private PlaceMode mode;
    /**Animation scale for line.*/
    private float selectScale;
    /**All requests for the line mode placing.*/
    private Array<PlaceRequest> requests = new Array<>();

    public DesktopInput(Player player){
        super(player);
    }

    /**Draws a placement icon for a specific block.*/
    void drawPlace(int x, int y, Block block, int rotation, PlaceRequest prev){
        if(validPlace(x, y, block, rotation)){
            if(prev != null){
                block.getPlaceDraw(placeDraw, rotation, prev.x - x, prev.y - y, prev.rotation);
            }else{
                block.getPlaceDraw(placeDraw, rotation, 0, 0, rotation);
            }

            Draw.color();

            Draw.rect(placeDraw.region, x * tilesize + block.offset(), y * tilesize + block.offset(),
                placeDraw.region.getWidth() * selectScale * Draw.scl * placeDraw.scalex,
                placeDraw.region.getHeight() * selectScale * Draw.scl * placeDraw.scaley,
                block.rotate ? placeDraw.rotation * 90 : 0);

            Draw.color(Pal.accent);
            for(int i = 0; i < 4; i++){
                Point2 p = Geometry.d8edge[i];
                float offset = -Math.max(block.size-1, 0)/2f * tilesize;
                Draw.rect("block-select", x * tilesize + block.offset() + offset * p.x, y * tilesize + block.offset() + offset * p.y, i * 90);
            }
            Draw.color();
        }else{
            Draw.color(Pal.removeBack);
            Lines.square(x * tilesize + block.offset(), y * tilesize + block.offset() - 1, block.size * tilesize / 2f);
            Draw.color(Pal.remove);
            Lines.square(x * tilesize + block.offset(), y * tilesize + block.offset(), block.size * tilesize / 2f);
        }
    }

    @Override
    public boolean isDrawing(){
        return mode != none || block != null;
    }

    @Override
    public void drawOutlined(){
        Lines.stroke(1f);
        int cursorX = tileX(Core.input.mouseX());
        int cursorY = tileY(Core.input.mouseY());

        //draw selection(s)
        if(mode == placing && block != null){
            int i = 0;
            PlaceRequest prev = null;
            for(PlaceRequest request : requests){
                int x = request.x, y = request.y;

                if(++i >= requests.size && block.rotate){
                    Draw.color(!validPlace(x, y, block, request.rotation) ? Pal.removeBack : Pal.accentBack);
                    Draw.rect(Core.atlas.find("place-arrow"),
                    x * tilesize + block.offset(),
                    y * tilesize + block.offset() - 1,
                    Core.atlas.find("place-arrow").getWidth() * Draw.scl,
                    Core.atlas.find("place-arrow").getHeight() * Draw.scl, request.rotation * 90 - 90);

                    Draw.color(!validPlace(x, y, block, request.rotation) ? Pal.remove : Pal.accent);
                    Draw.rect(Core.atlas.find("place-arrow"),
                    x * tilesize + block.offset(),
                    y * tilesize + block.offset(),
                    Core.atlas.find("place-arrow").getWidth() * Draw.scl,
                    Core.atlas.find("place-arrow").getHeight() * Draw.scl, request.rotation * 90 - 90);
                }

                drawPlace(request.x, request.y, block, request.rotation, prev);
                prev = request;
            }

        }else if(mode == breaking){
            NormalizeDrawResult result = PlaceUtils.normalizeDrawArea(Blocks.air, selectX, selectY, cursorX, cursorY, false, maxLength, 1f);
            NormalizeResult dresult = PlaceUtils.normalizeArea(selectX, selectY, cursorX, cursorY, rotation, false, maxLength);

            for(int x = dresult.x; x <= dresult.x2; x++){
                for(int y = dresult.y; y <= dresult.y2; y++){
                    Tile tile = world.tile(x, y);
                    if(tile == null || !validBreak(tile.x, tile.y)) continue;
                    tile = tile.target();

                    Draw.color(Pal.removeBack);
                    Lines.square(tile.drawx(), tile.drawy()-1, tile.block().size * tilesize / 2f - 1);
                    Draw.color(Pal.remove);
                    Lines.square(tile.drawx(), tile.drawy(), tile.block().size * tilesize / 2f - 1);
                }
            }

            Draw.color(Pal.removeBack);
            Lines.rect(result.x, result.y - 1, result.x2 - result.x, result.y2 - result.y);
            Draw.color(Pal.remove);
            Lines.rect(result.x, result.y, result.x2 - result.x, result.y2 - result.y);
        }else if(isPlacing()){
            if(block.rotate){
                Draw.color(!validPlace(cursorX, cursorY, block, rotation) ? Pal.removeBack : Pal.accentBack);
                Draw.rect(Core.atlas.find("place-arrow"),
                    cursorX * tilesize + block.offset(),
                    cursorY * tilesize + block.offset() - 1,
                    Core.atlas.find("place-arrow").getWidth() * Draw.scl,
                    Core.atlas.find("place-arrow").getHeight() * Draw.scl, rotation * 90 - 90);

                Draw.color(!validPlace(cursorX, cursorY, block, rotation) ? Pal.remove : Pal.accent);
                Draw.rect(Core.atlas.find("place-arrow"),
                    cursorX * tilesize + block.offset(),
                    cursorY * tilesize + block.offset(),
                    Core.atlas.find("place-arrow").getWidth() * Draw.scl,
                    Core.atlas.find("place-arrow").getHeight() * Draw.scl, rotation * 90 - 90);
            }
            drawPlace(cursorX, cursorY, block, rotation, null);
            block.drawPlace(cursorX, cursorY, rotation, validPlace(cursorX, cursorY, block, rotation));
        }

        Draw.reset();
    }

    @Override
    public void update(){
        if(Net.active() && Core.input.keyTap(Binding.player_list)){
            ui.listfrag.toggle();
        }

        if(Core.input.keyRelease(Binding.select)){
            player.isShooting = false;
        }

        if(state.is(State.menu) || Core.scene.hasDialog()) return;

        //zoom and rotate things
        if(Math.abs(Core.input.axisTap(Binding.zoom)) > 0 && (Core.input.keyDown(Binding.zoom_hold))){
            renderer.scaleCamera(Core.input.axisTap(Binding.zoom));
        }

        //renderer.minimap.zoomBy(-Core.input.axisTap(Binding.zoom_minimap));

        if(player.isDead()){
            cursorType = SystemCursor.arrow;
            return;
        }

        pollInput();

        //deselect if not placing
        if(!isPlacing() && mode == placing){
            mode = none;
            requests.clear();
        }

        if(player.isShooting && !canShoot()){
            player.isShooting = false;
        }

        if(isPlacing()){
            cursorType = SystemCursor.hand;
            selectScale = Mathf.lerpDelta(selectScale, 1f, 0.2f);
        }else{
            selectScale = 0f;
        }

        rotation = Mathf.mod(rotation + (int) Core.input.axisTap(Binding.rotate), 4);

        Tile cursor = tileAt(Core.input.mouseX(), Core.input.mouseY());

        if(cursor != null){
            cursor = cursor.target();

            cursorType = cursor.block().getCursor(cursor);

            if(isPlacing()){
                cursorType = SystemCursor.hand;
            }

            if(!isPlacing() && canMine(cursor)){
                cursorType = ui.drillCursor;
            }

            if(canTapPlayer(Core.input.mouseWorld().x, Core.input.mouseWorld().y)){
                cursorType = ui.unloadCursor;
            }
        }

        if(!Core.scene.hasMouse()){
            Core.graphics.cursor(cursorType);
        }

        cursorType = SystemCursor.arrow;
    }

    void pollInput(){
        Tile selected = tileAt(Core.input.mouseX(), Core.input.mouseY());
        int cursorX = tileX(Core.input.mouseX());
        int cursorY = tileY(Core.input.mouseY());

        if(Core.input.keyTap(Binding.deselect)){
            player.setMineTile(null);
        }

        if(Core.input.keyTap(Binding.select) && !Core.scene.hasMouse()){
            if(isPlacing()){
                selectX = cursorX;
                selectY = cursorY;
                mode = placing;
                requests.add(new PlaceRequest(selectX, selectY, rotation));
            }else if(selected != null){
                //only begin shooting if there's no cursor event
                if (!tileTapped(selected) && !tryTapPlayer(Core.input.mouseWorld().x, Core.input.mouseWorld().y) && player.getPlaceQueue().size == 0 && !droppingItem &&
                        !tryBeginMine(selected) && player.getMineTile() == null && !ui.chatfrag.chatOpen()) {
                    player.isShooting = true;
                }
            }else if(!ui.chatfrag.chatOpen()){ //if it's out of bounds, shooting is just fine
                player.isShooting = true;
            }
        }else if(Core.input.keyTap(Binding.deselect) && (block != null || mode != none || player.isBuilding()) &&
        !(player.getCurrentRequest() != null && player.getCurrentRequest().breaking && Core.keybinds.get(Binding.deselect) == Core.keybinds.get(Binding.break_block))){
            if(block == null){
                player.clearBuilding();
            }

            block = null;
            mode = none;
        }else if(Core.input.keyTap(Binding.break_block) && !Core.scene.hasMouse()){
            //is recalculated because setting the mode to breaking removes potential multiblock cursor offset
            mode = breaking;
            selectX = tileX(Core.input.mouseX());
            selectY = tileY(Core.input.mouseY());
        }

        if(isPlacing() && mode == placing){
            if((cursorX != selectX || cursorY != selectY)){
                points.clear();
                outPoints.clear();
                Pool<Point2> pool = Pools.get(Point2.class, Point2::new);
                Array<Point2> out = bres.line(selectX, selectY, cursorX, cursorY, pool, outPoints);

                for(int i = 0; i < out.size; i++){
                    points.add(out.get(i));

                    if(i != out.size - 1){
                        Point2 curr = out.get(i);
                        Point2 next = out.get(i + 1);
                        //diagonal
                        if(next.x != curr.x && next.y != curr.y){
                            points.add(new Point2(next.x, curr.y));
                        }
                    }
                }

                for(Point2 point : points){
                    if(checkUnused(point.x, point.y)){
                        addRequest(point);
                        selectX = point.x;
                        selectY = point.y;
                    }

                }

                pool.freeAll(outPoints);

            }
        }

        if(Core.input.keyRelease(Binding.break_block) || Core.input.keyRelease(Binding.select)){

            if(mode == placing && block != null){ //touch up while placing, place everything in selection
                int rot = rotation;

                for(PlaceRequest req : requests){
                    rotation = req.rotation;
                    tryPlaceBlock(req.x, req.y);
                }

                rotation = rot;
            }else if(mode == breaking){ //touch up while breaking, break everything in selection
                NormalizeResult result = PlaceUtils.normalizeArea(selectX, selectY, cursorX, cursorY, rotation, false, maxLength);
                for(int x = 0; x <= Math.abs(result.x2 - result.x); x++){
                    for(int y = 0; y <= Math.abs(result.y2 - result.y); y++){
                        int wx = selectX + x * Mathf.sign(cursorX - selectX);
                        int wy = selectY + y * Mathf.sign(cursorY - selectY);

                        tryBreakBlock(wx, wy);
                    }
                }
            }

            if(selected != null){
                tryDropItems(selected.target(), Core.input.mouseWorld().x, Core.input.mouseWorld().y);
            }

            requests.clear();
            mode = none;
        }
        
    }

    boolean checkUnused(int x, int y){
        Tmp.r2.setSize(block.size * tilesize);
        Tmp.r2.setCenter(x * tilesize + block.offset(), y * tilesize + block.offset());

        for(PlaceRequest req : requests){
            Tmp.r1.setSize(block.size * tilesize);
            Tmp.r1.setCenter(req.x*tilesize + block.offset(), req.y*tilesize + block.offset());

            if(Tmp.r2.overlaps(Tmp.r1)){
                return false;
            }
        }
        return true;
    }

    void addRequest(Point2 point){
        if(!checkUnused(point.x, point.y)) return;

        PlaceRequest last = requests.peek();

        if(last.x == point.x && last.y == point.y){
            return;
        }

        int rel = Tile.relativeTo(last.x, last.y, point.x, point.y);

        if(rel != -1){
            last.rotation = rel;
            rotation = rel;
        }

        requests.add(new PlaceRequest(point.x, point.y, rotation));
    }

    @Override
    public boolean selectedBlock(){
        return isPlacing() && mode != breaking;
    }

    @Override
    public float getMouseX(){
        return Core.input.mouseX();
    }

    @Override
    public float getMouseY(){
        return Core.input.mouseY();
    }

    @Override
    public void updateController(){

        if(state.is(State.menu)){
            droppingItem = false;
        }
    }

    private class PlaceRequest{
        int x, y, rotation;

        public PlaceRequest(int x, int y, int rotation){
            this.x = x;
            this.y = y;
            this.rotation = rotation;
        }
    }

}
