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
import arc.util.*;
import mindustry.ai.formations.patterns.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.*;
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
import mindustry.world.blocks.ConstructBlock.*;
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
    final static Rect r1 = new Rect(), r2 = new Rect();
    final static Seq<Point2> tmpPoints = new Seq<>(), tmpPoints2 = new Seq<>();

    public final OverlayFragment frag = new OverlayFragment();

    public Interval controlInterval = new Interval();
    public @Nullable Block block;
    public boolean overrideLineRotation;
    public int rotation;
    public boolean droppingItem;
    public Group uiGroup;
    public boolean isBuilding = true, buildWasAutoPaused = false, wasShooting = false;
    public @Nullable UnitType controlledType;

    public @Nullable Schematic lastSchematic;
    public GestureDetector detector;
    public PlaceLine line = new PlaceLine();
    public BuildPlan resultreq;
    public BuildPlan brequest = new BuildPlan();
    public Seq<BuildPlan> lineRequests = new Seq<>();
    public Seq<BuildPlan> selectRequests = new Seq<>();

    //methods to override

    @Remote(called = Loc.server, unreliable = true)
    public static void transferItemEffect(Item item, float x, float y, Itemsc to){
        if(to == null) return;
        createItemTransfer(item, 1, x, y, to, null);
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void takeItems(Building build, Item item, int amount, Unit to){
        if(to == null || build == null) return;

        int removed = build.removeStack(item, Math.min(to.maxAccepted(item), amount));
        if(removed == 0) return;

        to.addItem(item, removed);
        for(int j = 0; j < Mathf.clamp(removed / 3, 1, 8); j++){
            Time.run(j * 3f, () -> transferItemEffect(item, build.x, build.y, to));
        }
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void transferItemToUnit(Item item, float x, float y, Itemsc to){
        if(to == null) return;
        createItemTransfer(item, 1, x, y, to, () -> to.addItem(item));
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void setItem(Building build, Item item, int amount){
        if(build == null || build.items == null) return;
        build.items.set(item, amount);
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void transferItemTo(@Nullable Unit unit, Item item, int amount, float x, float y, Building build){
        if(build == null || build.items == null) return;

        if(unit != null && unit.item() == item) unit.stack.amount = Math.max(unit.stack.amount - amount, 0);

        for(int i = 0; i < Mathf.clamp(amount / 3, 1, 8); i++){
            Time.run(i * 3, () -> createItemTransfer(item, amount, x, y, build, () -> {}));
        }
        build.handleStack(item, amount, unit);
    }

    public static void createItemTransfer(Item item, int amount, float x, float y, Position to, Runnable done){
        Fx.itemTransfer.at(x, y, amount, item.color, to);
        if(done != null){
            Time.run(Fx.itemTransfer.lifetime, done);
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void requestItem(Player player, Building tile, Item item, int amount){
        if(player == null || tile == null || !tile.interactable(player.team()) || !player.within(tile, buildingRange) || player.dead()) return;

        if(net.server() && (!Units.canInteract(player, tile) ||
        !netServer.admins.allowAction(player, ActionType.withdrawItem, tile.tile(), action -> {
            action.item = item;
            action.itemAmount = amount;
        }))){
            throw new ValidateException(player, "Player cannot request items.");
        }

        //remove item for every controlling unit
        player.unit().eachGroup(unit -> {
            Call.takeItems(tile, item, unit.maxAccepted(item), unit);

            if(unit == player.unit()){
                Events.fire(new WithdrawEvent(tile, player, item, amount));
            }
        });
    }

    @Remote(targets = Loc.both, forward = true, called = Loc.server)
    public static void transferInventory(Player player, Building tile){
        if(player == null || tile == null || !player.within(tile, buildingRange) || tile.items == null || player.dead()) return;

        if(net.server() && (player.unit().stack.amount <= 0 || !Units.canInteract(player, tile) ||
        !netServer.admins.allowAction(player, ActionType.depositItem, tile.tile, action -> {
            action.itemAmount = player.unit().stack.amount;
            action.item = player.unit().item();
        }))){
            throw new ValidateException(player, "Player cannot transfer an item.");
        }

        //deposit for every controlling unit
        player.unit().eachGroup(unit -> {
            Item item = unit.item();
            int accepted = tile.acceptStack(item, unit.stack.amount, unit);

            Call.transferItemTo(unit, item, accepted, unit.x, unit.y, tile);

            if(unit == player.unit()){
                Events.fire(new DepositEvent(tile, player, item, accepted));
            }
        });
    }

    @Remote(variants = Variant.one)
    public static void removeQueueBlock(int x, int y, boolean breaking){
        player.unit().removeBuild(x, y, breaking);
    }

    @Remote(targets = Loc.both, called = Loc.server)
    public static void requestUnitPayload(Player player, Unit target){
        if(player == null) return;

        Unit unit = player.unit();
        Payloadc pay = (Payloadc)unit;

        if(target.isAI() && target.isGrounded() && pay.canPickup(target)
        && target.within(unit, unit.type.hitSize * 2f + target.type.hitSize * 2f)){
            Call.pickedUnitPayload(unit, target);
        }
    }

    @Remote(targets = Loc.both, called = Loc.server)
    public static void requestBuildPayload(Player player, Building tile){
        if(player == null) return;

        Unit unit = player.unit();
        Payloadc pay = (Payloadc)unit;

        if(tile != null && tile.team == unit.team
        && unit.within(tile, tilesize * tile.block.size * 1.2f + tilesize * 5f)){
            //pick up block directly
            if(tile.block.buildVisibility != BuildVisibility.hidden && tile.canPickup() && pay.canPickup(tile)){
                Call.pickedBuildPayload(unit, tile, true);
            }else{ //pick up block payload
                Payload current = tile.getPayload();
                if(current != null && pay.canPickupPayload(current)){
                    Call.pickedBuildPayload(unit, tile, false);
                }
            }
        }
    }

    @Remote(targets = Loc.server, called = Loc.server)
    public static void pickedUnitPayload(Unit unit, Unit target){
        if(target != null && unit instanceof Payloadc pay){
            pay.pickup(target);
        }else if(target != null){
            target.remove();
        }
    }

    @Remote(targets = Loc.server, called = Loc.server)
    public static void pickedBuildPayload(Unit unit, Building tile, boolean onGround){
        if(tile != null && unit instanceof Payloadc pay){
            if(onGround){
                if(tile.block.buildVisibility != BuildVisibility.hidden && tile.canPickup() && pay.canPickup(tile)){
                    pay.pickup(tile);
                }else{
                    Fx.unitPickup.at(tile);
                    tile.tile.remove();
                }
            }else{
                Payload current = tile.getPayload();
                if(current != null && pay.canPickupPayload(current)){
                    Payload taken = tile.takePayload();
                    if(taken != null){
                        pay.addPayload(taken);
                        Fx.unitPickup.at(tile);
                    }
                }
            }

        }else if(tile != null && onGround){
            Fx.unitPickup.at(tile);
            tile.tile.remove();
        }
    }

    @Remote(targets = Loc.both, called = Loc.server)
    public static void requestDropPayload(Player player, float x, float y){
        if(player == null || net.client()) return;

        Payloadc pay = (Payloadc)player.unit();

        //apply margin of error
        Tmp.v1.set(x, y).sub(pay).limit(tilesize * 4f).add(pay);
        float cx = Tmp.v1.x, cy = Tmp.v1.y;

        Call.payloadDropped(player.unit(), cx, cy);
    }

    @Remote(called = Loc.server, targets = Loc.server)
    public static void payloadDropped(Unit unit, float x, float y){
        if(unit instanceof Payloadc pay){
            float prevx = pay.x(), prevy = pay.y();
            pay.set(x, y);
            pay.dropLastPayload();
            pay.set(prevx, prevy);
            pay.controlling().each(u -> {
                if(u instanceof Payloadc){
                    Call.payloadDropped(u, u.x, u.y);
                }
            });
        }
    }

    @Remote(targets = Loc.client, called = Loc.server)
    public static void dropItem(Player player, float angle){
        if(player == null) return;

        if(net.server() && player.unit().stack.amount <= 0){
            throw new ValidateException(player, "Player cannot drop an item.");
        }

        Fx.dropItem.at(player.x, player.y, angle, Color.white, player.unit().item());
        player.unit().clearItem();
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true, unreliable = true)
    public static void rotateBlock(@Nullable Player player, Building tile, boolean direction){
        if(tile == null) return;

        if(net.server() && (!Units.canInteract(player, tile) ||
            !netServer.admins.allowAction(player, ActionType.rotate, tile.tile(), action -> action.rotation = Mathf.mod(tile.rotation + Mathf.sign(direction), 4)))){
            throw new ValidateException(player, "Player cannot rotate a block.");
        }

        if(player != null) tile.lastAccessed = player.name;
        tile.rotation = Mathf.mod(tile.rotation + Mathf.sign(direction), 4);
        tile.updateProximity();
        tile.noSleep();
    }

    @Remote(targets = Loc.both, called = Loc.both, forward = true)
    public static void tileConfig(@Nullable Player player, Building tile, @Nullable Object value){
        if(tile == null) return;
        if(net.server() && (!Units.canInteract(player, tile) ||
            !netServer.admins.allowAction(player, ActionType.configure, tile.tile, action -> action.config = value))) throw new ValidateException(player, "Player cannot configure a tile.");
        tile.configured(player == null || player.dead() ? null : player.unit(), value);
        Core.app.post(() -> Events.fire(new ConfigEvent(tile, player, value)));
    }

    //only useful for servers or local mods, and is not replicated across clients
    //uses unreliable packets due to high frequency
    @Remote(targets = Loc.both, called = Loc.both, unreliable = true)
    public static void tileTap(@Nullable Player player, Tile tile){
        if(tile == null) return;

        Events.fire(new TapEvent(player, tile));
    }

    @Remote(targets = Loc.both, called = Loc.both, forward = true)
    public static void unitControl(Player player, @Nullable Unit unit){
        if(player == null) return;

        //make sure player is allowed to control the unit
        if(net.server() && !netServer.admins.allowAction(player, ActionType.control, action -> action.unit = unit)){
            throw new ValidateException(player, "Player cannot control a unit.");
        }

        //clear player unit when they possess a core
        if(unit instanceof BlockUnitc block && block.tile() instanceof CoreBuild build){
            Fx.spawn.at(player);
            if(net.client()){
                control.input.controlledType = null;
            }

            player.clearUnit();
            player.deathTimer = 61f;
            build.requestSpawn(player);
        }else if(unit == null){ //just clear the unit (is this used?)
            player.clearUnit();
            //make sure it's AI controlled, so players can't overwrite each other
        }else if(unit.isAI() && unit.team == player.team() && !unit.dead){
            if(!net.client()){
                player.unit(unit);
            }

            Time.run(Fx.unitSpirit.lifetime, () -> Fx.unitControl.at(unit.x, unit.y, 0f, unit));
            if(!player.dead()){
                Fx.unitSpirit.at(player.x, player.y, 0f, unit);
            }
        }

        Events.fire(new UnitControlEvent(player, unit));
    }

    @Remote(targets = Loc.both, called = Loc.both, forward = true)
    public static void unitClear(Player player){
        //no free core teleports?
        if(player == null || !player.dead() && player.unit().spawnedByCore) return;

        Fx.spawn.at(player);
        player.clearUnit();
        player.deathTimer = 61f; //for instant respawn
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true)
    public static void unitCommand(Player player){
        if(player == null || player.dead() || !(player.unit() instanceof Commanderc commander)) return;

        //make sure player is allowed to make the command
        if(net.server() && !netServer.admins.allowAction(player, ActionType.command, action -> {})){
            throw new ValidateException(player, "Player cannot command a unit.");
        }

        if(commander.isCommanding()){
            commander.clearCommand();
        }else if(player.unit().type.commandLimit > 0){

            //TODO try out some other formations
            commander.commandNearby(new CircleFormation());
            Fx.commandSend.at(player);
        }

    }

    public Eachable<BuildPlan> allRequests(){
        return cons -> {
            for(BuildPlan request : player.unit().plans()) cons.get(request);
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
            player.unit().updateBuilding(isBuilding);
        }

        if(player.shooting && !wasShooting && player.unit().hasWeapons() && state.rules.unitAmmo && player.unit().ammo <= 0){
            player.unit().type.weapons.first().noAmmoSound.at(player.unit());
        }

        wasShooting = player.shooting;

        if(!player.dead()){
            controlledType = player.unit().type;
        }

        if(controlledType != null && player.dead()){
            Unit unit = Units.closest(player.team(), player.x, player.y, u -> !u.isPlayer() && u.type == controlledType && !u.dead);

            if(unit != null){
                //only trying controlling once a second to prevent packet spam
                if(!net.client() || controlInterval.get(0, 70f)){
                    Call.unitControl(player, unit);
                }
            }
        }
    }

    public void checkUnit(){
        if(controlledType != null){
            Unit unit = Units.closest(player.team(), player.x, player.y, u -> !u.isPlayer() && u.type == controlledType && !u.dead);
            if(unit == null && controlledType == UnitTypes.block){
                unit = world.buildWorld(player.x, player.y) instanceof ControlBlock cont && cont.canControl() ? cont.unit() : null;
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

    public void tryPickupPayload(){
        Unit unit = player.unit();
        if(!(unit instanceof Payloadc pay)) return;

        Unit target = Units.closest(player.team(), pay.x(), pay.y(), unit.type.hitSize * 2.5f, u -> u.isAI() && u.isGrounded() && pay.canPickup(u) && u.within(unit, u.hitSize + unit.hitSize * 1.2f));
        if(target != null){
            Call.requestUnitPayload(player, target);
        }else{
            Building tile = world.buildWorld(pay.x(), pay.y());

            if(tile != null && tile.team == unit.team){
                Call.requestBuildPayload(player, tile);
            }
        }
    }

    public void tryDropPayload(){
        Unit unit = player.unit();
        if(!(unit instanceof Payloadc)) return;

        Call.requestDropPayload(player, player.x, player.y);
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
        if(state.isMenu()){
            controlledType = null;
        }
    }

    public void drawBottom(){

    }

    public void drawTop(){

    }

    public void drawOverSelect(){

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
        return tile != null && tile.block() instanceof ConstructBlock && tile.<ConstructBuild>bc().cblock == request.block;
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

        ui.showTextInput("@schematic.add", "@name", "", text -> {
            Schematic replacement = schematics.all().find(s -> s.name().equals(text));
            if(replacement != null){
                ui.showConfirm("@confirm", "@schematic.replace", () -> {
                    schematics.overwrite(replacement, lastSchematic);
                    ui.showInfoFade("@schematic.saved");
                    ui.schematics.showInfo(replacement);
                });
            }else{
                lastSchematic.tags.put("name", text);
                lastSchematic.tags.put("description", "");
                schematics.add(lastSchematic);
                ui.showInfoFade("@schematic.saved");
                ui.schematics.showInfo(lastSchematic);
                Events.fire(new SchematicCreateEvent(lastSchematic));
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
            req.x = World.toTile(wx - req.block.offset) + ox;
            req.y = World.toTile(wy - req.block.offset) + oy;
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

        for(BuildPlan req : player.unit().plans()){
            if(test.get(req)) return req;
        }

        return selectRequests.find(test);
    }

    protected void drawBreakSelection(int x1, int y1, int x2, int y2, int maxLength){
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

        for(BuildPlan req : player.unit().plans()){
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

    protected void drawBreakSelection(int x1, int y1, int x2, int y2){
        drawBreakSelection(x1, y1, x2, y2, maxLength);
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
                player.unit().addBuild(copy);
            }
        }
    }

    protected void drawOverRequest(BuildPlan request){
        boolean valid = validPlace(request.x, request.y, request.block, request.rotation);

        Draw.reset();
        Draw.mixcol(!valid ? Pal.breakInvalid : Color.white, (!valid ? 0.4f : 0.24f) + Mathf.absin(Time.globalTime, 6f, 0.28f));
        Draw.alpha(1f);
        request.block.drawRequestConfigTop(request, selectRequests);
        Draw.reset();
    }

    protected void drawRequest(BuildPlan request){
        request.block.drawRequest(request, allRequests(), validPlace(request.x, request.y, request.block, request.rotation));
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
    protected void removeSelection(int x1, int y1, int x2, int y2, int maxLength){
        removeSelection(x1, y1, x2, y2, false, maxLength);
    }

    /** Remove everything from the queue in a selection. */
    protected void removeSelection(int x1, int y1, int x2, int y2, boolean flush){
        removeSelection(x1, y1, x2, y2, flush, maxLength);
    }

    /** Remove everything from the queue in a selection. */
    protected void removeSelection(int x1, int y1, int x2, int y2, boolean flush, int maxLength){
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

        Iterator<BuildPlan> it = player.unit().plans().iterator();
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
            BuildPlan req = new BuildPlan(l.x, l.y, l.rotation, block, block.nextConfig());
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
        if(tile.block.configurable && tile.interactable(player.team())){
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
            tile.tapped();
        }

        //consume tap event if necessary
        if(tile.interactable(player.team()) && tile.block.consumesTap){
            consumed = true;
        }else if(tile.interactable(player.team()) && tile.block.synthetic() && !consumed){
            if(tile.block.hasItems && tile.items.total() > 0){
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
            player.unit().mineTile = player.unit().mineTile == tile ? null : tile;
            return true;
        }
        return false;
    }

    boolean canMine(Tile tile){
        return !Core.scene.hasMouse()
            && tile.drop() != null
            && player.unit().validMine(tile)
            && !(tile.floor().playerUnmineable && tile.overlay().itemDrop == null)
            && player.unit().acceptsItem(tile.drop())
            && tile.block() == Blocks.air;
    }

    /** Returns the tile at the specified MOUSE coordinates. */
    Tile tileAt(float x, float y){
        return world.tile(tileX(x), tileY(y));
    }

    int rawTileX(){
        return World.toTile(Core.input.mouseWorld().x);
    }

    int rawTileY(){
        return World.toTile(Core.input.mouseWorld().y);
    }

    int tileX(float cursorX){
        Vec2 vec = Core.input.mouseWorld(cursorX, 0);
        if(selectedBlock()){
            vec.sub(block.offset, block.offset);
        }
        return World.toTile(vec.x);
    }

    int tileY(float cursorY){
        Vec2 vec = Core.input.mouseWorld(0, cursorY);
        if(selectedBlock()){
            vec.sub(block.offset, block.offset);
        }
        return World.toTile(vec.y);
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
        Unit unit = Units.closest(player.team(), Core.input.mouseWorld().x, Core.input.mouseWorld().y, 40f, Unitc::isAI);
        if(unit != null){
            unit.hitbox(Tmp.r1);
            Tmp.r1.grow(6f);
            if(Tmp.r1.contains(Core.input.mouseWorld())){
                return unit;
            }
        }

        Building tile = world.buildWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
        if(tile instanceof ControlBlock cont && cont.canControl() && tile.team == player.team()){
            return cont.unit();
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
            uiGroup.toBack();
            buildUI(uiGroup);

            frag.add();
        }
    }

    public boolean canShoot(){
        return block == null && !onConfigurable() && !isDroppingItem() && !player.unit().activelyBuilding() &&
            !(player.unit() instanceof Mechc && player.unit().isFlying());
    }

    public boolean onConfigurable(){
        return false;
    }

    public boolean isDroppingItem(){
        return droppingItem;
    }

    public boolean canDropItem(){
        return droppingItem && !canTapPlayer(Core.input.mouseWorldX(), Core.input.mouseWorldY());
    }

    public void tryDropItems(@Nullable Building tile, float x, float y){
        if(!droppingItem || player.unit().stack.amount <= 0 || canTapPlayer(x, y) || state.isPaused() ){
            droppingItem = false;
            return;
        }

        droppingItem = false;

        ItemStack stack = player.unit().stack;

        if(tile != null && tile.acceptStack(stack.item, stack.amount, player.unit()) > 0 && tile.interactable(player.team()) && tile.block.hasItems && player.unit().stack().amount > 0 && tile.interactable(player.team())){
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
        for(BuildPlan req : player.unit().plans()){
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
            player.unit().plans().remove(req);
        }
        player.unit().addBuild(new BuildPlan(x, y, rotation, block, block.nextConfig()));
    }

    public void breakBlock(int x, int y){
        Tile tile = world.tile(x, y);
        if(tile != null && tile.build != null) tile = tile.build.tile;
        player.unit().addBuild(new BuildPlan(tile.x, tile.y));
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
        Core.atlas.find("place-arrow").width * Draw.scl,
        Core.atlas.find("place-arrow").height * Draw.scl, rotation * 90 - 90);

        Draw.color(!valid ? Pal.remove : Pal.accent);
        Draw.rect(Core.atlas.find("place-arrow"),
        x * tilesize + block.offset + dx*trns,
        y * tilesize + block.offset + dy*trns,
        Core.atlas.find("place-arrow").width * Draw.scl,
        Core.atlas.find("place-arrow").height * Draw.scl, rotation * 90 - 90);
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

        if(block instanceof PowerNode node){
            var base = tmpPoints2;
            var result = tmpPoints.clear();

            base.selectFrom(points, p -> p == points.first() || p == points.peek() || Build.validPlace(block, player.team(), p.x, p.y, rotation, false));
            boolean addedLast = false;

            outer:
            for(int i = 0; i < base.size;){
                var point = base.get(i);
                result.add(point);
                if(i == base.size - 1) addedLast = true;

                //find the furthest node that overlaps this one
                for(int j = base.size - 1; j > i; j--){
                    var other = base.get(j);
                    boolean over = node.overlaps(world.tile(point.x, point.y), world.tile(other.x, other.y));

                    if(over){
                        //add node to list and start searching for node that overlaps the next one
                        i = j;
                        continue outer;
                    }
                }

                //if it got here, that means nothing was found. try to proceed to the next node anyway
                i ++;
            }

            if(!addedLast) result.add(base.peek());

            points.clear();
            points.addAll(result);
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
                if(next != null){
                    line.rotation = Tile.relativeTo(point.x, point.y, next.x, next.y);
                }else if(block.conveyorPlacement && i > 0){
                    Point2 prev = points.get(i - 1);
                    line.rotation = Tile.relativeTo(prev.x, prev.y, point.x, point.y);
                }else{
                    line.rotation = baseRotation;
                }
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
}
