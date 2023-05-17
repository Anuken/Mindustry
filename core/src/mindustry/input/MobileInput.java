package mindustry.input;

import arc.*;
import arc.graphics.g2d.*;
import arc.input.GestureDetector.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

import static arc.Core.*;
import static mindustry.Vars.*;
import static mindustry.input.PlaceMode.*;

public class MobileInput extends InputHandler implements GestureListener{
    /** Maximum speed the player can pan. */
    private static final float maxPanSpeed = 1.3f;
    /** Distance to edge of screen to start panning. */
    public final float edgePan = Scl.scl(60f);

    //gesture data
    public Vec2 vector = new Vec2(), movement = new Vec2(), targetPos = new Vec2();
    public float lastZoom = -1;

    /** Position where the player started dragging a line. */
    public int lineStartX, lineStartY, lastLineX, lastLineY;

    /** Animation scale for line. */
    public float lineScale;
    /** Animation data for crosshair. */
    public float crosshairScale;
    public Teamc lastTarget;
    /** Used for shifting build plans. */
    public float shiftDeltaX, shiftDeltaY;

    /** Place plans to be removed. */
    public Seq<BuildPlan> removals = new Seq<>();
    /** Whether the player is currently shifting all placed tiles. */
    public boolean selecting;
    /** Whether the player is currently in line-place mode. */
    public boolean lineMode, schematicMode, rebuildMode;
    /** Current place mode. */
    public PlaceMode mode = none;
    /** Whether no recipe was available when switching to break mode. */
    public @Nullable Block lastBlock;
    /** Last placed plan. Used for drawing block overlay. */
    public @Nullable BuildPlan lastPlaced;
    /** Down tracking for panning. */
    public boolean down = false;
    /** Whether manual shooting (point with finger) is enabled. */
    public boolean manualShooting = false;

    /** Current thing being shot at. */
    public @Nullable Teamc target;
    /** Payload target being moved to. Can be a position (for dropping), or a unit/block. */
    public @Nullable Position payloadTarget;
    /** Unit last tapped, or null if last tap was not on a unit. */
    public @Nullable Unit unitTapped;
    /** Control building last tapped. */
    public @Nullable Building buildingTapped;

    {
        Events.on(UnitDestroyEvent.class, e -> {
            if(e.unit != null && e.unit.isPlayer() && e.unit.getPlayer().isLocal() && e.unit.type.weapons.contains(w -> w.bullet.killShooter)){
                manualShooting = false;
            }
        });
    }

    //region utility methods

    /** Check and assign targets for a specific position. */
    void checkTargets(float x, float y){
        Unit unit = Units.closestEnemy(player.team(), x, y, 20f, u -> !u.dead);

        if(unit != null && player.unit().type.canAttack){
            player.unit().mineTile = null;
            target = unit;
        }else{
            Building tile = world.buildWorld(x, y);

            if((tile != null && player.team().isEnemy(tile.team) && (tile.team != Team.derelict || state.rules.coreCapture)) || (tile != null && player.unit().type.canHeal && tile.team == player.team() && tile.damaged())){
                player.unit().mineTile = null;
                target = tile;
            }
        }
    }

    /** Returns whether this tile is in the list of plans, or at least colliding with one. */
    boolean hasPlan(Tile tile){
        return getPlan(tile) != null;
    }

    /** Returns whether this block overlaps any selection plans. */
    boolean checkOverlapPlacement(int x, int y, Block block){
        r2.setSize(block.size * tilesize);
        r2.setCenter(x * tilesize + block.offset, y * tilesize + block.offset);

        for(var plan : selectPlans){
            Tile other = plan.tile();

            if(other == null || plan.breaking) continue;

            r1.setSize(plan.block.size * tilesize);
            r1.setCenter(other.worldx() + plan.block.offset, other.worldy() + plan.block.offset);

            if(r2.overlaps(r1)){
                return true;
            }
        }

        for(var plan : player.unit().plans()){
            Tile other = world.tile(plan.x, plan.y);

            if(other == null || plan.breaking) continue;

            r1.setSize(plan.block.size * tilesize);
            r1.setCenter(other.worldx() + plan.block.offset, other.worldy() + plan.block.offset);

            if(r2.overlaps(r1)){
                return true;
            }
        }
        return false;
    }

    /** Returns the selection plan that overlaps this tile, or null. */
    BuildPlan getPlan(Tile tile){
        r2.setSize(tilesize);
        r2.setCenter(tile.worldx(), tile.worldy());

        for(var plan : selectPlans){
            Tile other = plan.tile();

            if(other == null) continue;

            if(!plan.breaking){
                r1.setSize(plan.block.size * tilesize);
                r1.setCenter(other.worldx() + plan.block.offset, other.worldy() + plan.block.offset);

            }else{
                r1.setSize(other.block().size * tilesize);
                r1.setCenter(other.worldx() + other.block().offset, other.worldy() + other.block().offset);
            }

            if(r2.overlaps(r1)) return plan;
        }
        return null;
    }

    void removePlan(BuildPlan plan){
        selectPlans.remove(plan, true);
        if(!plan.breaking){
            removals.add(plan);
        }
    }

    boolean isLinePlacing(){
        return mode == placing && lineMode && Mathf.dst(lineStartX * tilesize, lineStartY * tilesize, Core.input.mouseWorld().x, Core.input.mouseWorld().y) >= 3 * tilesize;
    }

    boolean isAreaBreaking(){
        return mode == breaking && lineMode && Mathf.dst(lineStartX * tilesize, lineStartY * tilesize, Core.input.mouseWorld().x, Core.input.mouseWorld().y) >= 2 * tilesize;
    }

    //endregion
    //region UI and drawing

    @Override
    public void buildPlacementUI(Table table){
        table.image().color(Pal.gray).height(4f).colspan(4).growX();
        table.row();
        table.left().margin(0f).defaults().size(48f);

        table.button(Icon.hammer, Styles.clearNoneTogglei, () -> {
            mode = mode == breaking ? block == null ? none : placing : breaking;
            lastBlock = block;
        }).update(l -> l.setChecked(mode == breaking)).name("breakmode");

        //diagonal swap button
        table.button(Icon.diagonal, Styles.clearNoneTogglei, () -> {
            Core.settings.put("swapdiagonal", !Core.settings.getBool("swapdiagonal"));
        }).update(l -> l.setChecked(Core.settings.getBool("swapdiagonal")));

        //rotate button
        table.button(Icon.right, Styles.clearNoneTogglei, () -> {
            if(block != null && block.rotate){
                rotation = Mathf.mod(rotation + 1, 4);
            }else{
                schematicMode = !schematicMode;
                if(schematicMode){
                    block = null;
                    mode = none;
                }else{
                    rebuildMode = false;
                }
            }
        }).update(i -> {
            boolean arrow = block != null && block.rotate;

            i.getImage().setRotationOrigin(!arrow ? 0 : rotation * 90, Align.center);
            i.getStyle().imageUp = arrow ? Icon.right : Icon.copy;
            i.setChecked(!arrow && schematicMode);
        });

        //confirm button
        table.button(Icon.ok, Styles.clearNoneTogglei, () -> {
            if(schematicMode){
                rebuildMode = !rebuildMode;
            }else{
                for(BuildPlan plan : selectPlans){
                    Tile tile = plan.tile();

                    //actually place/break all selected blocks
                    if(tile != null){
                        if(!plan.breaking){
                            if(validPlace(plan.x, plan.y, plan.block, plan.rotation)){
                                BuildPlan other = getPlan(plan.x, plan.y, plan.block.size, null);
                                BuildPlan copy = plan.copy();

                                if(other == null){
                                    player.unit().addBuild(copy);
                                }else if(!other.breaking && other.x == plan.x && other.y == plan.y && other.block.size == plan.block.size){
                                    player.unit().plans().remove(other);
                                    player.unit().addBuild(copy);
                                }
                            }

                            rotation = plan.rotation;
                        }else{
                            tryBreakBlock(tile.x, tile.y);
                        }
                    }
                }

                //move all current plans to removal array so they fade out
                removals.addAll(selectPlans.select(r -> !r.breaking));
                selectPlans.clear();
                selecting = false;
            }
        }).visible(() -> !selectPlans.isEmpty() || schematicMode || rebuildMode).update(i -> {
            i.getStyle().imageUp = schematicMode || rebuildMode ? Icon.wrench : Icon.ok;
            i.setChecked(rebuildMode);

        }).name("confirmplace");
    }

    boolean showCancel(){
        return (player.unit().isBuilding() || block != null || mode == breaking || !selectPlans.isEmpty()) && !hasSchem();
    }

    boolean hasSchem(){
        return lastSchematic != null && !selectPlans.isEmpty();
    }

    @Override
    public void buildUI(Group group){

        group.fill(t -> {
            t.visible(this::showCancel);
            t.bottom().left();
            t.button("@cancel", Icon.cancel, () -> {
                player.unit().clearBuilding();
                selectPlans.clear();
                mode = none;
                block = null;
            }).width(155f).height(50f).margin(12f);
        });

        group.fill(t -> {
            t.visible(() -> !showCancel() && block == null && !hasSchem());
            t.bottom().left();
            t.button("@command", Icon.units, Styles.squareTogglet, () -> {
                commandMode = !commandMode;
            }).width(155f).height(50f).margin(12f).checked(b -> commandMode).row();

            //for better looking insets
            t.rect((x, y, w, h) -> {
                if(Core.scene.marginBottom > 0){
                    Tex.paneRight.draw(x, 0, w, y);
                }
            }).fillX().row();
        });

        group.fill(t -> {
            t.visible(this::hasSchem);
            t.bottom().left();
            t.table(Tex.pane, b -> {
                b.defaults().size(50f);

                ImageButtonStyle style = Styles.clearNonei;

                b.button(Icon.save, style, this::showSchematicSave).disabled(f -> lastSchematic == null || lastSchematic.file != null);
                b.button(Icon.cancel, style, () -> {
                    selectPlans.clear();
                    lastSchematic = null;
                });
                b.row();
                b.button(Icon.flipX, style, () -> flipPlans(selectPlans, true));
                b.button(Icon.flipY, style, () -> flipPlans(selectPlans, false));
                b.row();
                b.button(Icon.rotate, style, () -> rotatePlans(selectPlans, 1)).update(i -> {
                    var img = i.getCells().first().get();

                    img.setScale(-1f, 1f);
                    //why the heck doesn't setOrigin work for scaling
                    img.setTranslation(img.getWidth(), 0f);
                });

            }).margin(4f);
        });
    }

    @Override
    public void drawBottom(){
        Lines.stroke(1f);

        //draw plans about to be removed
        for(BuildPlan plan : removals){
            Tile tile = plan.tile();

            if(tile == null) continue;

            plan.animScale = Mathf.lerpDelta(plan.animScale, 0f, 0.2f);

            if(plan.breaking){
                drawSelected(plan.x, plan.y, tile.block(), Pal.remove);
            }else{
                plan.block.drawPlan(plan, allPlans(), true);
            }
        }

        Draw.mixcol();
        Draw.color(Pal.accent);

        //Draw lines
        if(lineMode){
            int tileX = tileX(Core.input.mouseX());
            int tileY = tileY(Core.input.mouseY());

            if(mode == placing && block != null){
                //draw placing
                for(int i = 0; i < linePlans.size; i++){
                    BuildPlan plan = linePlans.get(i);
                    if(i == linePlans.size - 1 && plan.block.rotate){
                        drawArrow(block, plan.x, plan.y, plan.rotation);
                    }
                    plan.block.drawPlan(plan, allPlans(), validPlace(plan.x, plan.y, plan.block, plan.rotation) && getPlan(plan.x, plan.y, plan.block.size, null) == null);
                    drawSelected(plan.x, plan.y, plan.block, Pal.accent);
                }
                linePlans.each(this::drawOverPlan);
            }else if(mode == breaking){
                drawBreakSelection(lineStartX, lineStartY, tileX, tileY);
            }
        }

        Draw.reset();
    }

    @Override
    public void drawTop(){
        if(mode == schematicSelect){
            drawSelection(lineStartX, lineStartY, lastLineX, lastLineY, Vars.maxSchematicSize);
        }else if(mode == rebuildSelect){
            drawRebuildSelection(lineStartX, lineStartY, lastLineX, lastLineY);
        }

        drawCommanded();
    }

    @Override
    public void drawOverSelect(){
        //draw list of plans
        for(BuildPlan plan : selectPlans){
            Tile tile = plan.tile();

            if(tile == null) continue;

            if((!plan.breaking && validPlace(tile.x, tile.y, plan.block, plan.rotation))
            || (plan.breaking && validBreak(tile.x, tile.y))){
                plan.animScale = Mathf.lerpDelta(plan.animScale, 1f, 0.2f);
            }else{
                plan.animScale = Mathf.lerpDelta(plan.animScale, 0.6f, 0.1f);
            }

            Tmp.c1.set(Draw.getMixColor());

            if(!plan.breaking && plan == lastPlaced && plan.block != null){
                Draw.mixcol();
                if(plan.block.rotate && plan.block.drawArrow) drawArrow(plan.block, tile.x, tile.y, plan.rotation);
            }

            Draw.reset();
            drawPlan(plan);
            if(!plan.breaking){
                drawOverPlan(plan);
            }

            //draw last placed plan
            if(!plan.breaking && plan == lastPlaced && plan.block != null){
                boolean valid = validPlace(tile.x, tile.y, plan.block, rotation);
                Draw.mixcol();
                plan.block.drawPlace(tile.x, tile.y, rotation, valid);

                drawOverlapCheck(plan.block, tile.x, tile.y, valid);
            }
        }

        //draw targeting crosshair
        if(target != null && !state.isEditor() && !manualShooting){
            if(target != lastTarget){
                crosshairScale = 0f;
                lastTarget = target;
            }

            crosshairScale = Mathf.lerpDelta(crosshairScale, 1f, 0.2f);

            Drawf.target(target.getX(), target.getY(), 7f * Interp.swingIn.apply(crosshairScale), Pal.remove);
        }

        Draw.reset();
    }

    @Override
    protected void drawPlan(BuildPlan plan){
        if(plan.tile() == null) return;
        bplan.animScale = plan.animScale = Mathf.lerpDelta(plan.animScale, 1f, 0.1f);

        if(plan.breaking){
            drawSelected(plan.x, plan.y, plan.tile().block(), Pal.remove);
        }else{
            plan.block.drawPlan(plan, allPlans(), validPlace(plan.x, plan.y, plan.block, plan.rotation));
            drawSelected(plan.x, plan.y, plan.block, Pal.accent);
        }
    }

    //endregion
    //region input events, overrides

    @Override
    public boolean isRebuildSelecting(){
        return rebuildMode;
    }

    @Override
    protected int schemOriginX(){
        Tmp.v1.setZero();
        selectPlans.each(r -> Tmp.v1.add(r.drawx(), r.drawy()));
        return World.toTile(Tmp.v1.scl(1f / selectPlans.size).x);
    }

    @Override
    protected int schemOriginY(){
        Tmp.v1.setZero();
        selectPlans.each(r -> Tmp.v1.add(r.drawx(), r.drawy()));
        return World.toTile(Tmp.v1.scl(1f / selectPlans.size).y);
    }

    @Override
    public boolean isPlacing(){
        return super.isPlacing() && mode == placing;
    }

    @Override
    public boolean isBreaking(){
        return mode == breaking;
    }

    @Override
    public void useSchematic(Schematic schem){
        selectPlans.clear();
        selectPlans.addAll(schematics.toPlans(schem, World.toTile(Core.camera.position.x), World.toTile(Core.camera.position.y)));
        lastSchematic = schem;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, KeyCode button){
        if(state.isMenu() || locked()) return false;

        down = true;

        if(player.dead()) return false;

        //get tile on cursor
        Tile cursor = tileAt(screenX, screenY);

        float worldx = Core.input.mouseWorld(screenX, screenY).x, worldy = Core.input.mouseWorld(screenX, screenY).y;

        //ignore off-screen taps
        if(cursor == null || Core.scene.hasMouse(screenX, screenY)) return false;

        //only begin selecting if the tapped block is a plan
        selecting = hasPlan(cursor);

        //call tap events
        if(pointer == 0 && !selecting){
            if(schematicMode && block == null){
                mode = rebuildMode ? rebuildSelect : schematicSelect;

                //engage schematic selection mode
                int tileX = tileX(screenX);
                int tileY = tileY(screenY);
                lineStartX = tileX;
                lineStartY = tileY;
                lastLineX = tileX;
                lastLineY = tileY;
            }else if(!tryTapPlayer(worldx, worldy) && Core.settings.getBool("keyboard")){
                //shoot on touch down when in keyboard mode
                player.shooting = true;
            }
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, KeyCode button){
        lastZoom = renderer.getScale();

        if(!Core.input.isTouched()){
            down = false;
        }

        manualShooting = false;
        selecting = false;

        //place down a line if in line mode
        if(lineMode){
            int tileX = tileX(screenX);
            int tileY = tileY(screenY);

            if(mode == placing && isPlacing()){
                flushSelectPlans(linePlans);
                Events.fire(new LineConfirmEvent());
            }else if(mode == breaking){
                removeSelection(lineStartX, lineStartY, tileX, tileY, true);
            }

            lineMode = false;
        }else if(mode == schematicSelect){
            selectPlans.clear();
            lastSchematic = schematics.create(lineStartX, lineStartY, lastLineX, lastLineY);
            useSchematic(lastSchematic);
            if(selectPlans.isEmpty()){
                lastSchematic = null;
            }
            schematicMode = false;
            mode = none;
        }else if(mode == rebuildSelect){
            rebuildArea(lineStartX, lineStartY, lastLineX, lastLineY);
            mode = none;
        }else{
            Tile tile = tileAt(screenX, screenY);

            tryDropItems(tile == null ? null : tile.build, Core.input.mouseWorld(screenX, screenY).x, Core.input.mouseWorld(screenX, screenY).y);
        }

        //select some units
        selectUnitsRect();

        return false;
    }

    @Override
    public boolean longPress(float x, float y){
        if(state.isMenu()|| player.dead() || locked()) return false;

        //get tile on cursor
        Tile cursor = tileAt(x, y);

        if(Core.scene.hasMouse(x, y) || schematicMode) return false;

        //handle long tap when player isn't building
        if(mode == none){
            Vec2 pos = Core.input.mouseWorld(x, y);

            if(commandMode){

                //long press begins rect selection.
                commandRect = true;
                commandRectX = input.mouseWorldX();
                commandRectY = input.mouseWorldY();

            }else{

                if(player.unit() instanceof Payloadc pay){
                    Unit target = Units.closest(player.team(), pos.x, pos.y, 8f, u -> u.isAI() && u.isGrounded() && pay.canPickup(u) && u.within(pos, u.hitSize + 8f));
                    if(target != null){
                        payloadTarget = target;
                    }else{
                        Building build = world.buildWorld(pos.x, pos.y);

                        if(build != null && build.team == player.team() && (pay.canPickup(build) || build.getPayload() != null && pay.canPickupPayload(build.getPayload()))){
                            payloadTarget = build;
                        }else if(pay.hasPayload()){
                            //drop off at position
                            payloadTarget = new Vec2(pos);
                        }else{
                            manualShooting = true;
                            this.target = null;
                        }
                    }
                }else{
                    manualShooting = true;
                    this.target = null;
                }
            }

            if(!state.isPaused()) Fx.select.at(pos);
        }else{

            //ignore off-screen taps
            if(cursor == null) return false;

            //remove plan if it's there
            //long pressing enables line mode otherwise
            lineStartX = cursor.x;
            lineStartY = cursor.y;
            lastLineX = cursor.x;
            lastLineY = cursor.y;
            lineMode = true;

            if(mode == breaking){
                if(!state.isPaused()) Fx.tapBlock.at(cursor.worldx(), cursor.worldy(), 1f);
            }else if(block != null){
                updateLine(lineStartX, lineStartY, cursor.x, cursor.y);
                if(!state.isPaused()) Fx.tapBlock.at(cursor.worldx() + block.offset, cursor.worldy() + block.offset, block.size);
            }
        }

        return false;
    }

    @Override
    public boolean tap(float x, float y, int count, KeyCode button){
        if(state.isMenu() || lineMode || locked()) return false;

        float worldx = Core.input.mouseWorld(x, y).x, worldy = Core.input.mouseWorld(x, y).y;

        //get tile on cursor
        Tile cursor = tileAt(x, y);

        //ignore off-screen taps
        if(cursor == null || Core.scene.hasMouse(x, y)) return false;

        Call.tileTap(player, cursor);

        Tile linked = cursor.build == null ? cursor : cursor.build.tile;

        if(!player.dead()){
            checkTargets(worldx, worldy);
        }

        //remove if plan present
        if(hasPlan(cursor)){
            removePlan(getPlan(cursor));
        }else if(mode == placing && isPlacing() && validPlace(cursor.x, cursor.y, block, rotation) && !checkOverlapPlacement(cursor.x, cursor.y, block)){
            //add to selection queue if it's a valid place position
            selectPlans.add(lastPlaced = new BuildPlan(cursor.x, cursor.y, rotation, block, block.nextConfig()));
            block.onNewPlan(lastPlaced);
        }else if(mode == breaking && validBreak(linked.x,linked.y) && !hasPlan(linked)){
            //add to selection queue if it's a valid BREAK position
            selectPlans.add(new BuildPlan(linked.x, linked.y));
        }else if((commandMode && selectedUnits.size > 0) || commandBuildings.size > 0){
            //handle selecting units with command mode
            commandTap(x, y);
        }else if(commandMode){
            tapCommandUnit();
        }else{
            //control units
            if(count == 2){
                //reset payload target
                payloadTarget = null;

                //control a unit/block detected on first tap of double-tap
                if(unitTapped != null && state.rules.possessionAllowed && unitTapped.isAI() && unitTapped.team == player.team() && !unitTapped.dead && unitTapped.type.playerControllable){
                    Call.unitControl(player, unitTapped);
                    recentRespawnTimer = 1f;
                }else if(buildingTapped != null && state.rules.possessionAllowed){
                    Call.buildingControlSelect(player, buildingTapped);
                    recentRespawnTimer = 1f;
                }else if(!checkConfigTap() && !tryBeginMine(cursor)){
                    tileTapped(linked.build);
                }
                return false;
            }

            unitTapped = selectedUnit();
            buildingTapped = selectedControlBuild();

            //prevent mining if placing/breaking blocks
            if(!tryStopMine() && !canTapPlayer(worldx, worldy) && !checkConfigTap() && !tileTapped(linked.build) && mode == none && !Core.settings.getBool("doubletapmine")){
                tryBeginMine(cursor);
            }
        }

        return false;
    }

    @Override
    public void updateState(){
        super.updateState();

        if(state.isMenu()){
            selectPlans.clear();
            removals.clear();
            mode = none;
            manualShooting = false;
            payloadTarget = null;
        }
    }

    @Override
    public void update(){
        super.update();

        boolean locked = locked();

        if(player.dead()){
            mode = none;
            manualShooting = false;
            payloadTarget = null;
        }

        if(locked || block != null || scene.hasField() || hasSchem() || selectPlans.size > 0){
            commandMode = false;
        }

        //validate commanding units
        selectedUnits.removeAll(u -> !u.isCommandable() || !u.isValid());

        if(!commandMode){
            commandBuildings.clear();
            selectedUnits.clear();
        }

        //zoom camera
        if(!locked && Math.abs(Core.input.axisTap(Binding.zoom)) > 0 && !Core.input.keyDown(Binding.rotateplaced) && (Core.input.keyDown(Binding.diagonal_placement) || ((!player.isBuilder() || !isPlacing() || !block.rotate) && selectPlans.isEmpty()))){
            renderer.scaleCamera(Core.input.axisTap(Binding.zoom));
        }

        if(!Core.settings.getBool("keyboard") && !locked){
            //move camera around
            float camSpeed = 6f;
            Core.camera.position.add(Tmp.v1.setZero().add(Core.input.axis(Binding.move_x), Core.input.axis(Binding.move_y)).nor().scl(Time.delta * camSpeed));
        }

        if(Core.settings.getBool("keyboard")){
            if(Core.input.keyRelease(Binding.select)){
                player.shooting = false;
            }

            if(player.shooting && !canShoot()){
                player.shooting = false;
            }
        }

        if(!player.dead() && !state.isPaused() && !locked){
            updateMovement(player.unit());
        }

        //reset state when not placing
        if(mode == none){
            lineMode = false;
        }

        if(lineMode && mode == placing && block == null){
            lineMode = false;
        }

        //if there is no mode and there's a recipe, switch to placing
        if(block != null && mode == none){
            mode = placing;
        }

        if(block == null && mode == placing){
            mode = none;
        }

        //stop schematic when in block mode
        if(block != null){
            schematicMode = false;
        }

        //stop select when not in schematic mode
        if(!schematicMode && (mode == schematicSelect || mode == rebuildSelect)){
            mode = none;
        }

        if(!rebuildMode && mode == rebuildSelect){
            mode = none;
        }

        if(mode == schematicSelect || mode == rebuildSelect){
            lastLineX = rawTileX();
            lastLineY = rawTileY();
            autoPan();
        }

        //automatically switch to placing after a new recipe is selected
        if(lastBlock != block && mode == breaking && block != null){
            mode = placing;
            lastBlock = block;
        }

        if(lineMode){
            lineScale = Mathf.lerpDelta(lineScale, 1f, 0.1f);

            //When in line mode, pan when near screen edges automatically
            if(Core.input.isTouched(0)){
                autoPan();
            }

            int lx = tileX(Core.input.mouseX()), ly = tileY(Core.input.mouseY());

            if((lastLineX != lx || lastLineY != ly) && isPlacing()){
                lastLineX = lx;
                lastLineY = ly;
                updateLine(lineStartX, lineStartY, lx, ly);
            }
        }else{
            linePlans.clear();
            lineScale = 0f;
        }

        //remove place plans that have disappeared
        for(int i = removals.size - 1; i >= 0; i--){

            if(removals.get(i).animScale <= 0.0001f){
                removals.remove(i);
                i--;
            }
        }

        if(player.shooting && (player.unit().activelyBuilding() || player.unit().mining())){
            player.shooting = false;
        }
    }

    protected void autoPan(){
        float screenX = Core.input.mouseX(), screenY = Core.input.mouseY();

        float panX = 0, panY = 0;

        if(screenX <= edgePan){
            panX = -(edgePan - screenX);
        }

        if(screenX >= Core.graphics.getWidth() - edgePan){
            panX = (screenX - Core.graphics.getWidth()) + edgePan;
        }

        if(screenY <= edgePan){
            panY = -(edgePan - screenY);
        }

        if(screenY >= Core.graphics.getHeight() - edgePan){
            panY = (screenY - Core.graphics.getHeight()) + edgePan;
        }

        vector.set(panX, panY).scl((Core.camera.width) / Core.graphics.getWidth());
        vector.limit(maxPanSpeed);

        //pan view
        Core.camera.position.x += vector.x;
        Core.camera.position.y += vector.y;
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY){
        if(Core.scene == null || Core.scene.hasDialog() || Core.settings.getBool("keyboard") || locked() || commandRect) return false;

        float scale = Core.camera.width / Core.graphics.getWidth();
        deltaX *= scale;
        deltaY *= scale;

        //can't pan in line mode with one finger or while dropping items!
        if((lineMode && !Core.input.isTouched(1)) || droppingItem || schematicMode){
            return false;
        }

        //do not pan with manual shooting enabled
        if(!down || manualShooting) return false;

        if(selecting){ //pan all plans
            shiftDeltaX += deltaX;
            shiftDeltaY += deltaY;

            int shiftedX = (int)(shiftDeltaX / tilesize);
            int shiftedY = (int)(shiftDeltaY / tilesize);

            if(Math.abs(shiftedX) > 0 || Math.abs(shiftedY) > 0){
                for(var plan : selectPlans){
                    if(plan.breaking) continue; //don't shift removal plans
                    plan.x += shiftedX;
                    plan.y += shiftedY;
                }

                shiftDeltaX %= tilesize;
                shiftDeltaY %= tilesize;
            }
        }else{
            //pan player
            Core.camera.position.x -= deltaX;
            Core.camera.position.y -= deltaY;
        }

        return false;
    }

    @Override
    public boolean panStop(float x, float y, int pointer, KeyCode button){
        shiftDeltaX = shiftDeltaY = 0f;
        return false;
    }

    @Override
    public boolean zoom(float initialDistance, float distance){
        if(Core.settings.getBool("keyboard")) return false;
        if(lastZoom < 0){
            lastZoom = renderer.getScale();
        }

        renderer.setScale(distance / initialDistance * lastZoom);
        return true;
    }

    //endregion
    //region movement

    protected void updateMovement(Unit unit){
        Rect rect = Tmp.r3;

        UnitType type = unit.type;
        if(type == null) return;

        boolean omni = unit.type.omniMovement;
        boolean allowHealing = type.canHeal;
        boolean validHealTarget = allowHealing && target instanceof Building b && b.isValid() && target.team() == unit.team && b.damaged() && target.within(unit, type.range);
        boolean boosted = (unit instanceof Mechc && unit.isFlying());

        //reset target if:
        // - in the editor, or...
        // - it's both an invalid standard target and an invalid heal target
        if((Units.invalidateTarget(target, unit, type.range) && !validHealTarget) || state.isEditor()){
            target = null;
        }

        targetPos.set(Core.camera.position);
        float attractDst = 15f;

        float speed = unit.speed();
        float range = unit.hasWeapons() ? unit.range() : 0f;
        float bulletSpeed = unit.hasWeapons() ? type.weapons.first().bullet.speed : 0f;
        float mouseAngle = unit.angleTo(unit.aimX(), unit.aimY());
        boolean aimCursor = omni && player.shooting && type.hasWeapons() && !boosted && type.faceTarget;

        if(aimCursor){
            unit.lookAt(mouseAngle);
        }else{
            unit.lookAt(unit.prefRotation());
        }

        //validate payload, if it's a destroyed unit/building, remove it
        if(payloadTarget instanceof Healthc h && !h.isValid()){
            payloadTarget = null;
        }

        if(payloadTarget != null && unit instanceof Payloadc pay){
            targetPos.set(payloadTarget);
            attractDst = 0f;

            if(unit.within(payloadTarget, 3f * Time.delta)){
                if(payloadTarget instanceof Vec2 && pay.hasPayload()){
                    //vec -> dropping something
                    tryDropPayload();
                }else if(payloadTarget instanceof Building build && build.team == unit.team){
                    //building -> picking building up
                    Call.requestBuildPayload(player, build);
                }else if(payloadTarget instanceof Unit other && pay.canPickup(other)){
                    //unit -> picking unit up
                    Call.requestUnitPayload(player, other);
                }

                payloadTarget = null;
            }
        }else{
            payloadTarget = null;
        }

        movement.set(targetPos).sub(player).limit(speed);
        movement.setAngle(Mathf.slerp(movement.angle(), unit.vel.angle(), 0.05f));

        if(player.within(targetPos, attractDst)){
            movement.setZero();
            unit.vel.approachDelta(Vec2.ZERO, unit.speed() * type.accel / 2f);
        }

        unit.hitbox(rect);
        rect.grow(4f);

        player.boosting = collisions.overlapsTile(rect, EntityCollisions::solid) || !unit.within(targetPos, 85f);

        unit.movePref(movement);

        //update shooting if not building + not mining
        if(!player.unit().activelyBuilding() && player.unit().mineTile == null){

            //autofire targeting
            if(manualShooting){
                player.shooting = !boosted;
                unit.aim(player.mouseX = Core.input.mouseWorldX(), player.mouseY = Core.input.mouseWorldY());
            }else if(target == null){
                player.shooting = false;
                if(Core.settings.getBool("autotarget") && !(player.unit() instanceof BlockUnitUnit u && u.tile() instanceof ControlBlock c && !c.shouldAutoTarget())){
                    if(player.unit().type.canAttack){
                        target = Units.closestTarget(unit.team, unit.x, unit.y, range, u -> u.checkTarget(type.targetAir, type.targetGround), u -> type.targetGround);
                    }

                    if(allowHealing && target == null){
                        target = Geometry.findClosest(unit.x, unit.y, indexer.getDamaged(Team.sharded));
                        if(target != null && !unit.within(target, range)){
                            target = null;
                        }
                    }
                }

                //when not shooting, aim at mouse cursor
                //this may be a bad idea, aiming for a point far in front could work better, test it out
                unit.aim(Core.input.mouseWorldX(), Core.input.mouseWorldY());
            }else{
                Vec2 intercept = Predict.intercept(unit, target, bulletSpeed);

                player.mouseX = intercept.x;
                player.mouseY = intercept.y;
                player.shooting = !boosted;

                unit.aim(player.mouseX, player.mouseY);
            }
        }

        unit.controlWeapons(player.shooting && !boosted);
    }

    //endregion
}
