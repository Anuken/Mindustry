package mindustry.input;

import arc.*;
import arc.Graphics.*;
import arc.Graphics.Cursor.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;

import static arc.Core.*;
import static mindustry.Vars.net;
import static mindustry.Vars.*;
import static mindustry.input.PlaceMode.*;

public class DesktopInput extends InputHandler{
    public Vec2 movement = new Vec2();
    /** Current cursor type. */
    public Cursor cursorType = SystemCursor.arrow;
    /** Position where the player started dragging a line. */
    public int selectX = -1, selectY = -1, schemX = -1, schemY = -1;
    /** Last known line positions.*/
    public int lastLineX, lastLineY, schematicX, schematicY;
    /** Whether selecting mode is active. */
    public PlaceMode mode;
    /** Animation scale for line. */
    public float selectScale;
    /** Selected build request for movement. */
    public @Nullable BuildPlan sreq;
    /** Whether player is currently deleting removal requests. */
    public boolean deleting = false, shouldShoot = false, panning = false;
    /** Mouse pan speed. */
    public float panScale = 0.005f, panSpeed = 4.5f, panBoostSpeed = 15f;
    /** Delta time between consecutive clicks. */
    public long selectMillis = 0;
    /** Previously selected tile. */
    public Tile prevSelected;

    boolean showHint(){
        return ui.hudfrag.shown && Core.settings.getBool("hints") && selectRequests.isEmpty() &&
            (!isBuilding && !Core.settings.getBool("buildautopause") || player.unit().isBuilding() || !player.dead() && !player.unit().spawnedByCore());
    }

    @Override
    public void buildUI(Group group){
        //building and respawn hints
        group.fill(t -> {
            t.color.a = 0f;
            t.visible(() -> (t.color.a = Mathf.lerpDelta(t.color.a, Mathf.num(showHint()), 0.15f)) > 0.001f);
            t.bottom();
            t.table(Styles.black6, b -> {
                StringBuilder str = new StringBuilder();
                b.defaults().left();
                b.label(() -> {
                    if(!showHint()) return str;
                    str.setLength(0);
                    if(!isBuilding && !Core.settings.getBool("buildautopause") && !player.unit().isBuilding()){
                        str.append(Core.bundle.format("enablebuilding", Core.keybinds.get(Binding.pause_building).key.toString()));
                    }else if(player.unit().isBuilding()){
                        str.append(Core.bundle.format(isBuilding ? "pausebuilding" : "resumebuilding", Core.keybinds.get(Binding.pause_building).key.toString()))
                            .append("\n").append(Core.bundle.format("cancelbuilding", Core.keybinds.get(Binding.clear_building).key.toString()))
                            .append("\n").append(Core.bundle.format("selectschematic", Core.keybinds.get(Binding.schematic_select).key.toString()));
                    }
                    if(!player.dead() && !player.unit().spawnedByCore()){
                        str.append(str.length() != 0 ? "\n" : "").append(Core.bundle.format("respawn", Core.keybinds.get(Binding.respawn).key.toString()));
                    }
                    return str;
                }).style(Styles.outlineLabel);
            }).margin(10f);
        });

        //schematic controls
        group.fill(t -> {
            t.visible(() -> ui.hudfrag.shown && lastSchematic != null && !selectRequests.isEmpty());
            t.bottom();
            t.table(Styles.black6, b -> {
                b.defaults().left();
                b.label(() -> Core.bundle.format("schematic.flip",
                    Core.keybinds.get(Binding.schematic_flip_x).key.toString(),
                    Core.keybinds.get(Binding.schematic_flip_y).key.toString())).style(Styles.outlineLabel).visible(() -> Core.settings.getBool("hints"));
                b.row();
                b.table(a -> {
                    a.button("@schematic.add", Icon.save, this::showSchematicSave).colspan(2).size(250f, 50f).disabled(f -> lastSchematic == null || lastSchematic.file != null);
                });
            }).margin(6f);
        });
    }

    @Override
    public void drawTop(){
        Lines.stroke(1f);
        int cursorX = tileX(Core.input.mouseX());
        int cursorY = tileY(Core.input.mouseY());

        //draw break selection
        if(mode == breaking){
            drawBreakSelection(selectX, selectY, cursorX, cursorY, !Core.input.keyDown(Binding.schematic_select) ? maxLength : Vars.maxSchematicSize);
        }

        if(Core.input.keyDown(Binding.schematic_select) && !Core.scene.hasKeyboard() && mode != breaking){
            drawSelection(schemX, schemY, cursorX, cursorY, Vars.maxSchematicSize);
        }

        Draw.reset();
    }

    @Override
    public void drawBottom(){
        int cursorX = tileX(Core.input.mouseX());
        int cursorY = tileY(Core.input.mouseY());

        //draw request being moved
        if(sreq != null){
            boolean valid = validPlace(sreq.x, sreq.y, sreq.block, sreq.rotation, sreq);
            if(sreq.block.rotate){
                drawArrow(sreq.block, sreq.x, sreq.y, sreq.rotation, valid);
            }

            sreq.block.drawPlan(sreq, allRequests(), valid);

            drawSelected(sreq.x, sreq.y, sreq.block, getRequest(sreq.x, sreq.y, sreq.block.size, sreq) != null ? Pal.remove : Pal.accent);
        }

        //draw hover request
        if(mode == none && !isPlacing()){
            BuildPlan req = getRequest(cursorX, cursorY);
            if(req != null){
                drawSelected(req.x, req.y, req.breaking ? req.tile().block() : req.block, Pal.accent);
            }
        }

        //draw schematic requests
        selectRequests.each(req -> {
            req.animScale = 1f;
            drawRequest(req);
        });

        selectRequests.each(this::drawOverRequest);

        if(player.isBuilder()){
            //draw things that may be placed soon
            if(mode == placing && block != null){
                for(int i = 0; i < lineRequests.size; i++){
                    BuildPlan req = lineRequests.get(i);
                    if(i == lineRequests.size - 1 && req.block.rotate){
                        drawArrow(block, req.x, req.y, req.rotation);
                    }
                    drawRequest(lineRequests.get(i));
                }
                lineRequests.each(this::drawOverRequest);
            }else if(isPlacing()){
                if(block.rotate && block.drawArrow){
                    drawArrow(block, cursorX, cursorY, rotation);
                }
                Draw.color();
                boolean valid = validPlace(cursorX, cursorY, block, rotation);
                drawRequest(cursorX, cursorY, block, rotation);
                block.drawPlace(cursorX, cursorY, rotation, valid);

                if(block.saveConfig){
                    Draw.mixcol(!valid ? Pal.breakInvalid : Color.white, (!valid ? 0.4f : 0.24f) + Mathf.absin(Time.globalTime, 6f, 0.28f));
                    brequest.set(cursorX, cursorY, rotation, block);
                    brequest.config = block.lastConfig;
                    block.drawRequestConfig(brequest, allRequests());
                    brequest.config = null;
                    Draw.reset();
                }
            }
        }

        Draw.reset();
    }

    @Override
    public void update(){
        super.update();

        if(net.active() && Core.input.keyTap(Binding.player_list) && (scene.getKeyboardFocus() == null || scene.getKeyboardFocus().isDescendantOf(ui.listfrag.content) || scene.getKeyboardFocus().isDescendantOf(ui.minimapfrag.elem))){
            ui.listfrag.toggle();
        }

        boolean panCam = false;
        float camSpeed = (!Core.input.keyDown(Binding.boost) ? panSpeed : panBoostSpeed) * Time.delta;

        if(input.keyDown(Binding.pan) && !scene.hasField() && !scene.hasDialog()){
            panCam = true;
            panning = true;
        }

        if((Math.abs(Core.input.axis(Binding.move_x)) > 0 || Math.abs(Core.input.axis(Binding.move_y)) > 0 || input.keyDown(Binding.mouse_move)) && (!scene.hasField())){
            panning = false;
        }

        if(((player.dead() || state.isPaused()) && !ui.chatfrag.shown()) && !scene.hasField() && !scene.hasDialog()){
            if(input.keyDown(Binding.mouse_move)){
                panCam = true;
            }

            Core.camera.position.add(Tmp.v1.setZero().add(Core.input.axis(Binding.move_x), Core.input.axis(Binding.move_y)).nor().scl(camSpeed));
        }else if(!player.dead() && !panning){
            Core.camera.position.lerpDelta(player, Core.settings.getBool("smoothcamera") ? 0.08f : 1f);
        }

        if(panCam){
            Core.camera.position.x += Mathf.clamp((Core.input.mouseX() - Core.graphics.getWidth() / 2f) * panScale, -1, 1) * camSpeed;
            Core.camera.position.y += Mathf.clamp((Core.input.mouseY() - Core.graphics.getHeight() / 2f) * panScale, -1, 1) * camSpeed;
        }

        shouldShoot = !scene.hasMouse();

        if(!scene.hasMouse()){
            if(Core.input.keyDown(Binding.control) && Core.input.keyTap(Binding.select)){
                Unit on = selectedUnit();
                var build = selectedControlBuild();
                if(on != null){
                    Call.unitControl(player, on);
                    shouldShoot = false;
                    recentRespawnTimer = 1f;
                }else if(build != null){
                    Call.buildingControlSelect(player, build);
                    recentRespawnTimer = 1f;
                }
            }
        }

        if(!player.dead() && !state.isPaused() && !scene.hasField() && !renderer.isCutscene()){
            updateMovement(player.unit());

            if(Core.input.keyTap(Binding.respawn)){
                controlledType = null;
                recentRespawnTimer = 1f;
                Call.unitClear(player);
            }
        }

        if(Core.input.keyRelease(Binding.select)){
            player.shooting = false;
        }

        if(state.isGame() && !scene.hasDialog() && !(scene.getKeyboardFocus() instanceof TextField)){
            if(Core.input.keyTap(Binding.minimap)) ui.minimapfrag.toggle();
            if(Core.input.keyTap(Binding.planet_map) && state.isCampaign()) ui.planet.toggle();
            if(Core.input.keyTap(Binding.research) && state.isCampaign()) ui.research.toggle();
        }

        if(state.isMenu() || Core.scene.hasDialog()) return;

        //zoom camera
        if((!Core.scene.hasScroll() || Core.input.keyDown(Binding.diagonal_placement)) && !ui.chatfrag.shown() && Math.abs(Core.input.axisTap(Binding.zoom)) > 0
            && !Core.input.keyDown(Binding.rotateplaced) && (Core.input.keyDown(Binding.diagonal_placement) || ((!player.isBuilder() || !isPlacing() || !block.rotate) && selectRequests.isEmpty()))){
            renderer.scaleCamera(Core.input.axisTap(Binding.zoom));
        }

        if(Core.input.keyTap(Binding.select) && !Core.scene.hasMouse()){
            Tile selected = world.tileWorld(input.mouseWorldX(), input.mouseWorldY());
            if(selected != null){
                Call.tileTap(player, selected);
            }
        }

        if(player.dead()){
            cursorType = SystemCursor.arrow;
            return;
        }

        pollInput();

        //deselect if not placing
        if(!isPlacing() && mode == placing){
            mode = none;
        }

        if(player.shooting && !canShoot()){
            player.shooting = false;
        }

        if(isPlacing() && player.isBuilder()){
            cursorType = SystemCursor.hand;
            selectScale = Mathf.lerpDelta(selectScale, 1f, 0.2f);
        }else{
            selectScale = 0f;
        }

        if(!Core.input.keyDown(Binding.diagonal_placement) && Math.abs((int)Core.input.axisTap(Binding.rotate)) > 0){
            rotation = Mathf.mod(rotation + (int)Core.input.axisTap(Binding.rotate), 4);

            if(sreq != null){
                sreq.rotation = Mathf.mod(sreq.rotation + (int)Core.input.axisTap(Binding.rotate), 4);
            }

            if(isPlacing() && mode == placing){
                updateLine(selectX, selectY);
            }else if(!selectRequests.isEmpty() && !ui.chatfrag.shown()){
                rotateRequests(selectRequests, Mathf.sign(Core.input.axisTap(Binding.rotate)));
            }
        }

        Tile cursor = tileAt(Core.input.mouseX(), Core.input.mouseY());

        if(cursor != null){
            if(cursor.build != null){
                cursorType = cursor.build.getCursor();
            }

            if((isPlacing() && player.isBuilder()) || !selectRequests.isEmpty()){
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

            if(cursor.build != null && cursor.interactable(player.team()) && !isPlacing() && Math.abs(Core.input.axisTap(Binding.rotate)) > 0 && Core.input.keyDown(Binding.rotateplaced) && cursor.block().rotate && cursor.block().quickRotate){
                Call.rotateBlock(player, cursor.build, Core.input.axisTap(Binding.rotate) > 0);
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
        table.image().color(Pal.gray).height(4f).colspan(4).growX();
        table.row();
        table.left().margin(0f).defaults().size(48f).left();

        table.button(Icon.paste, Styles.clearPartiali, () -> {
            ui.schematics.show();
        }).tooltip("@schematics");

        table.button(Icon.book, Styles.clearPartiali, () -> {
            ui.database.show();
        }).tooltip("@database");

        table.button(Icon.tree, Styles.clearPartiali, () -> {
            ui.research.show();
        }).visible(() -> state.isCampaign()).tooltip("@research");

        table.button(Icon.map, Styles.clearPartiali, () -> {
            ui.planet.show();
        }).visible(() -> state.isCampaign()).tooltip("@planetmap");
    }

    void pollInput(){
        if(scene.getKeyboardFocus() instanceof TextField) return;

        Tile selected = tileAt(Core.input.mouseX(), Core.input.mouseY());
        int cursorX = tileX(Core.input.mouseX());
        int cursorY = tileY(Core.input.mouseY());
        int rawCursorX = World.toTile(Core.input.mouseWorld().x), rawCursorY = World.toTile(Core.input.mouseWorld().y);

        //automatically pause building if the current build queue is empty
        if(Core.settings.getBool("buildautopause") && isBuilding && !player.unit().isBuilding()){
            isBuilding = false;
            buildWasAutoPaused = true;
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

        if(Core.input.keyTap(Binding.deselect) && !isPlacing()){
            player.unit().mineTile = null;
        }

        if(Core.input.keyTap(Binding.clear_building)){
            player.unit().clearBuilding();
        }

        if(Core.input.keyTap(Binding.schematic_select) && !Core.scene.hasKeyboard() && mode != breaking){
            schemX = rawCursorX;
            schemY = rawCursorY;
        }

        if(Core.input.keyTap(Binding.schematic_menu) && !Core.scene.hasKeyboard()){
            if(ui.schematics.isShown()){
                ui.schematics.hide();
            }else{
                ui.schematics.show();
            }
        }

        if(Core.input.keyTap(Binding.clear_building) || isPlacing()){
            lastSchematic = null;
            selectRequests.clear();
        }

        if(Core.input.keyRelease(Binding.schematic_select) && !Core.scene.hasKeyboard() && selectX == -1 && selectY == -1 && schemX != -1 && schemY != -1){
            lastSchematic = schematics.create(schemX, schemY, rawCursorX, rawCursorY);
            useSchematic(lastSchematic);
            if(selectRequests.isEmpty()){
                lastSchematic = null;
            }
            schemX = -1;
            schemY = -1;
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
            isBuilding = !isBuilding;
            buildWasAutoPaused = false;

            if(isBuilding){
                player.shooting = false;
            }
        }

        if((cursorX != lastLineX || cursorY != lastLineY) && isPlacing() && mode == placing){
            updateLine(selectX, selectY);
            lastLineX = cursorX;
            lastLineY = cursorY;
        }

        if(Core.input.keyTap(Binding.select) && !Core.scene.hasMouse()){
            BuildPlan req = getRequest(cursorX, cursorY);

            if(Core.input.keyDown(Binding.break_block)){
                mode = none;
            }else if(!selectRequests.isEmpty()){
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
                if(!tryTapPlayer(Core.input.mouseWorld().x, Core.input.mouseWorld().y) && !tileTapped(selected.build) && !player.unit().activelyBuilding() && !droppingItem
                    && !(tryStopMine(selected) || (!settings.getBool("doubletapmine") || selected == prevSelected && Time.timeSinceMillis(selectMillis) < 500) && tryBeginMine(selected)) && !Core.scene.hasKeyboard()){
                    player.shooting = shouldShoot;
                }
            }else if(!Core.scene.hasKeyboard()){ //if it's out of bounds, shooting is just fine
                player.shooting = shouldShoot;
            }
            selectMillis = Time.millis();
            prevSelected = selected;
        }else if(Core.input.keyTap(Binding.deselect) && isPlacing()){
            block = null;
            mode = none;
        }else if(Core.input.keyTap(Binding.deselect) && !selectRequests.isEmpty()){
            selectRequests.clear();
            lastSchematic = null;
        }else if(Core.input.keyTap(Binding.break_block) && !Core.scene.hasMouse() && player.isBuilder()){
            //is recalculated because setting the mode to breaking removes potential multiblock cursor offset
            deleting = false;
            mode = breaking;
            selectX = tileX(Core.input.mouseX());
            selectY = tileY(Core.input.mouseY());
            schemX = rawCursorX;
            schemY = rawCursorY;
        }

        if(Core.input.keyDown(Binding.select) && mode == none && !isPlacing() && deleting){
            BuildPlan req = getRequest(cursorX, cursorY);
            if(req != null && req.breaking){
                player.unit().plans().remove(req);
            }
        }else{
            deleting = false;
        }

        if(mode == placing && block != null){
            if(!overrideLineRotation && !Core.input.keyDown(Binding.diagonal_placement) && (selectX != cursorX || selectY != cursorY) && ((int)Core.input.axisTap(Binding.rotate) != 0)){
                rotation = ((int)((Angles.angle(selectX, selectY, cursorX, cursorY) + 45) / 90f)) % 4;
                overrideLineRotation = true;
            }
        }else{
            overrideLineRotation = false;
        }

        if(Core.input.keyRelease(Binding.break_block) && Core.input.keyDown(Binding.schematic_select) && mode == breaking){
            lastSchematic = schematics.create(schemX, schemY, rawCursorX, rawCursorY);
            schemX = -1;
            schemY = -1;
        }

        if(Core.input.keyRelease(Binding.break_block) || Core.input.keyRelease(Binding.select)){

            if(mode == placing && block != null){ //touch up while placing, place everything in selection
                flushRequests(lineRequests);
                lineRequests.clear();
                Events.fire(new LineConfirmEvent());
            }else if(mode == breaking){ //touch up while breaking, break everything in selection
                removeSelection(selectX, selectY, cursorX, cursorY, !Core.input.keyDown(Binding.schematic_select) ? maxLength : Vars.maxSchematicSize);
                if(lastSchematic != null){
                    useSchematic(lastSchematic);
                    lastSchematic = null;
                }
            }
            selectX = -1;
            selectY = -1;

            tryDropItems(selected == null ? null : selected.build, Core.input.mouseWorld().x, Core.input.mouseWorld().y);

            if(sreq != null){
                if(getRequest(sreq.x, sreq.y, sreq.block.size, sreq) != null){
                    player.unit().plans().remove(sreq, true);
                }
                sreq = null;
            }

            mode = none;
        }

        if(Core.input.keyTap(Binding.toggle_block_status)){
            Core.settings.put("blockstatus", !Core.settings.getBool("blockstatus"));
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
        super.updateState();

        if(state.isMenu()){
            droppingItem = false;
            mode = none;
            block = null;
            sreq = null;
            selectRequests.clear();
        }
    }

    protected void updateMovement(Unit unit){
        boolean omni = unit.type.omniMovement;

        float speed = unit.realSpeed();
        float xa = Core.input.axis(Binding.move_x);
        float ya = Core.input.axis(Binding.move_y);
        boolean boosted = (unit instanceof Mechc && unit.isFlying());

        movement.set(xa, ya).nor().scl(speed);
        if(Core.input.keyDown(Binding.mouse_move)){
            movement.add(input.mouseWorld().sub(player).scl(1f / 25f * speed)).limit(speed);
        }

        float mouseAngle = Angles.mouseAngle(unit.x, unit.y);
        boolean aimCursor = omni && player.shooting && unit.type.hasWeapons() && unit.type.faceTarget && !boosted && unit.type.rotateShooting;

        if(aimCursor){
            unit.lookAt(mouseAngle);
        }else{
            unit.lookAt(unit.prefRotation());
        }

        if(omni){
            unit.moveAt(movement);
        }else{
            unit.rotateMove(movement);
        }

        unit.aim(unit.type.faceTarget ? Core.input.mouseWorld() : Tmp.v1.trns(unit.rotation, Core.input.mouseWorld().dst(unit)).add(unit.x, unit.y));
        unit.controlWeapons(true, player.shooting && !boosted);

        player.boosting = Core.input.keyDown(Binding.boost);
        player.mouseX = unit.aimX();
        player.mouseY = unit.aimY();

        //update payload input
        if(unit instanceof Payloadc){
            if(Core.input.keyTap(Binding.pickupCargo)){
                tryPickupPayload();
            }

            if(Core.input.keyTap(Binding.dropCargo)){
                tryDropPayload();
            }
        }

        //update commander unit
        if(Core.input.keyTap(Binding.command) && unit.type.commandLimit > 0){
            Call.unitCommand(player);
        }
    }
}
