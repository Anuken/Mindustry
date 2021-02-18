package mindustry.entities.comp;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ai.*;
import mindustry.ai.types.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.payloads.*;

import static mindustry.Vars.*;

@Component(base = true)
abstract class UnitComp implements Healthc, Physicsc, Hitboxc, Statusc, Teamc, Itemsc, Rotc, Unitc, Weaponsc, Drawc, Boundedc, Syncc, Shieldc, Commanderc, Displayable, Senseable, Ranged, Minerc, Builderc{

    @Import boolean hovering, dead;
    @Import float x, y, rotation, elevation, maxHealth, drag, armor, hitSize, health, ammo, minFormationSpeed;
    @Import Team team;
    @Import int id;
    @Import @Nullable Tile mineTile;
    @Import Vec2 vel;

    private UnitController controller;
    UnitType type;
    boolean spawnedByCore;
    double flag;

    transient Seq<Ability> abilities = new Seq<>(0);
    private transient float resupplyTime = Mathf.random(10f);
    private transient boolean wasPlayer;

    public void moveAt(Vec2 vector){
        moveAt(vector, type.accel);
    }

    public void approach(Vec2 vector){
        vel.approachDelta(vector, type.accel * realSpeed());
    }

    public void aimLook(Position pos){
        aim(pos);
        lookAt(pos);
    }

    public void aimLook(float x, float y){
        aim(x, y);
        lookAt(x, y);
    }

    public boolean inRange(Position other){
        return within(other, type.range);
    }

    public boolean hasWeapons(){
        return type.hasWeapons();
    }

    public float speed(){
        float strafePenalty = isGrounded() || !isPlayer() ? 1f : Mathf.lerp(1f, type.strafePenalty, Angles.angleDist(vel().angle(), rotation) / 180f);
        //limit speed to minimum formation speed to preserve formation
        return (isCommanding() ? minFormationSpeed * 0.98f : type.speed) * strafePenalty;
    }

    /** @return speed with boost multipliers factored in. */
    public float realSpeed(){
        return Mathf.lerp(1f, type.canBoost ? type.boostMultiplier : 1f, elevation) * speed() * floorSpeedMultiplier();
    }

    /** Iterates through this unit and everything it is controlling. */
    public void eachGroup(Cons<Unit> cons){
        cons.get(self());
        controlling().each(cons);
    }

    /** @return where the unit wants to look at. */
    public float prefRotation(){
        if(activelyBuilding()){
            return angleTo(buildPlan());
        }else if(mineTile != null){
            return angleTo(mineTile);
        }else if(moving()){
            return vel().angle();
        }
        return rotation;
    }

    @Override
    public float range(){
        return type.maxRange;
    }

    @Replace
    public float clipSize(){
        if(isBuilding()){
            return state.rules.infiniteResources ? Float.MAX_VALUE : Math.max(type.clipSize, type.region.width) + buildingRange + tilesize*4f;
        }
        return Math.max(type.region.width * 2f, type.clipSize);
    }

    @Override
    public double sense(LAccess sensor){
        return switch(sensor){
            case totalItems -> stack().amount;
            case itemCapacity -> type.itemCapacity;
            case rotation -> rotation;
            case health -> health;
            case maxHealth -> maxHealth;
            case ammo -> !state.rules.unitAmmo ? type.ammoCapacity : ammo;
            case ammoCapacity -> type.ammoCapacity;
            case x -> World.conv(x);
            case y -> World.conv(y);
            case team -> team.id;
            case shooting -> isShooting() ? 1 : 0;
            case range -> range() / tilesize;
            case shootX -> World.conv(aimX());
            case shootY -> World.conv(aimY());
            case mining -> mining() ? 1 : 0;
            case mineX -> mining() ? mineTile.x : -1;
            case mineY -> mining() ? mineTile.y : -1;
            case flag -> flag;
            case controlled -> controller instanceof LogicAI || controller instanceof Player ? 1 : 0;
            case commanded -> controller instanceof FormationAI ? 1 : 0;
            case payloadCount -> self() instanceof Payloadc pay ? pay.payloads().size : 0;
            default -> 0;
        };
    }

    @Override
    public Object senseObject(LAccess sensor){
        return switch(sensor){
            case type -> type;
            case name -> controller instanceof Player p ? p.name : null;
            case firstItem -> stack().amount == 0 ? null : item();
            case payloadType -> self() instanceof Payloadc pay ?
                (pay.payloads().isEmpty() ? null :
                pay.payloads().peek() instanceof UnitPayload p1 ? p1.unit.type :
                pay.payloads().peek() instanceof BuildPayload p2 ? p2.block() : null) : null;
            default -> noSensed;
        };

    }

    @Override
    public double sense(Content content){
        if(content == stack().item) return stack().amount;
        return 0;
    }

    @Override
    @Replace
    public boolean canDrown(){
        return isGrounded() && !hovering && type.canDrown;
    }

    @Override
    @Replace
    public boolean canShoot(){
        //cannot shoot while boosting
        return !(type.canBoost && isFlying());
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
        controller(type.createController());
    }

    @Override
    public void set(UnitType def, UnitController controller){
        if(this.type != def){
            setType(def);
        }
        controller(controller);
    }

    /** @return pathfinder path type for calculating costs */
    public int pathType(){
        return Pathfinder.costGround;
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
        this.hovering = type.hovering;

        if(controller == null) controller(type.createController());
        if(mounts().length != type.weapons.size) setupWeapons(type);
        if(abilities.size != type.abilities.size){
            abilities = type.abilities.map(Ability::copy);
        }
    }

    @Override
    public void afterSync(){
        //set up type info after reading
        setType(this.type);
        controller.unit(self());
    }

    @Override
    public void afterRead(){
        afterSync();
        //reset controller state
        controller(type.createController());
    }

    @Override
    public void add(){
        team.data().updateCount(type, 1);

        //check if over unit cap
        if(count() > cap() && !spawnedByCore && !dead && !state.rules.editor){
            Call.unitCapDeath(self());
            team.data().updateCount(type, -1);
        }

    }

    @Override
    public void remove(){
        team.data().updateCount(type, -1);
        controller.removed(self());
    }

    @Override
    public void landed(){
        if(type.landShake > 0f){
            Effect.shake(type.landShake, type.landShake, this);
        }

        type.landed(self());
    }

    @Override
    public void update(){

        type.update(self());

        if(state.rules.unitAmmo && ammo < type.ammoCapacity - 0.0001f){
            resupplyTime += Time.delta;

            //resupply only at a fixed interval to prevent lag
            if(resupplyTime > 10f){
                type.ammoType.resupply(self());
                resupplyTime = 0f;
            }
        }

        if(abilities.size > 0){
            for(Ability a : abilities){
                a.update(self());
            }
        }

        drag = type.drag * (isGrounded() ? (floorOn().dragMultiplier) : 1f);

        //apply knockback based on spawns
        if(team != state.rules.waveTeam && state.hasSpawns() && (!net.client() || isLocal())){
            float relativeSize = state.rules.dropZoneRadius + hitSize/2f + 1f;
            for(Tile spawn : spawner.getSpawns()){
                if(within(spawn.worldx(), spawn.worldy(), relativeSize)){
                    vel().add(Tmp.v1.set(this).sub(spawn.worldx(), spawn.worldy()).setLength(0.1f + 1f - dst(spawn) / relativeSize).scl(0.45f * Time.delta));
                }
            }
        }

        //simulate falling down
        if(dead || health <= 0){
            //less drag when dead
            drag = 0.01f;

            //standard fall smoke
            if(Mathf.chanceDelta(0.1)){
                Tmp.v1.setToRandomDirection().scl(hitSize);
                type.fallEffect.at(x + Tmp.v1.x, y + Tmp.v1.y);
            }

            //thruster fall trail
            if(Mathf.chanceDelta(0.2)){
                float offset = type.engineOffset/2f + type.engineOffset/2f*elevation;
                float range = Mathf.range(type.engineSize);
                type.fallThrusterEffect.at(
                    x + Angles.trnsx(rotation + 180, offset) + Mathf.range(range),
                    y + Angles.trnsy(rotation + 180, offset) + Mathf.range(range),
                    Mathf.random()
                );
            }

            //move down
            elevation -= type.fallSpeed * Time.delta;

            if(isGrounded()){
                destroy();
            }
        }

        Tile tile = tileOn();
        Floor floor = floorOn();

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
        if(!net.client() && !dead){
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

    /** @return a preview icon for this unit. */
    public TextureRegion icon(){
        return type.icon(Cicon.full);
    }

    /** Actually destroys the unit, removing it and creating explosions. **/
    public void destroy(){
        if(!isAdded()) return;

        float explosiveness = 2f + item().explosiveness * stack().amount * 1.53f;
        float flammability = item().flammability * stack().amount / 1.9f;
        float power = item().charge * stack().amount * 150f;

        if(!spawnedByCore){
            Damage.dynamicExplosion(x, y, flammability, explosiveness, power, bounds() / 2f, state.rules.damageExplosions, item().flammability > 1, team);
        }

        float shake = hitSize / 3f;

        Effect.scorch(x, y, (int)(hitSize / 5));
        Fx.explosion.at(this);
        Effect.shake(shake, shake, this);
        type.deathSound.at(this);

        Events.fire(new UnitDestroyEvent(self()));

        if(explosiveness > 7f && (isLocal() || wasPlayer)){
            Events.fire(Trigger.suicideBomb);
        }

        //if this unit crash landed (was flying), damage stuff in a radius
        if(type.flying && !spawnedByCore){
            Damage.damage(team,x, y, Mathf.pow(hitSize, 0.94f) * 1.25f, Mathf.pow(hitSize, 0.75f) * type.crashDamageMultiplier * 5f, true, false, true);
        }

        if(!headless){
            for(int i = 0; i < type.wreckRegions.length; i++){
                if(type.wreckRegions[i].found()){
                    float range = type.hitSize /4f;
                    Tmp.v1.rnd(range);
                    Effect.decal(type.wreckRegions[i], x + Tmp.v1.x, y + Tmp.v1.y, rotation - 90);
                }
            }
        }

        remove();
    }

    /** @return name of direct or indirect player controller. */
    @Override
    public @Nullable String getControllerName(){
        if(isPlayer()) return getPlayer().name;
        if(controller instanceof LogicAI ai && ai.controller != null) return ai.controller.lastAccessed;
        if(controller instanceof FormationAI ai && ai.leader != null && ai.leader.isPlayer()) return ai.leader.getPlayer().name;
        return null;
    }

    @Override
    public void display(Table table){
        type.display(self(), table);
    }

    @Override
    public boolean isImmune(StatusEffect effect){
        return type.immunities.contains(effect);
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
        health = 0;
        dead = true;

        //don't waste time when the unit is already on the ground, just destroy it
        if(!type.flying){
            destroy();
        }
    }

    @Override
    @Replace
    public void kill(){
        if(dead || net.client()) return;

        //deaths are synced; this calls killed()
        Call.unitDeath(id);
    }
}
