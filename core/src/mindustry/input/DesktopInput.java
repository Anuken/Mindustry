package mindustry.input;

import arc.*;
import arc.Graphics.*;
import arc.Graphics.Cursor.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.input.KeyCode.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
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
    /** Selected build plan for movement. */
    public @Nullable BuildPlan splan;
    /** Whether player is currently deleting removal plans. */
    public boolean deleting = false, shouldShoot = false, panning = false, movedPlan = false;
    /** Mouse pan speed. */
    public float panScale = 0.005f, panSpeed = 4.5f, panBoostSpeed = 15f;
    /** Delta time between consecutive clicks. */
    public long selectMillis = 0;
    /** Previously selected tile. */
    public Tile prevSelected;

    /** Most recently selected control group by index */
    public int lastCtrlGroup;
    /** Time of most recent control group selection */
    public long lastCtrlGroupSelectMillis;

    /** Time of most recent payload pickup/drop key press*/
    public long lastPayloadKeyTapMillis;
    /** Time of most recent payload pickup/drop key hold*/
    public long lastPayloadKeyHoldMillis;

    private float buildPlanMouseOffsetX, buildPlanMouseOffsetY;
    private boolean changedCursor, pressedCommandRect;

    boolean showHint(){
        return ui.hudfrag.shown && Core.settings.getBool("hints") && selectPlans.isEmpty() && !player.dead() &&
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
                        str.append(Core.bundle.format("enablebuilding", Binding.pauseBuilding.value.key.toString()));
                    }else if(player.unit().isBuilding()){
                        str.append(Core.bundle.format(isBuilding ? "pausebuilding" : "resumebuilding", Binding.pauseBuilding.value.key.toString()))
                            .append("\n").append(Core.bundle.format("cancelbuilding", Binding.clearBuilding.value.key.toString()))
                            .append("\n").append(Core.bundle.format("selectschematic", Binding.schematicSelect.value.key.toString()));
                    }
                    if(!player.dead() && !player.unit().spawnedByCore()){
                        str.append(str.length() != 0 ? "\n" : "").append(Core.bundle.format("respawn", Binding.respawn.value.key.toString()));
                    }
                    return str;
                }).style(Styles.outlineLabel);
            }).margin(10f);
        });

        //schematic controls
        group.fill(t -> {
            t.visible(() -> ui.hudfrag.shown && lastSchematic != null && !selectPlans.isEmpty());
            t.bottom();
            t.table(Styles.black6, b -> {
                b.defaults().left();
                b.label(() -> Core.bundle.format("schematic.flip",
                    Binding.schematicFlipX.value.key.toString(),
                    Binding.schematicFlipY.value.key.toString())).style(Styles.outlineLabel).visible(() -> Core.settings.getBool("hints"));
                b.row();
                b.table(a -> {
                    a.button("@schematic.add", Icon.save, this::showSchematicSave).colspan(2).size(250f, 50f).disabled(f -> lastSchematic == null || lastSchematic.file != null);
                });
            }).margin(6f);
        });
    }

    @Override
    public void drawTop(){
        if(cursorType != SystemCursor.arrow && scene.hasMouse()){
           graphics.cursor(cursorType = SystemCursor.arrow);
        }

        Lines.stroke(1f);
        int cursorX = tileX(Core.input.mouseX());
        int cursorY = tileY(Core.input.mouseY());

        //draw break selection
        if(mode == breaking){
            drawBreakSelection(selectX, selectY, cursorX, cursorY, !(Core.input.keyDown(Binding.schematicSelect) && schemX != -1 && schemY != -1) ? maxLength : Vars.maxSchematicSize, false);
        }

        if(!Core.scene.hasKeyboard() && mode != breaking){

            if(Core.input.keyDown(Binding.schematicSelect) && schemX != -1 && schemY != -1){
                drawSelection(schemX, schemY, cursorX, cursorY, Vars.maxSchematicSize);
            }else if(Core.input.keyDown(Binding.rebuildSelect)){
                drawRebuildSelection(schemX, schemY, cursorX, cursorY);
            }
        }

        Draw.reset();
    }

    @Override
    public void drawBottom(){
        int cursorX = tileX(Core.input.mouseX());
        int cursorY = tileY(Core.input.mouseY());

        //draw plan being moved
        if(splan != null){
            boolean valid = validPlace(splan.x, splan.y, splan.block, splan.rotation, splan);
            if(splan.block.rotate && splan.block.drawArrow){
                drawArrow(splan.block, splan.x, splan.y, splan.rotation, valid);
            }

            splan.block.drawPlan(splan, allPlans(), valid);

            drawSelected(splan.x, splan.y, splan.block, getPlan(splan.x, splan.y, splan.block.size, splan) != null ? Pal.remove : Pal.accent);
        }

        //draw hover plans
        if(mode == none && !isPlacing()){
            var plan = getPlan(cursorX, cursorY);
            if(plan != null){
                drawSelected(plan.x, plan.y, plan.breaking ? plan.tile().block() : plan.block, Pal.accent);
            }
        }

        var items = selectPlans.items;
        int size = selectPlans.size;

        //draw schematic plans
        for(int i = 0; i < size; i++){
            var plan = items[i];
            plan.animScale = 1f;
            drawPlan(plan);
        }

        //draw schematic plans - over version, cached results
        for(int i = 0; i < size; i++){
            var plan = items[i];
            //use cached value from previous invocation
            drawOverPlan(plan, plan.cachedValid);
        }

        if(player.isBuilder()){
            //draw things that may be placed soon
            if(mode == placing && block != null){
                for(int i = 0; i < linePlans.size; i++){
                    var plan = linePlans.get(i);
                    if(i == linePlans.size - 1 && plan.block.rotate && plan.block.drawArrow){
                        drawArrow(block, plan.x, plan.y, plan.rotation);
                    }
                    drawPlan(linePlans.get(i));
                }
                linePlans.each(this::drawOverPlan);
            }else if(isPlacing()){
                int rot = block == null ? rotation : block.planRotation(rotation);
                if(block.rotate && block.drawArrow){
                    drawArrow(block, cursorX, cursorY, rot);
                }
                Draw.color();
                boolean valid = validPlace(cursorX, cursorY, block, rot);
                drawPlan(cursorX, cursorY, block, rot);
                block.drawPlace(cursorX, cursorY, rot, valid);

                if(block.saveConfig){
                    Draw.mixcol(!valid ? Pal.breakInvalid : Color.white, (!valid ? 0.4f : 0.24f) + Mathf.absin(Time.globalTime, 6f, 0.28f));
                    bplan.set(cursorX, cursorY, rot, block);
                    bplan.config = block.lastConfig;
                    block.drawPlanConfig(bplan, allPlans());
                    bplan.config = null;
                    Draw.reset();
                }

                drawOverlapCheck(block, cursorX, cursorY, valid);
            }
        }

        Draw.reset();
    }

    @Override
    public void update(){
        super.update();

        if(net.active() && Core.input.keyTap(Binding.playerList) && (scene.getKeyboardFocus() == null || scene.getKeyboardFocus().isDescendantOf(ui.listfrag.content) || scene.getKeyboardFocus().isDescendantOf(ui.minimapfrag.elem))){
            ui.listfrag.toggle();
        }

        boolean locked = locked();
        boolean panCam = false;
        float camSpeed = (!Core.input.keyDown(Binding.boost) ? panSpeed : panBoostSpeed) * Time.delta;
        boolean detached = settings.getBool("detach-camera", false);

        if(!scene.hasField() && !scene.hasDialog()){
            if(input.keyTap(Binding.debugHitboxes)){
                drawDebugHitboxes = !drawDebugHitboxes;
            }

            if(input.keyTap(Binding.detachCamera)){
                settings.put("detach-camera", detached = !detached);
                if(!detached){
                    panning = false;
                }
                spectating = null;
            }

            if(input.keyDown(Binding.pan)){
                panCam = true;
                panning = true;
                spectating = null;
            }

            if((Math.abs(Core.input.axis(Binding.moveX)) > 0 || Math.abs(Core.input.axis(Binding.moveY)) > 0 || input.keyDown(Binding.mouseMove))){
                panning = false;
                spectating = null;
            }
        }

        panning |= detached;


        if(!locked){
            if(((player.dead() || state.isPaused() || detached) && !ui.chatfrag.shown()) && !scene.hasField() && !scene.hasDialog()){
                if(input.keyDown(Binding.mouseMove)){
                    panCam = true;
                }

                Core.camera.position.add(Tmp.v1.setZero().add(Core.input.axis(Binding.moveX), Core.input.axis(Binding.moveY)).nor().scl(camSpeed));
            }else if((!player.dead() || spectating != null) && !panning){
                //TODO do not pan
                Team corePanTeam = state.won ? state.rules.waveTeam : player.team();
                Position coreTarget = state.gameOver && !state.rules.pvp && corePanTeam.data().lastCore != null ? corePanTeam.data().lastCore : null;
                Position panTarget = coreTarget != null ? coreTarget : spectating != null ? spectating : player;

                Core.camera.position.lerpDelta(panTarget, Core.settings.getBool("smoothcamera") ? 0.08f : 1f);
            }

            if(panCam){
                Core.camera.position.x += Mathf.clamp((Core.input.mouseX() - Core.graphics.getWidth() / 2f) * panScale, -1, 1) * camSpeed;
                Core.camera.position.y += Mathf.clamp((Core.input.mouseY() - Core.graphics.getHeight() / 2f) * panScale, -1, 1) * camSpeed;
            }
        }

        shouldShoot = !scene.hasMouse() && !locked && !state.isEditor();

        if(!locked && block == null && !scene.hasField() && !scene.hasDialog() &&
                //disable command mode when player unit can boost and command mode binding is the same
                !(!player.dead() && player.unit().type.canBoost && Binding.commandMode.value.key == Binding.boost.value.key)){
            if(settings.getBool("commandmodehold")){
                commandMode = input.keyDown(Binding.commandMode);
            }else if(input.keyTap(Binding.commandMode)){
                commandMode = !commandMode;
            }
        }else{
            commandMode = false;
        }

        //validate commanding units
        selectedUnits.removeAll(u -> !u.allowCommand() || !u.isValid() || u.team != player.team());

        if(commandMode && !scene.hasField() && !scene.hasDialog()){
            if(input.keyTap(Binding.selectAllUnits)){
                selectedUnits.clear();
                commandBuildings.clear();
                if(input.keyDown(Binding.selectAcrossScreen)){
                    camera.bounds(Tmp.r1);
                    selectedUnits.set(selectedCommandUnits(Tmp.r1.x, Tmp.r1.y, Tmp.r1.width, Tmp.r1.height).removeAll(u -> !u.type.controlSelectGlobal));
                }else {
                    for(var unit : player.team().data().units){
                        if(unit.isCommandable() && unit.type.controlSelectGlobal){
                            selectedUnits.add(unit);
                        }
                    }
                }
            }

            if(input.keyTap(Binding.selectAllUnitTransport)){
                selectedUnits.clear();
                commandBuildings.clear();
                if(input.keyDown(Binding.selectAcrossScreen)){
                    camera.bounds(Tmp.r1);
                    selectedUnits.set(selectedCommandUnits(Tmp.r1.x, Tmp.r1.y, Tmp.r1.width, Tmp.r1.height, u -> u instanceof Payloadc));
                }else {
                    for(var unit : player.team().data().units){
                        if(unit.isCommandable() && unit instanceof Payloadc){
                            selectedUnits.add(unit);
                        }
                    }
                }
            }

            if(input.keyTap(Binding.selectAllUnitFactories)){
                selectedUnits.clear();
                commandBuildings.clear();
                for(var build : player.team().data().buildings){
                    if(build.isCommandable()){
                        commandBuildings.add(build);
                    }
                }
                if(input.keyDown(Binding.selectAcrossScreen)){
                    camera.bounds(Tmp.r1);
                    commandBuildings.retainAll(b -> Tmp.r1.overlaps(b.x - (b.hitSize() /2), b.y - (b.hitSize() /2), b.hitSize(), b.hitSize()));
                }
            }

            for(int i = 0; i < controlGroupBindings.length; i++){
                if(input.keyTap(controlGroupBindings[i])){

                    //create control group if it doesn't exist yet
                    if(controlGroups[i] == null) controlGroups[i] = new IntSeq();

                    IntSeq group = controlGroups[i];
                    boolean creating = input.keyDown(Binding.createControlGroup);

                    //clear existing if making a new control group
                    //if any of the control group edit buttons are pressed take the current selection
                    if(creating){
                        group.clear();

                        IntSeq selectedUnitIds = selectedUnits.mapInt(u -> u.id);
                        if(Core.settings.getBool("distinctcontrolgroups", true)){
                            for(IntSeq cg : controlGroups){
                                if(cg != null){
                                    cg.removeAll(selectedUnitIds);
                                }
                            }
                        }
                        group.addAll(selectedUnitIds);
                    }

                    //remove invalid units
                    for(int j = 0; j < group.size; j++){
                        Unit u = Groups.unit.getByID(group.get(j));
                        if(u == null || !u.isCommandable() || !u.isValid()){
                            group.removeIndex(j);
                            j --;
                        }
                    }

                    //replace the selected units with the current control group
                    if(!group.isEmpty() && !creating){
                        selectedUnits.clear();
                        commandBuildings.clear();

                        group.each(id -> {
                            var unit = Groups.unit.getByID(id);
                            if(unit != null){
                                selectedUnits.addAll(unit);
                            }
                        });

                        //double tap to center camera
                        if(lastCtrlGroup == i && Time.timeSinceMillis(lastCtrlGroupSelectMillis) < 400){
                            float totalX = 0, totalY = 0;
                            for(Unit unit : selectedUnits){
                                totalX += unit.x;
                                totalY += unit.y;
                            }
                            panning = true;
                            Core.camera.position.set(totalX / selectedUnits.size, totalY / selectedUnits.size);
                        }
                        lastCtrlGroup = i;
                        lastCtrlGroupSelectMillis = Time.millis();
                    }
                }
            }
        }

        if(!scene.hasMouse() && !locked && state.rules.possessionAllowed){
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

        if(!player.dead() && !state.isPaused() && !scene.hasField() && !locked){
            updateMovement(player.unit());

            if(Core.input.keyTap(Binding.respawn)){
                controlledType = null;
                recentRespawnTimer = 1f;
                Call.unitClear(player);
            }
        }

        if(state.isGame() && !scene.hasDialog() && !scene.hasField()){
            if(Core.input.keyTap(Binding.minimap)) ui.minimapfrag.toggle();
            if(Core.input.keyTap(Binding.planetMap) && state.isCampaign()) ui.planet.toggle();
            if(Core.input.keyTap(Binding.research) && state.isCampaign()) ui.research.toggle();
            if(Core.input.keyTap(Binding.schematicMenu)) ui.schematics.toggle();

            if(Core.input.keyTap(Binding.toggleBlockStatus)){
                Core.settings.put("blockstatus", !Core.settings.getBool("blockstatus"));
            }

            if(Core.input.keyTap(Binding.togglePowerLines)){
                if(Core.settings.getInt("lasersopacity") == 0){
                    Core.settings.put("lasersopacity", Core.settings.getInt("preferredlaseropacity", 100));
                }else{
                    Core.settings.put("preferredlaseropacity", Core.settings.getInt("lasersopacity"));
                    Core.settings.put("lasersopacity", 0);
                }
            }
        }

        if(state.isMenu() || Core.scene.hasDialog()) return;

        //zoom camera
        if((!Core.scene.hasScroll() || Core.input.keyDown(Binding.diagonalPlacement)) && !ui.chatfrag.shown() && !ui.consolefrag.shown() && Math.abs(Core.input.axisTap(Binding.zoom)) > 0
            && !Core.input.keyDown(Binding.rotatePlaced) && (Core.input.keyDown(Binding.diagonalPlacement) ||
                !Binding.zoom.value.equals(Binding.rotate.value) || ((!player.isBuilder() || !isPlacing() || !block.rotate) && selectPlans.isEmpty()))){
            renderer.scaleCamera(Core.input.axisTap(Binding.zoom));
        }

        if(Core.input.keyTap(Binding.select) && !Core.scene.hasMouse()){
            Tile selected = world.tileWorld(input.mouseWorldX(), input.mouseWorldY());
            if(selected != null){
                Call.tileTap(player, selected);
            }
        }

        if(Core.input.keyRelease(Binding.select) && commandRect){
            selectUnitsRect();
        }

        if(player.dead() || locked){
            cursorType = SystemCursor.arrow;
            if(!locked){
                pollInputNoPlayer();
            }
        }else{
            pollInputPlayer();
        }

        if(Core.input.keyRelease(Binding.select)){
            player.shooting = false;
        }

        if(!Core.scene.hasMouse() && !ui.minimapfrag.shown()){
            Core.graphics.cursor(cursorType);
            changedCursor = cursorType != SystemCursor.arrow;
        }else{
            cursorType = SystemCursor.arrow;
            if(changedCursor){
                graphics.cursor(SystemCursor.arrow);
                changedCursor = false;
            }
        }
    }

    @Override
    public void useSchematic(Schematic schem, boolean checkHidden){
        block = null;
        schematicX = tileX(getMouseX());
        schematicY = tileY(getMouseY());

        selectPlans.clear();
        selectPlans.addAll(schematics.toPlans(schem, schematicX, schematicY, checkHidden));
        mode = none;
    }

    @Override
    public boolean isBreaking(){
        return mode == breaking;
    }

    @Override
    public void buildPlacementUI(Table table){
        table.left().margin(0f).defaults().size(48f).left();

        table.button(Icon.paste, Styles.clearNonei, () -> {
            ui.schematics.show();
        }).tooltip("@schematics");

        table.button(Icon.book, Styles.clearNonei, () -> {
            ui.database.show();
        }).tooltip("@database");

        table.button(Icon.tree, Styles.clearNonei, () -> {
            ui.research.show();
        }).visible(() -> state.isCampaign()).tooltip("@research");

        table.button(Icon.map, Styles.clearNonei, () -> {
            ui.planet.show();
        }).visible(() -> state.isCampaign()).tooltip("@planetmap");
    }

    void pollInputNoPlayer(){
        if(Core.input.keyTap(Binding.select) && !Core.scene.hasMouse()){
            tappedOne = false;

            Tile selected = tileAt(Core.input.mouseX(), Core.input.mouseY());

            if(commandMode){
                commandRect = true;
                commandRectX = input.mouseWorldX();
                commandRectY = input.mouseWorldY();
            }else if(selected != null){
                tileTapped(selected.build);
            }
        }
    }

    //player input: for controlling the player unit (will crash if the unit is not present)
    void pollInputPlayer(){
        if(scene.hasField()) return;

        Tile selected = tileAt(Core.input.mouseX(), Core.input.mouseY());
        int cursorX = tileX(Core.input.mouseX());
        int cursorY = tileY(Core.input.mouseY());
        int rawCursorX = World.toTile(Core.input.mouseWorld().x), rawCursorY = World.toTile(Core.input.mouseWorld().y);

        //automatically pause building if the current build queue is empty
        if(Core.settings.getBool("buildautopause") && isBuilding && !player.unit().isBuilding()){
            isBuilding = false;
            buildWasAutoPaused = true;
        }

        if(!selectPlans.isEmpty()){
            int shiftX = rawCursorX - schematicX, shiftY = rawCursorY - schematicY;

            selectPlans.each(s -> {
                s.x += shiftX;
                s.y += shiftY;
            });

            schematicX += shiftX;
            schematicY += shiftY;
        }

        if(Core.input.keyTap(Binding.deselect) && !ui.minimapfrag.shown() && !isPlacing() && player.unit().plans.isEmpty() && !commandMode){
            player.unit().mineTile = null;
        }

        if(Core.input.keyTap(Binding.clearBuilding) && !player.dead()){
            player.unit().clearBuilding();
        }

        if((Core.input.keyTap(Binding.schematicSelect) || Core.input.keyTap(Binding.rebuildSelect)) && !Core.scene.hasKeyboard() && mode != breaking){
            schemX = rawCursorX;
            schemY = rawCursorY;
        }

        if(Core.input.keyTap(Binding.clearBuilding) || isPlacing()){
            lastSchematic = null;
            selectPlans.clear();
        }

        if(!Core.scene.hasKeyboard() && selectX == -1 && selectY == -1 && schemX != -1 && schemY != -1){
            if(Core.input.keyRelease(Binding.schematicSelect)){
                lastSchematic = schematics.create(schemX, schemY, rawCursorX, rawCursorY);
                useSchematic(lastSchematic);
                if(selectPlans.isEmpty()){
                    lastSchematic = null;
                }
                schemX = -1;
                schemY = -1;
            }else if(input.keyRelease(Binding.rebuildSelect)){

                rebuildArea(schemX, schemY, rawCursorX, rawCursorY);
                schemX = -1;
                schemY = -1;
            }
        }

        if(!selectPlans.isEmpty()){
            if(Core.input.keyTap(Binding.schematicFlipX)){
                flipPlans(selectPlans, true);
            }

            if(Core.input.keyTap(Binding.schematicFlipY)){
                flipPlans(selectPlans, false);
            }
        }

        if(splan != null){
            int x = Math.round((Core.input.mouseWorld().x + buildPlanMouseOffsetX) / tilesize);
            int y = Math.round((Core.input.mouseWorld().y + buildPlanMouseOffsetY) / tilesize);
            if(splan.x != x || splan.y != y){
                splan.x = x;
                splan.y = y;
                movedPlan = true;
            }
        }

        if(block == null || mode != placing){
            linePlans.clear();
        }

        if(Core.input.keyTap(Binding.pauseBuilding)){
            isBuilding = !isBuilding;
            buildWasAutoPaused = false;

            if(isBuilding){
                player.shooting = false;
            }
        }

        if(isPlacing() && mode == placing && (cursorX != lastLineX || cursorY != lastLineY || Core.input.keyTap(Binding.diagonalPlacement) || Core.input.keyRelease(Binding.diagonalPlacement))){
            updateLine(selectX, selectY);
            lastLineX = cursorX;
            lastLineY = cursorY;
        }

        if(Core.input.keyRelease(Binding.select) && !Core.scene.hasMouse()){
            BuildPlan plan = getPlan(cursorX, cursorY);

            if(plan != null && !movedPlan){
                //move selected to front
                int index = player.unit().plans.indexOf(plan, true);
                if(index != -1){
                    player.unit().plans.removeIndex(index);
                    player.unit().plans.addFirst(plan);
                }
            }
        }

        if(Core.input.keyTap(Binding.select) && !Core.scene.hasMouse()){
            tappedOne = false;
            BuildPlan plan = getPlan(cursorX, cursorY);

            if(Core.input.keyDown(Binding.breakBlock)){
                mode = none;
            }else if(!selectPlans.isEmpty()){
                flushPlans(selectPlans);
                movedPlan = true;
            }else if(isPlacing()){
                selectX = cursorX;
                selectY = cursorY;
                lastLineX = cursorX;
                lastLineY = cursorY;
                mode = placing;
                updateLine(selectX, selectY);
            }else if(plan != null && !plan.breaking && mode == none && !plan.initialized && plan.progress <= 0f){
                splan = plan;
                movedPlan = false;
                buildPlanMouseOffsetX = splan.x * tilesize - Core.input.mouseWorld().x;
                buildPlanMouseOffsetY = splan.y * tilesize - Core.input.mouseWorld().y;
            }else if(plan != null && plan.breaking){
                deleting = true;
            }else if(commandMode){
                commandRect = true;
                commandRectX = input.mouseWorldX();
                commandRectY = input.mouseWorldY();
            }else if(!checkConfigTap() && selected != null && !tryRepairDerelict(selected)){
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
        }else if(Core.input.keyTap(Binding.deselect) && !selectPlans.isEmpty()){
            selectPlans.clear();
            lastSchematic = null;
        }else if(Core.input.keyTap(Binding.breakBlock) && !Core.scene.hasMouse() && player.isBuilder() && !commandMode){
            //is recalculated because setting the mode to breaking removes potential multiblock cursor offset
            deleting = false;
            mode = breaking;
            selectX = tileX(Core.input.mouseX());
            selectY = tileY(Core.input.mouseY());
            schemX = rawCursorX;
            schemY = rawCursorY;
        }

        if(Core.input.keyDown(Binding.select) && mode == none && !isPlacing() && deleting){
            var plan = getPlan(cursorX, cursorY);
            if(plan != null && plan.breaking){
                player.unit().plans().remove(plan);
            }
        }else{
            deleting = false;
        }

        if(mode == placing && block != null){
            if(!overrideLineRotation && !Core.input.keyDown(Binding.diagonalPlacement) && (selectX != cursorX || selectY != cursorY) && ((int)Core.input.axisTap(Binding.rotate) != 0)){
                rotation = ((int)((Angles.angle(selectX, selectY, cursorX, cursorY) + 45) / 90f)) % 4;
                overrideLineRotation = true;
            }
        }else{
            overrideLineRotation = false;
        }

        if(Core.input.keyRelease(Binding.breakBlock) && Core.input.keyDown(Binding.schematicSelect) && mode == breaking){
            lastSchematic = schematics.create(schemX, schemY, rawCursorX, rawCursorY);
            schemX = -1;
            schemY = -1;
        }

        if(Core.input.keyRelease(Binding.breakBlock) || Core.input.keyRelease(Binding.select)){

            if(mode == placing && block != null){ //touch up while placing, place everything in selection
                if(input.keyDown(Binding.boost)){
                    flushPlansReverse(linePlans);
                }else{
                    flushPlans(linePlans);
                }

                linePlans.clear();
                Events.fire(new LineConfirmEvent());
            }else if(mode == breaking){ //touch up while breaking, break everything in selection
                removeSelection(selectX, selectY, cursorX, cursorY, !Core.input.keyDown(Binding.schematicSelect) ? maxLength : Vars.maxSchematicSize);
                if(lastSchematic != null){
                    useSchematic(lastSchematic);
                    lastSchematic = null;
                }
            }
            selectX = -1;
            selectY = -1;

            tryDropItems(selected == null ? null : selected.build, Core.input.mouseWorld().x, Core.input.mouseWorld().y);

            if(splan != null){
                if(getPlan(splan.x, splan.y, splan.block.size, splan) != null){
                    player.unit().plans().remove(splan, true);
                }

                if(input.ctrl()){
                    inv.hide();
                    config.hideConfig();
                    planConfig.showConfig(splan);
                }else{
                    planConfig.hide();
                }

                splan = null;
            }

            mode = none;
        }


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

        if(!Core.input.keyDown(Binding.diagonalPlacement) && Math.abs((int)Core.input.axisTap(Binding.rotate)) > 0){
            rotation = Mathf.mod(rotation + (int)Core.input.axisTap(Binding.rotate), 4);

            if(splan != null){
                splan.rotation = Mathf.mod(splan.rotation + (int)Core.input.axisTap(Binding.rotate), 4);
            }

            if(isPlacing() && mode == placing){
                updateLine(selectX, selectY);
            }else if(!selectPlans.isEmpty() && !ui.chatfrag.shown()){
                rotatePlans(selectPlans, Mathf.sign(Core.input.axisTap(Binding.rotate)));
            }
        }

        Tile cursor = tileAt(Core.input.mouseX(), Core.input.mouseY());

        cursorType = SystemCursor.arrow;

        if(cursor != null){
            if(cursor.build != null && cursor.build.interactable(player.team())){
                cursorType = cursor.build.getCursor();
            }

            if(canRepairDerelict(cursor) && !player.dead() && player.unit().canBuild()){
                cursorType = ui.repairCursor;
            }

            if((isPlacing() && player.isBuilder()) || !selectPlans.isEmpty()){
                cursorType = SystemCursor.hand;
            }

            if(!isPlacing() && canMine(cursor)){
                cursorType = ui.drillCursor;
            }

            if(commandMode && selectedUnits.any()){
                boolean canAttack = (cursor.build != null && !cursor.build.inFogTo(player.team()) && cursor.build.team != player.team());

                if(!canAttack){
                    var unit = selectedEnemyUnit(input.mouseWorldX(), input.mouseWorldY());
                    if(unit != null){
                        canAttack = selectedUnits.contains(u -> u.canTarget(unit));
                    }
                }

                if(canAttack){
                    cursorType = ui.targetCursor;
                }

                if(input.keyTap(Binding.commandQueue) && Binding.commandQueue.value.key.type != KeyType.mouse){
                    commandTap(input.mouseX(), input.mouseY(), true);
                }
            }

            if(getPlan(cursor.x, cursor.y) != null && mode == none){
                cursorType = SystemCursor.hand;
            }

            if(canTapPlayer(Core.input.mouseWorld().x, Core.input.mouseWorld().y)){
                cursorType = ui.unloadCursor;
            }

            if(cursor.build != null && cursor.interactable(player.team()) && !isPlacing() && Math.abs(Core.input.axisTap(Binding.rotate)) > 0 && Core.input.keyDown(Binding.rotatePlaced) && cursor.block().rotate && cursor.block().quickRotate){
                Call.rotateBlock(player, cursor.build, Core.input.axisTap(Binding.rotate) > 0);
            }
        }
    }

    @Override
    public boolean tap(float x, float y, int count, KeyCode button){
        if(scene.hasMouse() || !commandMode) return false;

        tappedOne = true;

        //click: select a single unit
        if(button == KeyCode.mouseLeft){
            if(count >= 2){
                selectTypedUnits();
            }else{
                tapCommandUnit();
            }

        }

        return super.tap(x, y, count, button);
    }

    @Override
    public boolean touchDown(float x, float y, int pointer, KeyCode button){
        if(scene.hasMouse() || !commandMode) return false;

        if(button == KeyCode.mouseRight){
            commandTap(x, y);
        }

        if(button == Binding.commandQueue.value.key){
            commandTap(x, y, true);
        }

        return super.touchDown(x, y, pointer, button);
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
            lastSchematic = null;
            droppingItem = false;
            mode = none;
            block = null;
            splan = null;
            selectPlans.clear();
        }
    }

    @Override
    public void panCamera(Vec2 position){
        if(!locked()){
            panning = true;
            camera.position.set(position);
        }
    }

    protected void updateMovement(Unit unit){
        boolean omni = unit.type.omniMovement;

        float speed = unit.speed();
        float xa = Core.input.axis(Binding.moveX);
        float ya = Core.input.axis(Binding.moveY);
        boolean boosted = (unit instanceof Mechc && unit.isFlying());

        if(settings.getBool("detach-camera")){
            Vec2 targetPos = camera.position;

            movement.set(targetPos).sub(player).limit(speed);

            if(player.within(targetPos, 15f)){
                movement.setZero();
                unit.vel.approachDelta(Vec2.ZERO, unit.speed() * unit.type().accel / 2f);
            }
        }else{
            movement.set(xa, ya).nor().scl(speed);
            if(Core.input.keyDown(Binding.mouseMove)){
                movement.add(input.mouseWorld().sub(player).scl(1f / 25f * speed)).limit(speed);
            }
        }

        float mouseAngle = Angles.mouseAngle(unit.x, unit.y);
        boolean aimCursor = omni && player.shooting && unit.type.hasWeapons() && unit.type.faceTarget && !boosted;

        if(aimCursor){
            unit.lookAt(mouseAngle);
        }else{
            unit.lookAt(unit.prefRotation());
        }

        unit.movePref(movement);

        unit.aim(Core.input.mouseWorld());
        unit.controlWeapons(true, player.shooting && !boosted);

        player.boosting = Core.input.keyDown(Binding.boost);
        player.mouseX = unit.aimX();
        player.mouseY = unit.aimY();

        //update payload input
        if(unit instanceof Payloadc){
            if(Core.input.keyTap(Binding.pickupCargo)){
                tryPickupPayload();
                lastPayloadKeyTapMillis = Time.millis();
            }

            if(Core.input.keyDown(Binding.pickupCargo)
            && Time.timeSinceMillis(lastPayloadKeyHoldMillis) > 20
            && Time.timeSinceMillis(lastPayloadKeyTapMillis) > 200){
                tryPickupPayload();
                lastPayloadKeyHoldMillis = Time.millis();
            }

            if(Core.input.keyTap(Binding.dropCargo)){
                tryDropPayload();
                lastPayloadKeyTapMillis = Time.millis();
            }

            if(Core.input.keyDown(Binding.dropCargo)
            && Time.timeSinceMillis(lastPayloadKeyHoldMillis) > 20
            && Time.timeSinceMillis(lastPayloadKeyTapMillis) > 200){
                tryDropPayload();
                lastPayloadKeyHoldMillis = Time.millis();
            }
        }
    }
}
