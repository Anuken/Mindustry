package io.anuke.mindustry.input;

import io.anuke.arc.*;
import io.anuke.arc.Graphics.*;
import io.anuke.arc.Graphics.Cursor.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.Position;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.scene.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.entities.traits.BuilderTrait.*;
import io.anuke.mindustry.entities.traits.TargetTrait;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.world.*;

import static io.anuke.arc.Core.scene;
import static io.anuke.mindustry.Vars.*;
import static io.anuke.mindustry.core.Control.controltype_camera;
import static io.anuke.mindustry.core.Control.controltype_ship;
import static io.anuke.mindustry.input.PlaceMode.*;

public class DesktopInput extends InputHandler{
    /** Current cursor type. */
    private Cursor cursorType = SystemCursor.arrow;
    /** Position where the player started dragging a line. */
    private int selectX, selectY;
    /** Last known line positions.*/
    private int lastLineX, lastLineY;
    /** Whether selecting mode is active. */
    private PlaceMode mode;
    /** Animation scale for line. */
    private float selectScale;
    /** Selected build request for movement. */
    private @Nullable BuildRequest sreq;
    /** Whether player is currently deleting removal requests. */
    private boolean deleting = false;

    private TargetTrait lastClickTarget;
    private TargetTrait clickTarget;
    private long clickStart = 0;
    private long lastClick = 0;

    @Override
    public void buildUI(Group group){
        group.fill(t -> {
            t.bottom().update(() -> t.getColor().a = Mathf.lerpDelta(t.getColor().a, player.isBuilding() ? 1f : 0f, 0.15f));
            t.visible(() -> Core.settings.getBool("hints"));
            t.table(Styles.black6, b -> {
                b.defaults().left();
                b.label(() -> Core.bundle.format(!player.isBuilding ?  "resumebuilding" : "pausebuilding", Core.keybinds.get(Binding.pause_building).key.name())).style(Styles.outlineLabel);
                b.row();
                b.add(Core.bundle.format("cancelbuilding", Core.keybinds.get(Binding.clear_building).key.name())).style(Styles.outlineLabel);
            }).margin(10f);
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
            drawSelection(selectX, selectY, cursorX, cursorY);
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

        if(sreq != null){
            boolean valid = validPlace(sreq.x, sreq.y, sreq.block, sreq.rotation, sreq);
            if(sreq.block.rotate){
                drawArrow(sreq.block, sreq.x, sreq.y, sreq.rotation, valid);
            }

            sreq.block.drawRequest(sreq, allRequests(), valid);

            drawSelected(sreq.x, sreq.y, sreq.block, getRequest(sreq.x, sreq.y, sreq.block.size, sreq) != null ? Pal.remove : Pal.accent);
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
        if(Math.abs(Core.input.axisTap(Binding.zoom)) > 0 && (Core.input.keyDown(Binding.zoom_hold))){
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

        if(player.isShooting && !canShoot()){
            player.isShooting = false;
        }

        if(isPlacing()){
            cursorType = SystemCursor.hand;
            selectScale = Mathf.lerpDelta(selectScale, 1f, 0.2f);
        }else{
            selectScale = 0f;
        }

        rotation = Mathf.mod(rotation + (int)Core.input.axisTap(Binding.rotate), 4);

        if(sreq != null){
            sreq.rotation = Mathf.mod(sreq.rotation + (int)Core.input.axisTap(Binding.rotate), 4);
        }

        if(Math.abs((int)Core.input.axisTap(Binding.rotate)) > 0 && isPlacing() && mode == placing){
            updateLine(selectX, selectY);
        }

        Tile cursor = tileAt(Core.input.mouseX(), Core.input.mouseY());

        if(cursor != null){
            cursor = cursor.link();

            cursorType = cursor.block().getCursor(cursor);

            if(isPlacing()){
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

        updateCamera();
    }

    @Override
    public boolean isBreaking(){
        return mode == breaking;
    }

    void pollInput(){
        Tile selected = tileAt(Core.input.mouseX(), Core.input.mouseY());
        int cursorX = tileX(Core.input.mouseX());
        int cursorY = tileY(Core.input.mouseY());

        if(Core.input.keyTap(Binding.deselect)){
            player.setMineTile(null);
        }

        if(Core.input.keyTap(Binding.clear_building)){
            player.clearBuilding();
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
        }

        if((cursorX != lastLineX || cursorY != lastLineY) && isPlacing() && mode == placing){
            updateLine(selectX, selectY);
            lastLineX = cursorX;
            lastLineY = cursorY;
        }

        if(Core.input.keyTap(Binding.select) && !Core.scene.hasMouse()){
            BuildRequest req = getRequest(cursorX, cursorY);
            if (clickStart == 0) {
                clickStart = Time.millis();
                clickTarget = null;
            }
            boolean moveToCursor = false;
            if(isPlacing()){
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
                if(!tileTapped(selected) && !tryTapPlayer(Core.input.mouseWorld()) && player.buildQueue().size == 0 && !droppingItem &&
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
        if(Core.input.keyRelease(Binding.select)){
            float clickDuration = Time.timeSinceMillis(clickStart);
            clickStart = 0;

            ui.hudfrag.showDebug("click clickDuration:" + clickDuration + "ms");
            int singleClickDuration = 200; // TODO make this a setting
            int doubleClickDelay = 200; // TODO make this a setting
            if (clickDuration < singleClickDuration) {
                float lastClickDelay = Time.timeSinceMillis(lastClick);
                ui.hudfrag.showDebug("double click speed:" + lastClickDelay + "ms");
                lastClickTarget = tileAtMouse();
                if (lastClickDelay < doubleClickDelay && (lastClickTarget == clickTarget)){
                    clickTarget = tileAtMouse();
                    ui.hudfrag.showDebug("target:" + clickTarget.toString());
                }

                lastClick = Time.millis();
            }

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
        }
    }


    protected void updateCamera() {

        if (!Core.input.keyDown(Binding.gridMode) && !(Core.scene.getKeyboardFocus() instanceof TextField)) {

            Vector2 cameraMovement = new Vector2().setZero();

            if(Core.settings.getInt("controltype", controltype_ship) == controltype_camera){
                // Keyboard input
                Vector2 keyboardCamera = new Vector2(Core.input.axis(Binding.move_x), Core.input.axis(Binding.move_y));
                cameraMovement.add(keyboardCamera);

                // Mouse input
                Vector2 mouseCamera = mouseCameraVector();

                cameraMovement.add(mouseCamera);
            }

            float camSpeed = (float) Core.settings.getInt("cameraspeed");
            if(!cameraMovement.isZero()){

                ui.hudfrag.showDebug("camera movement");
                cameraMovement.scl(Time.delta() * camSpeed);
                clickTarget = null;
            } else if (clickTarget != null){
                float clickTargetDst = Core.camera.position.dst(clickTarget);

                ui.hudfrag.showDebug("target dist: "+ String.valueOf(clickTargetDst));
                if(clickTargetDst > 0 ){
                    cameraMovement.set(Math.min(clickTargetDst, Time.delta()*camSpeed),0);
                    cameraMovement.setAngle(Core.camera.position.angleTo(clickTarget));
                } else {
                    clickTarget = null;
                }
            } else {

                ui.hudfrag.showDebug("no target");
            }


            Core.camera.position.add(cameraMovement);
            clampCameraPosition();
        }
    }

    protected void clampCameraPosition(){
        Core.camera.position.clamp(0f - scene.getWidth()/tilesize/4, world.width() * tilesize - tilesize + scene.getWidth()/tilesize/4, 0F - scene.getHeight()/tilesize /4, world.height() * tilesize - tilesize + scene.getHeight() /tilesize/4);
    }

    protected Vector2 mouseCameraVector() {
        Vector2 directionVector = new Vector2().setZero();

        if (!Core.scene.hasMouse() ||
                getMouseX() <= 1f || getMouseY() <= 1f
                || getMouseX() >= scene.getWidth() - 1f || getMouseY() >= scene.getHeight() - 1f
        ) {
            float mouseXRatio = getMouseX() / scene.getWidth();
            float mouseYRatio = getMouseY() / scene.getHeight();

            directionVector.set(getMouseX() - scene.getWidth() / 2, getMouseY() - scene.getHeight() / 2);

            float linearTresholdX = linearTreshold(mouseXRatio, 0.1f);
            float accelerationX = accelerationCalc(linearTresholdX);

            float linearTresholdY = linearTreshold(mouseYRatio, 0.1f);
            float accelerationY = accelerationCalc(linearTresholdY);

            Vector2 accelerationVector = new Vector2(accelerationX, accelerationY);
            directionVector.setLength(accelerationVector.len());
        }
        return directionVector;
    }

    /**
     *
     * @param val      float [-1,1]
     * @return float [-1,1]
     */
    protected float accelerationCalc(float val) {
        val = Mathf.clamp(val, -1f, 1f);
        return (Mathf.cos(Mathf.PI + Mathf.PI* val)+ 1)/2;
    }

    /**
     * <pre>
     *
     *  1    │                        /
     *       │    ╷treshold          /
     *       │    ╷                 /
     *  0 ___│____╷‗‗‗‗‗‗‗╷‗‗‗‗‗‗‗‗/____
     *       │   /
     *       │  /
     *       │ /
     * -1    │/
     *
     *       0           0.5           1
     *
     * </pre>
     * @param val       float [0,1]
     * @param threshold float [0,1] (percentage)
     * @return float [-1,1]
     */
    protected float linearTreshold(float val, float threshold) {
        val = Mathf.clamp(val, 0f, 1f);
        float returnVal = 0;
        if (val < threshold) {
            returnVal = (val - threshold) / threshold;
        } else if (val > (1 - threshold)) {
            returnVal = (val - (1- threshold )) / threshold;
        }
        return returnVal;
    }

    /**
     * <pre>
     *
     *  1    │                 /
     *       │                /
     *       │
     *  0 ___│__‗‗‗‗‗‗╷‗‗‗‗‗‗___╷
     *       │
     *       │
     *       │ /
     *  -1   │/
     *       │
     *
     *       0       0.5        1
     * </pre>
     * @param val       float [0,1]
     * @param threshold float [0,0.5] (percentage)
     * @return float [-1,1]
     */
    protected float accelerationCalcRound(float val, float threshold) {
        return (float) Math.round(Math.abs((2 * (1 - threshold) * val))) * val;
    }

}
