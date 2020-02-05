package mindustry.entities.def;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.math.geom.QuadTree.*;
import arc.scene.ui.layout.*;
import arc.struct.Bits;
import arc.struct.Queue;
import arc.struct.*;
import arc.util.*;
import arc.util.ArcAnnotate.*;
import arc.util.pooling.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.net.Administration.*;
import mindustry.net.*;
import mindustry.net.Packets.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.BuildBlock.*;
import mindustry.world.consumers.*;
import mindustry.world.modules.*;

import java.io.*;
import java.util.*;

import static mindustry.Vars.*;

@SuppressWarnings({"unused", "unchecked"})
public class EntityComps{

    @Component
    abstract class UnitComp implements Healthc, Velc, Statusc, Teamc, Itemsc, Hitboxc, Rotc, Massc, Unitc, Weaponsc{
        private UnitController controller;
        private UnitDef type;

        @Override
        public int itemCapacity(){
            return type.itemCapacity;
        }

        @Override
        public float bounds(){
            return hitSize() *  2f;
        }

        @Override
        public void controller(UnitController controller){
            this.controller = controller;
            controller.unit(this);
        }

        @Override
        public UnitController controller(){
            return controller;
        }

        @Override
        public void set(UnitDef def, UnitController controller){
            type(type);
            controller(controller);
        }

        @Override
        public void type(UnitDef type){
            this.type = type;
            setupWeapons(type);
        }

        @Override
        public UnitDef type(){
            return type;
        }

        @Override
        public void update(){
            //apply knockback based on spawns
            //TODO move elsewhere
            if(team() != state.rules.waveTeam){
                float relativeSize = state.rules.dropZoneRadius + bounds()/2f + 1f;
                for(Tile spawn : spawner.getGroundSpawns()){
                    if(withinDst(spawn.worldx(), spawn.worldy(), relativeSize)){
                        vel().add(Tmp.v1.set(this).sub(spawn.worldx(), spawn.worldy()).setLength(0.1f + 1f - dst(spawn) / relativeSize).scl(0.45f * Time.delta()));
                    }
                }
            }

            Tile tile = tileOn();
            Floor floor = floorOn();

            if(tile != null){
                //unit block update
                tile.block().unitOn(tile, this);

                //apply damage
                if(floor.damageTaken > 0f){
                    damageContinuous(floor.damageTaken);
                }
            }
        }

        @Override
        public void drawLight(){
            //TODO move
            if(type.lightRadius > 0){
                renderer.lights.add(getX(), getY(), type.lightRadius, type.lightColor, 0.6f);
            }
        }

        @Override
        public void draw(){
            //draw power cell - TODO move
            Draw.color(Color.black, team().color, healthf() + Mathf.absin(Time.time(), Math.max(healthf() * 5f, 1f), 1f - healthf()));
            Draw.rect(type.cellRegion, getX(), getY(), rotation() - 90);
            Draw.color();
        }

        @Override
        public void killed(){
            float explosiveness = 2f + item().explosiveness * stack().amount;
            float flammability = item().flammability * stack().amount;
            Damage.dynamicExplosion(getX(), getY(), flammability, explosiveness, 0f, bounds() / 2f, Pal.darkFlame);

            //TODO cleanup
            //ScorchDecal.create(getX(), getY());
            Fx.explosion.at(this);
            Effects.shake(2f, 2f, this);

            Sounds.bang.at(this);
            Events.fire(new UnitDestroyEvent(this));

            //TODO implement suicide bomb trigger
            //if(explosiveness > 7f && this == player){
            //    Events.fire(Trigger.suicideBomb);
            //}
        }
    }

    @Component
    class OwnerComp{
        Entityc owner;
    }

    @Component
    abstract class ChildComp implements Posc{
        transient float x, y;

        private @Nullable Posc parent;
        private float offsetX, offsetY;

        @Override
        public void add(){
            if(parent != null){
                offsetX = x - parent.getX();
                offsetY = y - parent.getY();
            }
        }

        @Override
        public void update(){
            if(parent != null){
                x = parent.getX() + offsetX;
                y = parent.getY() + offsetY;
            }
        }
    }

    @Component
    abstract class BulletComp implements Timedc, Damagec, Hitboxc, Teamc, Posc, Drawc, Shielderc, Ownerc, Velc, Bulletc, Timerc{
        private float lifeScl;

        Object data;
        BulletType type;
        float damage;

        @Override
        public void add(){
            type.init(this);

            drag(type.drag);
            hitSize(type.hitSize);
            lifetime(lifeScl * type.lifetime);
        }

        @Override
        public void remove(){
            type.despawned(this);
        }

        @Override
        public float getLifetime(){
            return type.lifetime;
        }

        @Override
        public float damageMultiplier(){
            if(owner() instanceof Unitc){
                return ((Unitc)owner()).damageMultiplier();
            }
            return 1f;
        }

        @Override
        public void absorb(){
            //TODO
            remove();
        }

        @Override
        public float clipSize(){
            return type.drawSize;
        }

        @Override
        public float damage(){
            return type.damage * damageMultiplier();
        }

        @Override
        public void collision(Hitboxc other, float x, float y){
            if(!type.pierce) remove();
            type.hit(this, x, y);

            if(other instanceof Unitc){
                Unitc unit = (Unitc)other;
                unit.vel().add(Tmp.v3.set(other.x(), other.y()).sub(x, y).setLength(type.knockback / unit.mass()));
                unit.apply(type.status, type.statusDuration);
            }
        }

        @Override
        public void update(){
            type.update(this);

            if(type.hitTiles){
                world.raycastEach(world.toTile(lastX()), world.toTile(lastY()), tileX(), tileY(), (x, y) -> {

                    Tile tile = world.ltile(x, y);
                    if(tile == null) return false;

                    if(tile.entity != null && tile.entity.collide(this) && type.collides(this, tile) && !tile.entity.dead() && (type.collidesTeam || tile.team() != team())){
                        if(tile.team() != team()){
                            tile.entity.collision(this);
                        }

                        type.hitTile(this, tile);
                        remove();
                        return true;
                    }

                    return false;
                });
            }
        }

        @Override
        public void draw(){
            type.draw(this);
            //TODO refactor
            renderer.lights.add(x(), y(), 16f, Pal.powerLight, 0.3f);
        }

        /** Sets the bullet's rotation in degrees. */
        @Override
        public void rotation(float angle){
            vel().setAngle(angle);
        }

        /** @return the bullet's rotation. */
        @Override
        public float rotation(){
            float angle = Mathf.atan2(vel().x, vel().y) * Mathf.radiansToDegrees;
            if(angle < 0) angle += 360;
            return angle;
        }
    }

    @Component
    abstract class TimerComp{
        Interval timer = new Interval(6);

        public boolean timer(int index, float time){
            return timer.get(index, time);
        }
    }

    @Component
    abstract class DamageComp{
        abstract float damage();
    }

    @Component
    abstract class TimedComp implements Entityc, Scaled{
        float time, lifetime;

        @Override
        public void update(){
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
    abstract class HealthComp implements Entityc{
        static final float hitDuration = 9f;

        float health, maxHealth, hitTime;
        boolean dead;

        boolean isValid(){
            return !dead && isAdded();
        }

        float healthf(){
            return health / maxHealth;
        }

        @Override
        public void update(){
            hitTime -= Time.delta() / hitDuration;
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

        void damage(float amount, boolean withEffect){
            float pre = hitTime;

            damage(amount);

            if(!withEffect){
                hitTime = pre;
            }
        }

        void damageContinuous(float amount){
            damage(amount * Time.delta(), hitTime <= -20 + hitDuration);
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
    abstract class FlyingComp implements Posc, Velc, Healthc{
        transient float x, y;
        transient Vec2 vel;

        float elevation;
        float drownTime;

        boolean isGrounded(){
            return elevation < 0.001f;
        }

        @Override
        public void update(){
            Floor floor = floorOn();

            if(isGrounded() && floor.isLiquid && vel.len2() > 0.4f*0.4f && Mathf.chance((vel.len2() * floor.speedMultiplier) * 0.03f * Time.delta())){
                floor.walkEffect.at(x, y, 0, floor.color);
            }

            if(isGrounded() && floor.isLiquid && floor.drownTime > 0){
                drownTime += Time.delta() * 1f / floor.drownTime;
                drownTime = Mathf.clamp(drownTime);
                if(Mathf.chance(Time.delta() * 0.05f)){
                    floor.drownUpdateEffect.at(x, y, 0f, floor.color);
                }

                //TODO is the netClient check necessary?
                if(drownTime >= 0.999f && !net.client()){
                    kill();
                    //TODO drown event!
                }
            }else{
                drownTime = Mathf.lerpDelta(drownTime, 0f, 0.03f);
            }
        }
    }

    @Component
    abstract class LegsComp implements Posc, Flyingc{
        float baseRotation;
    }

    @Component
    abstract class RotComp implements Entityc{
        float rotation;

        void interpolate(){
            Syncc sync = as(Syncc.class);

            if(sync.interpolator().values.length > 0){
                rotation = sync.interpolator().values[0];
            }
        }
    }
    
    @Component
    static abstract class TileComp implements Posc, Teamc, Healthc, Tilec, Timerc{
        static final float timeToSleep = 60f * 1;
        static final ObjectSet<Tile> tmpTiles = new ObjectSet<>();
        static int sleepingEntities = 0;

        Tile tile;
        Block block;
        Array<Tile> proximity = new Array<>(8);

        PowerModule power;
        ItemModule items;
        LiquidModule liquids;
        ConsumeModule cons;

        private float timeScale = 1f, timeScaleDuration;

        private @Nullable SoundLoop sound;

        private boolean sleeping;
        private float sleepTime;

        /** Sets this tile entity data to this tile, and adds it if necessary. */
        @Override
        public Tilec init(Tile tile, boolean shouldAdd){
            this.tile = tile;
            this.block = tile.block();

            set(tile.drawx(), tile.drawy());
            if(block.activeSound != Sounds.none){
                sound = new SoundLoop(block.activeSound, block.activeSoundVolume);
            }

            health(block.health);
            maxHealth(block.health);
            timer(new Interval(block.timers));

            if(shouldAdd){
                add();
            }

            return this;
        }

        @Override
        public void applyBoost(float intensity, float  duration){
            timeScale = Math.max(timeScale, intensity);
            timeScaleDuration = Math.max(timeScaleDuration, duration);
        }

        @Override
        public float timeScale(){
            return timeScale;
        }

        @Override
        public boolean consValid(){
            return cons.valid();
        }

        @Override
        public void consume(){
            cons.trigger();
        }

        /** Scaled delta. */
        @Override
        public float delta(){
            return Time.delta() * timeScale;
        }

        /** Base efficiency. If this entity has non-buffered power, returns the power %, otherwise returns 1. */
        @Override
        public float efficiency(){
            return power != null && (block.consumes.has(ConsumeType.power) && !block.consumes.getPower().buffered) ? power.status : 1f;
        }

        /** Call when nothing is happening to the entity. This increments the internal sleep timer. */
        @Override
        public void sleep(){
            sleepTime += Time.delta();
            if(!sleeping && sleepTime >= timeToSleep){
                remove();
                sleeping = true;
                sleepingEntities++;
            }
        }

        /** Call when this entity is updating. This wakes it up. */
        @Override
        public void noSleep(){
            sleepTime = 0f;
            if(sleeping){
                add();
                sleeping = false;
                sleepingEntities--;
            }
        }

        /** Returns the version of this TileEntity IO code.*/
        @Override
        public byte version(){
            return 0;
        }

        @Override
        public boolean collide(Bulletc other){
            return true;
        }

        @Override
        public void collision(Bulletc other){
            block.handleBulletHit(this, other);
        }

        //TODO Implement damage!

        @Override
        public void removeFromProximity(){
            block.onProximityRemoved(tile);

            Point2[] nearby = Edges.getEdges(block.size);
            for(Point2 point : nearby){
                Tile other = world.ltile(tile.x + point.x, tile.y + point.y);
                //remove this tile from all nearby tile's proximities
                if(other != null){
                    other.block().onProximityUpdate(other);

                    if(other.entity != null){
                        other.entity.proximity().remove(tile, true);
                    }
                }
            }
        }

        @Override
        public void updateProximity(){
            tmpTiles.clear();
            proximity.clear();

            Point2[] nearby = Edges.getEdges(block.size);
            for(Point2 point : nearby){
                Tile other = world.ltile(tile.x + point.x, tile.y + point.y);

                if(other == null) continue;
                if(other.entity == null || !(other.interactable(tile.team()))) continue;

                //add this tile to proximity of nearby tiles
                if(!other.entity.proximity().contains(tile, true)){
                    other.entity.proximity().add(tile);
                }

                tmpTiles.add(other);
            }

            //using a set to prevent duplicates
            for(Tile tile : tmpTiles){
                proximity.add(tile);
            }

            block.onProximityAdded(tile);
            block.onProximityUpdate(tile);

            for(Tile other : tmpTiles){
                other.block().onProximityUpdate(other);
            }
        }

        @Override
        public Array<Tile> proximity(){
            return proximity;
        }

        /** Tile configuration. Defaults to 0. Used for block rebuilding. */
        @Override
        public int config(){
            return 0;
        }

        @Override
        public void remove(){
            if(sound != null){
                sound.stop();
            }
        }

        @Override
        public void killed(){
            Events.fire(new BlockDestroyEvent(tile));
            block.breakSound.at(tile);
            block.onDestroyed(tile);
            tile.remove();
        }

        @Override
        public void update(){
            timeScaleDuration -= Time.delta();
            if(timeScaleDuration <= 0f || !block.canOverdrive){
                timeScale = 1f;
            }

            if(sound != null){
                sound.update(x(), y(), block.shouldActiveSound(tile));
            }

            if(block.idleSound != Sounds.none && block.shouldIdleSound(tile)){
                loops.play(block.idleSound, this, block.idleSoundVolume);
            }

            block.update(tile);

            if(liquids != null){
                liquids.update();
            }

            if(cons != null){
                cons.update();
            }

            if(power != null){
                power.graph.update();
            }
        }
    }

    @Component
    abstract static class PlayerComp implements UnitController, Entityc, Syncc, Timerc{
        private static final Unitc noUnit = GenericUnitEntity.create();

        @NonNull @ReadOnly Unitc unit = noUnit;

        @ReadOnly Team team = Team.sharded;
        String name = "noname";
        @Nullable NetConnection con;
        boolean admin, typing;
        Color color = new Color();
        float mouseX, mouseY;

        @Nullable String lastText;
        float textFadeTime;

        public @Nullable Tilec closestCore(){
            return state.teams.closestCore(x(), y(), team);
        }

        public void reset(){
            team = state.rules.defaultTeam;
            admin = typing = false;
            lastText = null;
            textFadeTime = 0f;
            unit.controller(unit.type().createController());
            unit = null;
        }

        public void update(){
            if(!dead()){
                x(unit.x());
                y(unit.y());
                unit.team(team);
            }
            textFadeTime -= Time.delta() / (60 * 5);
        }

        public void team(Team team){
            if(unit != null){
                unit.team(team);
            }
        }

        public void clearUnit(){
            unit(noUnit);
        }

        public Unitc unit(){
            if(dead()){
                //TODO remove
                Log.err("WARNING: DEAD PLAYER UNIT ACCESSED");
                new RuntimeException().printStackTrace();
            }
            return unit;
        }

        public void unit(Unitc unit){
            if(unit == null) throw new IllegalArgumentException("Unit cannot be null. Use clearUnit() instead.");
            this.unit = unit;
            if(unit != noUnit){
                unit.team(team);
            }
        }

        boolean dead(){
            return unit == noUnit;
        }

        String uuid(){
            return con == null ? "[LOCAL]" : con.uuid;
        }

        String usid(){
            return con == null ? "[LOCAL]" : con.usid;
        }

        void kick(KickReason reason){
            con.kick(reason);
        }

        void kick(String reason){
            con.kick(reason);
        }

        void drawName(){
            BitmapFont font = Fonts.def;
            GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
            final float nameHeight = 11;
            final float textHeight = 15;

            boolean ints = font.usesIntegerPositions();
            font.setUseIntegerPositions(false);
            font.getData().setScale(0.25f / Scl.scl(1f));
            layout.setText(font, name);

            if(!isLocal()){
                Draw.color(0f, 0f, 0f, 0.3f);
                Fill.rect(unit.x(), unit.y() + nameHeight - layout.height / 2, layout.width + 2, layout.height + 3);
                Draw.color();
                font.setColor(color);
                font.draw(name, unit.x(), unit.y() + nameHeight, 0, Align.center, false);

                if(admin){
                    float s = 3f;
                    Draw.color(color.r * 0.5f, color.g * 0.5f, color.b * 0.5f, 1f);
                    Draw.rect(Icon.adminSmall.getRegion(), unit.x() + layout.width / 2f + 2 + 1, unit.y() + nameHeight - 1.5f, s, s);
                    Draw.color(color);
                    Draw.rect(Icon.adminSmall.getRegion(), unit.x() + layout.width / 2f + 2 + 1, unit.y() + nameHeight - 1f, s, s);
                }
            }

            if(Core.settings.getBool("playerchat") && ((textFadeTime > 0 && lastText != null) || typing)){
                String text = textFadeTime <= 0 || lastText == null ? "[LIGHT_GRAY]" + Strings.animated(Time.time(), 4, 15f, ".") : lastText;
                float width = 100f;
                float visualFadeTime = 1f - Mathf.curve(1f - textFadeTime, 0.9f);
                font.setColor(1f, 1f, 1f, textFadeTime <= 0 || lastText == null ? 1f : visualFadeTime);

                layout.setText(font, text, Color.white, width, Align.bottom, true);

                Draw.color(0f, 0f, 0f, 0.3f * (textFadeTime <= 0 || lastText == null  ? 1f : visualFadeTime));
                Fill.rect(unit.x(), unit.y() + textHeight + layout.height - layout.height/2f, layout.width + 2, layout.height + 3);
                font.draw(text, unit.x() - width/2f, unit.y() + textHeight + layout.height, width, Align.center, true);
            }

            Draw.reset();
            Pools.free(layout);
            font.getData().setScale(1f);
            font.setColor(Color.white);
            font.setUseIntegerPositions(ints);
        }

        void sendMessage(String text){
            if(isLocal()){
                if(ui != null){
                    ui.chatfrag.addMessage(text, null);
                }
            }else{
                Call.sendMessage(con, text, null, null);
            }
        }

        void sendMessage(String text, Playerc from){
            sendMessage(text, from, NetClient.colorizeName(from.id(), from.name()));
        }

         void sendMessage(String text, Playerc from, String fromName){
            if(isLocal()){
                if(ui != null){
                    ui.chatfrag.addMessage(text, fromName);
                }
            }else{
                Call.sendMessage(con, text, fromName, from);
            }
        }

        PlayerInfo getInfo(){
            if(isLocal()){
                throw new IllegalArgumentException("Local players cannot be traced and do not have info.");
            }else{
                return netServer.admins.getInfo(uuid());
            }
        }
    }

    @Component
    abstract class TeamComp implements Posc{
        transient float x, y;

        Team team = Team.sharded;

        public @Nullable Tilec closestCore(){
            return state.teams.closestCore(x, y, team);
        }
    }

    @Component
    abstract static class WeaponsComp implements Teamc, Posc, Rotc{
        transient float x, y, rotation;

        /** 1 */
        static final int[] one = {1};
        /** minimum cursor distance from player, fixes 'cross-eyed' shooting */
        static final float minAimDst = 20f;
        /** temporary weapon sequence number */
        static int sequenceNum = 0;

        /** weapon mount array, never null */
        @ReadOnly WeaponMount[] mounts = {};

        void setupWeapons(UnitDef def){
            mounts = new WeaponMount[def.weapons.size];
            for(int i = 0; i < mounts.length; i++){
                mounts[i] = new WeaponMount(def.weapons.get(i));
            }
        }

        /** Aim at something. This will make all mounts point at it. */
        void aim(Unitc unit, float x, float y){
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
        @Override
        public void update(){
            for(WeaponMount mount : mounts){
                Weapon weapon = mount.weapon;
                mount.reload -= Time.delta();

                float rotation = this.rotation - 90;

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
                ((Velc)this).vel().add(Tmp.v1);
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
            weapon.bullet.create(this, team(), x + Tmp.v1.x, y + Tmp.v1.y, angle, (1f - weapon.velocityRnd) + Mathf.random(weapon.velocityRnd));
        }
    }

    @Component
    abstract static class DrawShadowComp implements Drawc, Rotc, Flyingc{
        static final float shadowTX = -12, shadowTY = -13, shadowColor = Color.toFloatBits(0, 0, 0, 0.22f);

        transient float x, y, rotation;

        abstract TextureRegion getShadowRegion();

        void drawShadow(){
            if(!isGrounded()){
                Draw.color(shadowColor);
                Draw.rect(getShadowRegion(), x + shadowTX * elevation(), y + shadowTY * elevation(), rotation - 90);
                Draw.color();
            }
        }
    }

    @Component
    abstract class DrawItemsComp implements Drawc, Itemsc, Posc, Rotc{
        transient float x, y, rotation;

        float itemTime;

        //drawn after base
        @Override
        @MethodPriority(3)
        public void draw(){
            boolean number = isLocal();
            itemTime = Mathf.lerpDelta(itemTime, Mathf.num(hasItem()), 0.05f);

            //draw back items
            if(itemTime > 0.01f){
                float backTrns = 5f;
                float size = (itemSize + Mathf.absin(Time.time(), 5f, 1f)) * itemTime;

                Draw.mixcol(Pal.accent, Mathf.absin(Time.time(), 5f, 0.5f));
                Draw.rect(item().icon(Cicon.medium),
                x + Angles.trnsx(rotation + 180f, backTrns),
                y + Angles.trnsy(rotation + 180f, backTrns),
                size, size, rotation);

                Draw.mixcol();

                Lines.stroke(1f, Pal.accent);
                Lines.circle(
                x + Angles.trnsx(rotation + 180f, backTrns),
                y + Angles.trnsy(rotation + 180f, backTrns),
                (3f + Mathf.absin(Time.time(), 5f, 1f)) * itemTime);

                if(isLocal()){
                    Fonts.outline.draw(stack().amount + "",
                    x + Angles.trnsx(rotation + 180f, backTrns),
                    y + Angles.trnsy(rotation + 180f, backTrns) - 3,
                    Pal.accent, 0.25f * itemTime / Scl.scl(1f), false, Align.center
                    );
                }

                Draw.reset();
            }
        }
    }

    @Component
    abstract class DecalComp implements Drawc, Timedc, Rotc, Posc{
        Color color = new Color(1, 1, 1, 1);
        TextureRegion region;

        @Override
        public void draw(){
            Draw.color(color);
            Draw.rect(region, x(), y(), rotation());
            Draw.color();
        }

        @Override
        public float clipSize(){
            return region.getWidth()*2;
        }

    }

    @Component
    abstract class DrawLightComp implements Drawc{
        void drawLight(){}
    }

    @Component
    abstract class DrawOverComp implements Drawc{
        void drawOver(){}
    }

    @Component
    abstract class DrawUnderComp implements Drawc{
        void drawUnder(){}
    }

    @Component
    abstract class DrawComp{

        abstract float clipSize();

        void draw(){

        }
    }

    @Component
    abstract class SyncComp implements Posc{
        transient float x, y;

        Interpolator interpolator = new Interpolator();

        void setNet(float x, float y){
            set(x, y);

            //TODO change interpolator API
            interpolator.target.set(x, y);
            interpolator.last.set(x, y);
            interpolator.pos.set(0, 0);
            interpolator.updateSpacing = 16;
            interpolator.lastUpdated = 0;
        }

        @Override
        public void update(){
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
    abstract class BoundedComp implements Velc, Posc, Healthc, Flyingc{
        static final float warpDst = 180f;

        transient float x, y;
        transient Vec2 vel;

        @Override
        public void update(){
            //repel unit out of bounds
            if(x < 0) vel.x += (-x/warpDst);
            if(y < 0) vel.y += (-y/warpDst);
            if(x > world.unitWidth()) vel.x -= (x - world.unitWidth())/warpDst;
            if(y > world.unitHeight()) vel.y -= (y - world.unitHeight())/warpDst;

            //clamp position if not flying
            if(isGrounded()){
                x = Mathf.clamp(x, 0, world.width() * tilesize - tilesize);
                y = Mathf.clamp(y, 0, world.height() * tilesize - tilesize);
            }

            //kill when out of bounds
            if(x < -finalWorldBounds || y < -finalWorldBounds || x >= world.width() * tilesize + finalWorldBounds || y >= world.height() * tilesize + finalWorldBounds){
                kill();
            }
        }
    }

    @Component
    abstract class PosComp implements Position{
        float x, y;

        void set(float x, float y){
            this.x = x;
            this.y = y;
        }

        void trns(float x, float y){
            set(this.x + x, this.y + y);
        }

        int tileX(){
            return Vars.world.toTile(x);
        }

        int tileY(){
            return Vars.world.toTile(y);
        }

        /** Returns air if this unit is on a non-air top block. */
        public Floor floorOn(){
            Tile tile = tileOn();
            return tile == null || tile.block() != Blocks.air ? (Floor)Blocks.air : tile.floor();
        }

        public @Nullable Tile tileOn(){
            return world.tileWorld(x, y);
        }

        @Override
        public float getX(){
            return x;
        }

        @Override
        public float getY(){
            return y;
        }
    }

    @Component
    static abstract class MinerComp implements Itemsc, Posc, Teamc, Rotc{
        static float miningRange = 70f;

        transient float x, y, rotation;

        @Nullable Tile mineTile;

        abstract boolean canMine(Item item);

        abstract float miningSpeed();

        abstract boolean offloadImmediately();

        boolean mining(){
            return mineTile != null;
        }

        void updateMining(){
            Tilec core = closestCore();

            if(core != null && mineTile != null && mineTile.drop() != null && !acceptsItem(mineTile.drop()) && dst(core) < mineTransferRange){
                int accepted = core.tile().block().acceptStack(item(), stack().amount, core.tile(), this);
                if(accepted > 0){
                    Call.transferItemTo(item(), accepted,
                    mineTile.worldx() + Mathf.range(tilesize / 2f),
                    mineTile.worldy() + Mathf.range(tilesize / 2f), core.tile());
                    clearItem();
                }
            }

            if(mineTile == null || core == null || mineTile.block() != Blocks.air || dst(mineTile.worldx(), mineTile.worldy()) > miningRange
            || mineTile.drop() == null || !acceptsItem(mineTile.drop()) || !canMine(mineTile.drop())){
                mineTile = null;
            }else{
                Item item = mineTile.drop();
                rotation(Mathf.slerpDelta(rotation(), angleTo(mineTile.worldx(), mineTile.worldy()), 0.4f));

                if(Mathf.chance(Time.delta() * (0.06 - item.hardness * 0.01) * miningSpeed())){

                    if(dst(core) < mineTransferRange && core.tile().block().acceptStack(item, 1, core.tile(), this) == 1 && offloadImmediately()){
                        Call.transferItemTo(item, 1,
                        mineTile.worldx() + Mathf.range(tilesize / 2f),
                        mineTile.worldy() + Mathf.range(tilesize / 2f), core.tile());
                    }else if(acceptsItem(item)){
                        //this is clientside, since items are synced anyway
                        InputHandler.transferItemToUnit(item,
                        mineTile.worldx() + Mathf.range(tilesize / 2f),
                        mineTile.worldy() + Mathf.range(tilesize / 2f),
                        this);
                    }
                }

                if(Mathf.chance(0.06 * Time.delta())){
                    Fx.pulverizeSmall.at(mineTile.worldx() + Mathf.range(tilesize / 2f), mineTile.worldy() + Mathf.range(tilesize / 2f), 0f, item.color);
                }
            }
        }

        void drawOver(){
            if(!mining()) return;
            float focusLen = 4f + Mathf.absin(Time.time(), 1.1f, 0.5f);
            float swingScl = 12f, swingMag = tilesize / 8f;
            float flashScl = 0.3f;

            float px = x + Angles.trnsx(rotation, focusLen);
            float py = y + Angles.trnsy(rotation, focusLen);

            float ex = mineTile.worldx() + Mathf.sin(Time.time() + 48, swingScl, swingMag);
            float ey = mineTile.worldy() + Mathf.sin(Time.time() + 48, swingScl + 2f, swingMag);

            Draw.color(Color.lightGray, Color.white, 1f - flashScl + Mathf.absin(Time.time(), 0.5f, flashScl));

            Drawf.laser(Core.atlas.find("minelaser"), Core.atlas.find("minelaser-end"), px, py, ex, ey, 0.75f);

            //TODO hack?
            if(isLocal()){
                Lines.stroke(1f, Pal.accent);
                Lines.poly(mineTile.worldx(), mineTile.worldy(), 4, tilesize / 2f * Mathf.sqrt2, Time.time());
            }

            Draw.color();
        }
    }

    @Component
    abstract static class BuilderComp implements Unitc{
        static final float placeDistance = 220f;
        static final Vec2[] tmptr = new Vec2[]{new Vec2(), new Vec2(), new Vec2(), new Vec2()};

        transient float x, y, rotation;

        @ReadOnly Queue<BuildRequest> requests = new Queue<>();
        float buildSpeed = 1f;
        //boolean building;

        void updateBuilding(){
            float finalPlaceDst = state.rules.infiniteResources ? Float.MAX_VALUE : placeDistance;

            Iterator<BuildRequest> it = requests.iterator();
            while(it.hasNext()){
                BuildRequest req = it.next();
                Tile tile = world.tile(req.x, req.y);
                if(tile == null || (req.breaking && tile.block() == Blocks.air) || (!req.breaking && (tile.rotation() == req.rotation || !req.block.rotate) && tile.block() == req.block)){
                    it.remove();
                }
            }

            Tilec core = closestCore();

            //nothing to build.
            if(buildRequest() == null) return;

            //find the next build request
            if(requests.size > 1){
                int total = 0;
                BuildRequest req;
                while((dst((req = buildRequest()).tile()) > finalPlaceDst || shouldSkip(req, core)) && total < requests.size){
                    requests.removeFirst();
                    requests.addLast(req);
                    total++;
                }
            }

            BuildRequest current = buildRequest();

            if(dst(current.tile()) > finalPlaceDst) return;

            Tile tile = world.tile(current.x, current.y);

            if(!(tile.block() instanceof BuildBlock)){
                if(!current.initialized && !current.breaking && Build.validPlace(team(), current.x, current.y, current.block, current.rotation)){
                    Build.beginPlace(team(), current.x, current.y, current.block, current.rotation);
                }else if(!current.initialized && current.breaking && Build.validBreak(team(), current.x, current.y)){
                    Build.beginBreak(team(), current.x, current.y);
                }else{
                    requests.removeFirst();
                    return;
                }
            }

            if(tile.entity instanceof BuildEntity && !current.initialized){
                Core.app.post(() -> Events.fire(new BuildSelectEvent(tile, team(), (Builderc)this, current.breaking)));
                current.initialized = true;
            }

            //if there is no core to build with or no build entity, stop building!
            if((core == null && !state.rules.infiniteResources) || !(tile.entity instanceof BuildEntity)){
                return;
            }

            //otherwise, update it.
            BuildEntity entity = tile.ent();

            if(entity == null){
                return;
            }

            if(dst(tile) <= finalPlaceDst){
                rotation = Mathf.slerpDelta(rotation, angleTo(entity), 0.4f);
            }

            if(current.breaking){
                entity.deconstruct(this, core, 1f / entity.buildCost * Time.delta() * buildSpeed * state.rules.buildSpeedMultiplier);
            }else{
                if(entity.construct(this, core, 1f / entity.buildCost * Time.delta() * buildSpeed * state.rules.buildSpeedMultiplier, current.hasConfig)){
                    if(current.hasConfig){
                        Call.onTileConfig(null, tile, current.config);
                    }
                }
            }

            current.stuck = Mathf.equal(current.progress, entity.progress);
            current.progress = entity.progress;
        }


        /** Draw all current build requests. Does not draw the beam effect, only the positions. */
        void drawBuildRequests(){

            for(BuildRequest request : requests){
                if(request.progress > 0.01f || (buildRequest() == request && request.initialized && (dst(request.x * tilesize, request.y * tilesize) <= placeDistance || state.isEditor()))) continue;

                request.animScale = 1f;
                if(request.breaking){
                    control.input.drawBreaking(request);
                }else{
                    request.block.drawRequest(request, control.input.allRequests(),
                    Build.validPlace(team(), request.x, request.y, request.block, request.rotation) || control.input.requestMatches(request));
                }
            }

            Draw.reset();
        }

        /** @return whether this request should be skipped, in favor of the next one. */
        boolean shouldSkip(BuildRequest request, @Nullable Tilec core){
            //requests that you have at least *started* are considered
            if(state.rules.infiniteResources || request.breaking || !request.initialized || core == null) return false;
            return request.stuck && !core.items().has(request.block.requirements);
        }

        void removeBuild(int x, int y, boolean breaking){
            //remove matching request
            int idx = requests.indexOf(req -> req.breaking == breaking && req.x == x && req.y == y);
            if(idx != -1){
                requests.removeIndex(idx);
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

        void drawOver(){
            if(!isBuilding()) return;
            BuildRequest request = buildRequest();
            Tile tile = world.tile(request.x, request.y);

            if(dst(tile) > placeDistance && !state.isEditor()){
                return;
            }

            Lines.stroke(1f, Pal.accent);
            float focusLen = 3.8f + Mathf.absin(Time.time(), 1.1f, 0.6f);
            float px = x + Angles.trnsx(rotation, focusLen);
            float py = y + Angles.trnsy(rotation, focusLen);

            float sz = Vars.tilesize * tile.block().size / 2f;
            float ang = angleTo(tile);

            tmptr[0].set(tile.drawx() - sz, tile.drawy() - sz);
            tmptr[1].set(tile.drawx() + sz, tile.drawy() - sz);
            tmptr[2].set(tile.drawx() - sz, tile.drawy() + sz);
            tmptr[3].set(tile.drawx() + sz, tile.drawy() + sz);

            Arrays.sort(tmptr, Structs.comparingFloat(vec -> Angles.angleDist(angleTo(vec), ang)));

            float x1 = tmptr[0].x, y1 = tmptr[0].y,
            x3 = tmptr[1].x, y3 = tmptr[1].y;

            Draw.alpha(1f);

            Lines.line(px, py, x1, y1);
            Lines.line(px, py, x3, y3);

            Fill.circle(px, py, 1.6f + Mathf.absin(Time.time(), 0.8f, 1.5f));

            Draw.color();
        }
    }

    @Component
    abstract class ShielderComp implements Damagec, Teamc, Posc{

        void absorb(){

        }
    }

    @Component
    abstract class ItemsComp implements Posc{
        @ReadOnly ItemStack stack = new ItemStack();

        abstract int itemCapacity();

        @Override
        public void update(){
            stack.amount = Mathf.clamp(stack.amount, 0, itemCapacity());
        }

        Item item(){
            return stack.item;
        }

        void clearItem(){
            stack.amount = 0;
        }

        boolean acceptsItem(Item item){
            return !hasItem() || item == stack.item && stack.amount + 1 <= itemCapacity();
        }

        boolean hasItem(){
            return stack.amount > 0;
        }

        void addItem(Item item){
            addItem(item, 1);
        }

        void addItem(Item item, int amount){
            stack.amount = stack.item == item ? stack.amount + amount : amount;
            stack.item = item;
            stack.amount = Mathf.clamp(stack.amount, 0, itemCapacity());
        }

        int maxAccepted(Item item){
            return stack.item != item && stack.amount > 0 ? 0 : itemCapacity() - stack.amount;
        }
    }

    @Component
    abstract class MassComp implements Velc{
        float mass = 1f;

        public void applyImpulse(float x, float y){
            vel().add(x / mass, y / mass);
        }
    }

    @Component
    abstract class EffectComp implements Posc, Drawc, Timedc, Rotc{
        Effect effect;
        Color color = new Color(Color.white);
        Object data;

        @Override
        public void draw(){
            effect.render(id(), color, time(), rotation(), x(), y(), data);
        }

        @Override
        public float clipSize(){
            return effect.size;
        }
    }

    @Component
    abstract class VelComp implements Posc{
        transient float x, y;

        final Vec2 vel = new Vec2();
        float drag = 0f;

        @Override
        public void update(){
            //TODO handle solidity
            x += vel.x;
            y += vel.y;
            vel.scl(1f - drag * Time.delta());
        }
    }

    @Component
    abstract class HitboxComp implements Posc, QuadTreeObject{
        transient float x, y;

        float hitSize;
        float lastX, lastY;

        @Override
        public void update(){

        }

        void updateLastPosition(){
            lastX = x;
            lastY = y;
        }

        void collision(Hitboxc other, float x, float y){

        }

        float deltaX(){
            return x - lastX;
        }

        float deltaY(){
            return y - lastY;
        }

        boolean collides(Hitboxc other){
            return Intersector.overlapsRect(x - hitSize/2f, y - hitSize/2f, hitSize, hitSize,
            other.x() - other.hitSize()/2f, other.y() - other.hitSize()/2f, other.hitSize(), other.hitSize());
        }

        @Override
        public void hitbox(Rect rect){
            rect.setCentered(x, y, hitSize, hitSize);
        }

        public void hitboxTile(Rect rect){
            float scale = 0.6f;
            rect.setCentered(x, y, hitSize * scale, hitSize * scale);
        }
    }

    @Component
    abstract class StatusComp implements Posc, Flyingc{
        private Array<StatusEntry> statuses = new Array<>();
        private Bits applied = new Bits(content.getBy(ContentType.status).size);

        @ReadOnly float speedMultiplier;
        @ReadOnly float damageMultiplier;
        @ReadOnly float armorMultiplier;

        /** @return damage taken based on status armor multipliers */
        float getShieldDamage(float amount){
            return amount * Mathf.clamp(1f - armorMultiplier / 100f);
        }

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
                        entry.effect.getTransition((Unitc)this, effect, entry.time, duration, StatusEntry.tmp);
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

        boolean isBoss(){
            return hasEffect(StatusEffects.boss);
        }

        boolean isImmune(StatusEffect effect){
            return false;
        }

        Color statusColor(){
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

        @Override
        public void update(){
            Floor floor = floorOn();
            if(isGrounded() && floor.status != null){
                //apply effect
                apply(floor.status, floor.statusDuration);
            }

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
                    //TODO ugly casting
                    entry.effect.update((mindustry.gen.Unitc)this, entry.time);
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
    abstract class EntityComp{
        private boolean added;
        int id;

        boolean isAdded(){
            return added;
        }

        void init(){}

        void update(){}

        void remove(){
            added = false;
        }

        void add(){
            added = true;
        }

        boolean isLocal(){
            return ((Object)this) == player || ((Object)this) instanceof Unitc && ((Unitc)((Object)this)).controller() == player;
        }

        <T> T as(Class<T> type){
            return (T)this;
        }

        @InternalImpl
        abstract int classId();

        void read(DataInput input) throws IOException{
            //TODO dynamic io
        }

        void write(DataOutput output) throws IOException{
            //TODO dynamic io
        }
    }
}
