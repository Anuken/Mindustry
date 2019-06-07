package io.anuke.mindustry.input;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.function.BooleanProvider;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.input.GestureDetector;
import io.anuke.arc.input.GestureDetector.GestureListener;
import io.anuke.arc.input.KeyCode;
import io.anuke.arc.math.Interpolation;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.scene.actions.Actions;
import io.anuke.arc.scene.event.Touchable;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.*;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.traits.TargetTrait;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.entities.type.Unit;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.input.PlaceUtils.NormalizeDrawResult;
import io.anuke.mindustry.input.PlaceUtils.NormalizeResult;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.mindustry.input.PlaceMode.*;

public class MobileInput extends InputHandler implements GestureListener{
    /** Maximum speed the player can pan. */
    private static final float maxPanSpeed = 1.3f;
    private static Rectangle r1 = new Rectangle(), r2 = new Rectangle();
    /** Distance to edge of screen to start panning. */
    private final float edgePan = io.anuke.arc.scene.ui.layout.Unit.dp.scl(60f);

    //gesture data
    private Vector2 vector = new Vector2();
    private float lastDistance = -1f;

    /** Position where the player started dragging a line. */
    private int lineStartX, lineStartY;

    /** Animation scale for line. */
    private float lineScale;
    /** Animation data for crosshair. */
    private float crosshairScale;
    private TargetTrait lastTarget;
    /** Used for shifting build requests. */
    private float shiftDeltaX, shiftDeltaY;

    /** List of currently selected tiles to place. */
    private Array<PlaceRequest> selection = new Array<>();
    /** Place requests to be removed. */
    private Array<PlaceRequest> removals = new Array<>();
    /** Whether or not the player is currently shifting all placed tiles. */
    private boolean selecting;
    /** Whether the player is currently in line-place mode. */
    private boolean lineMode;
    /** Current place mode. */
    private PlaceMode mode = none;
    /** Whether no recipe was available when switching to break mode. */
    private Block lastBlock;
    /** Last placed request. Used for drawing block overlay. */
    private PlaceRequest lastPlaced;

    private int prevX, prevY, prevRotation;

    public MobileInput(){
        Core.input.addProcessor(new GestureDetector(20, 0.5f, 0.4f, 0.15f, this));
    }

    //region utility methods

    /** Check and assign targets for a specific position. */
    void checkTargets(float x, float y){
        Unit unit = Units.closestEnemy(player.getTeam(), x, y, 20f, u -> !u.isDead());

        if(unit != null){
            player.setMineTile(null);
            player.target = unit;
        }else{
            Tile tile = world.ltileWorld(x, y);

            if(tile != null && tile.synthetic() && state.teams.areEnemies(player.getTeam(), tile.getTeam())){
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

        for(PlaceRequest req : selection){
            Tile other = req.tile();

            if(other == null || req.remove) continue;

            r1.setSize(req.block.size * tilesize);
            r1.setCenter(other.worldx() + req.block.offset(), other.worldy() + req.block.offset());

            if(r2.overlaps(r1)){
                return true;
            }
        }
        return false;
    }

    /** Returns the selection request that overlaps this tile, or null. */
    PlaceRequest getRequest(Tile tile){
        r2.setSize(tilesize);
        r2.setCenter(tile.worldx(), tile.worldy());

        for(PlaceRequest req : selection){
            Tile other = req.tile();

            if(other == null) continue;

            if(!req.remove){
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

    void removeRequest(PlaceRequest request){
        selection.removeValue(request, true);
        removals.add(request);
    }

    void showGuide(String type, BooleanProvider done){
        if(!Core.settings.getBool(type, false)){
            Core.scene.table("guideDim", t -> {
                t.margin(10f);
                t.touchable(Touchable.disabled);
                t.top().table("button", s -> s.add("$" + type).growX().wrap().labelAlign(Align.center, Align.center)).growX();
                t.update(() -> {
                    if((done.get() || state.is(State.menu)) && t.getUserObject() == null){
                        t.actions(Actions.delay(1f), Actions.fadeOut(1f, Interpolation.fade), Actions.remove());
                        t.setUserObject("ha");
                    }
                });
            });
            Core.settings.put(type, true);
            data.modified();
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

    void drawRequest(PlaceRequest request, PlaceRequest prev){
        Tile tile = request.tile();

        if(!request.remove){
            if(prev != null){
                request.block.getPlaceDraw(placeDraw, request.rotation, prev.x - request.x, prev.y - request.y, prev.rotation);
            }else{
                request.block.getPlaceDraw(placeDraw, request.rotation, 0, 0, request.rotation);
            }

            //draw placing request
            float offset = request.block.offset();
            TextureRegion region = placeDraw.region;

            Draw.mixcol(Pal.accent, Mathf.clamp((1f - request.scale) / 0.5f));
            Draw.tint(Color.WHITE, Pal.breakInvalid, request.redness);

            Draw.rect(region, tile.worldx() + offset, tile.worldy() + offset,
            region.getWidth() * request.scale * Draw.scl * placeDraw.scalex,
            region.getHeight() * request.scale * Draw.scl * placeDraw.scaley,
            request.block.rotate ? placeDraw.rotation * 90 : 0);

            Draw.mixcol(Pal.accent, 1f);
            for(int i = 0; i < 4; i++){
                Point2 p = Geometry.d8edge[i];
                float poffset = -Math.max(request.block.size - 1, 0) / 2f * tilesize;
                TextureRegion find = Core.atlas.find("block-select");
                if(i % 2 == 0)
                    Draw.rect("block-select", request.tile().x * tilesize + request.block.offset() + poffset * p.x, request.tile().y * tilesize + request.block.offset() + poffset * p.y,
                    find.getWidth() * Draw.scl * request.scale, find.getHeight() * Draw.scl * request.scale, i * 90);
            }
            Draw.color();
        }else{
            float rad = Math.max((tile.block().size * tilesize / 2f - 1) * request.scale, 1f);

            if(rad <= 1.01f) return;
            Draw.mixcol();
            //draw removing request
            Draw.tint(Pal.removeBack);
            Lines.square(tile.drawx(), tile.drawy() - 1, rad);
            Draw.tint(Pal.remove);
            Lines.square(tile.drawx(), tile.drawy(), rad);
        }
    }

    /** Draws a placement icon for a specific block. */
    void drawPlace(int x, int y, Block block, int rotation, int prevX, int prevY, int prevRotation){
        if(validPlace(x, y, block, rotation)){
            block.getPlaceDraw(placeDraw, rotation, prevX, prevY, prevRotation);

            Draw.color();
            Draw.rect(placeDraw.region, x * tilesize + block.offset(), y * tilesize + block.offset(),
            placeDraw.region.getWidth() * Draw.scl * placeDraw.scalex,
            placeDraw.region.getHeight() * Draw.scl * placeDraw.scaley,
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
    public void buildUI(Table table){
        table.addImage("blank").color(Pal.accent).height(3f).colspan(4).growX();
        table.row();
        table.left().margin(0f).defaults().size(48f);

        table.addImageButton("icon-break", "clear-toggle-partial", 16 * 2f, () -> {
            mode = mode == breaking ? block == null ? none : placing : breaking;
            lastBlock = block;
            if(mode == breaking){
                showGuide("removearea", this::isAreaBreaking);
            }
        }).update(l -> l.setChecked(mode == breaking));

        //diagonal swap button
        table.addImageButton("icon-diagonal", "clear-toggle-partial", 16 * 2f, () -> {
            Core.settings.put("swapdiagonal", !Core.settings.getBool("swapdiagonal"));
            Core.settings.save();
        }).update(l -> l.setChecked(Core.settings.getBool("swapdiagonal")));

        //rotate button
        table.addImageButton("icon-arrow", "clear-partial", 16 * 2f, () -> rotation = Mathf.mod(rotation + 1, 4))
        .update(i -> i.getImage().setRotationOrigin(rotation * 90, Align.center)).visible(() -> block != null && block.rotate);

        //confirm button
        table.addImageButton("icon-check", "clear-partial", 16 * 2f, () -> {
            for(PlaceRequest request : selection){
                Tile tile = request.tile();

                //actually place/break all selected blocks
                if(tile != null){
                    if(!request.remove){
                        rotation = request.rotation;
                        Block before = block;
                        block = request.block;
                        tryPlaceBlock(tile.x, tile.y);
                        block = before;
                    }else{
                        tryBreakBlock(tile.x, tile.y);
                    }
                }
            }

            //move all current requests to removal array so they fade out
            removals.addAll(selection);
            selection.clear();
            selecting = false;
        }).visible(() -> !selection.isEmpty());

        Core.scene.table(t -> {
           t.bottom().left().visible(() -> (player.isBuilding() || block != null || mode == breaking) && !state.is(State.menu));
           t.addImageTextButton("$cancel", "icon-cancel", 16*2, () -> {
               player.clearBuilding();
               mode = none;
               block = null;
           }).width(155f);
        });
    }

    @Override
    public boolean isDrawing(){
        return selection.size > 0 || removals.size > 0 || lineMode || player.target != null || mode != PlaceMode.none;
    }

    @Override
    public boolean isPlacing(){
        return super.isPlacing() && mode == placing;
    }

    @Override
    public void drawOutlined(){
        Lines.stroke(1f);

        //draw removals
        for(PlaceRequest request : removals){
            Tile tile = request.tile();

            if(tile == null) continue;

            request.scale = Mathf.lerpDelta(request.scale, 0f, 0.2f);
            request.redness = Mathf.lerpDelta(request.redness, 0f, 0.2f);

            drawRequest(request, null);
        }

        PlaceRequest last = null;

        //draw list of requests
        for(PlaceRequest request : selection){
            Tile tile = request.tile();

            if(tile == null) continue;

            if((!request.remove && validPlace(tile.x, tile.y, request.block, request.rotation))
            || (request.remove && validBreak(tile.x, tile.y))){
                request.scale = Mathf.lerpDelta(request.scale, 1f, 0.2f);
                request.redness = Mathf.lerpDelta(request.redness, 0f, 0.2f);
            }else{
                request.scale = Mathf.lerpDelta(request.scale, 0.6f, 0.1f);
                request.redness = Mathf.lerpDelta(request.redness, 0.9f, 0.2f);
            }

            Tmp.c1.set(Draw.getMixColor());

            if(!request.remove && request == lastPlaced && request.block != null){
                Draw.mixcol();
                if(request.block.rotate) drawArrow(request.block, tile.x, tile.y, request.rotation);
            }

            Draw.mixcol(Tmp.c1, 1f);
            drawRequest(request, last);

            //draw last placed request
            if(!request.remove && request == lastPlaced && request.block != null){
                Draw.mixcol();
                request.block.drawPlace(tile.x, tile.y, rotation, validPlace(tile.x, tile.y, request.block, rotation));
            }

            last = request;
        }

        Draw.mixcol();
        Draw.color(Pal.accent);

        //Draw lines
        if(lineMode){
            int tileX = tileX(Core.input.mouseX());
            int tileY = tileY(Core.input.mouseY());

            if(mode == placing && block != null){
                //draw placing

                prevX = lineStartX;
                prevY = lineStartY;
                prevRotation = rotation;

                iterateLine(lineStartX, lineStartY, tileX, tileY, l -> {
                    if(l.last && block.rotate){
                        drawArrow(block, l.x, l.y, l.rotation);
                    }
                    drawPlace(l.x, l.y, block, l.rotation, prevX - l.x, prevY - l.y, prevRotation);

                    rotation = l.rotation;
                    prevX = l.x;
                    prevY = l.y;
                    prevRotation = l.rotation;
                });
            }else if(mode == breaking){
                //draw breaking
                NormalizeDrawResult result = PlaceUtils.normalizeDrawArea(Blocks.air, lineStartX, lineStartY, tileX, tileY, false, maxLength, 1f);
                NormalizeResult dresult = PlaceUtils.normalizeArea(lineStartX, lineStartY, tileX, tileY, rotation, false, maxLength);

                for(int x = dresult.x; x <= dresult.x2; x++){
                    for(int y = dresult.y; y <= dresult.y2; y++){
                        Tile other = world.ltile(x, y);
                        if(other == null || !validBreak(other.x, other.y)) continue;

                        Draw.color(Pal.removeBack);
                        Lines.square(other.drawx(), other.drawy() - 1, other.block().size * tilesize / 2f - 1);
                        Draw.color(Pal.remove);
                        Lines.square(other.drawx(), other.drawy(), other.block().size * tilesize / 2f - 1);
                    }
                }

                Draw.color(Pal.removeBack);
                Lines.rect(result.x, result.y - 1, result.x2 - result.x, result.y2 - result.y);
                Draw.color(Pal.remove);
                Lines.rect(result.x, result.y, result.x2 - result.x, result.y2 - result.y);

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

    //endregion
    //region input events

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, KeyCode button){
        if(state.is(State.menu) || player.isDead()) return false;

        //get tile on cursor
        Tile cursor = tileAt(screenX, screenY);

        float worldx = Core.input.mouseWorld(screenX, screenY).x, worldy = Core.input.mouseWorld(screenX, screenY).y;

        //ignore off-screen taps
        if(cursor == null || Core.scene.hasMouse(screenX, screenY)) return false;

        //only begin selecting if the tapped block is a request
        selecting = hasRequest(cursor) && isPlacing() && mode == placing;

        //call tap events
        if(pointer == 0 && !selecting && mode == none){
            tryTapPlayer(worldx, worldy);
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, KeyCode button){

        //place down a line if in line mode
        if(lineMode){
            int tileX = tileX(screenX);
            int tileY = tileY(screenY);

            if(mode == placing && isPlacing()){
                iterateLine(lineStartX, lineStartY, tileX, tileY, l -> {
                    Tile tile = world.tile(l.x, l.y);
                    if(tile != null && hasRequest(tile)){
                        return;
                    }

                    PlaceRequest request = new PlaceRequest(l.x, l.y, block, l.rotation);
                    request.scale = 1f;
                    selection.add(request);
                });
            }else if(mode == breaking){
                //normalize area
                NormalizeResult result = PlaceUtils.normalizeArea(lineStartX, lineStartY, tileX, tileY, rotation, false, maxLength);

                //break everything in area
                for(int x = 0; x <= Math.abs(result.x2 - result.x); x++){
                    for(int y = 0; y <= Math.abs(result.y2 - result.y); y++){
                        int wx = lineStartX + x * Mathf.sign(tileX - lineStartX);
                        int wy = lineStartY + y * Mathf.sign(tileY - lineStartY);

                        Tile tar = world.ltile(wx, wy);

                        if(tar == null) continue;

                        if(!hasRequest(world.tile(tar.x, tar.y)) && validBreak(tar.x, tar.y)){
                            PlaceRequest request = new PlaceRequest(tar.x, tar.y);
                            request.scale = 1f;
                            selection.add(request);
                        }
                    }
                }
            }

            lineMode = false;
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
        if(cursor == null || Core.scene.hasMouse(x, y)) return false;

        //remove request if it's there
        //long pressing enables line mode otherwise
        lineStartX = cursor.x;
        lineStartY = cursor.y;
        lineMode = true;

        if(mode == breaking){
            Effects.effect(Fx.tapBlock, cursor.worldx(), cursor.worldy(), 1f);
        }else if(block != null){
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
            selection.add(lastPlaced = new PlaceRequest(cursor.x, cursor.y, block, rotation));
        }else if(mode == breaking && validBreak(cursor.link().x, cursor.link().y) && !hasRequest(cursor.link())){
            //add to selection queue if it's a valid BREAK position
            cursor = cursor.link();
            selection.add(new PlaceRequest(cursor.x, cursor.y));
        }else if(!canTapPlayer(worldx, worldy) && !tileTapped(cursor.link())){
            tryBeginMine(cursor);
        }

        return false;
    }

    @Override
    public void update(){
        if(state.is(State.menu) || player.isDead()){
            selection.clear();
            removals.clear();
            mode = none;
        }

        //reset state when not placing
        if(mode == none){
            selecting = false;
            lineMode = false;
            removals.addAll(selection);
            selection.clear();
        }

        if(lineMode && mode == placing && block == null){
            lineMode = false;
        }

        //if there is no mode and there's a recipe, switch to placing
        if(block != null && mode == none){
            mode = placing;
        }

        if(block != null){
            showGuide("placeline", this::isLinePlacing);
        }

        if(block == null && mode == placing){
            mode = none;
        }

        //automatically switch to placing after a new recipe is selected
        if(lastBlock != block && mode == breaking && block != null){
            mode = placing;
            lastBlock = block;
        }

        if(lineMode){
            lineScale = Mathf.lerpDelta(lineScale, 1f, 0.1f);

            //When in line mode, pan when near screen edges automatically
            if(Core.input.isTouched(0) && lineMode){
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
        }else{
            lineScale = 0f;
        }

        //remove place requests that have disappeared
        for(int i = removals.size - 1; i >= 0; i--){
            PlaceRequest request = removals.get(i);

            if(request.scale <= 0.0001f){
                removals.remove(i);
                i--;
            }
        }
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY){
        if(Core.scene.hasDialog()) return false;

        float scale = Core.camera.width / Core.graphics.getWidth();
        deltaX *= scale;
        deltaY *= scale;

        //can't pan in line mode with one finger or while dropping items!
        if((lineMode && !Core.input.isTouched(1)) || droppingItem){
            return false;
        }

        if(selecting){ //pan all requests
            shiftDeltaX += deltaX;
            shiftDeltaY += deltaY;

            int shiftedX = (int)(shiftDeltaX / tilesize);
            int shiftedY = (int)(shiftDeltaY / tilesize);

            if(Math.abs(shiftedX) > 0 || Math.abs(shiftedY) > 0){
                for(PlaceRequest req : selection){
                    if(req.remove) continue; //don't shift removal requests
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
        if(lastDistance == -1) lastDistance = initialDistance;

        float amount = (Mathf.sign(distance > lastDistance) * 0.04f) * Time.delta();
        renderer.scaleCamera(io.anuke.arc.scene.ui.layout.Unit.dp.scl(amount));
        lastDistance = distance;
        return true;
    }

    float clerp(float value, float min, float max){
        final float alpha = 0.07f;
        return value < min ? Mathf.lerpDelta(value, min, alpha) : value > max ? Mathf.lerpDelta(value, max, alpha) : value;
    }

    //endregion

    private class PlaceRequest{
        int x, y;
        Block block;
        int rotation;
        boolean remove;

        //animation variables
        float scale;
        float redness;

        PlaceRequest(int x, int y, Block block, int rotation){
            this.x = x;
            this.y = y;
            this.block = block;
            this.rotation = rotation;
            this.remove = false;
        }

        PlaceRequest(int x, int y){
            this.x = x;
            this.y = y;
            this.remove = true;
        }

        Tile tile(){
            return world.tile(x, y);
        }
    }
}
