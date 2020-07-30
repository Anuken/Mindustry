package mindustry.input;

import arc.*;
import arc.Graphics.*;
import arc.Graphics.Cursor.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.*;
import mindustry.entities.*;
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
    private Vec2 movement = new Vec2();
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
    private @Nullable BuildPlan sreq;
    /** Whether player is currently deleting removal requests. */
    private boolean deleting = false, shouldShoot = false;

    @Override
    public void buildUI(Group group){
        group.fill(t -> {
            t.bottom();
            t.visible(() -> {
                t.color.a = Mathf.lerpDelta(t.color.a, player.builder().isBuilding() ? 1f : 0f, 0.15f);

                return Core.settings.getBool("hints") && selectRequests.isEmpty() && t.color.a > 0.01f;
            });
            t.touchable(() -> t.color.a < 0.1f ? Touchable.disabled : Touchable.childrenOnly);
            t.table(Styles.black6, b -> {
                b.defaults().left();
                b.label(() -> Core.bundle.format(!isBuilding ?  "resumebuilding" : "pausebuilding", Core.keybinds.get(Binding.pause_building).key.toString())).style(Styles.outlineLabel);
                b.row();
                b.label(() -> Core.bundle.format("cancelbuilding", Core.keybinds.get(Binding.clear_building).key.toString())).style(Styles.outlineLabel);
                b.row();
                b.label(() -> Core.bundle.format("selectschematic", Core.keybinds.get(Binding.schematic_select).key.toString())).style(Styles.outlineLabel);
            }).margin(10f);
        });

        group.fill(t -> {
            t.visible(() -> Core.settings.getBool("hints") && lastSchematic != null && !selectRequests.isEmpty());
            t.bottom();
            t.table(Styles.black6, b -> {
                b.defaults().left();
                b.label( () -> Core.bundle.format("schematic.flip",
                Core.keybinds.get(Binding.schematic_flip_x).key.toString(),
                Core.keybinds.get(Binding.schematic_flip_y).key.toString())).style(Styles.outlineLabel);
                b.row();
                b.table(a -> {
                    a.button("$schematic.add", Icon.save, this::showSchematicSave).colspan(2).size(250f, 50f).disabled(f -> lastSchematic == null || lastSchematic.file != null);
                });
            }).margin(6f);
        });

        group.fill(t -> {
            t.visible(() -> Core.settings.getBool("hints") && !player.dead() && !player.unit().spawnedByCore());
            t.bottom();
            t.table(Styles.black6, b -> {
                b.defaults().left();
                b.label(() -> Core.bundle.format("respawn", Core.keybinds.get(Binding.respawn).key.toString())).style(Styles.outlineLabel);
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
            drawBreakSelection(selectX, selectY, cursorX, cursorY);
        }

        if(Core.input.keyDown(Binding.schematic_select) && !Core.scene.hasKeyboard()){
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

            sreq.block.drawRequest(sreq, allRequests(), valid);

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
        for(BuildPlan request : selectRequests){
            request.animScale = 1f;
            drawRequest(request);
        }

        for(BuildPlan request : selectRequests){
            drawOverRequest(request);
        }

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
            }else if(isPlacing()){
                if(block.rotate){
                    drawArrow(block, cursorX, cursorY, rotation);
                }
                Draw.color();
                drawRequest(cursorX, cursorY, block, rotation);
                block.drawPlace(cursorX, cursorY, rotation, validPlace(cursorX, cursorY, block, rotation));

                if(block.saveConfig && block.lastConfig != null){
                    brequest.set(cursorX, cursorY, rotation, block);
                    brequest.config = block.lastConfig;
                    block.drawRequestConfig(brequest, allRequests());
                    brequest.config = null;
                }

            }
        }

        Draw.reset();
    }

    @Override
    public void update(){
        super.update();

        if(net.active() && Core.input.keyTap(Binding.player_list)){
            ui.listfrag.toggle();
        }

        //TODO awful UI state checking code
        if((player.dead() || state.isPaused()) && !ui.chatfrag.shown()){
            if(!(scene.getKeyboardFocus() instanceof TextField) && !scene.hasDialog()){
                //move camera around
                float camSpeed = !Core.input.keyDown(Binding.boost) ? 3f : 8f;
                Core.camera.position.add(Tmp.v1.setZero().add(Core.input.axis(Binding.move_x), Core.input.axis(Binding.move_y)).nor().scl(Time.delta * camSpeed));

                if(Core.input.keyDown(Binding.mouse_move)){
                    Core.camera.position.x += Mathf.clamp((Core.input.mouseX() - Core.graphics.getWidth() / 2f) * 0.005f, -1, 1) * camSpeed;
                    Core.camera.position.y += Mathf.clamp((Core.input.mouseY() - Core.graphics.getHeight() / 2f) * 0.005f, -1, 1) * camSpeed;
                }
            }
        }else if(!player.dead()){
            Core.camera.position.lerpDelta(player, Core.settings.getBool("smoothcamera") ? 0.08f : 1f);
        }

        shouldShoot = true;

        if(!scene.hasMouse()){
            if(Core.input.keyDown(Binding.control) && Core.input.keyTap(Binding.select)){
                Unit on = selectedUnit();
                if(on != null){
                    Call.unitControl(player, on);
                    shouldShoot = false;
                }
            }
        }

        if(!player.dead() && !state.isPaused() && !(Core.scene.getKeyboardFocus() instanceof TextField)){
            updateMovement(player.unit());

            if(Core.input.keyDown(Binding.respawn) && !player.unit().spawnedByCore()){
                Call.unitClear(player);
                controlledType = null;
            }
        }

        if(Core.input.keyRelease(Binding.select)){
            player.shooting = false;
        }

        if(state.isGame() && Core.input.keyTap(Binding.minimap) && !scene.hasDialog() && !(scene.getKeyboardFocus() instanceof TextField)){
            ui.minimapfrag.toggle();
        }

        if(state.isMenu() || Core.scene.hasDialog()) return;

        //zoom camera
        if((!Core.scene.hasScroll() || Core.input.keyDown(Binding.diagonal_placement)) && !ui.chatfrag.shown() && Math.abs(Core.input.axisTap(Binding.zoom)) > 0
            && !Core.input.keyDown(Binding.rotateplaced) && (Core.input.keyDown(Binding.diagonal_placement) || ((!isPlacing() || !block.rotate) && selectRequests.isEmpty()))){
            renderer.scaleCamera(Core.input.axisTap(Binding.zoom));
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
            }else if(!selectRequests.isEmpty()){
                rotateRequests(selectRequests, Mathf.sign(Core.input.axisTap(Binding.rotate)));
            }
        }

        Tile cursor = tileAt(Core.input.mouseX(), Core.input.mouseY());

        if(cursor != null){
            if(cursor.build != null){
                cursorType = cursor.build.getCursor();
            }

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

            if(cursor.build != null && cursor.interactable(player.team()) && !isPlacing() && Math.abs(Core.input.axisTap(Binding.rotate)) > 0 && Core.input.keyDown(Binding.rotateplaced) && cursor.block().rotate){
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
        });

        table.button(Icon.tree, Styles.clearPartiali, () -> {
            ui.research.show();
        }).visible(() -> state.isCampaign());

        table.button(Icon.map, Styles.clearPartiali, () -> {
            ui.planet.show();
        }).visible(() -> state.isCampaign());

        table.button(Icon.up, Styles.clearPartiali, () -> {
            ui.planet.show(state.getSector(), player.team().core());
        }).visible(() -> state.isCampaign())
        .disabled(b -> player.team().core() == null || !player.team().core().items.has(player.team().core().block.requirements));
    }

    void pollInput(){
        if(scene.getKeyboardFocus() instanceof TextField) return;

        Tile selected = tileAt(Core.input.mouseX(), Core.input.mouseY());
        int cursorX = tileX(Core.input.mouseX());
        int cursorY = tileY(Core.input.mouseY());
        int rawCursorX = world.toTile(Core.input.mouseWorld().x), rawCursorY = world.toTile(Core.input.mouseWorld().y);

        // automatically pause building if the current build queue is empty
        if(Core.settings.getBool("buildautopause") && isBuilding && !player.builder().isBuilding()){
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

        if(Core.input.keyTap(Binding.deselect)){
            player.miner().mineTile(null);
        }

        if(Core.input.keyTap(Binding.clear_building)){
            player.builder().clearBuilding();
        }

        if(Core.input.keyTap(Binding.schematic_select) && !Core.scene.hasKeyboard()){
            schemX = rawCursorX;
            schemY = rawCursorY;
        }

        if(Core.input.keyTap(Binding.schematic_menu) && !Core.scene.hasKeyboard()){
            if(ui.schematics.isShown()){
                ui.schematics.hide();
            }else{
                ui.schematics.show();
                ui.schematics.focusSearchField();
            }
        }

        if(Core.input.keyTap(Binding.clear_building) || isPlacing()){
            lastSchematic = null;
            selectRequests.clear();
        }

        if(Core.input.keyRelease(Binding.schematic_select) && !Core.scene.hasKeyboard()){
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
                if(!tileTapped(selected.build) && !tryTapPlayer(Core.input.mouseWorld().x, Core.input.mouseWorld().y) && (player.builder().plans().size == 0 || !player.builder().updateBuilding()) && !droppingItem &&
                !tryBeginMine(selected) && player.miner().mineTile() == null && !Core.scene.hasKeyboard()){
                    player.shooting = shouldShoot;
                }
            }else if(!Core.scene.hasKeyboard()){ //if it's out of bounds, shooting is just fine
                player.shooting = shouldShoot;
            }
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
        }

        if(Core.input.keyDown(Binding.select) && mode == none && !isPlacing() && deleting){
            BuildPlan req = getRequest(cursorX, cursorY);
            if(req != null && req.breaking){
                player.builder().plans().remove(req);
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

        if(Core.input.keyRelease(Binding.break_block) || Core.input.keyRelease(Binding.select)){

            if(mode == placing && block != null){ //touch up while placing, place everything in selection
                flushRequests(lineRequests);
                lineRequests.clear();
                Events.fire(new LineConfirmEvent());
            }else if(mode == breaking){ //touch up while breaking, break everything in selection
                removeSelection(selectX, selectY, cursorX, cursorY);
            }

            tryDropItems(selected == null ? null : selected.build, Core.input.mouseWorld().x, Core.input.mouseWorld().y);

            if(sreq != null){
                if(getRequest(sreq.x, sreq.y, sreq.block.size, sreq) != null){
                    player.builder().plans().remove(sreq, true);
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
        if(state.isMenu()){
            droppingItem = false;
            mode = none;
            block = null;
            sreq = null;
            selectRequests.clear();
        }
    }

    protected void updateMovement(Unit unit){
        boolean omni = !(unit instanceof WaterMovec);
        boolean legs = unit.isGrounded();

        float strafePenalty = legs ? 1f : Mathf.lerp(1f, unit.type().strafePenalty, Angles.angleDist(unit.vel().angle(), unit.rotation()) / 180f);
        float speed = unit.type().speed * Mathf.lerp(1f, unit.type().canBoost ? unit.type().boostMultiplier : 1f, unit.elevation) * strafePenalty;
        float xa = Core.input.axis(Binding.move_x);
        float ya = Core.input.axis(Binding.move_y);
        boolean boosted = (unit instanceof Mechc && unit.isFlying());

        movement.set(xa, ya).nor().scl(speed);
        if(Core.input.keyDown(Binding.mouse_move)){
            movement.add(input.mouseWorld().sub(player).scl(1f / 25f * speed)).limit(speed);
        }

        float mouseAngle = Angles.mouseAngle(unit.x, unit.y);
        boolean aimCursor = omni && player.shooting && unit.type().hasWeapons() && unit.type().faceTarget && !boosted && unit.type().rotateShooting;

        if(aimCursor){
            unit.lookAt(mouseAngle);
        }else{
            if(unit.moving()){
                unit.lookAt(unit.vel().angle());
            }
        }

        if(omni){
            unit.moveAt(movement);
        }else{
            unit.moveAt(Tmp.v2.trns(unit.rotation, movement.len()));
            if(!movement.isZero() && legs){
                unit.vel.rotateTo(movement.angle(), unit.type().rotateSpeed * Time.delta);
            }
        }

        unit.aim(unit.type().faceTarget ? Core.input.mouseWorld() : Tmp.v1.trns(unit.rotation, Core.input.mouseWorld().dst(unit)).add(unit.x, unit.y));
        unit.controlWeapons(true, player.shooting && !boosted);

        player.boosting = Core.input.keyDown(Binding.boost) && !movement.isZero();
        player.mouseX = unit.aimX();
        player.mouseY = unit.aimY();

        //update payload input
        if(unit instanceof Payloadc){
            Payloadc pay = (Payloadc)unit;

            if(Core.input.keyTap(Binding.pickupCargo) && pay.payloads().size < unit.type().payloadCapacity){
                Unit target = Units.closest(player.team(), pay.x(), pay.y(), unit.type().hitsize * 2.5f, u -> u.isAI() && u.isGrounded() && u.mass() < unit.mass() && u.within(unit, u.hitSize + unit.hitSize * 1.2f));
                if(target != null){
                    Call.pickupUnitPayload(player, target);
                }else if(!pay.hasPayload()){
                    Building tile = world.buildWorld(pay.x(), pay.y());

                    if(tile != null && tile.team() == unit.team){
                        Call.pickupBlockPayload(player, tile);
                    }
                }
            }

            if(Core.input.keyTap(Binding.dropCargo)){
                Call.dropPayload(player, player.x, player.y);
                pay.dropLastPayload();
            }
        }

        //update commander inut
        if(unit instanceof Commanderc){
            if(Core.input.keyTap(Binding.command)){
                Call.unitCommand(player);
            }
        }
    }
}
