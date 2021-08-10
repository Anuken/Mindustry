package mindustry.input;

import arc.*;
import arc.func.*;
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
    /** Used for shifting build requests. */
    public float shiftDeltaX, shiftDeltaY;

    /** Place requests to be removed. */
    public Seq<BuildPlan> removals = new Seq<>();
    /** Whether or not the player is currently shifting all placed tiles. */
    public boolean selecting;
    /** Whether the player is currently in line-place mode. */
    public boolean lineMode, schematicMode;
    /** Current place mode. */
    public PlaceMode mode = none;
    /** Whether no recipe was available when switching to break mode. */
    public @Nullable Block lastBlock;
    /** Last placed request. Used for drawing block overlay. */
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

    //region utility methods

    /** Check and assign targets for a specific position. */
    void checkTargets(float x, float y){
        Unit unit = Units.closestEnemy(player.team(), x, y, 20f, u -> !u.dead);

        if(unit != null){
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

    /** Returns whether this tile is in the list of requests, or at least colliding with one. */
    boolean hasRequest(Tile tile){
        return getRequest(tile) != null;
    }

    /** Returns whether this block overlaps any selection requests. */
    boolean checkOverlapPlacement(int x, int y, Block block){
        r2.setSize(block.size * tilesize);
        r2.setCenter(x * tilesize + block.offset, y * tilesize + block.offset);

        for(BuildPlan req : selectRequests){
            Tile other = req.tile();

            if(other == null || req.breaking) continue;

            r1.setSize(req.block.size * tilesize);
            r1.setCenter(other.worldx() + req.block.offset, other.worldy() + req.block.offset);

            if(r2.overlaps(r1)){
                return true;
            }
        }

        for(BuildPlan req : player.unit().plans()){
            Tile other = world.tile(req.x, req.y);

            if(other == null || req.breaking) continue;

            r1.setSize(req.block.size * tilesize);
            r1.setCenter(other.worldx() + req.block.offset, other.worldy() + req.block.offset);

            if(r2.overlaps(r1)){
                return true;
            }
        }
        return false;
    }

    /** Returns the selection request that overlaps this tile, or null. */
    BuildPlan getRequest(Tile tile){
        r2.setSize(tilesize);
        r2.setCenter(tile.worldx(), tile.worldy());

        for(BuildPlan req : selectRequests){
            Tile other = req.tile();

            if(other == null) continue;

            if(!req.breaking){
                r1.setSize(req.block.size * tilesize);
                r1.setCenter(other.worldx() + req.block.offset, other.worldy() + req.block.offset);

            }else{
                r1.setSize(other.block().size * tilesize);
                r1.setCenter(other.worldx() + other.block().offset, other.worldy() + other.block().offset);
            }

            if(r2.overlaps(r1)) return req;
        }
        return null;
    }

    void removeRequest(BuildPlan request){
        selectRequests.remove(request, true);
        if(!request.breaking){
            removals.add(request);
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

        table.button(Icon.hammer, Styles.clearTogglePartiali, () -> {
            mode = mode == breaking ? block == null ? none : placing : breaking;
            lastBlock = block;
        }).update(l -> l.setChecked(mode == breaking)).name("breakmode");

        //diagonal swap button
        table.button(Icon.diagonal, Styles.clearTogglePartiali, () -> {
            Core.settings.put("swapdiagonal", !Core.settings.getBool("swapdiagonal"));
        }).update(l -> l.setChecked(Core.settings.getBool("swapdiagonal")));

        //rotate button
        table.button(Icon.right, Styles.clearTogglePartiali, () -> {
            if(block != null && block.rotate){
                rotation = Mathf.mod(rotation + 1, 4);
            }else{
                schematicMode = !schematicMode;
                if(schematicMode){
                    block = null;
                    mode = none;
                }
            }
        }).update(i -> {
            boolean arrow = block != null && block.rotate;

            i.getImage().setRotationOrigin(!arrow ? 0 : rotation * 90, Align.center);
            i.getStyle().imageUp = arrow ? Icon.right : Icon.copy;
            i.setChecked(!arrow && schematicMode);
        });

        //confirm button
        table.button(Icon.ok, Styles.clearPartiali, () -> {
            for(BuildPlan request : selectRequests){
                Tile tile = request.tile();

                //actually place/break all selected blocks
                if(tile != null){
                    if(!request.breaking){
                        if(validPlace(request.x, request.y, request.block, request.rotation)){
                            BuildPlan other = getRequest(request.x, request.y, request.block.size, null);
                            BuildPlan copy = request.copy();

                            if(other == null){
                                player.unit().addBuild(copy);
                            }else if(!other.breaking && other.x == request.x && other.y == request.y && other.block.size == request.block.size){
                                player.unit().plans().remove(other);
                                player.unit().addBuild(copy);
                            }
                        }

                        rotation = request.rotation;
                    }else{
                        tryBreakBlock(tile.x, tile.y);
                    }
                }
            }

            //move all current requests to removal array so they fade out
            removals.addAll(selectRequests.select(r -> !r.breaking));
            selectRequests.clear();
            selecting = false;
        }).visible(() -> !selectRequests.isEmpty()).name("confirmplace");
    }

    @Override
    public void buildUI(Group group){
        Boolp schem = () -> lastSchematic != null && !selectRequests.isEmpty();

        group.fill(t -> {
            t.visible(() -> (player.unit().isBuilding() || block != null || mode == breaking || !selectRequests.isEmpty()) && !schem.get());
            t.bottom().left();
            t.button("@cancel", Icon.cancel, () -> {
                player.unit().clearBuilding();
                selectRequests.clear();
                mode = none;
                block = null;
            }).width(155f).height(50f).margin(12f);
        });

        group.fill(t -> {
            t.visible(schem);
            t.bottom().left();
            t.table(Tex.pane, b -> {
                b.defaults().size(50f);

                ImageButtonStyle style = Styles.clearPartiali;

                b.button(Icon.save, style, this::showSchematicSave).disabled(f -> lastSchematic == null || lastSchematic.file != null);
                b.button(Icon.cancel, style, () -> {
                    selectRequests.clear();
                    lastSchematic = null;
                });
                b.row();
                b.button(Icon.flipX, style, () -> flipRequests(selectRequests, true));
                b.button(Icon.flipY, style, () -> flipRequests(selectRequests, false));
                b.row();
                b.button(Icon.rotate, style, () -> rotateRequests(selectRequests, 1));

            }).margin(4f);
        });
    }

    @Override
    public void drawBottom(){
        Lines.stroke(1f);

        //draw requests about to be removed
        for(BuildPlan request : removals){
            Tile tile = request.tile();

            if(tile == null) continue;

            request.animScale = Mathf.lerpDelta(request.animScale, 0f, 0.2f);

            if(request.breaking){
                drawSelected(request.x, request.y, tile.block(), Pal.remove);
            }else{
                request.block.drawPlan(request, allRequests(), true);
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
                for(int i = 0; i < lineRequests.size; i++){
                    BuildPlan request = lineRequests.get(i);
                    if(i == lineRequests.size - 1 && request.block.rotate){
                        drawArrow(block, request.x, request.y, request.rotation);
                    }
                    request.block.drawPlan(request, allRequests(), validPlace(request.x, request.y, request.block, request.rotation) && getRequest(request.x, request.y, request.block.size, null) == null);
                    drawSelected(request.x, request.y, request.block, Pal.accent);
                }
                lineRequests.each(this::drawOverRequest);
            }else if(mode == breaking){
                drawBreakSelection(lineStartX, lineStartY, tileX, tileY);
            }
        }

        Draw.reset();
    }

    @Override
    public void drawTop(){
        //draw schematic selection
        if(mode == schematicSelect){
            drawSelection(lineStartX, lineStartY, lastLineX, lastLineY, Vars.maxSchematicSize);
        }
    }

    @Override
    public void drawOverSelect(){
        //draw list of requests
        for(BuildPlan request : selectRequests){
            Tile tile = request.tile();

            if(tile == null) continue;

            if((!request.breaking && validPlace(tile.x, tile.y, request.block, request.rotation))
            || (request.breaking && validBreak(tile.x, tile.y))){
                request.animScale = Mathf.lerpDelta(request.animScale, 1f, 0.2f);
            }else{
                request.animScale = Mathf.lerpDelta(request.animScale, 0.6f, 0.1f);
            }

            Tmp.c1.set(Draw.getMixColor());

            if(!request.breaking && request == lastPlaced && request.block != null){
                Draw.mixcol();
                if(request.block.rotate) drawArrow(request.block, tile.x, tile.y, request.rotation);
            }

            Draw.reset();
            drawRequest(request);
            if(!request.breaking){
                drawOverRequest(request);
            }

            //draw last placed request
            if(!request.breaking && request == lastPlaced && request.block != null && request.block.drawArrow){
                Draw.mixcol();
                request.block.drawPlace(tile.x, tile.y, rotation, validPlace(tile.x, tile.y, request.block, rotation));
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
    protected void drawRequest(BuildPlan request){
        if(request.tile() == null) return;
        brequest.animScale = request.animScale = Mathf.lerpDelta(request.animScale, 1f, 0.1f);

        if(request.breaking){
            drawSelected(request.x, request.y, request.tile().block(), Pal.remove);
        }else{
            request.block.drawPlan(request, allRequests(), validPlace(request.x, request.y, request.block, request.rotation));
            drawSelected(request.x, request.y, request.block, Pal.accent);
        }
    }

    //endregion
    //region input events, overrides

    @Override
    protected int schemOriginX(){
        Tmp.v1.setZero();
        selectRequests.each(r -> Tmp.v1.add(r.drawx(), r.drawy()));
        return World.toTile(Tmp.v1.scl(1f / selectRequests.size).x);
    }

    @Override
    protected int schemOriginY(){
        Tmp.v1.setZero();
        selectRequests.each(r -> Tmp.v1.add(r.drawx(), r.drawy()));
        return World.toTile(Tmp.v1.scl(1f / selectRequests.size).y);
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
        selectRequests.clear();
        selectRequests.addAll(schematics.toRequests(schem, World.toTile(Core.camera.position.x), World.toTile(Core.camera.position.y)));
        lastSchematic = schem;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, KeyCode button){
        if(state.isMenu()) return false;

        down = true;

        if(player.dead()) return false;

        //get tile on cursor
        Tile cursor = tileAt(screenX, screenY);

        float worldx = Core.input.mouseWorld(screenX, screenY).x, worldy = Core.input.mouseWorld(screenX, screenY).y;

        //ignore off-screen taps
        if(cursor == null || Core.scene.hasMouse(screenX, screenY)) return false;

        //only begin selecting if the tapped block is a request
        selecting = hasRequest(cursor);

        //call tap events
        if(pointer == 0 && !selecting){
            if(schematicMode && block == null){
                mode = schematicSelect;
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
                flushSelectRequests(lineRequests);
                Events.fire(new LineConfirmEvent());
            }else if(mode == breaking){
                removeSelection(lineStartX, lineStartY, tileX, tileY, true);
            }

            lineMode = false;
        }else if(mode == schematicSelect){
            selectRequests.clear();
            lastSchematic = schematics.create(lineStartX, lineStartY, lastLineX, lastLineY);
            useSchematic(lastSchematic);
            if(selectRequests.isEmpty()){
                lastSchematic = null;
            }
            schematicMode = false;
            mode = none;
        }else{
            Tile tile = tileAt(screenX, screenY);

            tryDropItems(tile == null ? null : tile.build, Core.input.mouseWorld(screenX, screenY).x, Core.input.mouseWorld(screenX, screenY).y);
        }
        return false;
    }

    @Override
    public boolean longPress(float x, float y){
        if(state.isMenu()|| player.dead()) return false;

        //get tile on cursor
        Tile cursor = tileAt(x, y);

        if(Core.scene.hasMouse(x, y) || schematicMode) return false;

        //handle long tap when player isn't building
        if(mode == none){
            Vec2 pos = Core.input.mouseWorld(x, y);

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

            if(!state.isPaused()) Fx.select.at(pos);
        }else{

            //ignore off-screen taps
            if(cursor == null) return false;

            //remove request if it's there
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
        if(state.isMenu() || lineMode) return false;

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

        //remove if request present
        if(hasRequest(cursor)){
            removeRequest(getRequest(cursor));
        }else if(mode == placing && isPlacing() && validPlace(cursor.x, cursor.y, block, rotation) && !checkOverlapPlacement(cursor.x, cursor.y, block)){
            //add to selection queue if it's a valid place position
            selectRequests.add(lastPlaced = new BuildPlan(cursor.x, cursor.y, rotation, block, block.nextConfig()));
            block.onNewPlan(lastPlaced);
        }else if(mode == breaking && validBreak(linked.x,linked.y) && !hasRequest(linked)){
            //add to selection queue if it's a valid BREAK position
            selectRequests.add(new BuildPlan(linked.x, linked.y));
        }else{
            //control units
            if(count == 2){
                //reset payload target
                payloadTarget = null;
                //apply command on double tap when own unit is tapped
                if(!player.dead() && Mathf.within(worldx, worldy, player.unit().x, player.unit().y, player.unit().hitSize * 0.6f + 8f) && player.unit().type.commandLimit > 0){
                    Call.unitCommand(player);
                }else{
                    //control a unit/block detected on first tap of double-tap
                    if(unitTapped != null){
                        Call.unitControl(player, unitTapped);
                        recentRespawnTimer = 1f;
                    }else if(buildingTapped != null){
                        Call.buildingControlSelect(player, buildingTapped);
                        recentRespawnTimer = 1f;
                    }else if(!tryBeginMine(cursor)){
                        tileTapped(linked.build);
                    }
                }
                return false;
            }

            unitTapped = selectedUnit();
            buildingTapped = selectedControlBuild();
            //prevent mining if placing/breaking blocks
            if(!tryStopMine() && !canTapPlayer(worldx, worldy) && !tileTapped(linked.build) && mode == none && !Core.settings.getBool("doubletapmine")){
                tryBeginMine(cursor);
            }
        }

        return false;
    }

    @Override
    public void updateState(){
        super.updateState();

        if(state.isMenu()){
            selectRequests.clear();
            removals.clear();
            mode = none;
            manualShooting = false;
            payloadTarget = null;
        }
    }

    @Override
    public void update(){
        super.update();

        if(player.dead()){
            mode = none;
            manualShooting = false;
            payloadTarget = null;
        }

        //zoom camera
        if(Math.abs(Core.input.axisTap(Binding.zoom)) > 0 && !Core.input.keyDown(Binding.rotateplaced) && (Core.input.keyDown(Binding.diagonal_placement) || ((!player.isBuilder() || !isPlacing() || !block.rotate) && selectRequests.isEmpty()))){
            renderer.scaleCamera(Core.input.axisTap(Binding.zoom));
        }

        if(!Core.settings.getBool("keyboard")){
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

        if(!player.dead() && !state.isPaused() && !renderer.isCutscene()){
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
        if(!schematicMode && mode == schematicSelect){
            mode = none;
        }

        if(mode == schematicSelect){
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
            lineRequests.clear();
            lineScale = 0f;
        }

        //remove place requests that have disappeared
        for(int i = removals.size - 1; i >= 0; i--){
            BuildPlan request = removals.get(i);

            if(request.animScale <= 0.0001f){
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
        if(Core.scene == null || Core.scene.hasDialog() || Core.settings.getBool("keyboard")) return false;

        float scale = Core.camera.width / Core.graphics.getWidth();
        deltaX *= scale;
        deltaY *= scale;

        //can't pan in line mode with one finger or while dropping items!
        if((lineMode && !Core.input.isTouched(1)) || droppingItem || schematicMode){
            return false;
        }

        //do not pan with manual shooting enabled
        if(!down || manualShooting) return false;

        if(selecting){ //pan all requests
            shiftDeltaX += deltaX;
            shiftDeltaY += deltaY;

            int shiftedX = (int)(shiftDeltaX / tilesize);
            int shiftedY = (int)(shiftDeltaY / tilesize);

            if(Math.abs(shiftedX) > 0 || Math.abs(shiftedY) > 0){
                for(BuildPlan req : selectRequests){
                    if(req.breaking) continue; //don't shift removal requests
                    req.x += shiftedX;
                    req.y += shiftedY;
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

        float speed = unit.realSpeed();
        float range = unit.hasWeapons() ? unit.range() : 0f;
        float bulletSpeed = unit.hasWeapons() ? type.weapons.first().bullet.speed : 0f;
        float mouseAngle = unit.angleTo(unit.aimX(), unit.aimY());
        boolean aimCursor = omni && player.shooting && type.hasWeapons() && type.faceTarget && !boosted && type.rotateShooting;

        if(aimCursor){
            unit.lookAt(mouseAngle);
        }else{
            unit.lookAt(unit.prefRotation());
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
        rect.grow(6f);

        player.boosting = collisions.overlapsTile(rect) || !unit.within(targetPos, 85f);

        if(omni){
            unit.moveAt(movement);
        }else{
            unit.rotateMove(movement);
        }

        //update shooting if not building + not mining
        if(!player.unit().activelyBuilding() && player.unit().mineTile == null){

            //autofire targeting
            if(manualShooting){
                player.shooting = !boosted;
                unit.aim(player.mouseX = Core.input.mouseWorldX(), player.mouseY = Core.input.mouseWorldY());
            }else if(target == null){
                player.shooting = false;
                if(Core.settings.getBool("autotarget") && !(player.unit() instanceof BlockUnitUnit u && u.tile() instanceof ControlBlock c && !c.shouldAutoTarget())){
                    target = Units.closestTarget(unit.team, unit.x, unit.y, range, u -> u.checkTarget(type.targetAir, type.targetGround), u -> type.targetGround);

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
