package io.anuke.mindustry.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Queue;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.content.Mechs;
import io.anuke.mindustry.content.fx.UnitFx;
import io.anuke.mindustry.entities.effect.ScorchDecal;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Trail;
import io.anuke.mindustry.io.TypeIO;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetConnection;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.mindustry.world.blocks.storage.CoreBlock.CoreEntity;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.EntityQuery;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class Player extends Unit implements BuilderTrait, CarryTrait, ShooterTrait{
    public static final int timerSync = 2;
    public static final int timerAbility = 3;
    private static final int timerShootLeft = 0;
    private static final int timerShootRight = 1;
    private static final float liftoffBoost = 0.2f;

    private static final Rectangle rect = new Rectangle();

    //region instance variables

    public float baseRotation;
    public float pointerX, pointerY;
    public String name = "name";
    public String uuid, usid;
    public boolean isAdmin, isTransferring, isShooting, isBoosting, isMobile;
    public float boostHeat, shootHeat;
    public boolean achievedFlight;
    public Color color = new Color();
    public Mech mech;
    public int spawner = -1;

    public NetConnection con;
    public int playerIndex = 0;
    public boolean isLocal = false;
    public Timer timer = new Timer(4);
    public TargetTrait target;
    public TargetTrait moveTarget;

    private float walktime;
    private Queue<BuildRequest> placeQueue = new Queue<>();
    private Tile mining;
    private CarriableTrait carrying;
    private Trail trail = new Trail(12);
    private Vector2 movement = new Translator();
    private boolean moved;

    //endregion

    //region unit and event overrides, utility methods

    @Remote(targets = Loc.server, called = Loc.server)
    public static void onPlayerDeath(Player player){
        if(player == null) return;

        player.dead = true;
        player.placeQueue.clear();

        player.dropCarry();

        float explosiveness = 2f + (player.inventory.hasItem() ? player.inventory.getItem().item.explosiveness * player.inventory.getItem().amount : 0f);
        float flammability = (player.inventory.hasItem() ? player.inventory.getItem().item.flammability * player.inventory.getItem().amount : 0f);
        Damage.dynamicExplosion(player.x, player.y, flammability, explosiveness, 0f, player.getSize() / 2f, Palette.darkFlame);

        ScorchDecal.create(player.x, player.y);
        player.onDeath();
    }

    @Override
    public void getHitbox(Rectangle rectangle){
        rectangle.setSize(mech.hitsize).setCenter(x, y);
    }

    @Override
    public void getHitboxTile(Rectangle rectangle){
        rectangle.setSize(mech.hitsize * 2f / 3f).setCenter(x, y);
    }

    @Override
    public float getDrag(){
        return mech.drag;
    }

    @Override
    public Timer getTimer(){
        return timer;
    }

    @Override
    public int getShootTimer(boolean left){
        return left ? timerShootLeft : timerShootRight;
    }

    @Override
    public Weapon getWeapon(){
        return mech.weapon;
    }

    @Override
    public float getMinePower(){
        return mech.mineSpeed;
    }

    @Override
    public TextureRegion getIconRegion(){
        return mech.iconRegion;
    }

    @Override
    public int getItemCapacity(){
        return mech.itemCapacity;
    }

    @Override
    public void interpolate(){
        super.interpolate();

        if(interpolator.values.length > 1){
            baseRotation = interpolator.values[1];
        }

        if(interpolator.target.dst(interpolator.last) > 1f){
            walktime += Timers.delta();
        }
    }

    @Override
    public CarriableTrait getCarry(){
        return carrying;
    }

    @Override
    public void setCarry(CarriableTrait unit){
        this.carrying = unit;
    }

    @Override
    public float getCarryWeight(){
        return mech.carryWeight;
    }

    @Override
    public float getBuildPower(Tile tile){
        return mech.buildPower;
    }

    @Override
    public float maxHealth(){
        return 200;
    }

    @Override
    public Tile getMineTile(){
        return mining;
    }

    @Override
    public void setMineTile(Tile tile){
        this.mining = tile;
    }

    @Override
    public boolean canMine(Item item){
        return item.hardness <= mech.drillPower;
    }

    @Override
    public float getArmor(){
        return mech.armor + mech.getExtraArmor(this);
    }

    @Override
    public void added(){
        baseRotation = 90f;
    }

    @Override
    public float getMass(){
        return mech.mass;
    }

    @Override
    public boolean isFlying(){
        return mech.flying || boostHeat > liftoffBoost || isCarried();
    }

    @Override
    public float getSize(){
        return 8;
    }

    @Override
    public void damage(float amount){
        hitTime = hitDuration;
        if(!Net.client()){
            health -= calculateDamage(amount);
        }

        if(health <= 0 && !dead){
            Call.onPlayerDeath(this);
        }
    }

    @Override
    public void set(float x, float y){
        this.x = x;
        this.y = y;
    }

    @Override
    public float getMaxVelocity(){
        return mech.maxSpeed;
    }

    @Override
    public Queue<BuildRequest> getPlaceQueue(){
        return placeQueue;
    }

    @Override
    public String toString(){
        return "Player{" + id + ", mech=" + mech.name + ", local=" + isLocal + ", " + x + ", " + y + "}";
    }

    @Override
    public void removed(){
        dropCarryLocal();

        TileEntity core = getClosestCore();
        if(core != null && ((CoreEntity) core).currentUnit == this){
            ((CoreEntity) core).currentUnit = null;
        }
    }

    @Override
    public EntityGroup targetGroup(){
        return playerGroup;
    }

    public void setTeam(Team team){
        this.team = team;
    }

    //endregion

    //region draw methods

    @Override
    public float drawSize(){
        return isLocal ? Float.MAX_VALUE : 40;
    }

    @Override
    public void drawShadow(float offsetX, float offsetY){
        float x = snappedX(), y = snappedY();
        float scl = mech.flying ? 1f : boostHeat/2f;

        Draw.rect(mech.iconRegion, x + offsetX*scl, y + offsetY*scl, rotation - 90);
    }

    @Override
    public void draw(){
        if(dead) return;

        float x = snappedX(), y = snappedY();

        if(!movement.isZero() && moved && !state.isPaused()){
            walktime += movement.len() / 0.7f * getFloorOn().speedMultiplier;
            baseRotation = Mathf.slerpDelta(baseRotation, movement.angle(), 0.13f);
        }

        float ft = Mathf.sin(walktime, 6f, 2f) * (1f - boostHeat);

        Floor floor = getFloorOn();

        Draw.color();
        Draw.alpha(hitTime / hitDuration);

        if(!mech.flying){
            if(floor.isLiquid){
                Draw.tint(Color.WHITE, floor.liquidColor, 0.5f);
            }

            float boostTrnsY = -boostHeat * 3f;
            float boostTrnsX = boostHeat * 3f;
            float boostAng = boostHeat * 40f;

            for(int i : Mathf.signs){
                Draw.rect(mech.legRegion,
                        x + Angles.trnsx(baseRotation, ft * i + boostTrnsY, -boostTrnsX * i),
                        y + Angles.trnsy(baseRotation, ft * i + boostTrnsY, -boostTrnsX * i),
                        mech.legRegion.getRegionWidth() * i, mech.legRegion.getRegionHeight() - Mathf.clamp(ft * i, 0, 2), baseRotation - 90 + boostAng * i);
            }

            Draw.rect(mech.baseRegion, x, y, baseRotation - 90);
        }

        if(floor.isLiquid){
            Draw.tint(Color.WHITE, floor.liquidColor, drownTime);
        }else{
            Draw.tint(Color.WHITE);
        }

        Draw.rect(mech.region, x, y, rotation - 90);

        mech.draw(this);

        for(int i : Mathf.signs){
            float tra = rotation - 90, trY = -mech.weapon.getRecoil(this, i > 0) + mech.weaponOffsetY;
            float w = i > 0 ? -mech.weapon.equipRegion.getRegionWidth() : mech.weapon.equipRegion.getRegionWidth();
            Draw.rect(mech.weapon.equipRegion,
                    x + Angles.trnsx(tra, (mech.weaponOffsetX + mech.spreadX(this)) * i, trY),
                    y + Angles.trnsy(tra, (mech.weaponOffsetX + mech.spreadX(this)) * i, trY), w, mech.weapon.equipRegion.getRegionHeight(), rotation - 90);
        }

        float backTrns = 4f, itemSize = 5f;
        if(inventory.hasItem()){
            ItemStack stack = inventory.getItem();
            int stored = Mathf.clamp(stack.amount / 6, 1, 8);

            for(int i = 0; i < stored; i++){
                float angT = i == 0 ? 0 : Mathf.randomSeedRange(i + 1, 60f);
                float lenT = i == 0 ? 0 : Mathf.randomSeedRange(i + 2, 1f) - 1f;
                Draw.rect(stack.item.region,
                        x + Angles.trnsx(rotation + 180f + angT, backTrns + lenT),
                        y + Angles.trnsy(rotation + 180f + angT, backTrns + lenT),
                        itemSize, itemSize, rotation);
            }
        }

        Draw.alpha(1f);
    }

    @Override
    public void drawStats(){
        float x = snappedX(), y = snappedY();

        Draw.color(Color.BLACK, team.color, healthf() + Mathf.absin(Timers.time(), healthf()*5f, 1f - healthf()));
        Draw.alpha(hitTime / hitDuration);
        Draw.rect(getPowerCellRegion(), x + Angles.trnsx(rotation, mech.cellTrnsY, 0f), y + Angles.trnsy(rotation, mech.cellTrnsY, 0f), rotation - 90);
        Draw.color();
    }

    @Override
    public void drawOver(){
        if(dead) return;

        drawBuilding(this);

        if(mech.flying || boostHeat > 0.001f){
            float wobblyness = 0.6f;
            if(!state.isPaused()) trail.update(x + Angles.trnsx(rotation + 180f, 5f) + Mathf.range(wobblyness),
                    y + Angles.trnsy(rotation + 180f, 5f) + Mathf.range(wobblyness));
            trail.draw(Hue.mix(mech.trailColor, mech.trailColorTo, mech.flying ? 0f : boostHeat, Tmp.c1), 5f * (isFlying() ? 1f : boostHeat));
        }else{
            trail.clear();
        }
    }

    public float snappedX(){
        return snapCamera && isLocal ? (int) (x + 0.0001f) : x;
    }

    public float snappedY(){
        return snapCamera && isLocal ? (int) (y + 0.0001f) : y;
    }

    public void drawName(){
        GlyphLayout layout = Pooling.obtain(GlyphLayout.class, GlyphLayout::new);

        boolean ints = Core.font.usesIntegerPositions();
        Core.font.setUseIntegerPositions(false);
        Draw.tscl(0.25f / io.anuke.ucore.scene.ui.layout.Unit.dp.scl(1f));
        layout.setText(Core.font, name);
        Draw.color(0f, 0f, 0f, 0.3f);
        Draw.rect("blank", x, y + 8 - layout.height / 2, layout.width + 2, layout.height + 3);
        Draw.color();
        Draw.tcolor(color);
        Draw.text(name, x, y + 8);

        if(isAdmin){
            float s = 3f;
            Draw.color(color.r * 0.5f, color.g * 0.5f, color.b * 0.5f, 1f);
            Draw.rect("icon-admin-small", x + layout.width / 2f + 2 + 1, y + 6.5f, s, s);
            Draw.color(color);
            Draw.rect("icon-admin-small", x + layout.width / 2f + 2 + 1, y + 7f, s, s);
        }

        Draw.reset();
        Pooling.free(layout);
        Draw.tscl(1f);
        Core.font.setUseIntegerPositions(ints);
    }

    /**Draw all current build requests. Does not draw the beam effect, only the positions.*/
    public void drawBuildRequests(){
        for(BuildRequest request : getPlaceQueue()){
            if(getCurrentRequest() == request) continue;

            if(request.breaking){
                Block block = world.tile(request.x, request.y).target().block();

                //draw removal request
                Lines.stroke(2f);

                Draw.color(Palette.removeBack);

                float rad = Mathf.absin(Timers.time(), 7f, 1f) + block.size * tilesize / 2f - 1;

                Lines.square(
                        request.x * tilesize + block.offset(),
                        request.y * tilesize + block.offset() - 1,
                        rad);

                Draw.color(Palette.remove);

                Lines.square(
                        request.x * tilesize + block.offset(),
                        request.y * tilesize + block.offset(),
                        rad);
            }else{
                //draw place request
                Lines.stroke(2f);

                Draw.color(Palette.accentBack);

                float rad = Mathf.absin(Timers.time(), 7f, 1f) - 2f + request.recipe.result.size * tilesize / 2f;

                Lines.square(
                        request.x * tilesize + request.recipe.result.offset(),
                        request.y * tilesize + request.recipe.result.offset() - 1,
                        rad);

                Draw.color(Palette.accent);

                Lines.square(
                        request.x * tilesize + request.recipe.result.offset(),
                        request.y * tilesize + request.recipe.result.offset(),
                        rad);
            }
        }

        Draw.reset();
    }

    //endregion

    //region update methods

    @Override
    public void update(){
        hitTime -= Timers.delta();

        if(Float.isNaN(x) || Float.isNaN(y)){
            velocity.set(0f, 0f);
            x = 0;
            y = 0;
            setDead(true);
        }

        if(netServer.isWaitingForPlayers()){
            setDead(true);
        }

        if(isDead()){
            isBoosting = false;
            boostHeat = 0f;
            updateRespawning();
            return;
        }else{
            spawner = -1;
        }

        avoidOthers(1f);

        Tile tile = world.tileWorld(x, y);

        boostHeat = Mathf.lerpDelta(boostHeat, (tile != null && tile.solid()) || (isBoosting && ((!movement.isZero() && moved) || !isLocal)) ? 1f : 0f, 0.08f);
        shootHeat = Mathf.lerpDelta(shootHeat, isShooting() ? 1f : 0f, 0.06f);
        mech.updateAlt(this); //updated regardless

        if(boostHeat > liftoffBoost + 0.1f){
            achievedFlight = true;
        }

        if(boostHeat <= liftoffBoost + 0.05f && achievedFlight && !mech.flying){
            if(tile != null){
                if(mech.shake > 1f){
                    Effects.shake(mech.shake, mech.shake, this);
                }
                Effects.effect(UnitFx.unitLand, tile.floor().minimapColor, x, y, tile.floor().isLiquid ? 1f : 0.5f);
            }
            mech.onLand(this);
            achievedFlight = false;
        }

        if(!isLocal){
            interpolate();
            updateBuilding(this); //building happens even with non-locals
            status.update(this); //status effect updating also happens with non locals for effect purposes
            updateVelocityStatus(); //velocity too, for visual purposes

            if(getCarrier() != null){
                x = getCarrier().getX();
                y = getCarrier().getY();
            }

            if(Net.server()){
                updateShooting(); //server simulates player shooting
            }
            return;
        }else{
            //unlock mech when used
            control.unlocks.unlockContent(mech);
        }

        if(mobile){
            updateFlying();
        }else{
            updateMech();
        }

        updateBuilding(this);

        x = Mathf.clamp(x, tilesize, world.width() * tilesize - tilesize);
        y = Mathf.clamp(y, tilesize, world.height() * tilesize - tilesize);
    }

    protected void updateMech(){
        Tile tile = world.tileWorld(x, y);

        isBoosting = Inputs.keyDown("dash") && !mech.flying;

        //if player is in solid block
        if(tile != null && tile.solid()){
            isBoosting = true;
        }

        float speed = isBoosting && !mech.flying ? mech.boostSpeed : mech.speed;
        //fraction of speed when at max load
        float carrySlowdown = 0.7f;

        speed *= ((inventory.hasItem() ? Mathf.lerp(1f, carrySlowdown, (float) inventory.getItem().amount / inventory.capacity()) : 1f));

        if(mech.flying){
            //prevent strafing backwards, have a penalty for doing so
            float penalty = 0.2f; //when going 180 degrees backwards, reduce speed to 0.2x
            speed *= Mathf.lerp(1f, penalty, Angles.angleDist(rotation, velocity.angle()) / 180f);
        }

        //drop from carrier on key press
        if(!ui.chatfrag.chatOpen() && Inputs.keyTap("drop_unit")){
            if(!mech.flying){
                if(getCarrier() != null){
                    Call.dropSelf(this);
                }
            }else if(getCarry() != null){
                dropCarry();
            }else{
                Unit unit = Units.getClosest(team, x, y, 8f, u -> !u.isFlying() && u.getMass() <= mech.carryWeight);

                if(unit != null){
                    carry(unit);
                }
            }
        }

        movement.setZero();

        String section = control.input(playerIndex).section;

        float xa = Inputs.getAxis(section, "move_x");
        float ya = Inputs.getAxis(section, "move_y");

        movement.y += ya * speed;
        movement.x += xa * speed;

        Vector2 vec = Graphics.world(control.input(playerIndex).getMouseX(), control.input(playerIndex).getMouseY());
        pointerX = vec.x;
        pointerY = vec.y;
        updateShooting();

        movement.limit(speed).scl(Timers.delta());

        if(getCarrier() == null){
            if(!ui.chatfrag.chatOpen()){
                velocity.add(movement.x, movement.y);
            }
            float prex = x, prey = y;
            updateVelocityStatus();
            moved = distanceTo(prex, prey) > 0.001f;
        }else{
            velocity.setZero();
            x = Mathf.lerpDelta(x, getCarrier().getX(), 0.1f);
            y = Mathf.lerpDelta(y, getCarrier().getY(), 0.1f);
        }

        if(!ui.chatfrag.chatOpen()){
            float baseLerp = mech.getRotationAlpha(this);
            if(!isShooting() || !mech.turnCursor){
                if(!movement.isZero()){
                    rotation = Mathf.slerpDelta(rotation, mech.flying ? velocity.angle() : movement.angle(), 0.13f * baseLerp);
                }
            }else{
                float angle = control.input(playerIndex).mouseAngle(x, y);
                this.rotation = Mathf.slerpDelta(this.rotation, angle, 0.1f * baseLerp);
            }
        }
    }

    protected void updateShooting(){
        if(isShooting() && mech.canShoot(this)){
            mech.weapon.update(this, pointerX, pointerY);
        }
    }

    protected void updateFlying(){
        if(Units.invalidateTarget(target, this)){
            target = null;
        }

        float targetX = Core.camera.position.x, targetY = Core.camera.position.y;
        float attractDst = 15f;

        if(moveTarget != null && !moveTarget.isDead()){
            targetX = moveTarget.getX();
            targetY = moveTarget.getY();
            boolean tapping = moveTarget instanceof TileEntity && moveTarget.getTeam() == team;
            attractDst = 0f;

            if(tapping){
                velocity.setAngle(Mathf.slerpDelta(velocity.angle(), angleTo(moveTarget), 0.1f));
            }

            if(distanceTo(moveTarget) < 2f){
                if(moveTarget instanceof CarriableTrait){
                    carry((CarriableTrait) moveTarget);
                }else if(tapping){
                    Tile tile = ((TileEntity) moveTarget).tile;
                    tile.block().tapped(tile, this);
                }

                moveTarget = null;
            }
        }else{
            moveTarget = null;
        }

        if(getCarrier() != null){
            velocity.setZero();
            x = Mathf.lerpDelta(x, getCarrier().getX(), 0.1f);
            y = Mathf.lerpDelta(y, getCarrier().getY(), 0.1f);
        }

        movement.set(targetX - x, targetY - y).limit(isBoosting && !mech.flying ? mech.boostSpeed : mech.speed);
        movement.setAngle(Mathf.slerp(movement.angle(), velocity.angle(), 0.05f));

        if(distanceTo(targetX, targetY) < attractDst){
            movement.setZero();
        }

        float expansion = 3f;

        getHitbox(rect);
        rect.x -= expansion;
        rect.y -= expansion;
        rect.width += expansion*2f;
        rect.height += expansion*2f;

        isBoosting = EntityQuery.collisions().overlapsTile(rect) || distanceTo(targetX, targetY) > 85f;

        velocity.add(movement.scl(Timers.delta()));

        if(velocity.len() <= 0.2f && mech.flying){
            rotation += Mathf.sin(Timers.time() + id * 99, 10f, 1f);
        }else if(target == null){
            rotation = Mathf.slerpDelta(rotation, velocity.angle(), velocity.len() / 10f);
        }

        float lx = x, ly = y;
        updateVelocityStatus();
        moved = distanceTo(lx, ly) > 0.001f && !isCarried();

        if(mech.flying){
            //hovering effect
            x += Mathf.sin(Timers.time() + id * 999, 25f, 0.08f);
            y += Mathf.cos(Timers.time() + id * 999, 25f, 0.08f);
        }

        //update shooting if not building, not mining and there's ammo left
        if(!isBuilding() && getMineTile() == null){

            //autofire: mobile only!
            if(mobile){
                if(target == null){
                    isShooting = false;
                    if(Settings.getBool("autotarget")){
                        target = Units.getClosestTarget(team, x, y, getWeapon().getAmmo().getRange());
                        if(target != null){
                            setMineTile(null);
                        }
                    }
                }else if(target.isValid()){
                    //rotate toward and shoot the target
                    if(mech.turnCursor){
                        rotation = Mathf.slerpDelta(rotation, angleTo(target), 0.2f);
                    }

                    Vector2 intercept =
                            Predict.intercept(x, y, target.getX(), target.getY(), target.getVelocity().x - velocity.x, target.getVelocity().y - velocity.y, getWeapon().getAmmo().bullet.speed);

                    pointerX = intercept.x;
                    pointerY = intercept.y;

                    updateShooting();
                    isShooting = true;
                }

            }else if(isShooting()){
                Vector2 vec = Graphics.world(control.input(playerIndex).getMouseX(),
                        control.input(playerIndex).getMouseY());
                pointerX = vec.x;
                pointerY = vec.y;

                updateShooting();
            }
        }
    }

    //endregion

    //region utility methods

    /** Resets all values of the player.*/
    public void reset(){
        resetNoAdd();

        add();
    }

    public void resetNoAdd(){
        status.clear();
        team = Team.blue;
        inventory.clear();
        placeQueue.clear();
        dead = true;
        trail.clear();
        carrier = null;
        health = maxHealth();
        boostHeat = drownTime = hitTime = 0f;
        mech = (isMobile ? Mechs.starterMobile : Mechs.starterDesktop);
        placeQueue.clear();
    }

    public boolean isShooting(){
        return isShooting && (!isBoosting || mech.flying);
    }

    public void updateRespawning(){

        if(spawner != -1 && world.tile(spawner) != null && world.tile(spawner).entity instanceof SpawnerTrait){
            ((SpawnerTrait) world.tile(spawner).entity).updateSpawning(this);
        }else{
            CoreEntity entity = (CoreEntity) getClosestCore();
            if(entity != null && !netServer.isWaitingForPlayers()){
                this.spawner = entity.tile.id();
            }
        }
    }

    public void beginRespawning(SpawnerTrait spawner){
        this.spawner = spawner.getTile().packedPosition();
        this.dead = true;
    }

    public void endRespawning(){
        spawner = -1;
    }

    //endregion

    //region read and write methods


    @Override
    public boolean isClipped(){
        return false;
    }

    @Override
    public void writeSave(DataOutput stream) throws IOException{
        stream.writeBoolean(isLocal);

        if(isLocal){
            stream.writeByte(mech.id);
            stream.writeByte(playerIndex);
            super.writeSave(stream, false);
        }
    }

    @Override
    public void readSave(DataInput stream) throws IOException{
        boolean local = stream.readBoolean();

        if(local && !headless){
            byte mechid = stream.readByte();
            int index = stream.readByte();
            players[index].readSaveSuper(stream);
            players[index].mech = content.getByID(ContentType.mech, mechid);
            players[index].dead = false;
        }else if(local){
            byte mechid = stream.readByte();
            stream.readByte();
            readSaveSuper(stream);
            mech = content.getByID(ContentType.mech, mechid);
            dead = false;
        }
    }

    private void readSaveSuper(DataInput stream) throws IOException{
        super.readSave(stream);

        add();
    }

    @Override
    public void write(DataOutput buffer) throws IOException{
        super.writeSave(buffer, !isLocal);
        TypeIO.writeStringData(buffer, name); //TODO writing strings is very inefficient
        buffer.writeByte(Bits.toByte(isAdmin) | (Bits.toByte(dead) << 1) | (Bits.toByte(isBoosting) << 2));
        buffer.writeInt(Color.rgba8888(color));
        buffer.writeByte(mech.id);
        buffer.writeInt(mining == null ? -1 : mining.packedPosition());
        buffer.writeInt(spawner);
        buffer.writeShort((short) (baseRotation * 2));

        writeBuilding(buffer);
    }

    @Override
    public void read(DataInput buffer, long time) throws IOException{
        float lastx = x, lasty = y, lastrot = rotation;
        super.readSave(buffer);
        name = TypeIO.readStringData(buffer);
        byte bools = buffer.readByte();
        isAdmin = (bools & 1) != 0;
        dead = (bools & 2) != 0;
        boolean boosting = (bools & 4) != 0;
        color.set(buffer.readInt());
        mech = content.getByID(ContentType.mech, buffer.readByte());
        int mine = buffer.readInt();
        int spawner = buffer.readInt();
        float baseRotation = buffer.readShort() / 2f;

        readBuilding(buffer, !isLocal);

        interpolator.read(lastx, lasty, x, y, time, rotation, baseRotation);
        rotation = lastrot;

        if(isLocal){
            x = lastx;
            y = lasty;
        }else{
            mining = world.tile(mine);
            isBoosting = boosting;
            this.spawner = spawner;
        }
    }

    //endregion
}
