package io.anuke.mindustry.input;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.input.GestureDetector;
import io.anuke.arc.input.GestureDetector.GestureListener;
import io.anuke.arc.input.KeyCode;
import io.anuke.arc.math.Interpolation;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Geometry;
import io.anuke.arc.math.geom.Point2;
import io.anuke.arc.math.geom.Rectangle;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Align;
import io.anuke.arc.util.Time;
import io.anuke.arc.util.Tmp;
import io.anuke.arc.util.pooling.Pool;
import io.anuke.arc.util.pooling.Pools;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.traits.TargetTrait;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.entities.type.Unit;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.input.PlaceUtils.NormalizeDrawResult;
import io.anuke.mindustry.input.PlaceUtils.NormalizeResult;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Block.Icon;
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
    /** Set of completed guides. */
    private ObjectSet<String> guides = new ObjectSet<>();

    /** Position where the player started dragging a line. */
    private int lineStartX, lineStartY;

    /** Animation scale for line. */
    private float lineScale;
    /** Animation data for crosshair. */
    private float crosshairScale;
    private TargetTrait lastTarget;

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

    public MobileInput(Player player){
        super(player);
        Core.input.addProcessor(new GestureDetector(20, 0.5f, 0.4f, 0.15f, this));
    }

    //region utility methods

    /** Check and assign targets for a specific position. */
    void checkTargets(float x, float y){
        Unit unit = Units.getClosestEnemy(player.getTeam(), x, y, 20f, u -> !u.isDead());

        if(unit != null){
            player.setMineTile(null);
            player.target = unit;
        }else{
            Tile tile = world.tileWorld(x, y);
            if(tile != null) tile = tile.target();

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

    void drawRequest(PlaceRequest request){
        Tile tile = request.tile();

        if(!request.remove){
            //draw placing request
            float offset = request.block.offset();
            TextureRegion region = request.block.icon(Icon.full);

            Draw.mixcol(Pal.accent, Mathf.clamp((1f - request.scale) / 0.5f));
            Draw.tint(Color.WHITE, Pal.breakInvalid, request.redness);

            Draw.rect(region, tile.worldx() + offset, tile.worldy() + offset,
            region.getWidth() * request.scale * Draw.scl,
            region.getHeight() * request.scale * Draw.scl,
            request.block.rotate ? request.rotation * 90 : 0);

            Draw.mixcol(Pal.accent, 1f);
            for(int i = 0; i < 4; i++){
                Point2 p = Geometry.d8edge[i];
                float poffset = -Math.max(request.block.size-1, 0)/2f * tilesize;
                TextureRegion find = Core.atlas.find("block-select");
                Draw.rect("block-select", request.tile().x * tilesize + request.block.offset() + poffset * p.x, request.tile().y * tilesize + request.block.offset() + poffset * p.y,
                        find.getWidth() * Draw.scl * request.scale, find.getHeight() * Draw.scl * request.scale, i * 90);
            }
            Draw.color();
        }else{
            float rad = (tile.block().size * tilesize / 2f - 1) * request.scale;
            Draw.mixcol();
            //draw removing request
            Draw.tint(Pal.removeBack);
            Lines.square(tile.drawx(), tile.drawy()-1, rad);
            Draw.tint(Pal.remove);
            Lines.square(tile.drawx(), tile.drawy(), rad);
        }
    }

    void showGuide(String type){
        if(!guides.contains(type) && !Core.settings.getBool(type, false)){
            FloatingDialog dialog = new FloatingDialog("$" + type + ".title");
            dialog.addCloseButton();
            dialog.cont.left();
            dialog.cont.add("$" + type).growX().wrap();
            dialog.cont.row();
            dialog.cont.addCheck("$showagain", false, checked -> {
                Core.settings.put(type, checked);
                Core.settings.save();
            }).growX().left().get().left();
            dialog.show();
            guides.add(type);
        }
    }

    void drawPlaceArrow(Block block, int x, int y, int rotation){
        if(!block.rotate) return;
        Draw.color(!validPlace(x, y, block, rotation) ? Pal.removeBack : Pal.accentBack);
        Draw.rect(Core.atlas.find("place-arrow"),
        x * tilesize + block.offset(),
        y * tilesize + block.offset() - 1,
        Core.atlas.find("place-arrow").getWidth() * Draw.scl,
        Core.atlas.find("place-arrow").getHeight() * Draw.scl, rotation * 90 - 90);

        Draw.color(!validPlace(x, y, block, rotation) ? Pal.remove : Pal.accent);
        Draw.rect(Core.atlas.find("place-arrow"),
        x * tilesize + block.offset(),
        y * tilesize + block.offset(),
        Core.atlas.find("place-arrow").getWidth() * Draw.scl,
        Core.atlas.find("place-arrow").getHeight() * Draw.scl, rotation * 90 - 90);
    }

    //endregion

    //region UI and drawing

    @Override
    public void buildUI(Table table){
        table.addImage("blank").color(Pal.accent).height(3f).colspan(4).growX();
        table.row();
        table.left().margin(0f).defaults().size(48f);

        table.addImageButton("icon-break", "clear-toggle-partial", 16 * 2f, () -> {
            mode = mode == breaking ? block == null ? none : placing : breaking;
            lastBlock = block;
            if(mode == breaking){
                showGuide("deconstruction");
            }
        }).update(l -> l.setChecked(mode == breaking));

        //cancel button
        table.addImageButton("icon-cancel", "clear-partial", 16 * 2f, () -> {
            player.clearBuilding();
            mode = none;
            block = null;
        }).visible(() -> player.isBuilding() || block != null || mode == breaking);

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

            drawRequest(request);
        }

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
                drawPlaceArrow(request.block, tile.x, tile.y, request.rotation);
            }

            Draw.mixcol(Tmp.c1, 1f);
            drawRequest(request);

            //draw last placed request
            if(!request.remove && request == lastPlaced && request.block != null){
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

            //draw placing
            if(mode == breaking){
                //draw breaking
                NormalizeDrawResult result = PlaceUtils.normalizeDrawArea(Blocks.air, lineStartX, lineStartY, tileX, tileY, false, maxLength, 1f);
                NormalizeResult dresult = PlaceUtils.normalizeArea(lineStartX, lineStartY, tileX, tileY, rotation, false, maxLength);

                for(int x = dresult.x; x <= dresult.x2; x++){
                    for(int y = dresult.y; y <= dresult.y2; y++){
                        Tile other = world.tile(x, y);
                        if(other == null || !validBreak(other.x, other.y)) continue;
                        other = other.target();

                        Draw.color(Pal.removeBack);
                        Lines.square(other.drawx(), other.drawy()-1, other.block().size * tilesize / 2f - 1);
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
        if(target != null){
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

            if(mode == breaking){
                //normalize area
                NormalizeResult result = PlaceUtils.normalizeArea(lineStartX, lineStartY, tileX, tileY, rotation, false, maxLength);

                //break everything in area
                for(int x = 0; x <= Math.abs(result.x2 - result.x); x++){
                    for(int y = 0; y <= Math.abs(result.y2 - result.y); y++){
                        int wx = lineStartX + x * Mathf.sign(tileX - lineStartX);
                        int wy = lineStartY + y * Mathf.sign(tileY - lineStartY);

                        Tile tar = world.tile(wx, wy);

                        if(tar == null) continue;

                        tar = tar.target();

                        if(!hasRequest(world.tile(tar.x, tar.y)) && validBreak(tar.x, tar.y)){
                            PlaceRequest request = new PlaceRequest(tar.worldx(), tar.worldy());
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

            tryDropItems(tile.target(), Core.input.mouseWorld(screenX, screenY).x, Core.input.mouseWorld(screenX, screenY).y);
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
            selection.add(new PlaceRequest(cursor.worldx(), cursor.worldy(), block, rotation));
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
            selection.add(lastPlaced = new PlaceRequest(cursor.worldx() + block.offset(), cursor.worldy() + block.offset(), block, rotation));
        }else if(mode == breaking && validBreak(cursor.target().x, cursor.target().y) && !hasRequest(cursor.target())){
            //add to selection queue if it's a valid BREAK position
            cursor = cursor.target();
            selection.add(new PlaceRequest(cursor.worldx(), cursor.worldy()));
        }else if(!canTapPlayer(worldx, worldy) && !tileTapped(cursor.target())){
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
            showGuide("construction");
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

                vector.set(panX, panY).scl((Core.camera.width ) / Core.graphics.getWidth());
                vector.limit(maxPanSpeed);

                //pan view
                Core.camera.position.x += vector.x;
                Core.camera.position.y += vector.y;
            }

            Tile selected = tileAt(Core.input.mouseX(), Core.input.mouseY());

            if(mode == placing && block != null && selected != null){
                int cursorX = tileX(Core.input.mouseX());
                int cursorY = tileY(Core.input.mouseY());

                if((cursorX != lineStartX || cursorY != lineStartY)){
                    points.clear();
                    outPoints.clear();
                    Pool<Point2> pool = Pools.get(Point2.class, Point2::new);
                    Array<Point2> out = bres.line(lineStartX, lineStartY, cursorX, cursorY, pool, outPoints);

                    for(int i = 0; i < out.size; i++){
                        points.add(out.get(i));

                        if(i != out.size - 1){
                            Point2 curr = out.get(i);
                            Point2 next = out.get(i + 1);
                            //diagonal
                            if(next.x != curr.x && next.y != curr.y){
                                points.add(new Point2(next.x, curr.y));
                            }
                        }
                    }

                    for(Point2 point : points){
                        if(!checkOverlapPlacement(point.x, point.y, block)){
                            addRequest(point);
                            lineStartX = point.x;
                            lineStartY = point.y;
                        }

                    }

                    pool.freeAll(outPoints);

                }
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

    void addRequest(Point2 point){
        PlaceRequest last = selection.peek();

        if(last.x == point.x && last.y == point.y){
            return;
        }

        Tile ltile = last.tile();

        int rel = ltile == null ? -1 : Tile.relativeTo(ltile.x, ltile.y, point.x, point.y);

        if(rel != -1){
            last.rotation = rel;
            rotation = rel;
        }

        selection.add(new PlaceRequest(point.x * tilesize, point.y * tilesize, block, rotation));
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
            for(PlaceRequest req : selection){
                if(req.remove) continue; //don't shift removal requests
                req.x += deltaX;
                req.y += deltaY;
            }
        }else{
            //pan player
            Core.camera.position.x -= deltaX;
            Core.camera.position.y -= deltaY;
        }

        return false;
    }

    @Override
    public boolean zoom(float initialDistance, float distance){
        if(lastDistance == -1) lastDistance = initialDistance;

        float amount = (Mathf.sign(distance > lastDistance) * 0.07f) * Time.delta();
        renderer.scaleCamera(io.anuke.arc.scene.ui.layout.Unit.dp.scl(amount));
        lastDistance = distance;
        return true;
    }

    //endregion

    class PlaceRequest{
        float x, y;
        Block block;
        int rotation;
        boolean remove;

        //animation variables
        float scale;
        float redness;

        PlaceRequest(float x, float y, Block block, int rotation){
            this.x = x;
            this.y = y;
            this.block = block;
            this.rotation = rotation;
            this.remove = false;
        }

        PlaceRequest(float x, float y){
            this.x = x;
            this.y = y;
            this.remove = true;
        }

        Tile tile(){
            return world.tileWorld(x - (block == null ? 0 : block.offset()), y - (block == null ? 0 : block.offset()));
        }
    }
}
