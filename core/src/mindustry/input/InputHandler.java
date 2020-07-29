package mindustry.input;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.input.GestureDetector.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.ai.formations.*;
import mindustry.ai.formations.patterns.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.Placement.*;
import mindustry.net.Administration.*;
import mindustry.net.*;
import mindustry.type.*;
import mindustry.ui.fragments.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.BuildBlock.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.meta.*;

import java.util.*;

import static mindustry.Vars.*;

public abstract class InputHandler implements InputProcessor, GestureListener{
    /** Used for dropping items. */
    final static float playerSelectRange = mobile ? 17f : 11f;
    /** Maximum line length. */
    final static int maxLength = 100;
    final static Vec2 stackTrns = new Vec2();
    final static Rect r1 = new Rect(), r2 = new Rect();
    final static Seq<Unit> units = new Seq<>();
    /** Distance on the back from where items originate. */
    final static float backTrns = 3f;

    public final OverlayFragment frag = new OverlayFragment();

    public @Nullable Block block;
    public boolean overrideLineRotation;
    public int rotation;
    public boolean droppingItem;
    public Group uiGroup;
    public boolean isBuilding = true, buildWasAutoPaused = false;
    public @Nullable UnitType controlledType;

    protected @Nullable Schematic lastSchematic;
    protected GestureDetector detector;
    protected PlaceLine line = new PlaceLine();
    protected BuildPlan resultreq;
    protected BuildPlan brequest = new BuildPlan();
    protected Seq<BuildPlan> lineRequests = new Seq<>();
    protected Seq<BuildPlan> selectRequests = new Seq<>();

    //methods to override

    @Remote(called = Loc.server, unreliable = true)
    public static void transferItemEffect(Item item, float x, float y, Itemsc to){
        if(to == null) return;
        createItemTransfer(item, 1, x, y, to, null);
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void transferItemToUnit(Item item, float x, float y, Itemsc to){
        if(to == null) return;
        createItemTransfer(item, 1, x, y, to, () -> to.addItem(item));
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void transferItemTo(Item item, int amount, float x, float y, Tile tile){
        if(tile == null || tile.build == null || tile.build.items == null) return;
        for(int i = 0; i < Mathf.clamp(amount / 3, 1, 8); i++){
            Time.run(i * 3, () -> createItemTransfer(item, amount, x, y, tile, () -> {}));
        }
        tile.build.items.add(item, amount);
    }

    public static void createItemTransfer(Item item, int amount, float x, float y, Position to, Runnable done){
        Fx.itemTransfer.at(x, y, amount, item.color, to);
        if(done != null){
            Time.run(Fx.itemTransfer.lifetime, done);
        }
    }

    @Remote(variants = Variant.one)
    public static void removeQueueBlock(int x, int y, boolean breaking){
        player.builder().removeBuild(x, y, breaking);
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true)
    public static void pickupUnitPayload(Player player, Unit target){
        Unit unit = player.unit();
        Payloadc pay = (Payloadc)unit;

        if(target.isAI() && target.isGrounded() && pay.payloads().size < unit.type().payloadCapacity
            && target.mass() < unit.mass()
            && target.within(unit, unit.type().hitsize * 1.5f + target.type().hitsize)){
            pay.pickup(target);
        }
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true)
    public static void pickupBlockPayload(Player player, Building tile){
        Unit unit = player.unit();
        Payloadc pay = (Payloadc)unit;

        if(tile != null && tile.team() == unit.team && pay.payloads().size < unit.type().payloadCapacity
            && unit.within(tile, tilesize * tile.block.size * 1.2f)){
            //pick up block directly
            if(tile.block().buildVisibility != BuildVisibility.hidden && tile.block().size <= 2){
                pay.pickup(tile);
            }else{ //pick up block payload
                Payload current = tile.getPayload();
                if(current != null && current.canBeTaken(pay)){
                    Payload taken = tile.takePayload();
                    if(taken != null){
                        pay.addPayload(taken);
                        Fx.unitPickup.at(tile);
                    }
                }
            }
        }
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true)
    public static void dropPayload(Player player, float x, float y){
        Payloadc pay = (Payloadc)player.unit();

        //allow a slight margin of error
        if(pay.within(x, y, tilesize * 2f)){
            float prevx = pay.x(), prevy = pay.y();
            pay.set(x, y);
            pay.dropLastPayload();
            pay.set(prevx, prevy);
        }
    }

    @Remote(targets = Loc.client, called = Loc.server)
    public static void dropItem(Player player, float angle){
        if(net.server() && player.unit().stack.amount <= 0){
            throw new ValidateException(player, "Player cannot drop an item.");
        }

        Fx.dropItem.at(player.x, player.y, angle, Color.white, player.unit().item());
        player.unit().clearItem();
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true, unreliable = true)
    public static void rotateBlock(Player player, Building tile, boolean direction){
        if(net.server() && (!Units.canInteract(player, tile) ||
            !netServer.admins.allowAction(player, ActionType.rotate, tile.tile(), action -> action.rotation = Mathf.mod(tile.rotation + Mathf.sign(direction), 4)))){
            throw new ValidateException(player, "Player cannot rotate a block.");
        }

        tile.rotation = Mathf.mod(tile.rotation + Mathf.sign(direction), 4);
        tile.updateProximity();
        tile.noSleep();
    }

    @Remote(targets = Loc.both, forward = true, called = Loc.server)
    public static void transferInventory(Player player, Building tile){
        if(player == null || tile == null) return;
        if(net.server() && (player.unit().stack.amount <= 0 || !Units.canInteract(player, tile) ||
            !netServer.admins.allowAction(player, ActionType.depositItem, tile.tile(), action -> {
                action.itemAmount = player.unit().stack.amount;
                action.item = player.unit().item();
            }))){
            throw new ValidateException(player, "Player cannot transfer an item.");
        }

        Item item = player.unit().item();
        int amount = player.unit().stack.amount;
        int accepted = tile.acceptStack(item, amount, player.unit());
        player.unit().stack.amount -= accepted;

        Core.app.post(() -> Events.fire(new DepositEvent(tile, player, item, accepted)));

        tile.getStackOffset(item, stackTrns);
        tile.handleStack(item, accepted, player.unit());

        createItemTransfer(
            item,
            amount,
            player.x + Angles.trnsx(player.unit().rotation + 180f, backTrns), player.y + Angles.trnsy(player.unit().rotation + 180f, backTrns),
            new Vec2(tile.x + stackTrns.x, tile.y + stackTrns.y),
            () -> {}
        );
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true)
    public static void tileTapped(Player player, Building tile){
        if(tile == null || player == null) return;
        if(net.server() && (!Units.canInteract(player, tile) ||
        !netServer.admins.allowAction(player, ActionType.tapTile, tile.tile(), action -> {}))) throw new ValidateException(player, "Player cannot tap a tile.");
        tile.tapped(player);
        Core.app.post(() -> Events.fire(new TapEvent(tile, player)));
    }

    @Remote(targets = Loc.both, called = Loc.both, forward = true)
    public static void tileConfig(Player player, Building tile, @Nullable Object value){
        if(tile == null) return;
        if(net.server() && (!Units.canInteract(player, tile) ||
            !netServer.admins.allowAction(player, ActionType.configure, tile.tile(), action -> action.config = value))) throw new ValidateException(player, "Player cannot configure a tile.");
        tile.configured(player, value);
        Core.app.post(() -> Events.fire(new TapConfigEvent(tile, player, value)));
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true)
    public static void unitControl(Player player, @Nullable Unit unit){
        //clear player unit when they possess a core
        if((unit instanceof BlockUnitc && ((BlockUnitc)unit).tile() instanceof CoreEntity)){
            Fx.spawn.at(player);
            player.clearUnit();
            player.deathTimer(60f); //for instant respawn
        }else if(unit == null){ //just clear the unit (is this used?)
            player.clearUnit();
            //make sure it's AI controlled, so players can't overwrite each other
        }else if(unit.isAI() && unit.team == player.team() && !unit.deactivated){
            player.unit(unit);
            Time.run(Fx.unitSpirit.lifetime, () -> Fx.unitControl.at(unit.x, unit.y, 0f, unit));
            if(!player.dead()){
                Fx.unitSpirit.at(player.x, player.y, 0f, unit);
            }
        }
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true)
    public static void unitClear(Player player){
        //no free core teleports?
        if(!player.dead() && player.unit().spawnedByCore) return;

        Fx.spawn.at(player);
        player.clearUnit();
        player.deathTimer(60f); //for instant respawn
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true)
    public static void unitCommand(Player player){
        if(player.dead() || !(player.unit() instanceof Commanderc)) return;

        Commanderc commander = (Commanderc)player.unit();

        if(commander.isCommanding()){
            commander.clearCommand();
        }else{
            FormationPattern pattern = new SquareFormation();
            Formation formation = new Formation(new Vec3(player.x, player.y, player.unit().rotation), pattern);
            formation.slotAssignmentStrategy = new DistanceAssignmentStrategy(pattern);

            units.clear();

            Fx.commandSend.at(player);
            Units.nearby(player.team(), player.x, player.y, 200f, u -> {
                if(u.isAI()){
                    units.add(u);
                }
            });

            units.sort(u -> u.dst2(player.unit()));
            units.truncate(player.unit().type().commandLimit);

            commander.command(formation, units);
        }

    }

    public Eachable<BuildPlan> allRequests(){
        return cons -> {
            for(BuildPlan request : player.builder().plans()) cons.get(request);
            for(BuildPlan request : selectRequests) cons.get(request);
            for(BuildPlan request : lineRequests) cons.get(request);
        };
    }

    public boolean isUsingSchematic(){
        return !selectRequests.isEmpty();
    }

    public OverlayFragment getFrag(){
        return frag;
    }

    public void update(){
        player.typing = ui.chatfrag.shown();

        if(player.isBuilder()){
            player.builder().updateBuilding(isBuilding);
        }

        if(!player.dead()){
            controlledType = player.unit().type();
        }

        if(controlledType != null && player.dead()){
            Unit unit = Units.closest(player.team(), player.x, player.y, u -> !u.isPlayer() && u.type() == controlledType && !u.deactivated);

            if(unit != null){
                Call.unitControl(player, unit);
            }
        }
    }

    public void checkUnit(){
        if(controlledType != null){
            Unit unit = Units.closest(player.team(), player.x, player.y, u -> !u.isPlayer() && u.type() == controlledType && !u.deactivated);
            if(unit == null && controlledType == UnitTypes.block){
                unit = world.buildWorld(player.x, player.y) instanceof ControlBlock ? ((ControlBlock)world.buildWorld(player.x, player.y)).unit() : null;
            }

            if(unit != null){
                if(net.client()){
                    Call.unitControl(player, unit);
                }else{
                    unit.controller(player);
                }
            }
        }
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

    public void drawSelected(int x, int y, Block block, Color color){
        Drawf.selected(x, y, block, color);
    }

    public void drawBreaking(BuildPlan request){
        if(request.breaking){
            drawBreaking(request.x, request.y);
        }else{
            drawSelected(request.x, request.y, request.block, Pal.remove);
        }
    }

    public boolean requestMatches(BuildPlan request){
        Tile tile = world.tile(request.x, request.y);
        return tile != null && tile.block() instanceof BuildBlock && tile.<BuildEntity>bc().cblock == request.block;
    }

    public void drawBreaking(int x, int y){
        Tile tile = world.tile(x, y);
        if(tile == null) return;
        Block block = tile.block();

        drawSelected(x, y, block, Pal.remove);
    }

    public void useSchematic(Schematic schem){
        selectRequests.addAll(schematics.toRequests(schem, player.tileX(), player.tileY()));
    }

    protected void showSchematicSave(){
        if(lastSchematic == null) return;

        ui.showTextInput("$schematic.add", "$name", "", text -> {
            Schematic replacement = schematics.all().find(s -> s.name().equals(text));
            if(replacement != null){
                ui.showConfirm("$confirm", "$schematic.replace", () -> {
                    schematics.overwrite(replacement, lastSchematic);
                    ui.showInfoFade("$schematic.saved");
                    ui.schematics.showInfo(replacement);
                });
            }else{
                lastSchematic.tags.put("name", text);
                schematics.add(lastSchematic);
                ui.showInfoFade("$schematic.saved");
                ui.schematics.showInfo(lastSchematic);
            }
        });
    }

    public void rotateRequests(Seq<BuildPlan> requests, int direction){
        int ox = schemOriginX(), oy = schemOriginY();

        requests.each(req -> {
            req.pointConfig(p -> {
                int cx = p.x, cy = p.y;
                int lx = cx;

                if(direction >= 0){
                    cx = -cy;
                    cy = lx;
                }else{
                    cx = cy;
                    cy = -lx;
                }
                p.set(cx, cy);
            });

            //rotate actual request, centered on its multiblock position
            float wx = (req.x - ox) * tilesize + req.block.offset, wy = (req.y - oy) * tilesize + req.block.offset;
            float x = wx;
            if(direction >= 0){
                wx = -wy;
                wy = x;
            }else{
                wx = wy;
                wy = -x;
            }
            req.x = world.toTile(wx - req.block.offset) + ox;
            req.y = world.toTile(wy - req.block.offset) + oy;
            req.rotation = Mathf.mod(req.rotation + direction, 4);
        });
    }

    public void flipRequests(Seq<BuildPlan> requests, boolean x){
        int origin = (x ? schemOriginX() : schemOriginY()) * tilesize;

        requests.each(req -> {
            float value = -((x ? req.x : req.y) * tilesize - origin + req.block.offset) + origin;

            if(x){
                req.x = (int)((value - req.block.offset) / tilesize);
            }else{
                req.y = (int)((value - req.block.offset) / tilesize);
            }

            req.pointConfig(p -> {
                int corigin = x ? req.originalWidth/2 : req.originalHeight/2;
                int nvalue = -(x ? p.x : p.y);
                if(x){
                    req.originalX = -(req.originalX - corigin) + corigin;
                    p.x = nvalue;
                }else{
                    req.originalY = -(req.originalY - corigin) + corigin;
                    p.y = nvalue;
                }
            });

            //flip rotation
            if(x == (req.rotation % 2 == 0)){
                req.rotation = Mathf.mod(req.rotation + 2, 4);
            }
        });
    }

    protected int schemOriginX(){
        return rawTileX();
    }

    protected int schemOriginY(){
        return rawTileY();
    }

    /** Returns the selection request that overlaps this position, or null. */
    protected BuildPlan getRequest(int x, int y){
        return getRequest(x, y, 1, null);
    }

    /** Returns the selection request that overlaps this position, or null. */
    protected BuildPlan getRequest(int x, int y, int size, BuildPlan skip){
        float offset = ((size + 1) % 2) * tilesize / 2f;
        r2.setSize(tilesize * size);
        r2.setCenter(x * tilesize + offset, y * tilesize + offset);
        resultreq = null;

        Boolf<BuildPlan> test = req -> {
            if(req == skip) return false;
            Tile other = req.tile();

            if(other == null) return false;

            if(!req.breaking){
                r1.setSize(req.block.size * tilesize);
                r1.setCenter(other.worldx() + req.block.offset, other.worldy() + req.block.offset);
            }else{
                r1.setSize(other.block().size * tilesize);
                r1.setCenter(other.worldx() + other.block().offset, other.worldy() + other.block().offset);
            }

            return r2.overlaps(r1);
        };

        for(BuildPlan req : player.builder().plans()){
            if(test.get(req)) return req;
        }

        for(BuildPlan req : selectRequests){
            if(test.get(req)) return req;
        }

        return null;
    }

    protected void drawBreakSelection(int x1, int y1, int x2, int y2){
        NormalizeDrawResult result = Placement.normalizeDrawArea(Blocks.air, x1, y1, x2, y2, false, maxLength, 1f);
        NormalizeResult dresult = Placement.normalizeArea(x1, y1, x2, y2, rotation, false, maxLength);

        for(int x = dresult.x; x <= dresult.x2; x++){
            for(int y = dresult.y; y <= dresult.y2; y++){
                Tile tile = world.tileBuilding(x, y);
                if(tile == null || !validBreak(tile.x, tile.y)) continue;

                drawBreaking(tile.x, tile.y);
            }
        }

        Tmp.r1.set(result.x, result.y, result.x2 - result.x, result.y2 - result.y);

        Draw.color(Pal.remove);
        Lines.stroke(1f);

        for(BuildPlan req : player.builder().plans()){
            if(req.breaking) continue;
            if(req.bounds(Tmp.r2).overlaps(Tmp.r1)){
                drawBreaking(req);
            }
        }

        for(BuildPlan req : selectRequests){
            if(req.breaking) continue;
            if(req.bounds(Tmp.r2).overlaps(Tmp.r1)){
                drawBreaking(req);
            }
        }

        for(BlockPlan req : player.team().data().blocks){
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

    protected void drawSelection(int x1, int y1, int x2, int y2, int maxLength){
        NormalizeDrawResult result = Placement.normalizeDrawArea(Blocks.air, x1, y1, x2, y2, false, maxLength, 1f);

        Lines.stroke(2f);

        Draw.color(Pal.accentBack);
        Lines.rect(result.x, result.y - 1, result.x2 - result.x, result.y2 - result.y);
        Draw.color(Pal.accent);
        Lines.rect(result.x, result.y, result.x2 - result.x, result.y2 - result.y);
    }

    protected void flushSelectRequests(Seq<BuildPlan> requests){
        for(BuildPlan req : requests){
            if(req.block != null && validPlace(req.x, req.y, req.block, req.rotation)){
                BuildPlan other = getRequest(req.x, req.y, req.block.size, null);
                if(other == null){
                    selectRequests.add(req.copy());
                }else if(!other.breaking && other.x == req.x && other.y == req.y && other.block.size == req.block.size){
                    selectRequests.remove(other);
                    selectRequests.add(req.copy());
                }
            }
        }
    }

    protected void flushRequests(Seq<BuildPlan> requests){
        for(BuildPlan req : requests){
            if(req.block != null && validPlace(req.x, req.y, req.block, req.rotation)){
                BuildPlan copy = req.copy();
                player.builder().addBuild(copy);
            }
        }
    }

    protected void drawOverRequest(BuildPlan request){
        boolean valid = validPlace(request.x, request.y, request.block, request.rotation);

        Draw.reset();
        Draw.mixcol(!valid ? Pal.breakInvalid : Color.white, (!valid ? 0.4f : 0.24f) + Mathf.absin(Time.globalTime(), 6f, 0.28f));
        Draw.alpha(1f);
        request.block.drawRequestConfigTop(request, selectRequests);
        Draw.reset();
    }

    protected void drawRequest(BuildPlan request){
        request.block.drawRequest(request, allRequests(), validPlace(request.x, request.y, request.block, request.rotation));

        if(request.block.saveConfig && request.block.lastConfig != null && !request.hasConfig){
            Object conf = request.config;
            request.config = request.block.lastConfig;
            request.block.drawRequestConfig(request, allRequests());
            request.config = conf;
        }
    }

    /** Draws a placement icon for a specific block. */
    protected void drawRequest(int x, int y, Block block, int rotation){
        brequest.set(x, y, rotation, block);
        brequest.animScale = 1f;
        block.drawRequest(brequest, allRequests(), validPlace(x, y, block, rotation));
    }

    /** Remove everything from the queue in a selection. */
    protected void removeSelection(int x1, int y1, int x2, int y2){
        removeSelection(x1, y1, x2, y2, false);
    }

    /** Remove everything from the queue in a selection. */
    protected void removeSelection(int x1, int y1, int x2, int y2, boolean flush){
        NormalizeResult result = Placement.normalizeArea(x1, y1, x2, y2, rotation, false, maxLength);
        for(int x = 0; x <= Math.abs(result.x2 - result.x); x++){
            for(int y = 0; y <= Math.abs(result.y2 - result.y); y++){
                int wx = x1 + x * Mathf.sign(x2 - x1);
                int wy = y1 + y * Mathf.sign(y2 - y1);

                Tile tile = world.tileBuilding(wx, wy);

                if(tile == null) continue;

                if(!flush){
                    tryBreakBlock(wx, wy);
                }else if(validBreak(tile.x, tile.y) && !selectRequests.contains(r -> r.tile() != null && r.tile() == tile)){
                    selectRequests.add(new BuildPlan(tile.x, tile.y));
                }
            }
        }

        //remove build requests
        Tmp.r1.set(result.x * tilesize, result.y * tilesize, (result.x2 - result.x) * tilesize, (result.y2 - result.y) * tilesize);

        Iterator<BuildPlan> it = player.builder().plans().iterator();
        while(it.hasNext()){
            BuildPlan req = it.next();
            if(!req.breaking && req.bounds(Tmp.r2).overlaps(Tmp.r1)){
                it.remove();
            }
        }

        it = selectRequests.iterator();
        while(it.hasNext()){
            BuildPlan req = it.next();
            if(!req.breaking && req.bounds(Tmp.r2).overlaps(Tmp.r1)){
                it.remove();
            }
        }

        //remove blocks to rebuild
        Iterator<BlockPlan> broken = state.teams.get(player.team()).blocks.iterator();
        while(broken.hasNext()){
            BlockPlan req = broken.next();
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
            BuildPlan req = new BuildPlan(l.x, l.y, l.rotation, block);
            req.animScale = 1f;
            lineRequests.add(req);
        });

        if(Core.settings.getBool("blockreplace")){
            lineRequests.each(req -> {
                Block replace = req.block.getReplacement(req, lineRequests);
                if(replace.unlockedNow()){
                    req.block = replace;
                }
            });
        }
    }

    protected void updateLine(int x1, int y1){
        updateLine(x1, y1, tileX(getMouseX()), tileY(getMouseY()));
    }

    /** Handles tile tap events that are not platform specific. */
    boolean tileTapped(@Nullable Building tile){
        if(tile == null){
            frag.inv.hide();
            frag.config.hideConfig();
            return false;
        }
        boolean consumed = false, showedInventory = false;

        //check if tapped block is configurable
        if(tile.block().configurable && tile.interactable(player.team())){
            consumed = true;
            if(((!frag.config.isShown() && tile.shouldShowConfigure(player)) //if the config fragment is hidden, show
            //alternatively, the current selected block can 'agree' to switch config tiles
            || (frag.config.isShown() && frag.config.getSelectedTile().onConfigureTileTapped(tile)))){
                Sounds.click.at(tile);
                frag.config.showConfig(tile);
            }
            //otherwise...
        }else if(!frag.config.hasConfigMouse()){ //make sure a configuration fragment isn't on the cursor
            //then, if it's shown and the current block 'agrees' to hide, hide it.
            if(frag.config.isShown() && frag.config.getSelectedTile().onConfigureTileTapped(tile)){
                consumed = true;
                frag.config.hideConfig();
            }

            if(frag.config.isShown()){
                consumed = true;
            }
        }

        //call tapped event
        if(!consumed && tile.interactable(player.team())){
            Call.tileTapped(player, tile);
        }

        //consume tap event if necessary
        if(tile.interactable(player.team()) && tile.block().consumesTap){
            consumed = true;
        }else if(tile.interactable(player.team()) && tile.block().synthetic() && !consumed){
            if(tile.block().hasItems && tile.items.total() > 0){
                frag.inv.showFor(tile);
                consumed = true;
                showedInventory = true;
            }
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
        return player.within(x, y, playerSelectRange) && player.unit().stack.amount > 0;
    }

    /** Tries to begin mining a tile, returns true if successful. */
    boolean tryBeginMine(Tile tile){
        if(canMine(tile)){
            //if a block is clicked twice, reset it
            player.miner().mineTile(player.miner().mineTile() == tile ? null : tile);
            return true;
        }
        return false;
    }

    boolean canMine(Tile tile){
        return !Core.scene.hasMouse()
        && tile.drop() != null && player.miner().canMine(tile.drop())
        && !(tile.floor().playerUnmineable && tile.overlay().itemDrop == null)
        && player.unit().acceptsItem(tile.drop())
        && tile.block() == Blocks.air && player.dst(tile.worldx(), tile.worldy()) <= miningRange;
    }

    /** Returns the tile at the specified MOUSE coordinates. */
    Tile tileAt(float x, float y){
        return world.tile(tileX(x), tileY(y));
    }

    int rawTileX(){
        return world.toTile(Core.input.mouseWorld().x);
    }

    int rawTileY(){
        return world.toTile(Core.input.mouseWorld().y);
    }

    int tileX(float cursorX){
        Vec2 vec = Core.input.mouseWorld(cursorX, 0);
        if(selectedBlock()){
            vec.sub(block.offset, block.offset);
        }
        return world.toTile(vec.x);
    }

    int tileY(float cursorY){
        Vec2 vec = Core.input.mouseWorld(0, cursorY);
        if(selectedBlock()){
            vec.sub(block.offset, block.offset);
        }
        return world.toTile(vec.y);
    }

    public boolean selectedBlock(){
        return isPlacing();
    }

    public boolean isPlacing(){
        return block != null;
    }

    public boolean isBreaking(){
        return false;
    }

    public float mouseAngle(float x, float y){
        return Core.input.mouseWorld(getMouseX(), getMouseY()).sub(x, y).angle();
    }

    public @Nullable Unit selectedUnit(){
        Unit unit = Units.closest(player.team(), Core.input.mouseWorld().x, Core.input.mouseWorld().y, 40f, u -> u.isAI() && !u.deactivated);
        if(unit != null){
            unit.hitbox(Tmp.r1);
            Tmp.r1.grow(6f);
            if(Tmp.r1.contains(Core.input.mouseWorld())){
                return unit;
            }
        }

        Building tile = world.buildWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
        if(tile instanceof ControlBlock && tile.team() == player.team()){
            return ((ControlBlock)tile).unit();
        }

        return null;
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
        Core.input.getInputProcessors().remove(i -> i instanceof InputHandler || (i instanceof GestureDetector && ((GestureDetector)i).getListener() instanceof InputHandler));
        Core.input.addProcessor(detector = new GestureDetector(20, 0.5f, 0.3f, 0.15f, this));
        Core.input.addProcessor(this);
        if(Core.scene != null){
            Table table = (Table)Core.scene.find("inputTable");
            if(table != null){
                table.clear();
                buildPlacementUI(table);
            }

            uiGroup = new WidgetGroup();
            uiGroup.touchable = Touchable.childrenOnly;
            uiGroup.setFillParent(true);
            ui.hudGroup.addChild(uiGroup);
            buildUI(uiGroup);

            frag.add();
        }
    }

    public boolean canShoot(){
        return block == null && !Core.scene.hasMouse() && !onConfigurable() && !isDroppingItem() && !(player.builder().updateBuilding() && player.builder().isBuilding());
    }

    public boolean onConfigurable(){
        return false;
    }

    public boolean isDroppingItem(){
        return droppingItem;
    }

    public void tryDropItems(@Nullable Building tile, float x, float y){
        if(!droppingItem || player.unit().stack.amount <= 0 || canTapPlayer(x, y) || state.isPaused() ){
            droppingItem = false;
            return;
        }

        droppingItem = false;

        ItemStack stack = player.unit().stack;

        if(tile != null && tile.acceptStack(stack.item, stack.amount, player.unit()) > 0 && tile.interactable(player.team()) && tile.block().hasItems && player.unit().stack().amount > 0 && tile.interactable(player.team())){
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

    public boolean validPlace(int x, int y, Block type, int rotation, BuildPlan ignore){
        for(BuildPlan req : player.builder().plans()){
            if(req != ignore
                    && !req.breaking
                    && req.block.bounds(req.x, req.y, Tmp.r1).overlaps(type.bounds(x, y, Tmp.r2))
                    && !(type.canReplace(req.block) && Tmp.r1.equals(Tmp.r2))){
                return false;
            }
        }
        return Build.validPlace(type, player.team(), x, y, rotation);
    }

    public boolean validBreak(int x, int y){
        return Build.validBreak(player.team(), x, y);
    }

    public void placeBlock(int x, int y, Block block, int rotation){
        BuildPlan req = getRequest(x, y);
        if(req != null){
            player.builder().plans().remove(req);
        }
        player.builder().addBuild(new BuildPlan(x, y, rotation, block));
    }

    public void breakBlock(int x, int y){
        Tile tile = world.tile(x, y);
        //TODO hacky
        if(tile != null && tile.build != null) tile = tile.build.tile();
        player.builder().addBuild(new BuildPlan(tile.x, tile.y));
    }

    public void drawArrow(Block block, int x, int y, int rotation){
        drawArrow(block, x, y, rotation, validPlace(x, y, block, rotation));
    }

    public void drawArrow(Block block, int x, int y, int rotation, boolean valid){
        float trns = (block.size / 2) * tilesize;
        int dx = Geometry.d4(rotation).x, dy = Geometry.d4(rotation).y;

        Draw.color(!valid ? Pal.removeBack : Pal.accentBack);
        Draw.rect(Core.atlas.find("place-arrow"),
        x * tilesize + block.offset + dx*trns,
        y * tilesize + block.offset - 1 + dy*trns,
        Core.atlas.find("place-arrow").getWidth() * Draw.scl,
        Core.atlas.find("place-arrow").getHeight() * Draw.scl, rotation * 90 - 90);

        Draw.color(!valid ? Pal.remove : Pal.accent);
        Draw.rect(Core.atlas.find("place-arrow"),
        x * tilesize + block.offset + dx*trns,
        y * tilesize + block.offset + dy*trns,
        Core.atlas.find("place-arrow").getWidth() * Draw.scl,
        Core.atlas.find("place-arrow").getHeight() * Draw.scl, rotation * 90 - 90);
    }

    void iterateLine(int startX, int startY, int endX, int endY, Cons<PlaceLine> cons){
        Seq<Point2> points;
        boolean diagonal = Core.input.keyDown(Binding.diagonal_placement);

        if(Core.settings.getBool("swapdiagonal") && mobile){
            diagonal = !diagonal;
        }

        if(block instanceof PowerNode){
            diagonal = !diagonal;
        }

        if(diagonal){
            points = Placement.pathfindLine(block != null && block.conveyorPlacement, startX, startY, endX, endY);
        }else{
            points = Placement.normalizeLine(startX, startY, endX, endY);
        }

        if(block instanceof PowerNode){
            Seq<Point2> skip = new Seq<>();
            
            for(int i = 1; i < points.size; i++){
                int overlaps = 0;
                Point2 point = points.get(i);

                //check with how many powernodes the *next* tile will overlap
                for(int j = 0; j < i; j++){
                    if(!skip.contains(points.get(j)) && ((PowerNode)block).overlaps(world.tile(point.x, point.y), world.tile(points.get(j).x, points.get(j).y))){
                        overlaps++;
                    }
                }

                //if it's more than one, it can bridge the gap
                if(overlaps > 1){
                    skip.add(points.get(i-1));
                }
            }
            //remove skipped points
            points.removeAll(skip);
        }

        float angle = Angles.angle(startX, startY, endX, endY);
        int baseRotation = rotation;
        if(!overrideLineRotation || diagonal){
            baseRotation = (startX == endX && startY == endY) ? rotation : ((int)((angle + 45) / 90f)) % 4;
        }

        Tmp.r3.set(-1, -1, 0, 0);

        for(int i = 0; i < points.size; i++){
            Point2 point = points.get(i);

            if(block != null && Tmp.r2.setSize(block.size * tilesize).setCenter(point.x * tilesize + block.offset, point.y * tilesize + block.offset).overlaps(Tmp.r3)){
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
            cons.get(line);

            Tmp.r3.setSize(block.size * tilesize).setCenter(point.x * tilesize + block.offset, point.y * tilesize + block.offset);
        }
    }

    static class PlaceLine{
        public int x, y, rotation;
        public boolean last;
    }

    //TODO implement all of this!
    /*
        protected void updateKeyboard(){
        Tile tile = world.tileWorld(x, y);
        boolean canMove = !Core.scene.hasKeyboard() || ui.minimapfrag.shown();

        isBoosting = Core.input.keyDown(Binding.dash) && !mech.flying;

        //if player is in solid block
        if(tile != null && tile.solid()){
            isBoosting = true;
        }

        float speed = isBoosting && !mech.flying ? mech.boostSpeed : mech.speed;

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
        }
    }

    protected void updateShooting(){
        if(!state.isEditor() && isShooting() && mech.canShoot(this)){
            weapons.update(this);
            //if(!mech.turnCursor){
                //shoot forward ignoring cursor
                //mech.weapon.update(this, x + Angles.trnsx(rotation, mech.weapon.targetDistance), y + Angles.trnsy(rotation, mech.weapon.targetDistance));
            //}else{
                //mech.weapon.update(this, pointerX, pointerY);
            //}
        }
    }

    protected void updateTouch(){
        if(Units.invalidateTarget(target, this) &&
            !(target instanceof Building && ((Building)target).damaged() && target.isValid() && target.team() == team && mech.canHeal && dst(target) < mech.range && !(((Building)target).block instanceof BuildBlock))){
            target = null;
        }

        if(state.isEditor()){
            target = null;
        }

        float targetX = Core.camera.position.x, targetY = Core.camera.position.y;
        float attractDst = 15f;
        float speed = isBoosting && !mech.flying ? mech.boostSpeed : mech.speed;

        if(moveTarget != null && !moveTarget.dead()){
            targetX = moveTarget.getX();
            targetY = moveTarget.getY();
            boolean tapping = moveTarget instanceof Building && moveTarget.team() == team;
            attractDst = 0f;

            if(tapping){
                velocity.setAngle(angleTo(moveTarget));
            }

            if(dst(moveTarget) <= 2f * Time.delta()){
                if(tapping && !dead()){
                    Tile tile = ((Building)moveTarget).tile;
                    tile.tapped(this);
                }

                moveTarget = null;
            }
        }else{
            moveTarget = null;
        }

        movement.set((targetX - x) / Time.delta(), (targetY - y) / Time.delta()).limit(speed);
        movement.setAngle(Mathf.slerp(movement.angle(), velocity.angle(), 0.05f));

        if(dst(targetX, targetY) < attractDst){
            movement.setZero();
        }

        float expansion = 3f;

        hitbox(rect);
        rect.x -= expansion;
        rect.y -= expansion;
        rect.width += expansion * 2f;
        rect.height += expansion * 2f;

        isBoosting = collisions.overlapsTile(rect) || dst(targetX, targetY) > 85f;

        velocity.add(movement.scl(Time.delta()));

        if(velocity.len() <= 0.2f && mech.flying){
            rotation += Mathf.sin(Time.time() + id * 99, 10f, 1f);
        }else if(target == null){
            rotation = Mathf.slerpDelta(rotation, velocity.angle(), velocity.len() / 10f);
        }

        float lx = x, ly = y;
        updateVelocityStatus();
        moved = dst(lx, ly) > 0.001f;

        if(mech.flying){
            //hovering effect
            x += Mathf.sin(Time.time() + id * 999, 25f, 0.08f);
            y += Mathf.cos(Time.time() + id * 999, 25f, 0.08f);
        }

        //update shooting if not building, not mining and there's ammo left
        if(!isBuilding() && mineTile() == null){

            //autofire
            if(target == null){
                isShooting = false;
                if(Core.settings.getBool("autotarget")){
                    target = Units.closestTarget(team, x, y, mech.range, u -> u.team() != Team.derelict, u -> u.team() != Team.derelict);

                    if(mech.canHeal && target == null){
                        target = Geometry.findClosest(x, y, indexer.getDamaged(Team.sharded));
                        if(target != null && dst(target) > mech.range){
                            target = null;
                        }else if(target != null){
                            target = ((Tile)target).entity;
                        }
                    }

                    if(target != null){
                        mineTile(null);
                    }
                }
            }else if(target.isValid() || (target instanceof Building && ((Building)target).damaged() && target.team() == team && mech.canHeal && dst(target) < mech.range)){
                //rotate toward and shoot the target
                if(mech.faceTarget){
                    rotation = Mathf.slerpDelta(rotation, angleTo(target), 0.2f);
                }

                Vec2 intercept = Predict.intercept(this, target, getWeapon().bullet.speed);

                pointerX = intercept.x;
                pointerY = intercept.y;

                updateShooting();
                isShooting = true;
            }

        }
    }
     */
}
