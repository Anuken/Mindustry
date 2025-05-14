package mindustry.entities.comp;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.ai.types.*;
import mindustry.annotations.Annotations.*;
import mindustry.async.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;
import static mindustry.logic.GlobalVars.*;

@Component(base = true)
abstract class UnitComp implements Healthc, Physicsc, Hitboxc, Statusc, Teamc, Itemsc, Rotc, Unitc, Weaponsc, Drawc, Syncc, Shieldc, Displayable, Ranged, Minerc, Builderc, Senseable, Settable{
    private static final Vec2 tmp1 = new Vec2(), tmp2 = new Vec2();
    static final float warpDst = 20f;

    @Import boolean dead, disarmed;
    @Import float x, y, rotation, maxHealth, drag, armor, hitSize, health, shield, ammo, dragMultiplier, armorOverride, speedMultiplier;
    @Import Team team;
    @Import int id;
    @Import @Nullable Tile mineTile;
    @Import Vec2 vel;
    @Import WeaponMount[] mounts;
    @Import ItemStack stack;

    private UnitController controller;
    Ability[] abilities = {};
    UnitType type = UnitTypes.alpha;
    boolean spawnedByCore;
    double flag;

    transient @Nullable Trail trail;
    //TODO could be better represented as a unit
    transient @Nullable UnitType dockedType;

    transient String lastCommanded;
    transient float shadowAlpha = -1f, healTime;
    transient int lastFogPos;
    private transient float resupplyTime = Mathf.random(10f);
    private transient boolean wasPlayer;
    private transient boolean wasHealed;

    @SyncLocal float elevation;
    private transient boolean wasFlying;
    transient float drownTime;
    transient float splashTimer;
    transient @Nullable Floor lastDrownFloor;

    public boolean checkTarget(boolean targetAir, boolean targetGround){
        return (isGrounded() && targetGround) || (isFlying() && targetAir);
    }

    public boolean isGrounded(){
        return elevation < 0.001f;
    }

    public boolean isFlying(){
        return elevation >= 0.09f;
    }

    public boolean canDrown(){
        return isGrounded() && type.canDrown;
    }

    public @Nullable Floor drownFloor(){
        return floorOn();
    }

    public void wobble(){
        x += Mathf.sin(Time.time + (id % 10) * 12, 25f, 0.05f) * Time.delta * elevation;
        y += Mathf.cos(Time.time + (id % 10) * 12, 25f, 0.05f) * Time.delta * elevation;
    }

    public void moveAt(Vec2 vector, float acceleration){
        Vec2 t = tmp1.set(vector); //target vector
        tmp2.set(t).sub(vel).limit(acceleration * vector.len() * Time.delta); //delta vector
        vel.add(tmp2);
    }

    public float floorSpeedMultiplier(){
        Floor on = isFlying() || type.hovering ? Blocks.air.asFloor() : floorOn();
        return on.speedMultiplier * speedMultiplier;
    }

    /** Called when this unit was unloaded from a factory or spawn point. */
    public void unloaded(){

    }

    public void updateBoosting(boolean boost){
        if(!type.canBoost || dead) return;

        elevation = Mathf.approachDelta(elevation, type.canBoost ? Mathf.num(boost || onSolid() || (isFlying() && !canLand())) : 0f, type.riseSpeed);
    }

    /** Move based on preferred unit movement type. */
    public void movePref(Vec2 movement){
        if(type.omniMovement){
            moveAt(movement);
        }else{
            rotateMove(movement);
        }
    }

    public void moveAt(Vec2 vector){
        moveAt(vector, type.accel);
    }

    public void approach(Vec2 vector){
        vel.approachDelta(vector, type.accel * speed());
    }

    public void rotateMove(Vec2 vec){
        moveAt(Tmp.v2.trns(rotation, vec.len()));

        if(!vec.isZero()){
            rotation = Angles.moveToward(rotation, vec.angle(), type.rotateSpeed * Time.delta * speedMultiplier);
        }
    }

    public void aimLook(Position pos){
        aim(pos);
        lookAt(pos);
    }

    public void aimLook(float x, float y){
        aim(x, y);
        lookAt(x, y);
    }

    public boolean isPathImpassable(int tileX, int tileY){
        return !type.flying && world.tiles.in(tileX, tileY) && type.pathCost.getCost(team.id, pathfinder.get(tileX, tileY)) == -1;
    }

    /** @return approx. square size of the physical hitbox for physics */
    public float physicSize(){
        return hitSize * 0.7f;
    }

    /** @return whether there is solid, un-occupied ground under this unit. */
    public boolean canLand(){
        return !onSolid() && Units.count(x, y, physicSize(), f -> f != self() && f.isGrounded()) == 0;
    }

    public boolean inRange(Position other){
        return within(other, type.range);
    }

    public boolean hasWeapons(){
        return type.hasWeapons();
    }

    /** @return speed with boost & floor multipliers factored in. */
    public float speed(){
        float strafePenalty = isGrounded() || !isPlayer() ? 1f : Mathf.lerp(1f, type.strafePenalty, Angles.angleDist(vel().angle(), rotation) / 180f);
        float boost = Mathf.lerp(1f, type.canBoost ? type.boostMultiplier : 1f, elevation);
        return type.speed * strafePenalty * boost * floorSpeedMultiplier();
    }

    /** @return where the unit wants to look at. */
    public float prefRotation(){
        if(activelyBuilding() && type.rotateToBuilding){
            return angleTo(buildPlan());
        }else if(mineTile != null){
            return angleTo(mineTile);
        }else if(moving() && type.omniMovement){
            return vel().angle();
        }
        return rotation;
    }

    @Override
    public boolean displayable(){
        return type.hoverable;
    }

    @Override
    @Replace
    public boolean isSyncHidden(Player player){
        //shooting reveals position so bullets can be seen
        return !isShooting() && inFogTo(player.team());
    }

    @Override
    public void handleSyncHidden(){
        remove();
        netClient.clearRemovedEntity(id);
    }

    @Override
    @Replace
    public boolean inFogTo(Team viewer){
        if(this.team == viewer || !state.rules.fog) return false;

        if(hitSize <= 16f){
            return !fogControl.isVisible(viewer, x, y);
        }else{
            //for large hitsizes, check around the unit instead
            float trns = hitSize / 2f;
            for(var p : Geometry.d8){
                if(fogControl.isVisible(viewer, x + p.x * trns, y + p.y * trns)){
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public float range(){
        return type.maxRange;
    }

    @Replace
    public float clipSize(){
        if(isBuilding()){
            return state.rules.infiniteResources ? Float.MAX_VALUE : Math.max(type.clipSize, type.region.width) + type.buildRange + tilesize*4f;
        }
        if(mining()){
            return type.clipSize + type.mineRange;
        }
        return type.clipSize;
    }

    @Override
    public double sense(LAccess sensor){
        return switch(sensor){
            case totalItems -> stack().amount;
            case itemCapacity -> type.itemCapacity;
            case rotation -> rotation;
            case health -> health;
            case shield -> shield;
            case maxHealth -> maxHealth;
            case ammo -> !state.rules.unitAmmo ? type.ammoCapacity : ammo;
            case ammoCapacity -> type.ammoCapacity;
            case x -> World.conv(x);
            case y -> World.conv(y);
            case velocityX -> vel.x * 60f / tilesize;
            case velocityY -> vel.y * 60f / tilesize;
            case dead -> dead || !isAdded() ? 1 : 0;
            case team -> team.id;
            case shooting -> isShooting() ? 1 : 0;
            case boosting -> type.canBoost && isFlying() ? 1 : 0;
            case range -> range() / tilesize;
            case shootX -> World.conv(aimX());
            case shootY -> World.conv(aimY());
            case cameraX -> controller instanceof Player player ? World.conv(player.con == null ? Core.camera.position.x : player.con.viewX) : 0;
            case cameraY -> controller instanceof Player player ? World.conv(player.con == null ? Core.camera.position.y : player.con.viewY) : 0;
            case cameraWidth -> controller instanceof Player player ? World.conv(player.con == null ? Core.camera.width : player.con.viewWidth) : 0;
            case cameraHeight -> controller instanceof Player player ? World.conv(player.con == null ? Core.camera.height : player.con.viewHeight) : 0;
            case mining -> mining() ? 1 : 0;
            case mineX -> mining() ? mineTile.x : -1;
            case mineY -> mining() ? mineTile.y : -1;
            case armor -> armorOverride >= 0f ? armorOverride : armor;
            case flag -> flag;
            case speed -> type.speed * 60f / tilesize * speedMultiplier;
            case controlled -> !isValid() ? 0 :
                    controller instanceof LogicAI ? ctrlProcessor :
                    controller instanceof Player ? ctrlPlayer :
                    controller instanceof CommandAI command && command.hasCommand() ? ctrlCommand :
                    0;
            case payloadCount -> ((Object)this) instanceof Payloadc pay ? pay.payloads().size : 0;
            case totalPayload -> ((Object)this) instanceof Payloadc pay ? pay.payloadUsed() / (tilesize * tilesize) : 0;
            case payloadCapacity -> type.payloadCapacity / tilePayload;
            case size -> hitSize / tilesize;
            case color -> Color.toDoubleBits(team.color.r, team.color.g, team.color.b, 1f);
            default -> Float.NaN;
        };
    }

    @Override
    public Object senseObject(LAccess sensor){
        return switch(sensor){
            case type -> type;
            case name -> controller instanceof Player p ? p.name : null;
            case firstItem -> stack().amount == 0 ? null : item();
            case controller -> !isValid() ? null : controller instanceof LogicAI log ? log.controller : this;
            case payloadType -> ((Object)this) instanceof Payloadc pay ?
                (pay.payloads().isEmpty() ? null :
                pay.payloads().peek() instanceof UnitPayload p1 ? p1.unit.type :
                pay.payloads().peek() instanceof BuildPayload p2 ? p2.block() : null) : null;
            default -> noSensed;
        };
    }

    @Override
    public double sense(Content content){
        if(content == stack().item) return stack().amount;
        if(content instanceof UnitType u){
            return ((Object)this) instanceof Payloadc pay ?
                    (pay.payloads().isEmpty() ? 0 :
                    pay.payloads().count(p -> p instanceof UnitPayload up && up.unit.type == u)) : 0;
        }
        if(content instanceof Block b){
            return ((Object)this) instanceof Payloadc pay ?
                    (pay.payloads().isEmpty() ? 0 :
                    pay.payloads().count(p -> p instanceof BuildPayload bp && bp.build.block == b)) : 0;
        }
        return Float.NaN;
    }

    @Override
    public void setProp(LAccess prop, double value){
        switch(prop){
            case health -> {
                health = (float)Mathf.clamp(value, 0, maxHealth);
                if(health <= 0f && !dead){
                    kill();
                }
            }
            case shield -> shield = Math.max((float)value, 0f);
            case x -> {
                x = World.unconv((float)value);
                if(!isLocal()) snapInterpolation();
            }
            case y -> {
                y = World.unconv((float)value);
                if(!isLocal()) snapInterpolation();
            }
            case velocityX -> vel.x = (float)(value * tilesize / 60d);
            case velocityY -> vel.y = (float)(value * tilesize / 60d);
            case rotation -> rotation = (float)value;
            case team -> {
                if(!net.client()){
                    Team team = Team.get((int)value);
                    if(controller instanceof Player p){
                        p.team(team);
                    }
                    this.team = team;
                }
            }
            case flag -> flag = value;
            case speed -> statusSpeed(Mathf.clamp((float)value, 0f, 1000f));
            case armor -> statusArmor(Math.max((float)value, 0f));
        }
    }

    @Override
    public void setProp(LAccess prop, Object value){
        switch(prop){
            case team -> {
                if(value instanceof Team t && !net.client()){
                    if(controller instanceof Player p) p.team(t);
                    team = t;
                }
            }
            case payloadType -> {
                //only serverside
                if(((Object)this) instanceof Payloadc pay && !net.client()){
                    if(value instanceof Block b){
                        if(b.synthetic()){
                            Building build = b.newBuilding().create(b, team());
                            if(pay.canPickup(build)) pay.addPayload(new BuildPayload(build));
                        }
                    }else if(value instanceof UnitType ut){
                        Unit unit = ut.create(team());
                        if(pay.canPickup(unit)) pay.addPayload(new UnitPayload(unit));
                    }else if(value == null && pay.payloads().size > 0){
                        pay.payloads().pop();
                    }
                }
            }
        }
    }

    @Override
    public void setProp(UnlockableContent content, double value){
        if(content instanceof Item item){
            stack.item = item;
            stack.amount = Mathf.clamp((int)value, 0, type.itemCapacity);
        }
    }

    @Override
    @Replace
    public boolean canShoot(){
        //cannot shoot while boosting
        return !disarmed && !(type.canBoost && isFlying());
    }

    public boolean isEnemy(){
        return type.isEnemy;
    }

    @Override
    @Replace
    public boolean collides(Hitboxc other){
        return hittable();
    }

    @Override
    public void collision(Hitboxc other, float x, float y){
        if(other instanceof Bullet bullet){
            controller.hit(bullet);
        }
    }

    @Override
    public int itemCapacity(){
        return type.itemCapacity;
    }

    @Override
    public float bounds(){
        return hitSize *  2f;
    }

    @Override
    public void controller(UnitController next){
        this.controller = next;
        if(controller.unit() != self()) controller.unit(self());
    }

    @Override
    public UnitController controller(){
        return controller;
    }

    public void resetController(){
        controller(type.createController(self()));
    }

    @Override
    public void set(UnitType def, UnitController controller){
        if(this.type != def){
            setType(def);
        }
        controller(controller);
    }

    /** @return the collision layer to use for unit physics. Returning anything outside of PhysicsProcess contents will crash the game. */
    public int collisionLayer(){
        return type.allowLegStep && type.legPhysicsLayer ? PhysicsProcess.layerLegs : isGrounded() ? PhysicsProcess.layerGround : PhysicsProcess.layerFlying;
    }

    public void lookAt(float angle){
        rotation = Angles.moveToward(rotation, angle, type.rotateSpeed * Time.delta * speedMultiplier());
    }

    public void lookAt(Position pos){
        lookAt(angleTo(pos));
    }

    public void lookAt(float x, float y){
        lookAt(angleTo(x, y));
    }

    public boolean isAI(){
        return controller instanceof AIController;
    }

    /** @return whether the unit *can* be commanded, even if its controller is not currently CommandAI. */
    public boolean allowCommand(){
        return controller instanceof CommandAI;
    }

    /** @return whether the unit has a CommandAI controller */
    public boolean isCommandable(){
        return controller instanceof CommandAI;
    }

    public boolean canTarget(Teamc other){
        return other != null && (other instanceof Unit u ? u.checkTarget(type.targetAir, type.targetGround) : (other instanceof Building b && type.targetGround));
    }

    public CommandAI command(){
        if(controller instanceof CommandAI ai){
            return ai;
        }else{
            throw new IllegalArgumentException("Unit cannot be commanded - check isCommandable() first.");
        }
    }

    public boolean isMissile(){
        return this instanceof TimedKillc;
    }

    public int count(){
        return team.data().countType(type);
    }

    public int cap(){
        return Units.getCap(team);
    }

    public void setType(UnitType type){
        this.type = type;
        this.maxHealth = type.health;
        this.drag = type.drag;
        this.armor = type.armor;
        this.hitSize = type.hitSize;

        if(mounts().length != type.weapons.size) setupWeapons(type);
        if(abilities.length != type.abilities.size){
            abilities = new Ability[type.abilities.size];
            for(int i = 0; i < type.abilities.size; i ++){
                abilities[i] = type.abilities.get(i).copy();
            }
        }
        if(controller == null) controller(type.createController(self()));
    }

    public boolean playerControllable(){
        return type.playerControllable && !(controller instanceof LogicAI ai && ai.controller != null && ai.controller.block.privileged);
    }

    public boolean targetable(Team targeter){
        return type.targetable(self(), targeter);
    }

    public boolean hittable(){
        return type.hittable(self());
    }

    @Override
    public void afterSync(){
        //set up type info after reading
        setType(this.type);
        controller.unit(self());
    }

    @Override
    public void afterRead(){
        setType(this.type);
        controller.unit(self());
        //reset controller state
        if(!(controller instanceof AIController ai && ai.keepState())){
            controller(type.createController(self()));
        }
    }

    @Override
    public void afterReadAll(){
        controller.afterRead(self());
    }

    @Override
    public void add(){
        team.data().updateCount(type, 1);

        //check if over unit cap
        if(type.useUnitCap && count() > cap() && !spawnedByCore && !dead && !state.rules.editor){
            Call.unitCapDeath(self());
            team.data().updateCount(type, -1);
        }

    }

    @Override
    public void remove(){
        team.data().updateCount(type, -1);
        controller.removed(self());

        //make sure trail doesn't just go poof
        if(trail != null && trail.size() > 0){
            Fx.trailFade.at(x, y, trail.width(), type.trailColor == null ? team.color : type.trailColor, trail.copy());
        }
    }

    @Override
    public void landed(){
        if(type.mechLandShake > 0f){
            Effect.shake(type.mechLandShake, type.mechLandShake, this);
        }

        type.landed(self());
    }

    @Override
    public void heal(float amount){
        if(health < maxHealth && amount > 0){
            wasHealed = true;
        }
    }

    public void updateDrowning(){
        Floor floor = drownFloor();

        if(floor != null && floor.isLiquid && floor.drownTime > 0 && canDrown()){
            lastDrownFloor = floor;
            drownTime += Time.delta / floor.drownTime / type.drownTimeMultiplier;
            if(Mathf.chanceDelta(0.05f)){
                floor.drownUpdateEffect.at(x, y, hitSize, floor.mapColor);
            }

            if(drownTime >= 0.999f && !net.client()){
                kill();
                Events.fire(new UnitDrownEvent(self()));
            }
        }else{
            drownTime -= Time.delta / 50f;
        }

        drownTime = Mathf.clamp(drownTime);
    }

    @Override
    public void update(){

        type.update(self());

        //update bounds

        if(type.bounded){
            float bot = 0f, left = 0f, top = world.unitHeight(), right = world.unitWidth();

            //TODO hidden map rules only apply to player teams? should they?
            if(state.rules.limitMapArea && !team.isAI()){
                bot = state.rules.limitY * tilesize;
                left = state.rules.limitX * tilesize;
                top = state.rules.limitHeight * tilesize + bot;
                right = state.rules.limitWidth * tilesize + left;
            }

            if(!net.client() || isLocal()){

                float dx = 0f, dy = 0f;

                //repel unit out of bounds
                if(x < left) dx += (-(x - left)/warpDst);
                if(y < bot) dy += (-(y - bot)/warpDst);
                if(x > right) dx -= (x - right)/warpDst;
                if(y > top) dy -= (y - top)/warpDst;

                velAddNet(dx * Time.delta, dy * Time.delta);
                float margin = tilesize * 2f;
                x = Mathf.clamp(x, left - margin, right - tilesize + margin);
                y = Mathf.clamp(y, bot - margin, top - tilesize + margin);
            }

            //clamp position if not flying
            if(isGrounded()){
                x = Mathf.clamp(x, left, right - tilesize);
                y = Mathf.clamp(y, bot, top - tilesize);
            }

            //kill when out of bounds
            if(x < -finalWorldBounds + left || y < -finalWorldBounds + bot || x >= right + finalWorldBounds || y >= top + finalWorldBounds){
                kill();
            }
        }

        //update drown/flying state

        Floor floor = floorOn();
        Tile tile = tileOn();

        if(isFlying() != wasFlying){
            if(wasFlying){
                if(tile != null){
                    Fx.unitLand.at(x, y, floor.isLiquid ? 1f : 0.5f, tile.floor().mapColor);
                }
            }

            wasFlying = isFlying();
        }

        if(!type.hovering && isGrounded() && type.emitWalkEffect){
            if((splashTimer += Mathf.dst(deltaX(), deltaY())) >= (7f + hitSize()/8f)){
                floor.walkEffect.at(x, y, hitSize() / 8f, floor.mapColor);
                splashTimer = 0f;

                if(type.emitWalkSound){
                    floor.walkSound.at(x, y, Mathf.random(floor.walkSoundPitchMin, floor.walkSoundPitchMax), floor.walkSoundVolume);
                }
            }
        }

        updateDrowning();

        if(wasHealed && healTime <= -1f){
            healTime = 1f;
        }
        healTime -= Time.delta / 20f;
        wasHealed = false;

        //die on captured sectors immediately
        if(team.isOnlyAI() && state.isCampaign() && state.getSector().isCaptured()){
            kill();
        }

        if(!headless && type.loopSound != Sounds.none){
            control.sound.loop(type.loopSound, this, type.loopSoundVolume);
        }

        //check if environment is unsupported
        if(!type.supportsEnv(state.rules.env) && !dead){
            Call.unitEnvDeath(self());
            team.data().updateCount(type, -1);
        }

        if(state.rules.unitAmmo && ammo < type.ammoCapacity - 0.0001f){
            resupplyTime += Time.delta;

            //resupply only at a fixed interval to prevent lag
            if(resupplyTime > 10f){
                type.ammoType.resupply(self());
                resupplyTime = 0f;
            }
        }

        for(Ability a : abilities){
            a.update(self());
        }

        if(trail != null){
            trail.length = type.trailLength;

            float scale = type.useEngineElevation ? elevation : 1f;
            float offset = type.engineOffset/2f + type.engineOffset/2f*scale;

            float cx = x + Angles.trnsx(rotation + 180, offset), cy = y + Angles.trnsy(rotation + 180, offset);
            trail.update(cx, cy);
        }

        drag = type.drag * (isGrounded() ? (floorOn().dragMultiplier) : 1f) * dragMultiplier * state.rules.dragMultiplier;

        //apply knockback based on spawns
        if(team != state.rules.waveTeam && state.hasSpawns() && (!net.client() || isLocal()) && hittable()){
            float relativeSize = state.rules.dropZoneRadius + hitSize/2f + 1f;
            for(Tile spawn : spawner.getSpawns()){
                if(within(spawn.worldx(), spawn.worldy(), relativeSize)){
                    velAddNet(Tmp.v1.set(this).sub(spawn.worldx(), spawn.worldy()).setLength(0.1f + 1f - dst(spawn) / relativeSize).scl(0.45f * Time.delta));
                }
            }
        }

        //simulate falling down
        if(dead || health <= 0){
            //less drag when dead
            drag = 0.01f;

            //standard fall smoke
            if(Mathf.chanceDelta(0.1)){
                Tmp.v1.rnd(Mathf.range(hitSize));
                type.fallEffect.at(x + Tmp.v1.x, y + Tmp.v1.y);
            }

            //thruster fall trail
            if(Mathf.chanceDelta(0.2)){
                float offset = type.engineOffset/2f + type.engineOffset/2f * elevation;
                float range = Mathf.range(type.engineSize);
                type.fallEngineEffect.at(
                    x + Angles.trnsx(rotation + 180, offset) + Mathf.range(range),
                    y + Angles.trnsy(rotation + 180, offset) + Mathf.range(range),
                    Mathf.random()
                );
            }

            //move down
            elevation -= type.fallSpeed * Time.delta;

            if(isGrounded() || health <= -maxHealth){
                Call.unitDestroy(id);
            }
        }

        if(tile != null && tile.build != null){
            tile.build.unitOnAny(self());
        }

        if(tile != null && isGrounded() && !type.hovering){
            //unit block update
            if(tile.build != null){
                tile.build.unitOn(self());
            }

            //apply damage
            if(floor.damageTaken > 0f){
                damageContinuous(floor.damageTaken);
            }
        }

        //kill entities on tiles that are solid to them
        if(tile != null && !canPassOn()){
            //boost if possible
            if(type.canBoost){
                elevation = 1f;
            }else if(!net.client()){
                kill();
            }
        }

        //AI only updates on the server
        if(!net.client() && !dead && shouldUpdateController()){
            controller.updateUnit();
        }

        //clear controller when it becomes invalid
        if(!controller.isValidController()){
            resetController();
        }

        //remove units spawned by the core
        if(spawnedByCore && !isPlayer() && !dead){
            Call.unitDespawn(self());
        }
    }

    public boolean shouldUpdateController(){
        return true;
    }

    /** @return a preview UI icon for this unit. */
    public TextureRegion icon(){
        return type.uiIcon;
    }

    /** Actually destroys the unit, removing it and creating explosions. **/
    public void destroy(){
        if(!isAdded() || !type.killable) return;

        float explosiveness = 2f + item().explosiveness * stack().amount * 1.53f;
        float flammability = item().flammability * stack().amount / 1.9f;
        float power = item().charge * Mathf.pow(stack().amount, 1.11f) * 160f;

        if(!spawnedByCore){
            Damage.dynamicExplosion(x, y, flammability, explosiveness, power, (bounds() + type.legLength/1.7f) / 2f, state.rules.damageExplosions && state.rules.unitCrashDamage(team) > 0, item().flammability > 1, team, type.deathExplosionEffect);
        }else{
            type.deathExplosionEffect.at(x, y, bounds() / 2f / 8f);
        }

        float shake = type.deathShake < 0 ? hitSize / 3f : type.deathShake;

        if(type.createScorch){
            Effect.scorch(x, y, (int)(hitSize / 5));
        }
        Effect.shake(shake, shake, this);
        type.deathSound.at(this);

        Events.fire(new UnitDestroyEvent(self()));

        if(explosiveness > 7f && (isLocal() || wasPlayer)){
            Events.fire(Trigger.suicideBomb);
        }

        for(WeaponMount mount : mounts){
            if(mount.weapon.shootOnDeath && !(mount.weapon.bullet.killShooter && mount.totalShots > 0)){
                mount.reload = 0f;
                mount.shoot = true;
                mount.weapon.update(self(), mount);
            }
        }

        //if this unit crash landed (was flying), damage stuff in a radius
        if(type.flying && !spawnedByCore && type.createWreck && state.rules.unitCrashDamage(team) > 0){
            var shields = indexer.getEnemy(team, BlockFlag.shield);
            float crashDamage = Mathf.pow(hitSize, 0.75f) * type.crashDamageMultiplier * 5f * state.rules.unitCrashDamage(team);
            if(shields.isEmpty() || !shields.contains(b -> b instanceof ExplosionShield s && s.absorbExplosion(x, y, crashDamage))){
                Damage.damage(team, x, y, Mathf.pow(hitSize, 0.94f) * 1.25f, crashDamage, true, false, true);
            }
        }

        if(!headless && type.createScorch){
            for(int i = 0; i < type.wreckRegions.length; i++){
                if(type.wreckRegions[i].found()){
                    float range = type.hitSize /4f;
                    Tmp.v1.rnd(range);
                    Effect.decal(type.wreckRegions[i], x + Tmp.v1.x, y + Tmp.v1.y, rotation - 90);
                }
            }
        }

        for(Ability a : abilities){
            a.death(self());
        }

        type.killed(self());

        remove();
    }

    /** @return name of direct or indirect player controller. */
    @Override
    public @Nullable String getControllerName(){
        if(isPlayer()) return getPlayer().coloredName();
        if(controller instanceof LogicAI ai && ai.controller != null) return ai.controller.lastAccessed;
        return null;
    }

    @Override
    public void display(Table table){
        type.display(self(), table);
    }

    @Override
    public void draw(){
        type.draw(self());
    }

    @Override
    public boolean isPlayer(){
        return controller instanceof Player;
    }

    @Nullable
    public Player getPlayer(){
        return isPlayer() ? (Player)controller : null;
    }

    @Override
    public void killed(){
        wasPlayer = isLocal();
        health = Math.min(health, 0);
        dead = true;

        //don't waste time when the unit is already on the ground, just destroy it
        if(!type.flying || !type.createWreck){
            destroy();
        }
    }

    @Override
    @Replace
    public void kill(){
        if(dead || net.client() || !type.killable) return;

        //deaths are synced; this calls killed()
        Call.unitDeath(id);
    }

    @Override
    @Replace
    public String toString(){
        return "Unit#" + id() + ":" + type + " (" + x + ", " + y + ")";
    }
}
