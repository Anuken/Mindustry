package mindustry.input;

import arc.*;
import arc.Graphics.*;
import arc.Graphics.Cursor.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.formations.*;
import mindustry.ai.formations.patterns.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;

import static arc.Core.scene;
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
    private @Nullable BuildRequest sreq;
    /** Whether player is currently deleting removal requests. */
    private boolean deleting = false;

    @Override
    public void buildUI(Group group){
        group.fill(t -> {
            t.bottom().update(() -> t.getColor().a = Mathf.lerpDelta(t.getColor().a, player.builder().isBuilding() ? 1f : 0f, 0.15f));
            t.visible(() -> Core.settings.getBool("hints") && selectRequests.isEmpty());
            t.touchable(() -> t.getColor().a < 0.1f ? Touchable.disabled : Touchable.childrenOnly);
            t.table(Styles.black6, b -> {
                b.defaults().left();
                b.label(() -> Core.bundle.format(!isBuilding ?  "resumebuilding" : "pausebuilding", Core.keybinds.get(Binding.pause_building).key.toString())).style(Styles.outlineLabel);
                b.row();
                b.label(() -> {
                    if((block instanceof ItemBridge && ((ItemBridge) block).lastPlaced != -1))
                        return "clear lastPlaced";
                    else if((block.saveConfig && block.lastConfig != null))
                        return "clear lastConfig";
                    else
                        return Core.bundle.format("cancelbuilding", Core.keybinds.get(Binding.clear_building).key.toString());
                }).style(Styles.outlineLabel);
                b.row();
                b.label(() -> Core.bundle.format("selectschematic", Core.keybinds.get(Binding.schematic_select).key.toString())).style(Styles.outlineLabel);
            }).margin(10f);
        });

        group.fill(t -> {
            t.visible(() -> lastSchematic != null && !selectRequests.isEmpty());
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

        for(BuildRequest request : selectRequests){
            drawOverRequest(request);
        }

        if(player.isBuilder()){
            //draw things that may be placed soon
            if(mode == placing && block != null){
                for(int i = 0; i < lineRequests.size; i++){
                    BuildRequest req = lineRequests.get(i);
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

        if((player.dead() || state.isPaused()) && !ui.chatfrag.shown()){
            if(!(scene.getKeyboardFocus() instanceof TextField)){
                //move camera around
                float camSpeed = !Core.input.keyDown(Binding.boost) ? 3f : 8f;
                Core.camera.position.add(Tmp.v1.setZero().add(Core.input.axis(Binding.move_x), Core.input.axis(Binding.move_y)).nor().scl(Time.delta() * camSpeed));

                if(Core.input.keyDown(Binding.mouse_move)){
                    Core.camera.position.x += Mathf.clamp((Core.input.mouseX() - Core.graphics.getWidth() / 2f) * 0.005f, -1, 1) * camSpeed;
                    Core.camera.position.y += Mathf.clamp((Core.input.mouseY() - Core.graphics.getHeight() / 2f) * 0.005f, -1, 1) * camSpeed;
                }
            }
        }else if(!player.dead()){
            Core.camera.position.lerpDelta(player, 0.08f);
        }

        if(!scene.hasMouse()){
            if(Core.input.keyDown(Binding.control) && Core.input.keyTap(Binding.select)){
                Unitc on = selectedUnit();
                if(on != null){
                    Call.onUnitControl(player, on);
                }
            }

            //TODO this is for debugging, remove later
            if(Core.input.keyTap(KeyCode.g) && !player.dead() && player.unit() instanceof Commanderc){
                Commanderc commander = (Commanderc)player.unit();

                if(commander.isCommanding()){
                    commander.clearCommand();
                }else{

                    FormationPattern pattern = new SquareFormation();
                    Formation formation = new Formation(new Vec3(player.x(), player.y(), player.unit().rotation()), pattern);
                    formation.slotAssignmentStrategy = new DistanceAssignmentStrategy(pattern);

                    units.clear();

                    Fx.commandSend.at(player);
                    Units.nearby(player.team(), player.x(), player.y(), 200f, u -> {
                        if(u.isAI()){
                            units.add(u);
                        }
                    });

                    commander.command(formation, units);
                }
            }
        }

        if(!player.dead() && !state.isPaused() && !(Core.scene.getKeyboardFocus() instanceof TextField)){
            updateMovement(player.unit());
        }

        if(Core.input.keyRelease(Binding.select)){
            isShooting = false;
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

        if(isShooting && !canShoot()){
            isShooting = false;
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
            if(cursor.entity != null){
                cursorType = cursor.entity.getCursor();
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

            if(cursor.entity != null && cursor.interactable(player.team()) && !isPlacing() && Math.abs(Core.input.axisTap(Binding.rotate)) > 0 && Core.input.keyDown(Binding.rotateplaced) && cursor.block().rotate){
                Call.rotateBlock(player, cursor.entity, Core.input.axisTap(Binding.rotate) > 0);
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
            if(block) Log.info(block);
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
        }

        if((cursorX != lastLineX || cursorY != lastLineY) && isPlacing() && mode == placing){
            updateLine(selectX, selectY);
            lastLineX = cursorX;
            lastLineY = cursorY;
        }

        if(Core.input.keyTap(Binding.select) && !Core.scene.hasMouse()){
            BuildRequest req = getRequest(cursorX, cursorY);

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
                if(!tileTapped(selected.entity) && !tryTapPlayer(Core.input.mouseWorld().x, Core.input.mouseWorld().y) && (player.builder().requests().size == 0 || !player.builder().isBuilding()) && !droppingItem &&
                !tryBeginMine(selected) && player.miner().mineTile() == null && !Core.scene.hasKeyboard()){
                    isShooting = true;
                }
            }else if(!Core.scene.hasKeyboard()){ //if it's out of bounds, shooting is just fine
                isShooting = true;
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
            BuildRequest req = getRequest(cursorX, cursorY);
            if(req != null && req.breaking){
                player.builder().requests().remove(req);
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

            if(selected != null && selected.entity != null){
                tryDropItems(selected.entity, Core.input.mouseWorld().x, Core.input.mouseWorld().y);
            }

            if(sreq != null){
                if(getRequest(sreq.x, sreq.y, sreq.block.size, sreq) != null){
                    player.builder().requests().remove(sreq, true);
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

    protected void updateMovement(Unitc unit){
        boolean omni = !(unit instanceof WaterMovec);
        boolean legs = unit.isGrounded();
        float speed = unit.type().speed;
        float xa = Core.input.axis(Binding.move_x);
        float ya = Core.input.axis(Binding.move_y);

        movement.set(xa, ya).nor().scl(speed);
        float mouseAngle = Angles.mouseAngle(unit.x(), unit.y());
        boolean aimCursor = omni && isShooting && unit.type().hasWeapons();

        if(aimCursor){
            unit.lookAt(mouseAngle);
        }else{
            if(!unit.vel().isZero(0.01f)){
                if(unit.type().flying){
                    unit.rotation(unit.vel().angle());
                }else{
                    unit.lookAt(unit.vel().angle());
                }
            }
        }

        if(omni){
            unit.moveAt(movement);
        }else{
            unit.moveAt(Tmp.v2.trns(unit.rotation(), movement.len()));
            if(!movement.isZero() && legs){
                unit.vel().rotateTo(movement.angle(), unit.type().rotateSpeed * Time.delta());
            }
        }

        unit.aim(Core.input.mouseWorld());
        unit.controlWeapons(true, isShooting);
        /*
        Tile tile = unit.tileOn();
        boolean canMove = !Core.scene.hasKeyboard() || ui.minimapfrag.shown();

        //TODO implement
        boolean isBoosting = Core.input.keyDown(Binding.dash) && !mech.flying;

        //if player is in solid block
        if(tile != null && tile.solid()){
            isBoosting = true;
        }

        float speed = isBoosting && unit.type().flying ? mech.boostSpeed : mech.speed;

        if(mech.flying){
            //prevent strafing backwards, have a penalty for doing so
            float penalty = 0.2f; //when going 180 degrees backwards, reduce speed to 0.2x
            speed *= Mathf.lerp(1f, penalty, Angles.angleDist(rotation, velocity.angle()) / 180f);
        }

        movement.setZero();

        float xa = Core.input.axis(Binding.move_x);
        float ya = Core.input.axis(Binding.move_y);
        if(!(Core.scene.getKeyboardFocus() instanceof TextField)){
            movement.y += ya * speed;
            movement.x += xa * speed;
        }

        if(Core.input.keyDown(Binding.mouse_move)){
            movement.x += Mathf.clamp((Core.input.mouseX() - Core.graphics.getWidth() / 2f) * 0.005f, -1, 1) * speed;
            movement.y += Mathf.clamp((Core.input.mouseY() - Core.graphics.getHeight() / 2f) * 0.005f, -1, 1) * speed;
        }

        Vec2 vec = Core.input.mouseWorld(control.input.getMouseX(), control.input.getMouseY());
        pointerX = vec.x;
        pointerY = vec.y;
        updateShooting();

        movement.limit(speed).scl(Time.delta());

        if(canMove){
            velocity.add(movement.x, movement.y);
        }else{
            isShooting = false;
        }
        float prex = x, prey = y;
        updateVelocityStatus();
        moved = dst(prex, prey) > 0.001f;

        if(canMove){
            float baseLerp = mech.getRotationAlpha(this);
            if(!isShooting() || !mech.faceTarget){
                if(!movement.isZero()){
                    rotation = Mathf.slerpDelta(rotation, mech.flying ? velocity.angle() : movement.angle(), 0.13f * baseLerp);
                }
            }else{
                float angle = control.input.mouseAngle(x, y);
                this.rotation = Mathf.slerpDelta(this.rotation, angle, 0.1f * baseLerp);
            }
        }*/
    }
}
