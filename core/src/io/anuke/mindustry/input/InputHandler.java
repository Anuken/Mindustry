package io.anuke.mindustry.input;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.function.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.input.*;
import io.anuke.arc.input.GestureDetector.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.scene.*;
import io.anuke.arc.scene.event.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.effect.*;
import io.anuke.mindustry.entities.traits.BuilderTrait.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.Teams.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.input.PlaceUtils.*;
import io.anuke.mindustry.net.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.fragments.*;
import io.anuke.mindustry.world.*;

import java.util.*;

import static io.anuke.mindustry.Vars.*;

public abstract class InputHandler implements InputProcessor, GestureListener{
    /** Used for dropping items. */
    final static float playerSelectRange = mobile ? 17f : 11f;
    /** Maximum line length. */
    final static int maxLength = 100;
    final static Vector2 stackTrns = new Vector2();
    final static Rectangle r1 = new Rectangle(), r2 = new Rectangle();
    /** Distance on the back from where items originate. */
    final static float backTrns = 3f;

    public final OverlayFragment frag = new OverlayFragment();

    public Block block;
    public boolean overrideLineRotation;
    public int rotation;
    public boolean droppingItem;
    public Group uiGroup;

    protected GestureDetector detector;
    protected PlaceLine line = new PlaceLine();
    protected BuildRequest resultreq;
    protected BuildRequest brequest = new BuildRequest();
    protected Array<BuildRequest> lineRequests = new Array<>();
    protected Array<BuildRequest> selectRequests = new Array<>();

    //methods to override

    @Remote(targets = Loc.client, called = Loc.server)
    public static void dropItem(Player player, float angle){
        if(net.server() && player.item().amount <= 0){
            throw new ValidateException(player, "Player cannot drop an item.");
        }

        Effects.effect(Fx.dropItem, Color.white, player.x, player.y, angle, player.item().item);
        player.clearItem();
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true, unreliable = true)
    public static void rotateBlock(Player player, Tile tile, boolean direction){
        if(net.server() && !Units.canInteract(player, tile)){
            throw new ValidateException(player, "Player cannot drop an item.");
        }

        tile.rotation(Mathf.mod(tile.rotation() + Mathf.sign(direction), 4));

        if(tile.entity != null){
            tile.entity.updateProximity();
            tile.entity.noSleep();
        }
    }

    @Remote(targets = Loc.both, forward = true, called = Loc.server)
    public static void transferInventory(Player player, Tile tile){
        if(player == null || player.timer == null || !player.timer.get(Player.timerTransfer, 40)) return;
        if(net.server() && (player.item().amount <= 0 || player.isTransferring|| !Units.canInteract(player, tile))){
            throw new ValidateException(player, "Player cannot transfer an item.");
        }

        if(tile.entity == null) return;

        player.isTransferring = true;

        Item item = player.item().item;
        int amount = player.item().amount;
        int accepted = tile.block().acceptStack(item, amount, tile, player);
        player.item().amount -= accepted;

        int sent = Mathf.clamp(accepted / 4, 1, 8);
        int removed = accepted / sent;
        int[] remaining = {accepted, accepted};
        Block block = tile.block();

        Events.fire(new DepositEvent());

        for(int i = 0; i < sent; i++){
            boolean end = i == sent - 1;
            Time.run(i * 3, () -> {
                tile.block().getStackOffset(item, tile, stackTrns);

                ItemTransfer.create(item,
                player.x + Angles.trnsx(player.rotation + 180f, backTrns), player.y + Angles.trnsy(player.rotation + 180f, backTrns),
                new Vector2(tile.drawx() + stackTrns.x, tile.drawy() + stackTrns.y), () -> {
                    if(tile.block() != block || tile.entity == null || tile.entity.items == null) return;

                    tile.block().handleStack(item, removed, tile, player);
                    remaining[1] -= removed;

                    if(end && remaining[1] > 0){
                        tile.block().handleStack(item, remaining[1], tile, player);
                    }
                });

                remaining[0] -= removed;

                if(end){
                    player.isTransferring = false;
                }
            });
        }
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true)
    public static void onTileTapped(Player player, Tile tile){
        if(tile == null || player == null) return;
        if(!Units.canInteract(player, tile)) return;
        tile.block().tapped(tile, player);
    }

    @Remote(targets = Loc.both, called = Loc.both, forward = true)
    public static void onTileConfig(Player player, Tile tile, int value){
        if(tile == null || !Units.canInteract(player, tile)) return;
        tile.block().configured(tile, player, value);
    }

    public Eachable<BuildRequest> allRequests(){
        return cons -> {
            for(BuildRequest request : player.buildQueue()) cons.accept(request);
            for(BuildRequest request : selectRequests) cons.accept(request);
            for(BuildRequest request : lineRequests) cons.accept(request);
        };
    }

    public OverlayFragment getFrag(){
        return frag;
    }

    public void update(){

    }

    public float getMouseX(){
        return Core.input.mouseX();
    }

    public float getMouseY(){
        return Core.input.mouseY();
    }

    public void buildPlacementUI(Table table){

    }

    public void buildUI(Group group){

    }

    public void updateState(){

    }

    public void drawBottom(){

    }

    public void drawTop(){

    }

    public boolean isDrawing(){
        return false;
    }

    public void drawSelected(int x, int y, Block block, Color color){
        Draw.color(color);
        for(int i = 0; i < 4; i++){
            Point2 p = Geometry.d8edge[i];
            float offset = -Math.max(block.size - 1, 0) / 2f * tilesize;
            Draw.rect("block-select",
                x*tilesize + block.offset() + offset * p.x,
                y*tilesize + block.offset() + offset * p.y, i * 90);
        }
        Draw.reset();
    }

    public void drawBreaking(BuildRequest request){
        if(request.breaking){
            drawBreaking(request.x, request.y);
        }else{
            drawSelected(request.x, request.y, request.block, Pal.remove);
        }
    }

    public void drawBreaking(int x, int y){
        Tile tile = world.ltile(x, y);
        if(tile == null) return;
        Block block = tile.block();

        drawSelected(x, y, block, Pal.remove);
    }

    /** Returns the selection request that overlaps this position, or null. */
    protected BuildRequest getRequest(int x, int y){
        return getRequest(x, y, 1, null);
    }

    /** Returns the selection request that overlaps this position, or null. */
    protected BuildRequest getRequest(int x, int y, int size, BuildRequest skip){
        float offset = ((size + 1) % 2) * tilesize / 2f;
        r2.setSize(tilesize * size);
        r2.setCenter(x * tilesize + offset, y * tilesize + offset);
        resultreq = null;

        Predicate<BuildRequest> test = req -> {
            if(req == skip) return false;
            Tile other = req.tile();

            if(other == null) return false;

            if(!req.breaking){
                r1.setSize(req.block.size * tilesize);
                r1.setCenter(other.worldx() + req.block.offset(), other.worldy() + req.block.offset());
            }else{
                r1.setSize(other.block().size * tilesize);
                r1.setCenter(other.worldx() + other.block().offset(), other.worldy() + other.block().offset());
            }

            return r2.overlaps(r1);
        };

        for(BuildRequest req : player.buildQueue()){
            if(test.test(req)) return req;
        }

        for(BuildRequest req : selectRequests){
            if(test.test(req)) return req;
        }

        return null;
    }

    protected void drawSelection(int x1, int y1, int x2, int y2){
        NormalizeDrawResult result = PlaceUtils.normalizeDrawArea(Blocks.air, x1, y1, x2, y2, false, maxLength, 1f);
        NormalizeResult dresult = PlaceUtils.normalizeArea(x1, y1, x2, y2, rotation, false, maxLength);

        for(int x = dresult.x; x <= dresult.x2; x++){
            for(int y = dresult.y; y <= dresult.y2; y++){
                Tile tile = world.ltile(x, y);
                if(tile == null || !validBreak(tile.x, tile.y)) continue;

                drawBreaking(tile.x, tile.y);
            }
        }

        Tmp.r1.set(result.x, result.y, result.x2 - result.x, result.y2 - result.y);

        Draw.color(Pal.remove);
        Lines.stroke(1f);

        for(BuildRequest req : player.buildQueue()){
            if(req.breaking) continue;
            if(req.bounds(Tmp.r2).overlaps(Tmp.r1)){
                drawBreaking(req);
            }
        }

        for(BrokenBlock req : state.teams.get(player.getTeam()).brokenBlocks){
            Block block = content.block(req.block);
            if(block.bounds(req.x, req.y, Tmp.r2).overlaps(Tmp.r1)){
                drawSelected(req.x, req.y, content.block(req.block), Pal.remove);
            }
        }

        Lines.stroke(2f);

        Draw.color(Pal.removeBack);
        Lines.rect(result.x, result.y - 1, result.x2 - result.x, result.y2 - result.y);
        Draw.color(Pal.remove);
        Lines.rect(result.x, result.y, result.x2 - result.x, result.y2 - result.y);
    }

    protected void flushSelectRequests(Array<BuildRequest> requests){
        for(BuildRequest req : requests){
            if(req.block != null && validPlace(req.x, req.y, req.block, req.rotation)){
                selectRequests.add(req);
            }
        }
    }

    protected void flushRequests(Array<BuildRequest> requests){
        for(BuildRequest req : requests){
            if(req.block != null && validPlace(req.x, req.y, req.block, req.rotation)){
                player.addBuildRequest(req);
            }
        }
    }

    protected void drawRequest(BuildRequest request){
        drawRequest(request.x, request.y, request.block, request.rotation);
    }

    /** Draws a placement icon for a specific block. */
    protected void drawRequest(int x, int y, Block block, int rotation){
        brequest.set(x, y, rotation, block);
        block.drawRequest(brequest, allRequests(), validPlace(x, y, block, rotation));
    }

    /** Remove everything from the queue in a selection. */
    protected void removeSelection(int x1, int y1, int x2, int y2){
        removeSelection(x1, y1, x2, y2, false);
    }

    /** Remove everything from the queue in a selection. */
    protected void removeSelection(int x1, int y1, int x2, int y2, boolean flush){
        NormalizeResult result = PlaceUtils.normalizeArea(x1, y1, x2, y2, rotation, false, maxLength);
        for(int x = 0; x <= Math.abs(result.x2 - result.x); x++){
            for(int y = 0; y <= Math.abs(result.y2 - result.y); y++){
                int wx = x1 + x * Mathf.sign(x2 - x1);
                int wy = y1 + y * Mathf.sign(y2 - y1);

                if(!flush){
                    tryBreakBlock(wx, wy);
                }else{
                    selectRequests.add(new BuildRequest(wx, wy));
                }
            }
        }

        //remove build requests
        Tmp.r1.set(result.x * tilesize, result.y * tilesize, (result.x2 - result.x) * tilesize, (result.y2 - result.y) * tilesize);
        Iterator<BuildRequest> it = player.buildQueue().iterator();
        while(it.hasNext()){
            BuildRequest req = it.next();
            if(!req.breaking && req.bounds(Tmp.r2).overlaps(Tmp.r1)){
                it.remove();
            }
        }

        //remove blocks to rebuild
        Iterator<BrokenBlock> broken = state.teams.get(player.getTeam()).brokenBlocks.iterator();
        while(broken.hasNext()){
            BrokenBlock req = broken.next();
            Block block = content.block(req.block);
            if(block.bounds(req.x, req.y, Tmp.r2).overlaps(Tmp.r1)){
                broken.remove();
            }
        }
    }

    protected void updateLine(int x1, int y1, int x2, int y2){
        lineRequests.clear();
        iterateLine(x1, y1, x2, y2, l -> {
            rotation = l.rotation;
            lineRequests.add(new BuildRequest(l.x, l.y, l.rotation, block));
        });
    }

    protected void updateLine(int x1, int y1){
        updateLine(x1, y1, tileX(getMouseX()), tileY(getMouseY()));
    }

    /** Handles tile tap events that are not platform specific. */
    boolean tileTapped(Tile tile){
        tile = tile.link();

        boolean consumed = false, showedInventory = false;

        //check if tapped block is configurable
        if(tile.block().configurable && tile.interactable(player.getTeam())){
            consumed = true;
            if(((!frag.config.isShown() && tile.block().shouldShowConfigure(tile, player)) //if the config fragment is hidden, show
            //alternatively, the current selected block can 'agree' to switch config tiles
            || (frag.config.isShown() && frag.config.getSelectedTile().block().onConfigureTileTapped(frag.config.getSelectedTile(), tile)))){
                Sounds.click.at(tile);
                frag.config.showConfig(tile);
            }
            //otherwise...
        }else if(!frag.config.hasConfigMouse()){ //make sure a configuration fragment isn't on the cursor
            //then, if it's shown and the current block 'agrees' to hide, hide it.
            if(frag.config.isShown() && frag.config.getSelectedTile().block().onConfigureTileTapped(frag.config.getSelectedTile(), tile)){
                consumed = true;
                frag.config.hideConfig();
            }

            if(frag.config.isShown()){
                consumed = true;
            }
        }

        //call tapped event
        if(!consumed && tile.interactable(player.getTeam())){
            Call.onTileTapped(player, tile);
        }

        //consume tap event if necessary
        if(tile.interactable(player.getTeam()) && tile.block().consumesTap){
            consumed = true;
        }else if(tile.interactable(player.getTeam()) && tile.block().synthetic() && !consumed){
            if(tile.block().hasItems && tile.entity.items.total() > 0){
                frag.inv.showFor(tile);
                consumed = true;
                showedInventory = true;
            }
        }

        //clear when the player taps on something else
        if(!consumed && !mobile && player.isBuilding() && block == null){
            //player.clearBuilding();
            block = null;
            return true;
        }

        if(!showedInventory){
            frag.inv.hide();
        }

        return consumed;
    }

    /** Tries to select the player to drop off items, returns true if successful. */
    boolean tryTapPlayer(float x, float y){
        if(canTapPlayer(x, y)){
            droppingItem = true;
            return true;
        }
        return false;
    }

    boolean canTapPlayer(float x, float y){
        return Mathf.dst(x, y, player.x, player.y) <= playerSelectRange && player.item().amount > 0;
    }

    /** Tries to begin mining a tile, returns true if successful. */
    boolean tryBeginMine(Tile tile){
        if(canMine(tile)){
            //if a block is clicked twice, reset it
            player.setMineTile(player.getMineTile() == tile ? null : tile);
            return true;
        }
        return false;
    }

    boolean canMine(Tile tile){
        return !Core.scene.hasMouse()
        && tile.drop() != null && tile.drop().hardness <= player.mech.drillPower
        && !(tile.floor().playerUnmineable && tile.overlay().itemDrop == null)
        && player.acceptsItem(tile.drop())
        && tile.block() == Blocks.air && player.dst(tile.worldx(), tile.worldy()) <= Player.mineDistance;
    }

    /** Returns the tile at the specified MOUSE coordinates. */
    Tile tileAt(float x, float y){
        return world.tile(tileX(x), tileY(y));
    }

    int tileX(float cursorX){
        Vector2 vec = Core.input.mouseWorld(cursorX, 0);
        if(selectedBlock()){
            vec.sub(block.offset(), block.offset());
        }
        return world.toTile(vec.x);
    }

    int tileY(float cursorY){
        Vector2 vec = Core.input.mouseWorld(0, cursorY);
        if(selectedBlock()){
            vec.sub(block.offset(), block.offset());
        }
        return world.toTile(vec.y);
    }

    public boolean selectedBlock(){
        return isPlacing();
    }

    public boolean isPlacing(){
        return block != null;
    }

    public float mouseAngle(float x, float y){
        return Core.input.mouseWorld(getMouseX(), getMouseY()).sub(x, y).angle();
    }

    public void remove(){
        Core.input.removeProcessor(this);
        frag.remove();
        if(Core.scene != null){
            Table table = (Table)Core.scene.find("inputTable");
            if(table != null){
                table.clear();
            }
        }
        if(detector != null){
            Core.input.removeProcessor(detector);
        }
        if(uiGroup != null){
            uiGroup.remove();
            uiGroup = null;
        }
    }

    public void add(){
        Core.input.addProcessor(detector = new GestureDetector(20, 0.5f, 0.4f, 0.15f, this));
        Core.input.addProcessor(this);
        if(Core.scene != null){
            Table table = (Table)Core.scene.find("inputTable");
            if(table != null){
                table.clear();
                buildPlacementUI(table);
            }

            uiGroup = new WidgetGroup();
            uiGroup.touchable(Touchable.childrenOnly);
            uiGroup.setFillParent(true);
            ui.hudGroup.addChild(uiGroup);
            buildUI(uiGroup);

            frag.add();
        }

        if(player != null){
            player.isBuilding = true;
        }
    }

    public boolean canShoot(){
        return block == null && !Core.scene.hasMouse() && !onConfigurable() && !isDroppingItem();
    }

    public boolean onConfigurable(){
        return false;
    }

    public boolean isDroppingItem(){
        return droppingItem;
    }

    public void tryDropItems(Tile tile, float x, float y){
        if(!droppingItem || player.item().amount <= 0 || canTapPlayer(x, y) || state.isPaused() || !player.timer.check(Player.timerTransfer, 40)){
            droppingItem = false;
            return;
        }

        droppingItem = false;

        ItemStack stack = player.item();

        if(tile.block().acceptStack(stack.item, stack.amount, tile, player) > 0 && tile.interactable(player.getTeam()) && tile.block().hasItems && player.item().amount > 0 && !player.isTransferring && tile.interactable(player.getTeam())){
            Call.transferInventory(player, tile);
        }else{
            Call.dropItem(player.angleTo(x, y));
        }
    }

    public void tryPlaceBlock(int x, int y){
        if(block != null && validPlace(x, y, block, rotation)){
            placeBlock(x, y, block, rotation);
        }
    }

    public void tryBreakBlock(int x, int y){
        if(validBreak(x, y)){
            breakBlock(x, y);
        }
    }

    public boolean validPlace(int x, int y, Block type, int rotation){
        return validPlace(x, y, type, rotation, null);
    }

    public boolean validPlace(int x, int y, Block type, int rotation, BuildRequest ignore){
        for(BuildRequest req : player.buildQueue()){
            if(req != ignore && !req.breaking && req.block.bounds(req.x, req.y, Tmp.r1).overlaps(type.bounds(x, y, Tmp.r2))){
                return false;
            }
        }
        return Build.validPlace(player.getTeam(), x, y, type, rotation);
    }

    public boolean validBreak(int x, int y){
        return Build.validBreak(player.getTeam(), x, y);
    }

    public void placeBlock(int x, int y, Block block, int rotation){
        player.addBuildRequest(new BuildRequest(x, y, rotation, block));
    }

    public void breakBlock(int x, int y){
        Tile tile = world.ltile(x, y);
        player.addBuildRequest(new BuildRequest(tile.x, tile.y));
    }

    public void drawArrow(Block block, int x, int y, int rotation){
        drawArrow(block, x, y, rotation, validPlace(x, y, block, rotation));
    }

    public void drawArrow(Block block, int x, int y, int rotation, boolean valid){
        Draw.color(!valid ? Pal.removeBack : Pal.accentBack);
        Draw.rect(Core.atlas.find("place-arrow"),
        x * tilesize + block.offset(),
        y * tilesize + block.offset() - 1,
        Core.atlas.find("place-arrow").getWidth() * Draw.scl,
        Core.atlas.find("place-arrow").getHeight() * Draw.scl, rotation * 90 - 90);

        Draw.color(!valid ? Pal.remove : Pal.accent);
        Draw.rect(Core.atlas.find("place-arrow"),
        x * tilesize + block.offset(),
        y * tilesize + block.offset(),
        Core.atlas.find("place-arrow").getWidth() * Draw.scl,
        Core.atlas.find("place-arrow").getHeight() * Draw.scl, rotation * 90 - 90);
    }

    void iterateLine(int startX, int startY, int endX, int endY, Consumer<PlaceLine> cons){
        Array<Point2> points;
        boolean diagonal = Core.input.keyDown(Binding.diagonal_placement);
        if(Core.settings.getBool("swapdiagonal")){
            diagonal = !diagonal;
        }

        if(diagonal){
            points = PlaceUtils.normalizeDiagonal(startX, startY, endX, endY);
        }else{
            points = PlaceUtils.normalizeLine(startX, startY, endX, endY);
        }

        float angle = Angles.angle(startX, startY, endX, endY);
        int baseRotation = rotation;
        if(!overrideLineRotation || diagonal){
            baseRotation = (startX == endX && startY == endY) ? rotation : ((int)((angle + 45) / 90f)) % 4;
        }

        Tmp.r3.set(-1, -1, 0, 0);

        for(int i = 0; i < points.size; i++){
            Point2 point = points.get(i);

            if(block != null && Tmp.r2.setSize(block.size * tilesize).setCenter(point.x * tilesize + block.offset(), point.y * tilesize + block.offset()).overlaps(Tmp.r3)){
                continue;
            }

            Point2 next = i == points.size - 1 ? null : points.get(i + 1);
            line.x = point.x;
            line.y = point.y;
            if(!overrideLineRotation || diagonal){
                line.rotation = next != null ? Tile.relativeTo(point.x, point.y, next.x, next.y) : baseRotation;
            }else{
                line.rotation = rotation;
            }
            line.last = next == null;
            cons.accept(line);

            Tmp.r3.setSize(block.size * tilesize).setCenter(point.x * tilesize + block.offset(), point.y * tilesize + block.offset());
        }
    }

    class PlaceLine{
        public int x, y, rotation;
        public boolean last;
    }
}
