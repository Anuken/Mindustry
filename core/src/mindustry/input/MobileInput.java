package mindustry.input;

import arc.*;
import arc.struct.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.input.GestureDetector.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.entities.*;
import mindustry.entities.traits.BuilderTrait.*;
import mindustry.entities.traits.*;
import mindustry.entities.type.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;

import static mindustry.Vars.*;
import static mindustry.input.PlaceMode.*;

public class MobileInput extends InputHandler implements GestureListener{
    /** Maximum speed the player can pan. */
    private static final float maxPanSpeed = 1.3f;
    /** Distance to edge of screen to start panning. */
    private final float edgePan = Scl.scl(60f);

    //gesture data
    private Vec2 vector = new Vec2();
    private float lastZoom = -1;

    /** Position where the player started dragging a line. */
    private int lineStartX, lineStartY, lastLineX, lastLineY;

    /** Animation scale for line. */
    private float lineScale;
    /** Animation data for crosshair. */
    private float crosshairScale;
    private TargetTrait lastTarget;
    /** Used for shifting build requests. */
    private float shiftDeltaX, shiftDeltaY;

    /** Place requests to be removed. */
    private Array<BuildRequest> removals = new Array<>();
    /** Whether or not the player is currently shifting all placed tiles. */
    private boolean selecting;
    /** Whether the player is currently in line-place mode. */
    private boolean lineMode, schematicMode;
    /** Current place mode. */
    private PlaceMode mode = none;
    /** Whether no recipe was available when switching to break mode. */
    private Block lastBlock;
    /** Last placed request. Used for drawing block overlay. */
    private BuildRequest lastPlaced;
    /** Down tracking for panning.*/
    private boolean down = false;

    //region utility methods

    /** Check and assign targets for a specific position. */
    void checkTargets(float x, float y){
        Unit unit = Units.closestEnemy(player.getTeam(), x, y, 20f, u -> !u.isDead());

        if(unit != null){
            player.setMineTile(null);
            player.target = unit;
        }else{
            Tile tile = world.ltileWorld(x, y);

            if(tile != null && tile.synthetic() && player.getTeam().isEnemy(tile.getTeam())){
                TileEntity entity = tile.entity;
                player.setMineTile(null);
                player.target = entity;
            }else if(tile != null && player.mech.canHeal && tile.entity != null && tile.getTeam() == player.getTeam() && tile.entity.damaged()){
                player.setMineTile(null);
                player.target = tile.entity;
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
        r2.setCenter(x * tilesize + block.offset(), y * tilesize + block.offset());

        for(BuildRequest req : selectRequests){
            Tile other = req.tile();

            if(other == null || req.breaking) continue;

            r1.setSize(req.block.size * tilesize);
            r1.setCenter(other.worldx() + req.block.offset(), other.worldy() + req.block.offset());

            if(r2.overlaps(r1)){
                return true;
            }
        }

        for(BuildRequest req : player.buildQueue()){
            Tile other = world.tile(req.x, req.y);

            if(other == null || req.breaking) continue;

            r1.setSize(req.block.size * tilesize);
            r1.setCenter(other.worldx() + req.block.offset(), other.worldy() + req.block.offset());

            if(r2.overlaps(r1)){
                return true;
            }
        }
        return false;
    }

    /** Returns the selection request that overlaps this tile, or null. */
    BuildRequest getRequest(Tile tile){
        r2.setSize(tilesize);
        r2.setCenter(tile.worldx(), tile.worldy());

        for(BuildRequest req : selectRequests){
            Tile other = req.tile();

            if(other == null) continue;

            if(!req.breaking){
                r1.setSize(req.block.size * tilesize);
                r1.setCenter(other.worldx() + req.block.offset(), other.worldy() + req.block.offset());

                if(r2.overlaps(r1)){
                    return req;
                }
            }else{
                r1.setSize(other.block().size * tilesize);
                r1.setCenter(other.worldx() + other.block().offset(), other.worldy() + other.block().offset());

                if(r2.overlaps(r1)){
                    return req;
                }
            }
        }
        return null;
    }

    void removeRequest(BuildRequest request){
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
        table.addImage().color(Pal.gray).height(4f).colspan(4).growX();
        table.row();
        table.left().margin(0f).defaults().size(48f);

        table.addImageButton(Icon.hammer, Styles.clearTogglePartiali, () -> {
            mode = mode == breaking ? block == null ? none : placing : breaking;
            lastBlock = block;
        }).update(l -> l.setChecked(mode == breaking)).name("breakmode");

        //diagonal swap button
        table.addImageButton(Icon.diagonal, Styles.clearTogglePartiali, () -> {
            Core.settings.put("swapdiagonal", !Core.settings.getBool("swapdiagonal"));
            Core.settings.save();
        }).update(l -> l.setChecked(Core.settings.getBool("swapdiagonal")));

        //rotate button
        table.addImageButton(Icon.right, Styles.clearTogglePartiali, () -> {
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
            i.getStyle().imageUp = arrow ? Icon.right : Icon.paste;
            i.setChecked(!arrow && schematicMode);
        });

        //confirm button
        table.addImageButton(Icon.ok, Styles.clearPartiali, () -> {
            for(BuildRequest request : selectRequests){
                Tile tile = request.tile();

                //actually place/break all selected blocks
                if(tile != null){
                    if(!request.breaking){
                        if(validPlace(request.x, request.y, request.block, request.rotation)){
                            BuildRequest other = getRequest(request.x, request.y, request.block.size, null);
                            BuildRequest copy = request.copy();

                            if(copy.hasConfig && copy.block.posConfig){
                                copy.config = Pos.get(Pos.x(copy.config) + copy.x - copy.originalX, Pos.y(copy.config) + copy.y - copy.originalY);
                            }

                            if(other == null){
                                player.addBuildRequest(copy);
                            }else if(!other.breaking && other.x == request.x && other.y == request.y && other.block.size == request.block.size){
                                player.buildQueue().remove(other);
                                player.addBuildRequest(copy);
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
            t.bottom().left().visible(() -> (player.isBuilding() || block != null || mode == breaking || !selectRequests.isEmpty()) && !schem.get());
            t.addImageTextButton("$cancel", Icon.cancel, () -> {
                player.clearBuilding();
                selectRequests.clear();
                mode = none;
                block = null;
            }).width(155f).margin(12f);
        });

        group.fill(t -> {
            t.visible(schem);
            t.bottom().left();
            t.table(Tex.pane, b -> {
                b.defaults().size(50f);

                ImageButtonStyle style = Styles.clearPartiali;

                b.addImageButton(Icon.save, style, this::showSchematicSave).disabled(f -> lastSchematic == null || lastSchematic.file != null);
                b.addImageButton(Icon.cancel, style, () -> {
                    selectRequests.clear();
                });
                b.row();
                b.addImageButton(Icon.flipX, style, () -> flipRequests(selectRequests, true));
                b.addImageButton(Icon.flipY, style, () -> flipRequests(selectRequests, false));
                b.row();
                b.addImageButton(Icon.rotate, style, () -> rotateRequests(selectRequests, 1));

            }).margin(4f);
        });
    }

    @Override
    protected int schemOriginX(){
        Tmp.v1.setZero();
        selectRequests.each(r -> Tmp.v1.add(r.drawx(), r.drawy()));
        return world.toTile(Tmp.v1.scl(1f / selectRequests.size).x);
    }

    @Override
    protected int schemOriginY(){
        Tmp.v1.setZero();
        selectRequests.each(r -> Tmp.v1.add(r.drawx(), r.drawy()));
        return world.toTile(Tmp.v1.scl(1f / selectRequests.size).y);
    }

    @Override
    public boolean isPlacing(){
        return super.isPlacing() && mode == placing;
    }

    @Override
    public void drawBottom(){
        Lines.stroke(1f);

        //draw removals
        for(BuildRequest request : removals){
            Tile tile = request.tile();

            if(tile == null) continue;

            request.animScale = Mathf.lerpDelta(request.animScale, 0f, 0.2f);

            if(request.breaking){
                drawSelected(request.x, request.y, tile.block(), Pal.remove);
            }else{
                request.block.drawRequest(request, allRequests(), true);
            }
        }

        //draw list of requests
        for(BuildRequest request : selectRequests){
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

            //Draw.mixcol(Tmp.c1, 1f);
            Draw.reset();
            drawRequest(request);

            //draw last placed request
            if(!request.breaking && request == lastPlaced && request.block != null){
                Draw.mixcol();
                request.block.drawPlace(tile.x, tile.y, rotation, validPlace(tile.x, tile.y, request.block, rotation));
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
                    BuildRequest req = lineRequests.get(i);
                    if(i == lineRequests.size - 1 && req.block.rotate){
                        drawArrow(block, req.x, req.y, req.rotation);
                    }

                    BuildRequest request = lineRequests.get(i);
                    request.block.drawRequest(request, allRequests(), validPlace(request.x, request.y, request.block, request.rotation) && getRequest(req.x, request.y, request.block.size, null) == null);
                    drawSelected(request.x, request.y, request.block, Pal.accent);
                }
            }else if(mode == breaking){
                drawBreakSelection(lineStartX, lineStartY, tileX, tileY);
            }
        }

        TargetTrait target = player.target;

        //draw targeting crosshair
        if(target != null && !state.isEditor()){
            if(target != lastTarget){
                crosshairScale = 0f;
                lastTarget = target;
            }

            crosshairScale = Mathf.lerpDelta(crosshairScale, 1f, 0.2f);

            Draw.color(Pal.remove);
            Lines.stroke(1f);

            float radius = Interpolation.swingIn.apply(crosshairScale);

            Lines.poly(target.getX(), target.getY(), 4, 7f * radius, Time.time() * 1.5f);
            Lines.spikes(target.getX(), target.getY(), 3f * radius, 6f * radius, 4, Time.time() * 1.5f);
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
    protected void drawRequest(BuildRequest request){
        if(request.tile() == null) return;
        brequest.animScale = request.animScale = Mathf.lerpDelta(request.animScale, 1f, 0.1f);

        if(request.breaking){
            drawSelected(request.x, request.y, request.tile().block(), Pal.remove);
        }else{
            request.block.drawRequest(request, allRequests(), validPlace(request.x, request.y, request.block, request.rotation));
            drawSelected(request.x, request.y, request.block, Pal.accent);
        }
    }

    //endregion
    //region input events

    @Override
    public boolean isBreaking(){
        return mode == breaking;
    }

    @Override
    public void useSchematic(Schematic schem){
        selectRequests.clear();
        selectRequests.addAll(schematics.toRequests(schem, world.toTile(player.x), world.toTile(player.y)));
        lastSchematic = schem;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, KeyCode button){
        if(state.is(State.menu)) return false;

        down = true;

        if(player.isDead()) return false;

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
                player.isShooting = true;
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

            if(tile == null) return false;

            tryDropItems(tile.link(), Core.input.mouseWorld(screenX, screenY).x, Core.input.mouseWorld(screenX, screenY).y);
        }
        return false;
    }

    @Override
    public boolean longPress(float x, float y){
        if(state.is(State.menu) || mode == none || player.isDead()) return false;

        //get tile on cursor
        Tile cursor = tileAt(x, y);

        //ignore off-screen taps
        if(cursor == null || Core.scene.hasMouse(x, y) || schematicMode) return false;

        //remove request if it's there
        //long pressing enables line mode otherwise
        lineStartX = cursor.x;
        lineStartY = cursor.y;
        lastLineX = cursor.x;
        lastLineY = cursor.y;
        lineMode = true;

        if(mode == breaking){
            Effects.effect(Fx.tapBlock, cursor.worldx(), cursor.worldy(), 1f);
        }else if(block != null){
            updateLine(lineStartX, lineStartY, cursor.x, cursor.y);
            Effects.effect(Fx.tapBlock, cursor.worldx() + block.offset(), cursor.worldy() + block.offset(), block.size);
        }

        return false;
    }

    @Override
    public boolean tap(float x, float y, int count, KeyCode button){
        if(state.is(State.menu) || lineMode) return false;

        float worldx = Core.input.mouseWorld(x, y).x, worldy = Core.input.mouseWorld(x, y).y;

        //get tile on cursor
        Tile cursor = tileAt(x, y);

        //ignore off-screen taps
        if(cursor == null || Core.scene.hasMouse(x, y)) return false;

        checkTargets(worldx, worldy);

        //remove if request present
        if(hasRequest(cursor)){
            removeRequest(getRequest(cursor));
        }else if(mode == placing && isPlacing() && validPlace(cursor.x, cursor.y, block, rotation) && !checkOverlapPlacement(cursor.x, cursor.y, block)){
            //add to selection queue if it's a valid place position
            selectRequests.add(lastPlaced = new BuildRequest(cursor.x, cursor.y, rotation, block));
        }else if(mode == breaking && validBreak(cursor.link().x, cursor.link().y) && !hasRequest(cursor.link())){
            //add to selection queue if it's a valid BREAK position
            cursor = cursor.link();
            selectRequests.add(new BuildRequest(cursor.x, cursor.y));
        }else if(!canTapPlayer(worldx, worldy) && !tileTapped(cursor.link())){
            tryBeginMine(cursor);
        }

        return false;
    }

    @Override
    public void update(){
        if(state.is(State.menu) ){
            selectRequests.clear();
            removals.clear();
            mode = none;
        }

        if(player.isDead()){
            mode = none;
        }

        //zoom camera
        if(Math.abs(Core.input.axisTap(Binding.zoom)) > 0 && !Core.input.keyDown(Binding.rotateplaced) && (Core.input.keyDown(Binding.diagonal_placement) || ((!isPlacing() || !block.rotate) && selectRequests.isEmpty()))){
            renderer.scaleCamera(Core.input.axisTap(Binding.zoom));
        }

        if(!Core.settings.getBool("keyboard")){
            //move camera around
            float camSpeed = 6f;
            Core.camera.position.add(Tmp.v1.setZero().add(Core.input.axis(Binding.move_x), Core.input.axis(Binding.move_y)).nor().scl(Time.delta() * camSpeed));
        }

        if(Core.settings.getBool("keyboard")){
            if(Core.input.keyRelease(Binding.select)){
                player.isShooting = false;
            }

            if(player.isShooting && !canShoot()){
                player.isShooting = false;
            }
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
            BuildRequest request = removals.get(i);

            if(request.animScale <= 0.0001f){
                removals.remove(i);
                i--;
            }
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
        if(Core.scene.hasDialog() || Core.settings.getBool("keyboard")) return false;

        float scale = Core.camera.width / Core.graphics.getWidth();
        deltaX *= scale;
        deltaY *= scale;

        //can't pan in line mode with one finger or while dropping items!
        if((lineMode && !Core.input.isTouched(1)) || droppingItem || schematicMode){
            return false;
        }

        if(!down) return false;

        if(selecting){ //pan all requests
            shiftDeltaX += deltaX;
            shiftDeltaY += deltaY;

            int shiftedX = (int)(shiftDeltaX / tilesize);
            int shiftedY = (int)(shiftDeltaY / tilesize);

            if(Math.abs(shiftedX) > 0 || Math.abs(shiftedY) > 0){
                for(BuildRequest req : selectRequests){
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
}
