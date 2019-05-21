package io.anuke.mindustry.input;

import io.anuke.arc.Core;
import io.anuke.arc.Graphics.Cursor;
import io.anuke.arc.Graphics.Cursor.SystemCursor;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Geometry;
import io.anuke.arc.math.geom.Point2;
import io.anuke.arc.scene.ui.TextField;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.input.PlaceUtils.NormalizeDrawResult;
import io.anuke.mindustry.input.PlaceUtils.NormalizeResult;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

import static io.anuke.arc.Core.scene;
import static io.anuke.mindustry.Vars.*;
import static io.anuke.mindustry.input.PlaceMode.*;

public class DesktopInput extends InputHandler{
    /** Current cursor type. */
    private Cursor cursorType = SystemCursor.arrow;

    /** Position where the player started dragging a line. */
    private int selectX, selectY;
    /** Whether selecting mode is active. */
    private PlaceMode mode;
    /** Animation scale for line. */
    private float selectScale;

    private int prevX, prevY, prevRotation;

    /** Draws a placement icon for a specific block. */
    void drawPlace(int x, int y, Block block, int rotation, int prevX, int prevY, int prevRotation){
        if(validPlace(x, y, block, rotation)){
            block.getPlaceDraw(placeDraw, rotation, prevX, prevY, prevRotation);

            Draw.color();
            Draw.rect(placeDraw.region, x * tilesize + block.offset(), y * tilesize + block.offset(),
            placeDraw.region.getWidth() * selectScale * Draw.scl * placeDraw.scalex,
            placeDraw.region.getHeight() * selectScale * Draw.scl * placeDraw.scaley,
            block.rotate ? placeDraw.rotation * 90 : 0);

            Draw.color(Pal.accent);
            for(int i = 0; i < 4; i++){
                Point2 p = Geometry.d8edge[i];
                float offset = -Math.max(block.size - 1, 0) / 2f * tilesize;
                if(i % 2 == 0)
                    Draw.rect("block-select", x * tilesize + block.offset() + offset * p.x, y * tilesize + block.offset() + offset * p.y, i * 90);
            }
            Draw.color();
        }else{
            Draw.color(Pal.removeBack);
            Lines.square(x * tilesize + block.offset(), y * tilesize + block.offset() - 1, block.size * tilesize / 2f - 1);
            Draw.color(Pal.remove);
            Lines.square(x * tilesize + block.offset(), y * tilesize + block.offset(), block.size * tilesize / 2f - 1);
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
            prevX = selectX;
            prevY = selectY;
            prevRotation = rotation;

            iterateLine(selectX, selectY, cursorX, cursorY, l -> {
                if(l.last && block.rotate){
                    drawArrow(block, l.x, l.y, l.rotation);
                }
                drawPlace(l.x, l.y, block, l.rotation, prevX - l.x, prevY - l.y, prevRotation);

                prevX = l.x;
                prevY = l.y;
                prevRotation = l.rotation;
            });

        }else if(mode == breaking){
            NormalizeDrawResult result = PlaceUtils.normalizeDrawArea(Blocks.air, selectX, selectY, cursorX, cursorY, false, maxLength, 1f);
            NormalizeResult dresult = PlaceUtils.normalizeArea(selectX, selectY, cursorX, cursorY, rotation, false, maxLength);

            for(int x = dresult.x; x <= dresult.x2; x++){
                for(int y = dresult.y; y <= dresult.y2; y++){
                    Tile tile = world.ltile(x, y);
                    if(tile == null || !validBreak(tile.x, tile.y)) continue;

                    Draw.color(Pal.removeBack);
                    Lines.square(tile.drawx(), tile.drawy() - 1, tile.block().size * tilesize / 2f - 1);
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
                drawArrow(block, cursorX, cursorY, rotation);
            }
            drawPlace(cursorX, cursorY, block, rotation, cursorX, cursorY, rotation);
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

        if(!state.is(State.menu) && Core.input.keyTap(Binding.minimap) && !ui.chatfrag.chatOpen() && !(scene.getKeyboardFocus() instanceof TextField)){
            if(!ui.minimap.isShown()){
                ui.minimap.show();
            }else{
                ui.minimap.hide();
            }
        }

        if(state.is(State.menu) || Core.scene.hasDialog()) return;

        //zoom and rotate things
        if(Math.abs(Core.input.axisTap(Binding.zoom)) > 0 && (Core.input.keyDown(Binding.zoom_hold))){
            renderer.scaleCamera(Core.input.axisTap(Binding.zoom));
        }

        if(player.isDead()){
            cursorType = SystemCursor.arrow;
            return;
        }

        pollInput();

        //deselect if not placing
        if(!isPlacing() && mode == placing){
            mode = none;
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

        rotation = Mathf.mod(rotation + (int)Core.input.axisTap(Binding.rotate), 4);

        Tile cursor = tileAt(Core.input.mouseX(), Core.input.mouseY());

        if(cursor != null){
            cursor = cursor.link();

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
            }else if(selected != null){
                //only begin shooting if there's no cursor event
                if(!tileTapped(selected) && !tryTapPlayer(Core.input.mouseWorld().x, Core.input.mouseWorld().y) && player.getPlaceQueue().size == 0 && !droppingItem &&
                !tryBeginMine(selected) && player.getMineTile() == null && !ui.chatfrag.chatOpen()){
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

        if(Core.input.keyRelease(Binding.break_block) || Core.input.keyRelease(Binding.select)){

            if(mode == placing && block != null){ //touch up while placing, place everything in selection
                iterateLine(selectX, selectY, cursorX, cursorY, l -> {
                    rotation = l.rotation;
                    tryPlaceBlock(l.x, l.y);
                });
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
                tryDropItems(selected.link(), Core.input.mouseWorld().x, Core.input.mouseWorld().y);
            }

            mode = none;
        }
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
            mode = none;
            block = null;
        }
    }
}
