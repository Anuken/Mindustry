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
import mindustry.*;
import mindustry.ai.types.*;
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
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.meta.*;

import java.util.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public abstract class InputHandler implements InputProcessor, GestureListener{
    /** Used for dropping items. */
    final static float playerSelectRange = mobile ? 17f : 11f;
    final static IntSeq removed = new IntSeq();
    /** Maximum line length. */
    final static int maxLength = 100;
    final static Rect r1 = new Rect(), r2 = new Rect();
    final static Seq<Unit> tmpUnits = new Seq<>(false);

    /** If true, there is a cutscene currently occurring in logic. */
    public boolean logicCutscene;
    public Vec2 logicCamPan = new Vec2();
    public float logicCamSpeed = 0.1f;
    public float logicCutsceneZoom = -1f;

    /** If any of these functions return true, input is locked. */
    public Seq<Boolp> inputLocks = Seq.with(() -> renderer.isCutscene(), () -> logicCutscene);
    public Interval controlInterval = new Interval();
    public @Nullable Block block;
    public boolean overrideLineRotation;
    public int rotation;
    public boolean droppingItem;
    public Group uiGroup;
    public boolean isBuilding = true, buildWasAutoPaused = false, wasShooting = false;
    public @Nullable UnitType controlledType;
    public float recentRespawnTimer;

    public @Nullable Schematic lastSchematic;
    public GestureDetector detector;
    public PlaceLine line = new PlaceLine();
    public BuildPlan resultplan;
    public BuildPlan bplan = new BuildPlan();
    public Seq<BuildPlan> linePlans = new Seq<>();
    public Seq<BuildPlan> selectPlans = new Seq<>(BuildPlan.class);

    //for RTS controls
    public Seq<Unit> selectedUnits = new Seq<>();
    public @Nullable Building commandBuild;
    public boolean commandMode = false;
    public boolean commandRect = false;
    public boolean tappedOne = false;
    public float commandRectX, commandRectY;

    private Seq<BuildPlan> plansOut = new Seq<>(BuildPlan.class);
    private QuadTree<BuildPlan> playerPlanTree = new QuadTree<>(new Rect());

    public final BlockInventoryFragment inv;
    public final BlockConfigFragment config;

    private WidgetGroup group = new WidgetGroup();

    private final Eachable<BuildPlan> allPlans = cons -> {
        player.unit().plans().each(cons);
        selectPlans.each(cons);
        linePlans.each(cons);
    };

    private final Eachable<BuildPlan> allSelectLines = cons -> {
        selectPlans.each(cons);
        linePlans.each(cons);
    };

    public InputHandler(){
        group.touchable = Touchable.childrenOnly;
        inv = new BlockInventoryFragment();
        config = new BlockConfigFragment();

        Events.on(UnitDestroyEvent.class, e -> {
            if(e.unit != null && e.unit.isPlayer() && e.unit.getPlayer().isLocal() && e.unit.type.weapons.contains(w -> w.bullet.killShooter)){
                player.shooting = false;
            }
        });

        Events.on(WorldLoadEvent.class, e -> {
            playerPlanTree = new QuadTree<>(new Rect(0f, 0f, world.unitWidth(), world.unitHeight()));
        });

        Events.on(ResetEvent.class, e -> {
            logicCutscene = false;
        });
    }

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
    public static void clearItems(Building build){
        if(build == null || build.items == null) return;
        build.items.clear();
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void transferItemTo(@Nullable Unit unit, Item item, int amount, float x, float y, Building build){
        if(build == null || build.items == null) return;

        if(unit != null && unit.item() == item) unit.stack.amount = Math.max(unit.stack.amount - amount, 0);

        for(int i = 0; i < Mathf.clamp(amount / 3, 1, 8); i++){
            Time.run(i * 3, () -> createItemTransfer(item, amount, x, y, build, () -> {}));
        }
        if(amount > 0){
            build.handleStack(item, amount, unit);
        }
    }

    @Remote(called = Loc.both, targets = Loc.both, forward = true, unreliable = true)
    public static void deletePlans(Player player, int[] positions){
        if(net.server() && !netServer.admins.allowAction(player, ActionType.removePlanned, a -> a.plans = positions)){
            throw new ValidateException(player, "Player cannot remove plans.");
        }

        if(player == null) return;

        var it = player.team().data().plans.iterator();
        //O(n^2) search here; no way around it
        outer:
        while(it.hasNext()){
            var plan = it.next();

            for(int pos : positions){
                if(plan.x == Point2.x(pos) && plan.y == Point2.y(pos)){
                    plan.removed = true;
                    it.remove();
                    continue outer;
                }
            }
        }
    }

    public static void createItemTransfer(Item item, int amount, float x, float y, Position to, Runnable done){
        Fx.itemTransfer.at(x, y, amount, item.color, to);
        if(done != null){
            Time.run(Fx.itemTransfer.lifetime, done);
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandUnits(Player player, int[] unitIds, @Nullable Building buildTarget, @Nullable Unit unitTarget, @Nullable Vec2 posTarget){
        if(player == null || unitIds == null) return;

        //why did I ever think this was a good idea
        if(unitTarget != null && unitTarget.isNull()) unitTarget = null;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        Teamc teamTarget = buildTarget == null ? unitTarget : buildTarget;

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit != null && unit.team == player.team() && unit.controller() instanceof CommandAI ai){
                if(teamTarget != null && teamTarget.team() != player.team()){
                    ai.commandTarget(teamTarget);
                }else if(posTarget != null){
                    ai.commandPosition(posTarget);
                }
                unit.lastCommanded = player.coloredName();
            }
        }

        if(unitIds.length > 0 && player == Vars.player && !state.isPaused()){
            if(teamTarget != null){
                Fx.attackCommand.at(teamTarget);
            }else{
                Fx.moveCommand.at(posTarget);
            }
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandBuilding(Player player, Building build, Vec2 target){
        if(player == null || build == null || build.team != player.team() || !build.block.commandable || target == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandBuilding, event -> {
            event.tile = build.tile;
        })){
            throw new ValidateException(player, "Player cannot command building.");
        }

        build.onCommand(target);
        if(!state.isPaused() && player == Vars.player){
            Fx.moveCommand.at(target);
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void requestItem(Player player, Building build, Item item, int amount){
        if(player == null || build == null || !build.interactable(player.team()) || !player.within(build, itemTransferRange) || player.dead()) return;

        if(net.server() && (!Units.canInteract(player, build) ||
        !netServer.admins.allowAction(player, ActionType.withdrawItem, build.tile(), action -> {
            action.item = item;
            action.itemAmount = amount;
        }))){
            throw new ValidateException(player, "Player cannot request items.");
        }

        Call.takeItems(build, item, Math.min(player.unit().maxAccepted(item), amount), player.unit());
        Events.fire(new WithdrawEvent(build, player, item, amount));
    }

    @Remote(targets = Loc.both, forward = true, called = Loc.server)
    public static void transferInventory(Player player, Building build){
        if(player == null || build == null || !player.within(build, itemTransferRange) || build.items == null || player.dead() || (state.rules.onlyDepositCore && !(build instanceof CoreBuild))) return;

        if(net.server() && (player.unit().stack.amount <= 0 || !Units.canInteract(player, build) ||
        !netServer.admins.allowAction(player, ActionType.depositItem, build.tile, action -> {
            action.itemAmount = player.unit().stack.amount;
            action.item = player.unit().item();
        }))){
            throw new ValidateException(player, "Player cannot transfer an item.");
        }

        var unit = player.unit();
        Item item = unit.item();
        int accepted = build.acceptStack(item, unit.stack.amount, unit);

        Call.transferItemTo(unit, item, accepted, unit.x, unit.y, build);

        Events.fire(new DepositEvent(build, player, item, accepted));
    }

    @Remote(variants = Variant.one)
    public static void removeQueueBlock(int x, int y, boolean breaking){
        player.unit().removeBuild(x, y, breaking);
    }

    @Remote(targets = Loc.both, called = Loc.server)
    public static void requestUnitPayload(Player player, Unit target){
        if(player == null || !(player.unit() instanceof Payloadc pay)) return;

        Unit unit = player.unit();

        if(target.isAI() && target.isGrounded() && pay.canPickup(target)
        && target.within(unit, unit.type.hitSize * 2f + target.type.hitSize * 2f)){
            Call.pickedUnitPayload(unit, target);
        }
    }

    @Remote(targets = Loc.both, called = Loc.server)
    public static void requestBuildPayload(Player player, Building build){
        if(player == null || !(player.unit() instanceof Payloadc pay)) return;

        Unit unit = player.unit();

        if(build != null && state.teams.canInteract(unit.team, build.team)
        && unit.within(build, tilesize * build.block.size * 1.2f + tilesize * 5f)){

            //pick up block's payload
            Payload current = build.getPayload();
            if(current != null && pay.canPickupPayload(current)){
                Call.pickedBuildPayload(unit, build, false);
                //pick up whole building directly
            }else if(build.block.buildVisibility != BuildVisibility.hidden && build.canPickup() && pay.canPickup(build)){
                Call.pickedBuildPayload(unit, build, true);
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
    public static void pickedBuildPayload(Unit unit, Building build, boolean onGround){
        if(build != null && unit instanceof Payloadc pay){
            if(onGround){
                if(build.block.buildVisibility != BuildVisibility.hidden && build.canPickup() && pay.canPickup(build)){
                    pay.pickup(build);
                }else{
                    Fx.unitPickup.at(build);
                    build.tile.remove();
                }
            }else{
                Payload current = build.getPayload();
                if(current != null && pay.canPickupPayload(current)){
                    Payload taken = build.takePayload();
                    if(taken != null){
                        pay.addPayload(taken);
                        Fx.unitPickup.at(build);
                    }
                }
            }

        }else if(build != null && onGround){
            Fx.unitPickup.at(build);
            build.tile.remove();
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
        }
    }

    @Remote(targets = Loc.client, called = Loc.server)
    public static void dropItem(Player player, float angle){
        if(player == null) return;

        if(net.server() && player.unit().stack.amount <= 0){
            throw new ValidateException(player, "Player cannot drop an item.");
        }

        var unit = player.unit();
        Fx.dropItem.at(unit.x, unit.y, angle, Color.white, unit.item());
        unit.clearItem();
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true, unreliable = true)
    public static void rotateBlock(@Nullable Player player, Building build, boolean direction){
        if(build == null) return;

        if(net.server() && (!Units.canInteract(player, build) ||
            !netServer.admins.allowAction(player, ActionType.rotate, build.tile(), action -> action.rotation = Mathf.mod(build.rotation + Mathf.sign(direction), 4)))){
            throw new ValidateException(player, "Player cannot rotate a block.");
        }

        if(player != null) build.lastAccessed = player.name;
        build.rotation = Mathf.mod(build.rotation + Mathf.sign(direction), 4);
        build.updateProximity();
        build.noSleep();
        Fx.rotateBlock.at(build.x, build.y, build.block.size);
    }

    @Remote(targets = Loc.both, called = Loc.both, forward = true)
    public static void tileConfig(@Nullable Player player, Building build, @Nullable Object value){
        if(build == null) return;
        if(net.server() && (!Units.canInteract(player, build) ||
            !netServer.admins.allowAction(player, ActionType.configure, build.tile, action -> action.config = value))){
            var packet = new TileConfigCallPacket(); //undo the config on the client
            packet.player = player;
            packet.build = build;
            packet.value = build.config();
            player.con.send(packet, true);
            throw new ValidateException(player, "Player cannot configure a tile.");
        }
        build.configured(player == null || player.dead() ? null : player.unit(), value);
        Core.app.post(() -> Events.fire(new ConfigEvent(build, player, value)));
    }

    //only useful for servers or local mods, and is not replicated across clients
    //uses unreliable packets due to high frequency
    @Remote(targets = Loc.both, called = Loc.both, unreliable = true)
    public static void tileTap(@Nullable Player player, Tile tile){
        if(tile == null) return;

        Events.fire(new TapEvent(player, tile));
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true)
    public static void buildingControlSelect(Player player, Building build){
        if(player == null || build == null || player.dead()) return;

        //make sure player is allowed to control the building
        if(net.server() && !netServer.admins.allowAction(player, ActionType.buildSelect, action -> action.tile = build.tile)){
            throw new ValidateException(player, "Player cannot control a building.");
        }

        if(player.team() == build.team && build.canControlSelect(player.unit())){
            build.onControlSelect(player.unit());
        }
    }

    @Remote(called = Loc.server)
    public static void unitBuildingControlSelect(Unit unit, Building build){
        if(unit == null || unit.dead()) return;

        //client skips checks to prevent ghost units
        if(unit.team() == build.team && (net.client() || build.canControlSelect(unit))){
            build.onControlSelect(unit);
        }
    }

    @Remote(targets = Loc.both, called = Loc.both, forward = true)
    public static void unitControl(Player player, @Nullable Unit unit){
        if(player == null) return;

        //make sure player is allowed to control the unit
        if(net.server() && (!state.rules.possessionAllowed || !netServer.admins.allowAction(player, ActionType.control, action -> action.unit = unit))){
            throw new ValidateException(player, "Player cannot control a unit.");
        }

        //clear player unit when they possess a core
        if(unit == null){ //just clear the unit (is this used?)
            player.clearUnit();
            //make sure it's AI controlled, so players can't overwrite each other
        }else if(unit.isAI() && unit.team == player.team() && !unit.dead && unit.type.playerControllable){
            if(net.client() && player.isLocal()){
                player.justSwitchFrom = player.unit();
                player.justSwitchTo = unit;
            }

            //TODO range check for docking?
            var before = player.unit();

            player.unit(unit);

            if(before != null && !before.isNull()){
                if(before.spawnedByCore){
                    unit.dockedType = before.type;
                }else if(before.dockedType != null && before.dockedType.coreUnitDock){
                    //direct dock transfer???
                    unit.dockedType = before.dockedType;
                }
            }

            Time.run(Fx.unitSpirit.lifetime, () -> Fx.unitControl.at(unit.x, unit.y, 0f, unit));
            if(!player.dead()){
                Fx.unitSpirit.at(player.x, player.y, 0f, unit);
            }
        }else if(net.server()){
            //reject forwarding the packet if the unit was dead, AI or team
            throw new ValidateException(player, "Player attempted to control invalid unit.");
        }

        Events.fire(new UnitControlEvent(player, unit));
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true)
    public static void unitClear(Player player){
        if(player == null) return;

        if(!player.dead() && !player.unit().spawnedByCore){
            var docked = player.unit().dockedType;

            //get best core unit type as approximation
            if(docked == null){
                var closest = player.bestCore();
                if(closest != null){
                    docked = ((CoreBlock)closest.block).unitType;
                }
            }

            //respawn if necessary
            if(docked != null && docked.coreUnitDock){
                //TODO animation, etc
                Fx.spawn.at(player);

                if(!net.client()){
                    Unit unit = docked.create(player.team());
                    unit.set(player.unit());
                    //translate backwards so it doesn't spawn stuck in the unit
                    if(player.unit().isFlying() && unit.type.flying){
                        Tmp.v1.trns(player.unit().rotation + 180f, player.unit().hitSize / 2f + unit.hitSize / 2f);
                        unit.x += Tmp.v1.x;
                        unit.y += Tmp.v1.y;
                    }
                    unit.rotation(player.unit().rotation);
                    //unit.impulse(0f, -3f);
                    //TODO should there be an impulse?
                    unit.controller(player);
                    unit.spawnedByCore(true);
                    unit.add();
                }

                //skip standard respawn code
                return;
            }

        }
        //should only get to this code if docking failed or this isn't a docking unit

        //problem: this gets called on both ends. it shouldn't be.
        Fx.spawn.at(player);
        player.clearUnit();
        player.checkSpawn();
        player.deathTimer = Player.deathDelay + 1f; //for instant respawn
    }

    /** Adds an input lock; if this function returns true, input is locked. Used for mod 'cutscenes' or custom camera panning. */
    public void addLock(Boolp lock){
        inputLocks.add(lock);
    }

    /** @return whether most input is locked, for 'cutscenes' */
    public boolean locked(){
        return inputLocks.contains(Boolp::get);
    }

    public Eachable<BuildPlan> allPlans(){
        return allPlans;
    }

    public boolean isUsingSchematic(){
        return !selectPlans.isEmpty();
    }

    public void update(){
        if(logicCutscene && !renderer.isCutscene()){
            Core.camera.position.lerpDelta(logicCamPan, logicCamSpeed);
        }else{
            logicCutsceneZoom = -1f;
        }

        if(commandBuild != null && !commandBuild.isValid()){
            commandBuild = null;
        }

        if(!commandMode){
            commandRect = false;
        }

        playerPlanTree.clear();
        player.unit().plans.each(playerPlanTree::insert);

        player.typing = ui.chatfrag.shown();

        if(player.dead()){
            droppingItem = false;
        }

        if(player.isBuilder()){
            player.unit().updateBuilding(isBuilding);
        }

        if(player.shooting && !wasShooting && player.unit().hasWeapons() && state.rules.unitAmmo && !player.team().rules().infiniteAmmo && player.unit().ammo <= 0){
            player.unit().type.weapons.first().noAmmoSound.at(player.unit());
        }

        //you don't want selected blocks while locked, looks weird
        if(locked()){
            block = null;

        }

        wasShooting = player.shooting;

        //only reset the controlled type and control a unit after the timer runs out
        //essentially, this means the client waits for ~1 second after controlling something before trying to control something else automatically
        if(!player.dead() && (recentRespawnTimer -= Time.delta / 70f) <= 0f && player.justSwitchFrom != player.unit()){
            controlledType = player.unit().type;
        }

        if(controlledType != null && player.dead() && controlledType.playerControllable){
            Unit unit = Units.closest(player.team(), player.x, player.y, u -> !u.isPlayer() && u.type == controlledType && !u.dead);

            if(unit != null){
                //only trying controlling once a second to prevent packet spam
                if(!net.client() || controlInterval.get(0, 70f)){
                    recentRespawnTimer = 1f;
                    Call.unitControl(player, unit);
                }
            }
        }
    }

    public void checkUnit(){
        if(controlledType != null && controlledType.playerControllable){
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

        Unit target = Units.closest(player.team(), pay.x(), pay.y(), unit.type.hitSize * 2f, u -> u.isAI() && u.isGrounded() && pay.canPickup(u) && u.within(unit, u.hitSize + unit.hitSize));
        if(target != null){
            Call.requestUnitPayload(player, target);
        }else{
            Building build = world.buildWorld(pay.x(), pay.y());

            if(build != null && state.teams.canInteract(unit.team, build.team)){
                Call.requestBuildPayload(player, build);
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
            logicCutscene = false;
            config.forceHide();
            commandMode = commandRect = false;
        }
    }

    //TODO when shift is held? ctrl?
    public boolean multiUnitSelect(){
        return false;
    }

    public void selectUnitsRect(){
        if(commandMode && commandRect){
            if(!tappedOne){
                var units = selectedCommandUnits(commandRectX, commandRectY, input.mouseWorldX() - commandRectX, input.mouseWorldY() - commandRectY);
                if(multiUnitSelect()){
                    //tiny brain method of unique addition
                    selectedUnits.removeAll(units);
                }else{
                    //nothing selected, clear units
                    selectedUnits.clear();
                }
                selectedUnits.addAll(units);
                commandBuild = null;
            }
            commandRect = false;
        }
    }

    public void selectTypedUnits(){
        if(commandMode){
            Unit unit = selectedCommandUnit(input.mouseWorldX(), input.mouseWorldY());
            if(unit != null){
                selectedUnits.clear();
                camera.bounds(Tmp.r1);
                selectedUnits.addAll(selectedCommandUnits(Tmp.r1.x, Tmp.r1.y, Tmp.r1.width, Tmp.r1.height, u -> u.type == unit.type));
            }
        }
    }

    public void tapCommandUnit(){
        if(commandMode){

            Unit unit = selectedCommandUnit(input.mouseWorldX(), input.mouseWorldY());
            Building build = world.buildWorld(input.mouseWorldX(), input.mouseWorldY());
            if(unit != null){
                if(!selectedUnits.contains(unit)){
                    selectedUnits.add(unit);
                }else{
                    selectedUnits.remove(unit);
                }
                commandBuild = null;
            }else{
                //deselect
                selectedUnits.clear();

                if(build != null && build.team == player.team() && build.block.commandable){
                    commandBuild = (commandBuild == build ? null : build);
                }else{
                    commandBuild = null;
                }
            }
        }
    }

    public void commandTap(float screenX, float screenY){
        if(commandMode){
            //right click: move to position

            //move to location - TODO right click instead?
            Vec2 target = input.mouseWorld(screenX, screenY).cpy();

            if(selectedUnits.size > 0){

                Teamc attack = world.buildWorld(target.x, target.y);

                if(attack == null || attack.team() == player.team()){
                    attack = selectedEnemyUnit(target.x, target.y);
                }

                int[] ids = new int[selectedUnits.size];
                for(int i = 0; i < ids.length; i++){
                    ids[i] = selectedUnits.get(i).id;
                }

                Call.commandUnits(player, ids, attack instanceof Building b ? b : null, attack instanceof Unit u ? u : null, target);
            }

            if(commandBuild != null){
                Call.commandBuilding(player, commandBuild, target);
            }
        }
    }

    public void drawCommand(Unit sel){
        Drawf.square(sel.x, sel.y, sel.hitSize / 1.4f + Mathf.absin(4f, 1f), selectedUnits.contains(sel) ? Pal.remove : Pal.accent);
    }

    public void drawCommanded(){
        if(commandMode){
            //happens sometimes
            selectedUnits.removeAll(u -> !u.isCommandable());

            //draw command overlay UI
            for(Unit unit : selectedUnits){
                CommandAI ai = unit.command();
                //draw target line
                if(ai.targetPos != null){
                    Position lineDest = ai.attackTarget != null ? ai.attackTarget : ai.targetPos;
                    Drawf.limitLine(unit, lineDest, unit.hitSize / 2f, 3.5f);

                    if(ai.attackTarget == null){
                        Drawf.square(lineDest.getX(), lineDest.getY(), 3.5f);
                    }
                }

                Drawf.square(unit.x, unit.y, unit.hitSize / 1.4f + 1f);

                if(ai.attackTarget != null){
                    Drawf.target(ai.attackTarget.getX(), ai.attackTarget.getY(), 6f, Pal.remove);
                }
            }

            if(commandBuild != null){
                Drawf.square(commandBuild.x, commandBuild.y, commandBuild.hitSize() / 1.4f + 1f);
                var cpos = commandBuild.getCommandPosition();

                if(cpos != null){
                    Drawf.limitLine(commandBuild, cpos, commandBuild.hitSize() / 2f, 3.5f);
                    Drawf.square(cpos.x, cpos.y, 3.5f);
                }
            }

            if(commandMode && !commandRect){
                Unit sel = selectedCommandUnit(input.mouseWorldX(), input.mouseWorldY());

                if(sel != null && !(!multiUnitSelect() && selectedUnits.size == 1 && selectedUnits.contains(sel))){
                    drawCommand(sel);
                }
            }

            if(commandRect){
                float x2 = input.mouseWorldX(), y2 = input.mouseWorldY();
                var units = selectedCommandUnits(commandRectX, commandRectY, x2 - commandRectX, y2 - commandRectY);
                for(var unit : units){
                    drawCommand(unit);
                }

                Draw.color(Pal.accent, 0.3f);
                Fill.crect(commandRectX, commandRectY, x2 - commandRectX, y2 - commandRectY);
            }
        }

        Draw.reset();

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

    public void drawBreaking(BuildPlan plan){
        if(plan.breaking){
            drawBreaking(plan.x, plan.y);
        }else{
            drawSelected(plan.x, plan.y, plan.block, Pal.remove);
        }
    }

    public void drawOverlapCheck(Block block, int cursorX, int cursorY, boolean valid){
        if(!valid && state.rules.placeRangeCheck){
            var blocker = Build.getEnemyOverlap(block, player.team(), cursorX, cursorY);
            if(blocker != null && blocker.wasVisible){
                Drawf.selected(blocker, Pal.remove);
                Tmp.v1.set(cursorX, cursorY).scl(tilesize).add(block.offset, block.offset).sub(blocker).scl(-1f).nor();
                Drawf.dashLineDst(Pal.remove,
                cursorX * tilesize + block.offset + Tmp.v1.x * block.size * tilesize/2f,
                cursorY * tilesize + block.offset + Tmp.v1.y * block.size * tilesize/2f,
                blocker.x + Tmp.v1.x * -blocker.block.size * tilesize/2f,
                blocker.y + Tmp.v1.y * -blocker.block.size * tilesize/2f
                );
            }
        }
    }

    public boolean planMatches(BuildPlan plan){
        Tile tile = world.tile(plan.x, plan.y);
        return tile != null && tile.build instanceof ConstructBuild cons && cons.current == plan.block;
    }

    public void drawBreaking(int x, int y){
        Tile tile = world.tile(x, y);
        if(tile == null) return;
        Block block = tile.block();

        drawSelected(x, y, block, Pal.remove);
    }

    public void useSchematic(Schematic schem){
        selectPlans.addAll(schematics.toPlans(schem, player.tileX(), player.tileY()));
    }

    protected void showSchematicSave(){
        if(lastSchematic == null) return;

        var last = lastSchematic;

        ui.showTextInput("@schematic.add", "@name", "", text -> {
            Schematic replacement = schematics.all().find(s -> s.name().equals(text));
            if(replacement != null){
                ui.showConfirm("@confirm", "@schematic.replace", () -> {
                    schematics.overwrite(replacement, last);
                    ui.showInfoFade("@schematic.saved");
                    ui.schematics.showInfo(replacement);
                });
            }else{
                last.tags.put("name", text);
                last.tags.put("description", "");
                schematics.add(last);
                ui.showInfoFade("@schematic.saved");
                ui.schematics.showInfo(last);
                Events.fire(new SchematicCreateEvent(last));
            }
        });
    }

    public void rotatePlans(Seq<BuildPlan> plans, int direction){
        int ox = schemOriginX(), oy = schemOriginY();

        plans.each(plan -> {
            if(plan.breaking) return;

            plan.pointConfig(p -> {
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

            //rotate actual plan, centered on its multiblock position
            float wx = (plan.x - ox) * tilesize + plan.block.offset, wy = (plan.y - oy) * tilesize + plan.block.offset;
            float x = wx;
            if(direction >= 0){
                wx = -wy;
                wy = x;
            }else{
                wx = wy;
                wy = -x;
            }
            plan.x = World.toTile(wx - plan.block.offset) + ox;
            plan.y = World.toTile(wy - plan.block.offset) + oy;
            plan.rotation = Mathf.mod(plan.rotation + direction, 4);
        });
    }

    public void flipPlans(Seq<BuildPlan> plans, boolean x){
        int origin = (x ? schemOriginX() : schemOriginY()) * tilesize;

        plans.each(plan -> {
            if(plan.breaking) return;

            float value = -((x ? plan.x : plan.y) * tilesize - origin + plan.block.offset) + origin;

            if(x){
                plan.x = (int)((value - plan.block.offset) / tilesize);
            }else{
                plan.y = (int)((value - plan.block.offset) / tilesize);
            }

            plan.pointConfig(p -> {
                int corigin = x ? plan.originalWidth/2 : plan.originalHeight/2;
                int nvalue = -(x ? p.x : p.y);
                if(x){
                    plan.originalX = -(plan.originalX - corigin) + corigin;
                    p.x = nvalue;
                }else{
                    plan.originalY = -(plan.originalY - corigin) + corigin;
                    p.y = nvalue;
                }
            });

            //flip rotation
            plan.block.flipRotation(plan, x);
        });
    }

    protected int schemOriginX(){
        return rawTileX();
    }

    protected int schemOriginY(){
        return rawTileY();
    }

    /** @return the selection plan that overlaps this position, or null. */
    protected @Nullable BuildPlan getPlan(int x, int y){
        return getPlan(x, y, 1, null);
    }

    /** Returns the selection plan that overlaps this position, or null. */
    protected @Nullable BuildPlan getPlan(int x, int y, int size, BuildPlan skip){
        float offset = ((size + 1) % 2) * tilesize / 2f;
        r2.setSize(tilesize * size);
        r2.setCenter(x * tilesize + offset, y * tilesize + offset);
        resultplan = null;

        Boolf<BuildPlan> test = plan -> {
            if(plan == skip) return false;
            Tile other = plan.tile();

            if(other == null) return false;

            if(!plan.breaking){
                r1.setSize(plan.block.size * tilesize);
                r1.setCenter(other.worldx() + plan.block.offset, other.worldy() + plan.block.offset);
            }else{
                r1.setSize(other.block().size * tilesize);
                r1.setCenter(other.worldx() + other.block().offset, other.worldy() + other.block().offset);
            }

            return r2.overlaps(r1);
        };

        for(var plan : player.unit().plans()){
            if(test.get(plan)) return plan;
        }

        return selectPlans.find(test);
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

        for(var plan : player.unit().plans()){
            if(plan.breaking) continue;
            if(plan.bounds(Tmp.r2).overlaps(Tmp.r1)){
                drawBreaking(plan);
            }
        }

        for(var plan : selectPlans){
            if(plan.breaking) continue;
            if(plan.bounds(Tmp.r2).overlaps(Tmp.r1)){
                drawBreaking(plan);
            }
        }

        for(BlockPlan plan : player.team().data().plans){
            Block block = content.block(plan.block);
            if(block.bounds(plan.x, plan.y, Tmp.r2).overlaps(Tmp.r1)){
                drawSelected(plan.x, plan.y, content.block(plan.block), Pal.remove);
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

    protected void flushSelectPlans(Seq<BuildPlan> plans){
        for(BuildPlan plan : plans){
            if(plan.block != null && validPlace(plan.x, plan.y, plan.block, plan.rotation)){
                BuildPlan other = getPlan(plan.x, plan.y, plan.block.size, null);
                if(other == null){
                    selectPlans.add(plan.copy());
                }else if(!other.breaking && other.x == plan.x && other.y == plan.y && other.block.size == plan.block.size){
                    selectPlans.remove(other);
                    selectPlans.add(plan.copy());
                }
            }
        }
    }

    protected void flushPlansReverse(Seq<BuildPlan> plans){
        //reversed iteration.
        for(int i = plans.size - 1; i >= 0; i--){
            var plan = plans.get(i);
            if(plan.block != null && validPlace(plan.x, plan.y, plan.block, plan.rotation)){
                BuildPlan copy = plan.copy();
                plan.block.onNewPlan(copy);
                player.unit().addBuild(copy, false);
            }
        }
    }

    protected void flushPlans(Seq<BuildPlan> plans){
        for(var plan : plans){
            if(plan.block != null && validPlace(plan.x, plan.y, plan.block, plan.rotation)){
                BuildPlan copy = plan.copy();
                plan.block.onNewPlan(copy);
                player.unit().addBuild(copy);
            }
        }
    }

    protected void drawOverPlan(BuildPlan plan){
        drawOverPlan(plan, validPlace(plan.x, plan.y, plan.block, plan.rotation));
    }

    protected void drawOverPlan(BuildPlan plan, boolean valid){
        Draw.reset();
        Draw.mixcol(!valid ? Pal.breakInvalid : Color.white, (!valid ? 0.4f : 0.24f) + Mathf.absin(Time.globalTime, 6f, 0.28f));
        Draw.alpha(1f);
        plan.block.drawPlanConfigTop(plan, allSelectLines);
        Draw.reset();
    }

    protected void drawPlan(BuildPlan plan){
        drawPlan(plan, plan.cachedValid = validPlace(plan.x, plan.y, plan.block, plan.rotation));
    }

    protected void drawPlan(BuildPlan plan, boolean valid){
        plan.block.drawPlan(plan, allPlans(), valid);
    }

    /** Draws a placement icon for a specific block. */
    protected void drawPlan(int x, int y, Block block, int rotation){
        bplan.set(x, y, rotation, block);
        bplan.animScale = 1f;
        block.drawPlan(bplan, allPlans(), validPlace(x, y, block, rotation));
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
                }else if(validBreak(tile.x, tile.y) && !selectPlans.contains(r -> r.tile() != null && r.tile() == tile)){
                    selectPlans.add(new BuildPlan(tile.x, tile.y));
                }
            }
        }

        //remove build plans
        Tmp.r1.set(result.x * tilesize, result.y * tilesize, (result.x2 - result.x) * tilesize, (result.y2 - result.y) * tilesize);

        Iterator<BuildPlan> it = player.unit().plans().iterator();
        while(it.hasNext()){
            var plan = it.next();
            if(!plan.breaking && plan.bounds(Tmp.r2).overlaps(Tmp.r1)){
                it.remove();
            }
        }

        it = selectPlans.iterator();
        while(it.hasNext()){
            var plan = it.next();
            if(!plan.breaking && plan.bounds(Tmp.r2).overlaps(Tmp.r1)){
                it.remove();
            }
        }

        removed.clear();

        //remove blocks to rebuild
        Iterator<BlockPlan> broken = player.team().data().plans.iterator();
        while(broken.hasNext()){
            BlockPlan plan = broken.next();
            Block block = content.block(plan.block);
            if(block.bounds(plan.x, plan.y, Tmp.r2).overlaps(Tmp.r1)){
                removed.add(Point2.pack(plan.x, plan.y));
                plan.removed = true;
                broken.remove();
            }
        }

        //TODO array may be too large?
        if(removed.size > 0 && net.active()){
            Call.deletePlans(player, removed.toArray());
        }
    }

    protected void updateLine(int x1, int y1, int x2, int y2){
        linePlans.clear();
        iterateLine(x1, y1, x2, y2, l -> {
            rotation = l.rotation;
            var plan = new BuildPlan(l.x, l.y, l.rotation, block, block.nextConfig());
            plan.animScale = 1f;
            linePlans.add(plan);
        });

        if(Core.settings.getBool("blockreplace")){
            linePlans.each(plan -> {
                Block replace = plan.block.getReplacement(plan, linePlans);
                if(replace.unlockedNow()){
                    plan.block = replace;
                }
            });

            block.handlePlacementLine(linePlans);
        }
    }

    protected void updateLine(int x1, int y1){
        updateLine(x1, y1, tileX(getMouseX()), tileY(getMouseY()));
    }

    boolean checkConfigTap(){
        return config.isShown() && config.getSelected().onConfigureTapped(input.mouseWorldX(), input.mouseWorldY());
    }

    /** Handles tile tap events that are not platform specific. */
    boolean tileTapped(@Nullable Building build){
        if(build == null){
            inv.hide();
            config.hideConfig();
            commandBuild = null;
            return false;
        }
        boolean consumed = false, showedInventory = false;

        //select building for commanding
        if(build.block.commandable && commandMode){
            //TODO handled in tap.
            consumed = true;
        }else if(build.block.configurable && build.interactable(player.team())){ //check if tapped block is configurable
            consumed = true;
            if((!config.isShown() && build.shouldShowConfigure(player)) //if the config fragment is hidden, show
            //alternatively, the current selected block can 'agree' to switch config tiles
            || (config.isShown() && config.getSelected().onConfigureBuildTapped(build))){
                Sounds.click.at(build);
                config.showConfig(build);
            }
            //otherwise...
        }else if(!config.hasConfigMouse()){ //make sure a configuration fragment isn't on the cursor
            //then, if it's shown and the current block 'agrees' to hide, hide it.
            if(config.isShown() && config.getSelected().onConfigureBuildTapped(build)){
                consumed = true;
                config.hideConfig();
            }

            if(config.isShown()){
                consumed = true;
            }
        }

        //call tapped event
        if(!consumed && build.interactable(player.team())){
            build.tapped();
        }

        //consume tap event if necessary
        if(build.interactable(player.team()) && build.block.consumesTap){
            consumed = true;
        }else if(build.interactable(player.team()) && build.block.synthetic() && (!consumed || build.block.allowConfigInventory)){
            if(build.block.hasItems && build.items.total() > 0){
                inv.showFor(build);
                consumed = true;
                showedInventory = true;
            }
        }

        if(!showedInventory){
            inv.hide();
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
            player.unit().mineTile = tile;
            return true;
        }
        return false;
    }

    /** Tries to stop mining, returns true if mining was stopped. */
    boolean tryStopMine(){
        if(player.unit().mining()){
            player.unit().mineTile = null;
            return true;
        }
        return false;
    }

    boolean tryStopMine(Tile tile){
        if(player.unit().mineTile == tile){
            player.unit().mineTile = null;
            return true;
        }
        return false;
    }

    boolean canMine(Tile tile){
        return !Core.scene.hasMouse()
            && player.unit().validMine(tile)
            && player.unit().acceptsItem(player.unit().getMineResult(tile))
            && !((!Core.settings.getBool("doubletapmine") && tile.floor().playerUnmineable) && tile.overlay().itemDrop == null);
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
        Unit unit = Units.closest(player.team(), Core.input.mouseWorld().x, Core.input.mouseWorld().y, 40f, u -> u.isAI() && u.type.playerControllable);
        if(unit != null){
            unit.hitbox(Tmp.r1);
            Tmp.r1.grow(6f);
            if(Tmp.r1.contains(Core.input.mouseWorld())){
                return unit;
            }
        }

        Building build = world.buildWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
        if(build instanceof ControlBlock cont && cont.canControl() && build.team == player.team() && cont.unit() != player.unit() && cont.unit().isAI()){
            return cont.unit();
        }

        return null;
    }

    public @Nullable Building selectedControlBuild(){
        Building build = world.buildWorld(Core.input.mouseWorld().x, Core.input.mouseWorld().y);
        if(build != null && !player.dead() && build.canControlSelect(player.unit()) && build.team == player.team()){
            return build;
        }
        return null;
    }

    public @Nullable Unit selectedCommandUnit(float x, float y){
        var tree = player.team().data().tree();
        tmpUnits.clear();
        float rad = 4f;
        tree.intersect(x - rad/2f, y - rad/2f, rad, rad, tmpUnits);
        return tmpUnits.min(u -> u.isCommandable(), u -> u.dst(x, y) - u.hitSize/2f);
    }

    public @Nullable Unit selectedEnemyUnit(float x, float y){
        tmpUnits.clear();
        float rad = 4f;

        Seq<TeamData> data = state.teams.present;
        for(int i = 0; i < data.size; i++){
            if(data.items[i].team != player.team()){
                data.items[i].tree().intersect(x - rad / 2f, y - rad / 2f, rad, rad, tmpUnits);
            }
        }

        return tmpUnits.min(u -> !u.inFogTo(player.team()), u -> u.dst(x, y) - u.hitSize/2f);
    }

    public Seq<Unit> selectedCommandUnits(float x, float y, float w, float h, Boolf<Unit> predicate){
        var tree = player.team().data().tree();
        tmpUnits.clear();
        float rad = 4f;
        tree.intersect(Tmp.r1.set(x - rad/2f, y - rad/2f, rad*2f + w, rad*2f + h).normalize(), tmpUnits);
        tmpUnits.removeAll(u -> !u.isCommandable() || !predicate.get(u));
        return tmpUnits;
    }

    public Seq<Unit> selectedCommandUnits(float x, float y, float w, float h){
        return selectedCommandUnits(x, y, w, h, u -> true);
    }

    public void remove(){
        Core.input.removeProcessor(this);
        group.remove();
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

            group.setFillParent(true);
            Vars.ui.hudGroup.addChildBefore(Core.scene.find("overlaymarker"), group);

            inv.build(group);
            config.build(group);
        }
    }

    public boolean canShoot(){
        return block == null && !onConfigurable() && !isDroppingItem() && !player.unit().activelyBuilding() &&
            !(player.unit() instanceof Mechc && player.unit().isFlying()) && !player.unit().mining() && !commandMode;
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

    public void tryDropItems(@Nullable Building build, float x, float y){
        if(!droppingItem || player.unit().stack.amount <= 0 || canTapPlayer(x, y) || state.isPaused() ){
            droppingItem = false;
            return;
        }

        droppingItem = false;

        ItemStack stack = player.unit().stack;

        if(build != null && build.acceptStack(stack.item, stack.amount, player.unit()) > 0 && build.interactable(player.team()) &&
                build.block.hasItems && player.unit().stack().amount > 0 && build.interactable(player.team())){
            if(!(state.rules.onlyDepositCore && !(build instanceof CoreBuild))){
                Call.transferInventory(player, build);
            }
        }else{
            Call.dropItem(player.angleTo(x, y));
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
        if(player.unit().plans.size > 0){
            Tmp.r1.setCentered(x * tilesize + type.offset, y * tilesize + type.offset, type.size * tilesize);
            plansOut.clear();
            playerPlanTree.intersect(Tmp.r1, plansOut);

            for(int i = 0; i < plansOut.size; i++){
                var plan = plansOut.items[i];
                if(plan != ignore
                && !plan.breaking
                && plan.block.bounds(plan.x, plan.y, Tmp.r1).overlaps(type.bounds(x, y, Tmp.r2))
                && !(type.canReplace(plan.block) && Tmp.r1.equals(Tmp.r2))){
                    return false;
                }
            }
        }

        return Build.validPlace(type, player.team(), x, y, rotation);
    }

    public boolean validBreak(int x, int y){
        return Build.validBreak(player.team(), x, y);
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

        if(block != null && block.swapDiagonalPlacement){
            diagonal = !diagonal;
        }

        int endRotation = -1;
        var start = world.build(startX, startY);
        var end = world.build(endX, endY);
        if(diagonal && (block == null || block.allowDiagonal)){
            if(block != null && start instanceof ChainedBuilding && end instanceof ChainedBuilding
                    && block.canReplace(end.block) && block.canReplace(start.block)){
                points = Placement.upgradeLine(startX, startY, endX, endY);
            }else{
                points = Placement.pathfindLine(block != null && block.conveyorPlacement, startX, startY, endX, endY);
            }
        }else{
            points = Placement.normalizeLine(startX, startY, endX, endY);
        }
        if(points.size > 1 && end instanceof ChainedBuilding){
            Point2 secondToLast = points.get(points.size - 2);
            if(!(world.build(secondToLast.x, secondToLast.y) instanceof ChainedBuilding)){
                endRotation = end.rotation;
            }
        }

        if(block != null){
            block.changePlacementPath(points, rotation, diagonal);
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
                int result = baseRotation;
                if(next != null){
                    result = Tile.relativeTo(point.x, point.y, next.x, next.y);
                }else if(endRotation != -1){
                    result = endRotation;
                }else if(block.conveyorPlacement && i > 0){
                    Point2 prev = points.get(i - 1);
                    result = Tile.relativeTo(prev.x, prev.y, point.x, point.y);
                }
                if(result != -1){
                    line.rotation = result;
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
