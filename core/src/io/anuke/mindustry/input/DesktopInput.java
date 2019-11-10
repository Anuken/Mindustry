package io.anuke.mindustry.input;

import io.anuke.arc.*;
import io.anuke.arc.Graphics.*;
import io.anuke.arc.Graphics.Cursor.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.scene.*;
import io.anuke.arc.scene.event.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.entities.traits.BuilderTrait.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.world.*;

import static io.anuke.arc.Core.scene;
import static io.anuke.mindustry.Vars.*;
import static io.anuke.mindustry.input.PlaceMode.*;

public class DesktopInput extends InputHandler{
    /** Current cursor type. */
    private Cursor cursorType = SystemCursor.arrow;
    /** Position where the player started dragging a line. */
    private int selectX, selectY, schemX, schemY;
    /** Last known line positions.*/
    private int lastLineX, lastLineY, schematicX, schematicY;
    /** Whether selecting mode is active. */
    private PlaceMode mode;
    /** Animation scale for line. */
    private float selectScale;
    /** Selected build request for movement. */
    private @Nullable BuildRequest sreq;
    /** Whether player is currently deleting removal requests. */
    private boolean deleting = false;

    @Override
    public void buildUI(Group group){
        group.fill(t -> {
            t.bottom().update(() -> t.getColor().a = Mathf.lerpDelta(t.getColor().a, player.isBuilding() ? 1f : 0f, 0.15f));
            t.visible(() -> Core.settings.getBool("hints") && selectRequests.isEmpty());
            t.touchable(() -> t.getColor().a < 0.1f ? Touchable.disabled : Touchable.childrenOnly);
            t.table(Styles.black6, b -> {
                b.defaults().left();
                b.label(() -> Core.bundle.format(!player.isBuilding ?  "resumebuilding" : "pausebuilding", Core.keybinds.get(Binding.pause_building).key.toString())).style(Styles.outlineLabel);
                b.row();
                b.add(Core.bundle.format("cancelbuilding", Core.keybinds.get(Binding.clear_building).key.toString())).style(Styles.outlineLabel);
                b.row();
                b.add(Core.bundle.format("selectschematic", Core.keybinds.get(Binding.schematic_select).key.toString())).style(Styles.outlineLabel);
            }).margin(10f);
        });

        group.fill(t -> {
            t.visible(() -> lastSchematic != null && !selectRequests.isEmpty());
            t.bottom();
            t.table(Styles.black6, b -> {
                b.defaults().left();
                b.add(Core.bundle.format("schematic.flip",
                Core.keybinds.get(Binding.schematic_flip_x).key.toString(),
                Core.keybinds.get(Binding.schematic_flip_y).key.toString())).style(Styles.outlineLabel);
                b.row();
                b.table(a -> {
                    a.addImageTextButton("$schematic.add", Icon.saveSmall, this::showSchematicSave).colspan(2).size(250f, 50f).disabled(f -> lastSchematic == null || lastSchematic.file != null);
                });
            }).margin(6f);
        });
    }

    @Override
    public void drawTop(){
        Lines.stroke(1f);
        int cursorX = tileX(Core.input.mouseX());
        int cursorY = tileY(Core.input.mouseY());

        //draw selection(s)
        if(mode == placing && block != null){
            for(int i = 0; i < lineRequests.size; i++){
                BuildRequest req = lineRequests.get(i);
                if(i == lineRequests.size - 1 && req.block.rotate){
                    drawArrow(block, req.x, req.y, req.rotation);
                }
                drawRequest(lineRequests.get(i));
            }
        }else if(mode == breaking){
            drawBreakSelection(selectX, selectY, cursorX, cursorY);
        }else if(isPlacing()){
            if(block.rotate){
                drawArrow(block, cursorX, cursorY, rotation);
            }
            Draw.color();
            drawRequest(cursorX, cursorY, block, rotation);
            block.drawPlace(cursorX, cursorY, rotation, validPlace(cursorX, cursorY, block, rotation));
        }

        if(mode == none && !isPlacing()){
            BuildRequest req = getRequest(cursorX, cursorY);
            if(req != null){
                drawSelected(req.x, req.y, req.breaking ? req.tile().block() : req.block, Pal.accent);
            }
        }

        //draw schematic requests
        for(BuildRequest request : selectRequests){
            request.animScale = 1f;
            drawRequest(request);
        }

        if(sreq != null){
            boolean valid = validPlace(sreq.x, sreq.y, sreq.block, sreq.rotation, sreq);
            if(sreq.block.rotate){
                drawArrow(sreq.block, sreq.x, sreq.y, sreq.rotation, valid);
            }

            sreq.block.drawRequest(sreq, allRequests(), valid);

            drawSelected(sreq.x, sreq.y, sreq.block, getRequest(sreq.x, sreq.y, sreq.block.size, sreq) != null ? Pal.remove : Pal.accent);
        }

        if(Core.input.keyDown(Binding.schematic_select) && !ui.chatfrag.chatOpen()){
            drawSelection(schemX, schemY, cursorX, cursorY, Vars.maxSchematicSize);
        }

        Draw.reset();
    }

    @Override
    public void update(){
        if(net.active() && Core.input.keyTap(Binding.player_list)){
            ui.listfrag.toggle();
        }

        if(Core.input.keyRelease(Binding.select)){
            player.isShooting = false;
        }

        if(!state.is(State.menu) && Core.input.keyTap(Binding.minimap) && (scene.getKeyboardFocus() == ui.minimap || !scene.hasDialog()) && !ui.chatfrag.chatOpen() && !(scene.getKeyboardFocus() instanceof TextField)){
            if(!ui.minimap.isShown()){
                ui.minimap.show();
            }else{
                ui.minimap.hide();
            }
        }

        if(state.is(State.menu) || Core.scene.hasDialog()) return;

        //zoom things
        if(Math.abs(Core.input.axisTap(Binding.zoom)) > 0 && Core.input.keyDown(Binding.zoom_hold)){
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

        if(mode != none || isPlacing()){
            selectRequests.clear();
            lastSchematic = null;
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

        if(!Core.input.keyDown(Binding.zoom_hold) && Math.abs((int)Core.input.axisTap(Binding.rotate)) > 0){

            rotation = Mathf.mod(rotation + (int)Core.input.axisTap(Binding.rotate), 4);

            if(sreq != null){
                sreq.rotation = Mathf.mod(sreq.rotation + (int)Core.input.axisTap(Binding.rotate), 4);
            }

            if(isPlacing() && mode == placing){
                updateLine(selectX, selectY);
            }else if(!selectRequests.isEmpty()){
                rotateRequests(selectRequests, (int)Core.input.axisTap(Binding.rotate));
            }
        }

        Tile cursor = tileAt(Core.input.mouseX(), Core.input.mouseY());

        if(cursor != null){
            cursor = cursor.link();

            cursorType = cursor.block().getCursor(cursor);

            if(isPlacing() || !selectRequests.isEmpty()){
                cursorType = SystemCursor.hand;
            }

            if(!isPlacing() && canMine(cursor)){
                cursorType = ui.drillCursor;
            }

            if(getRequest(cursor.x, cursor.y) != null && mode == none){
                cursorType = SystemCursor.hand;
            }

            if(canTapPlayer(Core.input.mouseWorld().x, Core.input.mouseWorld().y)){
                cursorType = ui.unloadCursor;
            }

            if(!isPlacing() && Math.abs(Core.input.axisTap(Binding.rotate)) > 0 && Core.input.keyDown(Binding.rotateplaced) && cursor.block().rotate){
                Call.rotateBlock(player, cursor, Core.input.axisTap(Binding.rotate) > 0);
            }
        }

        if(!Core.scene.hasMouse()){
            Core.graphics.cursor(cursorType);
        }

        cursorType = SystemCursor.arrow;
    }

    @Override
    public void useSchematic(Schematic schem){
        block = null;
        schematicX = tileX(getMouseX());
        schematicY = tileY(getMouseY());

        selectRequests.clear();
        selectRequests.addAll(schematics.toRequests(schem, schematicX, schematicY));
        mode = none;
    }

    @Override
    public boolean isBreaking(){
        return mode == breaking;
    }

    @Override
    public void buildPlacementUI(Table table){
        table.addImage().color(Pal.gray).height(4f).colspan(4).growX();
        table.row();
        table.left().margin(0f).defaults().size(48f).left();

        table.addImageButton(Icon.pasteSmall, Styles.clearPartiali, () -> {
            ui.schematics.show();
        });
    }

    void pollInput(){
        if(scene.getKeyboardFocus() instanceof TextField) return;

        Tile selected = tileAt(Core.input.mouseX(), Core.input.mouseY());
        int cursorX = tileX(Core.input.mouseX());
        int cursorY = tileY(Core.input.mouseY());
        int rawCursorX = world.toTile(Core.input.mouseWorld().x), rawCursorY = world.toTile(Core.input.mouseWorld().y);

        // automatically pause building if the current build queue is empty
        if(Core.settings.getBool("buildautopause") && player.isBuilding && !player.isBuilding()){
            player.isBuilding = false;
            player.buildWasAutoPaused = true;
        }

        if(!selectRequests.isEmpty()){
            int shiftX = rawCursorX - schematicX, shiftY = rawCursorY - schematicY;

            selectRequests.each(s -> {
                s.x += shiftX;
                s.y += shiftY;
            });

            schematicX += shiftX;
            schematicY += shiftY;
        }

        if(Core.input.keyTap(Binding.deselect)){
            player.setMineTile(null);
        }

        if(Core.input.keyTap(Binding.clear_building)){
            player.clearBuilding();
        }

        if(Core.input.keyTap(Binding.schematic_select) && !ui.chatfrag.chatOpen()){
            schemX = rawCursorX;
            schemY = rawCursorY;
        }

        if(Core.input.keyTap(Binding.schematic_menu) && !ui.chatfrag.chatOpen()){
            if(ui.schematics.isShown()){
                ui.schematics.hide();
            }else{
                ui.schematics.show();
            }
        }

        if(Core.input.keyTap(Binding.clear_building)){
            lastSchematic = null;
            selectRequests.clear();
        }

        if(Core.input.keyRelease(Binding.schematic_select) && !ui.chatfrag.chatOpen()){
            lastSchematic = schematics.create(schemX, schemY, rawCursorX, rawCursorY);
            useSchematic(lastSchematic);
            if(selectRequests.isEmpty()){
                lastSchematic = null;
            }
        }

        if(!selectRequests.isEmpty()){
            if(Core.input.keyTap(Binding.schematic_flip_x)){
                flipRequests(selectRequests, true);
            }

            if(Core.input.keyTap(Binding.schematic_flip_y)){
                flipRequests(selectRequests, false);
            }
        }

        if(sreq != null){
            float offset = ((sreq.block.size + 2) % 2) * tilesize / 2f;
            float x = Core.input.mouseWorld().x + offset;
            float y = Core.input.mouseWorld().y + offset;
            sreq.x = (int)(x / tilesize);
            sreq.y = (int)(y / tilesize);
        }

        if(block == null || mode != placing){
            lineRequests.clear();
        }

        if(Core.input.keyTap(Binding.pause_building)){
            player.isBuilding = !player.isBuilding;
            player.buildWasAutoPaused = false;
        }

        if((cursorX != lastLineX || cursorY != lastLineY) && isPlacing() && mode == placing){
            updateLine(selectX, selectY);
            lastLineX = cursorX;
            lastLineY = cursorY;
        }

        if(Core.input.keyTap(Binding.select) && !Core.scene.hasMouse()){
            BuildRequest req = getRequest(cursorX, cursorY);

            if(!selectRequests.isEmpty()){
                flushRequests(selectRequests);
            }else if(isPlacing()){
                selectX = cursorX;
                selectY = cursorY;
                lastLineX = cursorX;
                lastLineY = cursorY;
                mode = placing;
                updateLine(selectX, selectY);
            }else if(req != null && !req.breaking && mode == none && !req.initialized){
                sreq = req;
            }else if(req != null && req.breaking){
                deleting = true;
            }else if(selected != null){
                //only begin shooting if there's no cursor event
                if(!tileTapped(selected) && !tryTapPlayer(Core.input.mouseWorld().x, Core.input.mouseWorld().y) && (player.buildQueue().size == 0 || !player.isBuilding) && !droppingItem &&
                !tryBeginMine(selected) && player.getMineTile() == null && !ui.chatfrag.chatOpen()){
                    player.isShooting = true;
                }
            }else if(!ui.chatfrag.chatOpen()){ //if it's out of bounds, shooting is just fine
                player.isShooting = true;
            }
        }else if(Core.input.keyTap(Binding.deselect) && block != null){
            block = null;
            mode = none;
        }else if(Core.input.keyTap(Binding.break_block) && !Core.scene.hasMouse()){
            //is recalculated because setting the mode to breaking removes potential multiblock cursor offset
            deleting = false;
            mode = breaking;
            selectX = tileX(Core.input.mouseX());
            selectY = tileY(Core.input.mouseY());
        }

        if(Core.input.keyDown(Binding.select) && mode == none && !isPlacing() && deleting){
            BuildRequest req = getRequest(cursorX, cursorY);
            if(req != null && req.breaking){
                player.buildQueue().remove(req);
            }
        }else{
            deleting = false;
        }

        if(mode == placing && block != null){
            if(!overrideLineRotation && !Core.input.keyDown(Binding.diagonal_placement) && (selectX != cursorX || selectY != cursorY) && ((int) Core.input.axisTap(Binding.rotate) != 0)){
                rotation = ((int)((Angles.angle(selectX, selectY, cursorX, cursorY) + 45) / 90f)) % 4;
                overrideLineRotation = true;
            }
        }else{
            overrideLineRotation = false;
        }

        if(Core.input.keyRelease(Binding.break_block) || Core.input.keyRelease(Binding.select)){

            if(mode == placing && block != null){ //touch up while placing, place everything in selection
                flushRequests(lineRequests);
                lineRequests.clear();
                Events.fire(new LineConfirmEvent());
            }else if(mode == breaking){ //touch up while breaking, break everything in selection
                removeSelection(selectX, selectY, cursorX, cursorY);
            }

            if(selected != null){
                tryDropItems(selected.link(), Core.input.mouseWorld().x, Core.input.mouseWorld().y);
            }

            if(sreq != null){
                if(getRequest(sreq.x, sreq.y, sreq.block.size, sreq) != null){
                    player.buildQueue().remove(sreq, true);
                }
                sreq = null;
            }

            mode = none;
        }

        if(Core.input.keyTap(Binding.toggle_power_lines)){
            if(Core.settings.getInt("lasersopacity") == 0){
                Core.settings.put("lasersopacity", Core.settings.getInt("preferredlaseropacity", 100));
            }else{
                Core.settings.put("preferredlaseropacity", Core.settings.getInt("lasersopacity"));
                Core.settings.put("lasersopacity", 0);
            }
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
    public void updateState(){
        if(state.is(State.menu)){
            droppingItem = false;
            mode = none;
            block = null;
            sreq = null;
            selectRequests.clear();
        }
    }
}
