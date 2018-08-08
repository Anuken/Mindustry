package io.anuke.mindustry.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.input.PlaceUtils.NormalizeDrawResult;
import io.anuke.mindustry.input.PlaceUtils.NormalizeResult;
import io.anuke.mindustry.maps.generation.StructureFormat;
import io.anuke.mindustry.maps.generation.StructureFormat.StructBlock;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Inputs.DeviceType;
import io.anuke.ucore.core.KeyBinds;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.input.Input;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.mindustry.input.CursorType.*;
import static io.anuke.mindustry.input.PlaceMode.*;

public class DesktopInput extends InputHandler{
    private final String section;
    //controller info
    private float controlx, controly;
    private boolean controlling;
    /**Current cursor type.*/
    private CursorType cursorType = normal;

    /**Position where the player started dragging a line.*/
    private int selectX, selectY;
    /**Whether selecting mode is active.*/
    private PlaceMode mode;
    /**Animation scale for line.*/
    private float selectScale;

    public DesktopInput(Player player){
        super(player);
        this.section = "player_" + (player.playerIndex + 1);
    }

    /**Draws a placement icon for a specific block.*/
    void drawPlace(int x, int y, Block block, int rotation){
        if(validPlace(x, y, block, rotation)){
            Draw.color();

            TextureRegion[] regions = block.getBlockIcon();

            for(TextureRegion region : regions){
                Draw.rect(region, x * tilesize + block.offset(), y * tilesize + block.offset(),
                        region.getRegionWidth() * selectScale, region.getRegionHeight() * selectScale, block.rotate ? rotation * 90 : 0);
            }
        }else{
            Draw.color(Palette.remove);
            Lines.square(x * tilesize + block.offset(), y * tilesize + block.offset(), block.size * tilesize / 2f);
        }
    }

    void printArea(NormalizeResult result){
        StructBlock[][] blocks = new StructBlock[Math.abs(result.x2 - result.x) + 1][Math.abs(result.y2 - result.y) + 1];

        for(int x = 0; x <= Math.abs(result.x2 - result.x); x++){
            for(int y = 0; y <= Math.abs(result.y2 - result.y); y++){
                int wx = result.x + x;
                int wy = result.y + y;

                Block block = world.tile(wx, wy).block();

                blocks[x][y] = new StructBlock(block == Blocks.blockpart ? Blocks.air : block, world.tile(wx, wy).getRotation());
            }
        }

        Log.info(StructureFormat.writeBase64(blocks));
    }

    @Override
    public boolean isDrawing(){
        return mode != none || recipe != null;
    }

    @Override
    public void drawOutlined(){
        Tile cursor = tileAt(control.gdxInput().getX(), control.gdxInput().getY());

        if(cursor == null) return;

        //draw selection(s)
        if(mode == placing){
            NormalizeResult result = PlaceUtils.normalizeArea(selectX, selectY, cursor.x, cursor.y, rotation, true, maxLength);

            for(int i = 0; i <= result.getLength(); i += recipe.result.size){
                int x = selectX + i * Mathf.sign(cursor.x - selectX) * Mathf.bool(result.isX());
                int y = selectY + i * Mathf.sign(cursor.y - selectY) * Mathf.bool(!result.isX());

                if(i + recipe.result.size > result.getLength() && recipe.result.rotate){
                    Draw.color(!validPlace(x, y, recipe.result, result.rotation) ? Palette.remove : Palette.placeRotate);
                    Draw.grect("place-arrow", x * tilesize + recipe.result.offset(),
                            y * tilesize + recipe.result.offset(), result.rotation * 90 - 90);
                }

                drawPlace(x, y, recipe.result, result.rotation);
            }

            Draw.reset();
        }else if(mode == breaking){
            NormalizeDrawResult result = PlaceUtils.normalizeDrawArea(Blocks.air, selectX, selectY, cursor.x, cursor.y, false, maxLength, 1f);
            NormalizeResult dresult = PlaceUtils.normalizeArea(selectX, selectY, cursor.x, cursor.y, rotation, false, maxLength);

            Draw.color(Palette.remove);

            for(int x = dresult.x; x <= dresult.x2; x++){
                for(int y = dresult.y; y <= dresult.y2; y++){
                    Tile tile = world.tile(x, y);
                    if(tile == null || !validBreak(tile.x, tile.y)) continue;
                    tile = tile.target();

                    Lines.poly(tile.drawx(), tile.drawy(), 4, tile.block().size * tilesize / 2f, 45 + 15);
                }
            }

            Lines.rect(result.x, result.y, result.x2 - result.x, result.y2 - result.y);
        }else if(isPlacing()){
            if(recipe.result.rotate){
                Draw.color(!validPlace(cursor.x, cursor.y, recipe.result, rotation) ? Palette.remove : Palette.placeRotate);
                Draw.grect("place-arrow", cursor.worldx() + recipe.result.offset(),
                        cursor.worldy() + recipe.result.offset(), rotation * 90 - 90);
            }
            drawPlace(cursor.x, cursor.y, recipe.result, rotation);
            recipe.result.drawPlace(cursor.x, cursor.y, rotation, validPlace(cursor.x, cursor.y, recipe.result, rotation));
        }

        Draw.reset();
    }

    @Override
    public void update(){
        if(Net.active() && Inputs.keyTap("player_list")){
            ui.listfrag.toggle();
        }

        if(state.is(State.menu) || ui.hasDialog()) return;

        boolean controller = KeyBinds.getSection(section).device.type == DeviceType.controller;

        //zoom and rotate things
        if(Inputs.getAxisActive("zoom") && (Inputs.keyDown(section, "zoom_hold") || controller)){
            renderer.scaleCamera((int) Inputs.getAxisTapped(section, "zoom"));
        }

        renderer.minimap().zoomBy(-(int) Inputs.getAxisTapped(section, "zoom_minimap"));

        if(player.isDead()) return;

        if(recipe != null && !Settings.getBool("desktop-place-help", false)){
            ui.showInfo("Desktop controls have been changed.\nTo deselect a block or stop building, [accent]use the middle mouse button[].");
            Settings.putBool("desktop-place-help", true);
            Settings.save();
        }

        player.isBoosting = Inputs.keyDown("dash");

        //deslect if not placing
        if(!isPlacing() && mode == placing){
            mode = none;
        }

        if(player.isShooting && !canShoot()){
            player.isShooting = false;
        }

        if(isPlacing()){
            cursorType = hand;
            selectScale = Mathf.lerpDelta(selectScale, 1f, 0.2f);
        }else{
            selectScale = 0f;
        }

        rotation = Mathf.mod(rotation + (int) Inputs.getAxisTapped(section, "rotate"), 4);

        Tile cursor = tileAt(control.gdxInput().getX(), control.gdxInput().getY());

        if(player.isDead()){
            cursorType = normal;
        }else if(cursor != null){
            cursor = cursor.target();

            cursorType = cursor.block().getCursor(cursor);

            if(isPlacing()){
                cursorType = hand;
            }

            if(!isPlacing() && canMine(cursor)){
                cursorType = drill;
            }

            if(canTapPlayer(Graphics.mouseWorld().x, Graphics.mouseWorld().y)){
                cursorType = unload;
            }
        }

        if(!ui.hasMouse()){
            cursorType.set();
        }

        cursorType = normal;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button){
        if(player.isDead() || state.is(State.menu) || ui.hasDialog() || ui.hasMouse()) return false;

        Tile cursor = tileAt(screenX, screenY);
        if(cursor == null) return false;

        float worldx = Graphics.world(screenX, screenY).x, worldy = Graphics.world(screenX, screenY).y;

        if(button == Buttons.LEFT){ //left = begin placing
            if(isPlacing()){
                selectX = cursor.x;
                selectY = cursor.y;
                mode = placing;
            }else{
                //only begin shooting if there's no cursor event
                if(!tileTapped(cursor) && !tryTapPlayer(worldx, worldy) && player.getPlaceQueue().size == 0 && !droppingItem &&
                        !tryBeginMine(cursor) && player.getMineTile() == null){
                    player.isShooting = true;
                }
            }
        }else if(button == Buttons.RIGHT){ //right = begin breaking
            selectX = cursor.x;
            selectY = cursor.y;
            mode = breaking;
        }else if(button == Buttons.MIDDLE){ //middle button = cancel placing
            if(recipe == null){
                player.clearBuilding();
            }

            recipe = null;
            mode = none;
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button){
        if(button == Buttons.LEFT){
            player.isShooting = false;
        }

        if(player.isDead() || state.is(State.menu) || ui.hasDialog()) return false;

        Tile cursor = tileAt(screenX, screenY);

        if(cursor == null){
            mode = none;
            return false;
        }

        if(mode == placing){ //touch up while placing, place everything in selection
            NormalizeResult result = PlaceUtils.normalizeArea(selectX, selectY, cursor.x, cursor.y, rotation, true, maxLength);

            for(int i = 0; i <= result.getLength(); i += recipe.result.size){
                int x = selectX + i * Mathf.sign(cursor.x - selectX) * Mathf.bool(result.isX());
                int y = selectY + i * Mathf.sign(cursor.y - selectY) * Mathf.bool(!result.isX());

                rotation = result.rotation;

                tryPlaceBlock(x, y);
            }
        }else if(mode == breaking){ //touch up while breaking, break everything in selection
            NormalizeResult result = PlaceUtils.normalizeArea(selectX, selectY, cursor.x, cursor.y, rotation, false, maxLength);

            if(debug && Inputs.keyDown(Input.CONTROL_LEFT)){
                printArea(result);
            }else{
                for(int x = 0; x <= Math.abs(result.x2 - result.x); x++){
                    for(int y = 0; y <= Math.abs(result.y2 - result.y); y++){
                        int wx = selectX + x * Mathf.sign(cursor.x - selectX);
                        int wy = selectY + y * Mathf.sign(cursor.y - selectY);

                        tryBreakBlock(wx, wy);
                    }
                }
            }
        }

        tryDropItems(cursor.target(), Graphics.world(screenX, screenY).x, Graphics.world(screenX, screenY).y);

        mode = none;

        return false;
    }

    @Override
    public float getMouseX(){
        return !controlling ? control.gdxInput().getX() : controlx;
    }

    @Override
    public float getMouseY(){
        return !controlling ? control.gdxInput().getY() : controly;
    }

    @Override
    public boolean isCursorVisible(){
        return controlling;
    }

    @Override
    public void updateController(){
        boolean mousemove = Gdx.input.getDeltaX() > 1 || Gdx.input.getDeltaY() > 1;

        if(state.is(State.menu)){
            droppingItem = false;
        }

        if(KeyBinds.getSection(section).device.type == DeviceType.controller && (!mousemove || player.playerIndex > 0)){
            if(player.playerIndex > 0){
                controlling = true;
            }
            /*
            if(Inputs.keyTap(section,"select")){
                Inputs.getProcessor().touchDown((int)getMouseX(), (int)getMouseY(), player.playerIndex, Buttons.LEFT);
            }

            if(Inputs.keyRelease(section,"select")){
                Inputs.getProcessor().touchUp((int)getMouseX(), (int)getMouseY(), player.playerIndex, Buttons.LEFT);
            }*/

            float xa = Inputs.getAxis(section, "cursor_x");
            float ya = Inputs.getAxis(section, "cursor_y");

            if(Math.abs(xa) > controllerMin || Math.abs(ya) > controllerMin){
                float scl = Settings.getInt("sensitivity", 100) / 100f * Unit.dp.scl(1f);
                controlx += xa * baseControllerSpeed * scl;
                controly -= ya * baseControllerSpeed * scl;
                controlling = true;

                if(player.playerIndex == 0){
                    Gdx.input.setCursorCatched(true);
                }

                Inputs.getProcessor().touchDragged((int) getMouseX(), (int) getMouseY(), player.playerIndex);
            }

            controlx = Mathf.clamp(controlx, 0, Gdx.graphics.getWidth());
            controly = Mathf.clamp(controly, 0, Gdx.graphics.getHeight());
        }else{
            controlling = false;
            Gdx.input.setCursorCatched(false);
        }

        if(!controlling){
            controlx = control.gdxInput().getX();
            controly = control.gdxInput().getY();
        }
    }

}
