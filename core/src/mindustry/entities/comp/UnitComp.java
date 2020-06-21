package mindustry.entities.comp;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

@Component
abstract class UnitComp implements Healthc, Physicsc, Hitboxc, Statusc, Teamc, Itemsc, Rotc, Unitc, Weaponsc, Drawc, Boundedc, Syncc, Shieldc, Displayable{

    @Import float x, y, rotation, elevation, maxHealth, drag, armor;

    private UnitController controller;
    private UnitType type;
    boolean spawnedByCore;

    public void moveAt(Vec2 vector){
        moveAt(vector, type.accel);
    }

    public void aimLook(Position pos){
        aim(pos);
        lookAt(pos);
    }

    public void aimLook(float x, float y){
        aim(x, y);
        lookAt(x, y);
    }

    public boolean hasWeapons(){
        return type.hasWeapons();
    }

    @Replace
    public float clipSize(){
        return type.region.getWidth() * 2f;
    }

    @Override
    public int itemCapacity(){
        return type.itemCapacity;
    }

    @Override
    public float bounds(){
        return hitSize() *  2f;
    }

    @Override
    public void controller(UnitController next){
        this.controller = next;
        if(controller.unit() != this) controller.unit(this);
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
        type(type);
        controller(controller);
    }

    @Override
    public void type(UnitType type){
        this.type = type;
        this.maxHealth = type.health;
        this.drag = type.drag;
        this.elevation = type.flying ? 1f : type.baseElevation;
        this.armor = type.armor;

        heal();
        hitSize(type.hitsize);
        controller(type.createController());
        setupWeapons(type);
    }

    @Override
    public UnitType type(){
        return type;
    }

    public void lookAt(float angle){
        rotation = Angles.moveToward(rotation, angle, type.rotateSpeed * Time.delta());
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

    @Override
    public void afterRead(){
        //set up type info after reading
        type(this.type);
    }

    @Override
    public void add(){
        teamIndex.updateCount(team(), 1);
    }

    @Override
    public void remove(){
        teamIndex.updateCount(team(), -1);
        controller.removed(this);
    }

    @Override
    public void landed(){
        if(type.landShake > 0f){
            Effects.shake(type.landShake, type.landShake, this);
        }

        type.landed(this);
    }

    @Override
    public void update(){
        type.update(this);

        drag(type.drag * (isGrounded() ? (floorOn().dragMultiplier) : 1f));

        //apply knockback based on spawns
        if(team() != state.rules.waveTeam){
            float relativeSize = state.rules.dropZoneRadius + bounds()/2f + 1f;
            for(Tile spawn : spawner.getSpawns()){
                if(within(spawn.worldx(), spawn.worldy(), relativeSize)){
                    vel().add(Tmp.v1.set(this).sub(spawn.worldx(), spawn.worldy()).setLength(0.1f + 1f - dst(spawn) / relativeSize).scl(0.45f * Time.delta()));
                }
            }
        }

        Tile tile = tileOn();
        Floor floor = floorOn();

        if(tile != null && isGrounded()){
            //unit block update
            if(tile.entity != null){
                tile.entity.unitOn(this);
            }

            //kill when stuck in wall
            if(tile.solid()){
                kill();
            }

            //apply damage
            if(floor.damageTaken > 0f){
                damageContinuous(floor.damageTaken);
            }
        }

        controller.update();

        //remove units spawned by the core
        if(spawnedByCore && !isPlayer()){
            Fx.unitDespawn.at(x, y, 0, this);
            remove();
        }
    }

    @Override
    public void display(Table table){
        type.display(this, table);
    }

    @Override
    public boolean isImmune(StatusEffect effect){
        return type.immunities.contains(effect);
    }

    @Override
    public void draw(){
        type.draw(this);
    }

    @Override
    public boolean isPlayer(){
        return controller instanceof Playerc;
    }

    @Nullable
    public Playerc getPlayer(){
        return isPlayer() ? (Playerc)controller : null;
    }

    @Override
    public void killed(){
        float explosiveness = 2f + item().explosiveness * stack().amount;
        float flammability = item().flammability * stack().amount;
        Damage.dynamicExplosion(x, y, flammability, explosiveness, 0f, bounds() / 2f, Pal.darkFlame);

        Effects.scorch(x, y, (int)(hitSize() / 5));
        Fx.explosion.at(this);
        Effects.shake(2f, 2f, this);
        type.deathSound.at(this);

        Events.fire(new UnitDestroyEvent(this));

        if(explosiveness > 7f && isLocal()){
            Events.fire(Trigger.suicideBomb);
        }
    }

    @Override
    public boolean canMine(Item item){
        return type.drillTier >= item.hardness;
    }

    @Override
    public float miningSpeed(){
        return type.mineSpeed;
    }

    @Override
    public boolean offloadImmediately(){
        return false;
    }
}
