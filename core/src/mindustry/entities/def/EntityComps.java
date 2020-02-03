package mindustry.entities.def;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.Bits;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.def.EntityComps.MinerComp.*;
import mindustry.entities.effect.*;
import mindustry.entities.traits.*;
import mindustry.entities.type.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.net.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.BuildBlock.*;

import java.io.*;

import static arc.math.Mathf.dst;
import static mindustry.Vars.*;

@SuppressWarnings("unused")
public class EntityComps{

    @Component({HealthComp.class, VelComp.class, StatusComp.class, TeamComp.class, ItemsComp.class})
    class UnitComp{
        UnitDef type;
    }

    class OwnerComp{
        Entityc owner;
    }

    @Component({TimedComp.class, DamageComp.class, HitboxComp.class})
    class BulletComp{
        BulletType bullet;

        float getDamage(){
            return bullet.damage;
        }

        void init(){
            //TODO
            bullet.init(null);
        }

        void remove(){
            //TODO
            bullet.despawned(null);
        }
    }

    @Component
    class DamageComp{
        native float getDamage();
    }

    @Component
    abstract class TimedComp extends EntityComp implements Scaled{
        float time, lifetime;

        void update(){
            time = Math.min(time + Time.delta(), lifetime);

            if(time >= lifetime){
                remove();
            }
        }

        @Override
        public float fin(){
            return time / lifetime;
        }
    }

    @Component
    class HealthComp{
        float health, maxHealth, hitTime;
        boolean dead;

        float healthf(){
            return health / maxHealth;
        }

        void update(){
            hitTime -= Time.delta() / 9f;
        }

        void killed(){
            //implement by other components
        }

        void kill(){
            health = 0;
            dead = true;
        }

        void heal(){
            dead = false;
            health = maxHealth;
        }

        boolean damaged(){
            return health <= maxHealth - 0.0001f;
        }

        void damage(float amount){
            health -= amount;
            if(health <= 0 && !dead){
                dead = true;
                killed();
            }
        }

        void clampHealth(){
            health = Mathf.clamp(health, 0, maxHealth);
        }

        void heal(float amount){
            health += amount;
            clampHealth();
        }
    }

    @Component
    class FlyingComp{
        boolean flying;
    }

    @Component
    class LegsComp{
        float baseRotation;
        float drownTime;
    }

    @Component
    class RotComp{
        float rotation;
    }

    @Component
    abstract class TeamComp{
        transient float x, y;

        Team team = Team.sharded;

        public @Nullable TileEntity getClosestCore(){
            return state.teams.closestCore(x, y, team);
        }
    }

    @Component({RotComp.class, PosComp.class})
    abstract static class WeaponsComp implements Teamc{
        transient float x, y, rotation;

        /** 1 */
        static final int[] one = {1};
        /** minimum cursor distance from player, fixes 'cross-eyed' shooting */
        static final float minAimDst = 20f;
        /** temporary weapon sequence number */
        static int sequenceNum = 0;

        /** weapon mount array, never null */
        WeaponMount[] mounts = {};

        void init(UnitDef def){
            mounts = new WeaponMount[def.weapons.size];
            for(int i = 0; i < mounts.length; i++){
                mounts[i] = new WeaponMount(def.weapons.get(i));
            }
        }

        /** Aim at something. This will make all mounts point at it. */
        void aim(Unit unit, float x, float y){
            Tmp.v1.set(x, y).sub(this.x, this.y);
            if(Tmp.v1.len() < minAimDst) Tmp.v1.setLength(minAimDst);

            x = Tmp.v1.x + this.x;
            y = Tmp.v1.y + this.y;

            for(WeaponMount mount : mounts){
                mount.aimX = x;
                mount.aimY = y;
            }
        }

        /** Update shooting and rotation for this unit. */
        void update(Unit unit){
            for(WeaponMount mount : mounts){
                Weapon weapon = mount.weapon;
                mount.reload -= Time.delta();

                float rotation = unit.rotation - 90;

                //rotate if applicable
                if(weapon.rotate){
                    float axisXOffset = weapon.mirror ? 0f : weapon.x;
                    float axisX = this.x + Angles.trnsx(rotation, axisXOffset, weapon.y),
                    axisY = this.y + Angles.trnsy(rotation, axisXOffset, weapon.y);

                    mount.rotation = Angles.moveToward(mount.rotation, Angles.angle(axisX, axisY, mount.aimX, mount.aimY), weapon.rotateSpeed);
                }

                //shoot if applicable
                //TODO only shoot if angle is reached, don't shoot inaccurately
                if(mount.reload <= 0){
                    for(int i : (weapon.mirror && !weapon.alternate ? Mathf.signs : one)){
                        i *= Mathf.sign(weapon.flipped) * Mathf.sign(mount.side);

                        //m a t h
                        float weaponRotation = rotation + (weapon.rotate ? mount.rotation : 0);
                        float mountX = this.x + Angles.trnsx(rotation, weapon.x * i, weapon.y),
                        mountY = this.y + Angles.trnsy(rotation, weapon.x * i, weapon.y);
                        float shootX = mountX + Angles.trnsx(weaponRotation, weapon.shootX * i, weapon.shootY),
                        shootY = mountY + Angles.trnsy(weaponRotation, weapon.shootX * i, weapon.shootY);
                        float shootAngle = weapon.rotate ? weaponRotation : Angles.angle(shootX, shootY, mount.aimX, mount.aimY);

                        shoot(weapon, shootX, shootY, shootAngle);
                    }

                    mount.side = !mount.side;
                    mount.reload = weapon.reload;
                }
            }
        }

        /** Draw weapon mounts. */
        void draw(){
            for(WeaponMount mount : mounts){
                Weapon weapon = mount.weapon;

                for(int i : (weapon.mirror ? Mathf.signs : one)){
                    i *= Mathf.sign(weapon.flipped);

                    float rotation = this.rotation - 90 + (weapon.rotate ? mount.rotation : 0);
                    float trY = weapon.y - (mount.reload / weapon.reload * weapon.recoil) * (weapon.alternate ? Mathf.num(i == Mathf.sign(mount.side)) : 1);
                    float width = i > 0 ? -weapon.region.getWidth() : weapon.region.getWidth();

                    Draw.rect(weapon.region,
                    x + Angles.trnsx(rotation, weapon.x * i, trY),
                    y + Angles.trnsy(rotation, weapon.x * i, trY),
                    width * Draw.scl,
                    weapon.region.getHeight() * Draw.scl,
                    rotation - 90);
                }
            }
        }

        private void shoot(Weapon weapon, float x, float y, float rotation){
            float baseX = this.x, baseY = this.y;

            weapon.shootSound.at(x, y, Mathf.random(0.8f, 1.0f));

            sequenceNum = 0;
            if(weapon.shotDelay > 0.01f){
                Angles.shotgun(weapon.shots, weapon.spacing, rotation, f -> {
                    Time.run(sequenceNum * weapon.shotDelay, () -> bullet(weapon, x + this.x - baseX, y + this.y - baseY, f + Mathf.range(weapon.inaccuracy)));
                    sequenceNum++;
                });
            }else{
                Angles.shotgun(weapon.shots, weapon.spacing, rotation, f -> bullet(weapon, x, y, f + Mathf.range(weapon.inaccuracy)));
            }

            BulletType ammo = weapon.bullet;

            Tmp.v1.trns(rotation + 180f, ammo.recoil);

            if(this instanceof Velc){
                //TODO apply force?
                ((Velc)this).getVel().add(Tmp.v1);
            }

            Tmp.v1.trns(rotation, 3f);
            boolean parentize = ammo.keepVelocity;

            Effects.shake(weapon.shake, weapon.shake, x, y);
            weapon.ejectEffect.at(x, y, rotation);
            ammo.shootEffect.at(x + Tmp.v1.x, y + Tmp.v1.y, rotation, parentize ? this : null);
            ammo.smokeEffect.at(x + Tmp.v1.x, y + Tmp.v1.y, rotation, parentize ? this : null);
        }

        private void bullet(Weapon weapon, float x, float y, float angle){
            Tmp.v1.trns(angle, 3f);
            Bullet.create(weapon.bullet, this, getTeam(), x + Tmp.v1.x, y + Tmp.v1.y, angle, (1f - weapon.velocityRnd) + Mathf.random(weapon.velocityRnd));
        }
    }

    @Component
    class DrawComp{
        //TODO ponder.

        native float drawSize();

        void draw(){

        }
    }

    @Component(PosComp.class)
    abstract class SyncComp extends PosComp{
        Interpolator interpolator = new Interpolator();

        void setNet(float x, float y){
            set(x, y);

            //TODO change interpolator API
            if(interpolator != null){
                interpolator.target.set(x, y);
                interpolator.last.set(x, y);
                interpolator.pos.set(0, 0);
                interpolator.updateSpacing = 16;
                interpolator.lastUpdated = 0;
            }
        }

        void update(){
            if(Vars.net.client() && !isLocal()){
                interpolate();
            }
        }

        void interpolate(){
            interpolator.update();
            x = interpolator.pos.x;
            y = interpolator.pos.y;
        }
    }

    @Component
    abstract class PosComp extends EntityComp implements Position{
        float x, y;

        void set(float x, float y){
            this.x = x;
            this.y = y;
        }

        void trns(float x, float y){
            set(this.x + x, this.y + y);
        }

        int tileX(){
            return Vars.world.toTile(getX());
        }

        int tileY(){
            return Vars.world.toTile(getY());
        }
    }

    @Component({ItemsComp.class, TeamComp.class, RotComp.class})
    static abstract class MinerComp implements Itemsc, Posc, Teamc, Rotc{
        static float miningRange = 70f;

        @Nullable Tile mineTile;

        native boolean canMine(Item item);

        native float getMiningSpeed();

        native boolean offloadImmediately();

        boolean isMining(){
            return mineTile != null;
        }

        void updateMining(){
            TileEntity core = getClosestCore();

            if(core != null && mineTile != null && mineTile.drop() != null && !acceptsItem(mineTile.drop()) && dst(core) < mineTransferRange){
                int accepted = core.tile.block().acceptStack(item(), getStack().amount, core.tile, unit);
                if(accepted > 0){
                    Call.transferItemTo(item(), accepted,
                    mineTile.worldx() + Mathf.range(tilesize / 2f),
                    mineTile.worldy() + Mathf.range(tilesize / 2f), core.tile);
                    clearItem();
                }
            }

            if(mineTile == null || core == null || mineTile.block() != Blocks.air || dst(mineTile.worldx(), mineTile.worldy()) > miningRange
            || mineTile.drop() == null || !acceptsItem(mineTile.drop()) || !canMine(mineTile.drop())){
                mineTile = null;
            }else{
                Item item = mineTile.drop();
                setRotation(Mathf.slerpDelta(getRotation(), angleTo(mineTile.worldx(), mineTile.worldy()), 0.4f));

                if(Mathf.chance(Time.delta() * (0.06 - item.hardness * 0.01) * getMiningSpeed())){

                    if(dst(core) < mineTransferRange && core.tile.block().acceptStack(item, 1, core.tile, unit) == 1 && offloadImmediately()){
                        Call.transferItemTo(item, 1,
                        mineTile.worldx() + Mathf.range(tilesize / 2f),
                        mineTile.worldy() + Mathf.range(tilesize / 2f), core.tile);
                    }else if(acceptsItem(item)){
                        //this is clientside, since items are synced anyway
                        ItemTransfer.transferItemToUnit(item,
                        mineTile.worldx() + Mathf.range(tilesize / 2f),
                        mineTile.worldy() + Mathf.range(tilesize / 2f),
                        unit);
                    }
                }

                if(Mathf.chance(0.06 * Time.delta())){
                    Fx.pulverizeSmall.at(mineTile.worldx() + Mathf.range(tilesize / 2f), mineTile.worldy() + Mathf.range(tilesize / 2f), 0f, item.color);
                }
            }
        }
    }

    @Component
    class BuilderComp{
        Queue<BuildRequest> requests = new Queue<>();
        float buildSpeed = 1f;
        //boolean building;

        void removeBuild(int x, int y, boolean breaking){
            //remove matching request
            int idx = player.buildQueue().indexOf(req -> req.breaking == breaking && req.x == x && req.y == y);
            if(idx != -1){
                player.buildQueue().removeIndex(idx);
            }
        }

        /** Return whether this builder's place queue contains items. */
        boolean isBuilding(){
            return requests.size != 0;
        }

        /** Clears the placement queue. */
        void clearBuilding(){
            requests.clear();
        }

        /** Add another build requests to the tail of the queue, if it doesn't exist there yet. */
        void addBuild(BuildRequest place){
            addBuild(place, true);
        }

        /** Add another build requests to the queue, if it doesn't exist there yet. */
        void addBuild(BuildRequest place, boolean tail){
            BuildRequest replace = null;
            for(BuildRequest request : requests){
                if(request.x == place.x && request.y == place.y){
                    replace = request;
                    break;
                }
            }
            if(replace != null){
                requests.remove(replace);
            }
            Tile tile = world.tile(place.x, place.y);
            if(tile != null && tile.entity instanceof BuildEntity){
                place.progress = tile.<BuildEntity>ent().progress;
            }
            if(tail){
                requests.addLast(place);
            }else{
                requests.addFirst(place);
            }
        }

        /** Return the build requests currently active, or the one at the top of the queue.*/
        @Nullable BuildRequest buildRequest(){
            return requests.size == 0 ? null : requests.first();
        }
    }

    @Component(DamageComp.class)
    class ShielderComp{

        void absorb(){

        }
    }

    @Component
    class ItemsComp{
        ItemStack stack = new ItemStack();

        native int getItemCapacity();

        public Item item(){
            return stack.item;
        }

        void clearItem(){
            stack.amount = 0;
        }

        boolean acceptsItem(Item item){
            return !hasItem() || item == stack.item && stack.amount + 1 <= getItemCapacity();
        }

        boolean hasItem(){
            return stack.amount > 0;
        }
    }

    @Component(VelComp.class)
    class MassComp{
        float mass;
    }

    @Component({PosComp.class, DrawComp.class, TimedComp.class})
    class EffectComp extends EntityComp{
        Effect effect;
        Color color = new Color(Color.white);
        Object data;
        float rotation = 0f;

        void draw(){

        }

        void update(){
            //TODO fix effects, make everything poolable
        }
    }

    @Component
    abstract class VelComp extends PosComp{
        final Vec2 vel = new Vec2();
        float drag = 0f;

        void update(){
            x += vel.x;
            y += vel.y;
            vel.scl(1f - drag * Time.delta());
        }
    }

    @Component(PosComp.class)
    class HitboxComp{
        transient float x, y;

        float hitSize;
        float lastX, lastY;

        void update(){

        }

        void updateLastPosition(){
            lastX = x;
            lastY = y;
        }

        void collision(Hitboxc other){

        }

        float getDeltaX(){
            return x - lastX;
        }

        float getDeltaY(){
            return y - lastY;
        }

        boolean collides(Hitboxc other){
            return Intersector.overlapsRect(x - hitSize/2f, y - hitSize/2f, hitSize, hitSize,
            other.getX() - other.getHitSize()/2f, other.getY() - other.getHitSize()/2f, other.getHitSize(), other.getHitSize());
        }
    }

    @Component(PosComp.class)
    class StatusComp{
        private Array<StatusEntry> statuses = new Array<>();
        private Bits applied = new Bits(content.getBy(ContentType.status).size);

        private float speedMultiplier;
        private float damageMultiplier;
        private float armorMultiplier;

        void apply(StatusEffect effect, float duration){
            if(effect == StatusEffects.none || effect == null || isImmune(effect)) return; //don't apply empty or immune effects

            if(statuses.size > 0){
                //check for opposite effects
                for(StatusEntry entry : statuses){
                    //extend effect
                    if(entry.effect == effect){
                        entry.time = Math.max(entry.time, duration);
                        return;
                    }else if(entry.effect.reactsWith(effect)){ //find opposite
                        StatusEntry.tmp.effect = entry.effect;
                        //TODO unit cannot be null here
                        entry.effect.getTransition(null, effect, entry.time, duration, StatusEntry.tmp);
                        entry.time = StatusEntry.tmp.time;

                        if(StatusEntry.tmp.effect != entry.effect){
                            entry.effect = StatusEntry.tmp.effect;
                        }

                        //stop looking when one is found
                        return;
                    }
                }
            }

            //otherwise, no opposites found, add direct effect
            StatusEntry entry = Pools.obtain(StatusEntry.class, StatusEntry::new);
            entry.set(effect, duration);
            statuses.add(entry);
        }

        boolean isImmune(StatusEffect effect){
            return false;
        }

        Color getStatusColor(){
            if(statuses.size == 0){
                return Tmp.c1.set(Color.white);
            }

            float r = 0f, g = 0f, b = 0f;
            for(StatusEntry entry : statuses){
                r += entry.effect.color.r;
                g += entry.effect.color.g;
                b += entry.effect.color.b;
            }
            return Tmp.c1.set(r / statuses.size, g / statuses.size, b / statuses.size, 1f);
        }

        void update(){
            applied.clear();
            speedMultiplier = damageMultiplier = armorMultiplier = 1f;

            if(statuses.isEmpty()) return;

            statuses.eachFilter(entry -> {
                entry.time = Math.max(entry.time - Time.delta(), 0);
                applied.set(entry.effect.id);

                if(entry.time <= 0){
                    Pools.free(entry);
                    return true;
                }else{
                    speedMultiplier *= entry.effect.speedMultiplier;
                    armorMultiplier *= entry.effect.armorMultiplier;
                    damageMultiplier *= entry.effect.damageMultiplier;
                    //TODO unit can't be null
                    entry.effect.update(null, entry.time);
                }

                return false;
            });
        }

        boolean hasEffect(StatusEffect effect){
            return applied.get(effect.id);
        }

        void writeSave(DataOutput stream) throws IOException{
            stream.writeByte(statuses.size);
            for(StatusEntry entry : statuses){
                stream.writeByte(entry.effect.id);
                stream.writeFloat(entry.time);
            }
        }

        void readSave(DataInput stream, byte version) throws IOException{
            for(StatusEntry effect : statuses){
                Pools.free(effect);
            }

            statuses.clear();

            byte amount = stream.readByte();
            for(int i = 0; i < amount; i++){
                byte id = stream.readByte();
                float time = stream.readFloat();
                StatusEntry entry = Pools.obtain(StatusEntry.class, StatusEntry::new);
                entry.set(content.getByID(ContentType.status, id), time);
                statuses.add(entry);
            }
        }
    }

    @Component
    @BaseComponent
    class EntityComp{
        int id;

        void init(){}

        void update(){}

        void remove(){

        }

        void add(){

        }

        boolean isLocal(){
            //TODO fix
            return this == (Object)Vars.player;
        }

        <T> T as(Class<T> type){
            return (T)this;
        }
    }
}
