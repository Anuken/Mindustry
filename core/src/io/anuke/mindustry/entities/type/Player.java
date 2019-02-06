package io.anuke.mindustry.entities.type;

import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.arc.Core;
import io.anuke.arc.collection.Queue;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.Angles;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Geometry;
import io.anuke.arc.math.geom.Rectangle;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.util.*;
import io.anuke.arc.util.pooling.Pools;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.content.Mechs;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.traits.BuilderTrait;
import io.anuke.mindustry.entities.traits.ShooterTrait;
import io.anuke.mindustry.entities.traits.SpawnerTrait;
import io.anuke.mindustry.entities.traits.TargetTrait;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.input.Binding;
import io.anuke.mindustry.io.TypeIO;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetConnection;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Block.Icon;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class Player extends Unit implements BuilderTrait, ShooterTrait{
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
    public float boostHeat, shootHeat, destructTime;
    public boolean achievedFlight;
    public Color color = new Color();
    public Mech mech;
    public SpawnerTrait spawner, lastSpawner;

    public NetConnection con;
    public int playerIndex = 0;
    public boolean isLocal = false;
    public Interval timer = new Interval(4);
    public TargetTrait target;
    public TargetTrait moveTarget;

    private float walktime;
    private Queue<BuildRequest> placeQueue = new Queue<>();
    private Tile mining;
    private Vector2 movement = new Vector2();
    private boolean moved;

    //endregion

    //region unit and event overrides, utility methods

    @Remote(targets = Loc.server, called = Loc.server)
    public static void onPlayerDeath(Player player){
        if(player == null) return;

        player.dead = true;
        player.placeQueue.clear();
        player.onDeath();
        player.mech = (player.isMobile ? Mechs.starterMobile : Mechs.starterDesktop);
    }

    @Override
    public void hitbox(Rectangle rectangle){
        rectangle.setSize(mech.hitsize).setCenter(x, y);
    }

    @Override
    public void hitboxTile(Rectangle rectangle){
        rectangle.setSize(mech.hitsize * 2f / 3f).setCenter(x, y);
    }

    @Override
    public void onRespawn(Tile tile){
        boostHeat = 1f;
        achievedFlight = true;
    }

    @Override
    public void move(float x, float y){
        if(!mech.flying){
            EntityQuery.collisions().move(this, x, y);
        }else{
            moveBy(x, y);
        }
    }

    @Override
    public boolean collidesGrid(int x, int y){
        Tile tile = world.tile(x, y);
        return !isFlying() || (!mech.flying && tile != null && !tile.block().synthetic() && tile.block().solid);
    }

    @Override
    public float drag(){
        return mech.drag;
    }

    @Override
    public Interval getTimer(){
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
            walktime += Time.delta();
        }
    }

    @Override
    public float getBuildPower(Tile tile){
        return mech.buildPower;
    }

    @Override
    public float maxHealth(){
        return mech.health;
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
    public float calculateDamage(float amount){
        return amount * Mathf.clamp(1f - (status.getArmorMultiplier() + mech.getExtraArmor(this)) / 100f);
    }

    @Override
    public void added(){
        baseRotation = 90f;
    }

    @Override
    public float mass(){
        return mech.mass;
    }

    @Override
    public boolean isFlying(){
        return mech.flying || boostHeat > liftoffBoost;
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
    public float maxVelocity(){
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
        float scl = mech.flying ? 1f : boostHeat / 2f;

        Draw.rect(mech.iconRegion, x + offsetX * scl, y + offsetY * scl, rotation - 90);
    }

    @Override
    public void draw(){
        if(dead) return;

        if(!movement.isZero() && moved && !state.isPaused()){
            walktime += movement.len() / 1f * getFloorOn().speedMultiplier;
            baseRotation = Mathf.slerpDelta(baseRotation, movement.angle(), 0.13f);
        }

        float ft = Mathf.sin(walktime, 6f, 2f) * (1f - boostHeat);

        Floor floor = getFloorOn();

        Draw.color();
        Draw.alpha(Draw.getShader() != Shaders.mix ? 1f : hitTime / hitDuration);

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
                mech.legRegion.getWidth() * i * Draw.scl,
                (mech.legRegion.getHeight() - Mathf.clamp(ft * i, 0, 2)) * Draw.scl,
                baseRotation - 90 + boostAng * i);
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
            float w = i > 0 ? -mech.weapon.region.getWidth() : mech.weapon.region.getWidth();
            Draw.rect(mech.weapon.region,
            x + Angles.trnsx(tra, (mech.weaponOffsetX + mech.spreadX(this)) * i, trY),
            y + Angles.trnsy(tra, (mech.weaponOffsetX + mech.spreadX(this)) * i, trY),
            w * Draw.scl,
            mech.weapon.region.getHeight() * Draw.scl,
            rotation - 90);
        }

        float backTrns = 4f, itemSize = 5f;
        if(item.amount > 0){
            ItemStack stack = item;
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
        Draw.color(Color.BLACK, team.color, healthf() + Mathf.absin(Time.time(), healthf() * 5f, 1f - healthf()));
        Draw.alpha(hitTime / hitDuration);
        Draw.rect(getPowerCellRegion(), x + Angles.trnsx(rotation, mech.cellTrnsY, 0f), y + Angles.trnsy(rotation, mech.cellTrnsY, 0f), rotation - 90);
        Draw.color();
    }

    @Override
    public void drawOver(){
        if(dead) return;

        drawBuilding();
    }

    @Override
    public void drawUnder(){
        float size = mech.engineSize * (mech.flying ? 1f : boostHeat);

        Draw.color(mech.engineColor);
        Fill.circle(x + Angles.trnsx(rotation + 180, mech.engineOffset), y + Angles.trnsy(rotation + 180, mech.engineOffset),
        size + Mathf.absin(Time.time(), 2f, size/4f));

        Draw.color(Color.WHITE);
        Fill.circle(x + Angles.trnsx(rotation + 180, mech.engineOffset-1f), y + Angles.trnsy(rotation + 180, mech.engineOffset-1f),
        (size + Mathf.absin(Time.time(), 2f, size/4f)) / 2f);
        Draw.color();
    }

    public void drawName(){
        BitmapFont font = Core.scene.skin.getFont("default-font");
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);

        boolean ints = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);
        font.getData().setScale(0.25f / io.anuke.arc.scene.ui.layout.Unit.dp.scl(1f));
        layout.setText(font, name);
        Draw.color(0f, 0f, 0f, 0.3f);
        Fill.rect(x, y + 8 - layout.height / 2, layout.width + 2, layout.height + 3);
        Draw.color();
        font.setColor(color);

        font.draw(name, x, y + 8, 0, Align.center, false);

        if(isAdmin){
            float s = 3f;
            Draw.color(color.r * 0.5f, color.g * 0.5f, color.b * 0.5f, 1f);
            Draw.rect(Core.atlas.find("icon-admin-small"), x + layout.width / 2f + 2 + 1, y + 6.5f, s, s);
            Draw.color(color);
            Draw.rect(Core.atlas.find("icon-admin-small"), x + layout.width / 2f + 2 + 1, y + 7f, s, s);
        }

        Draw.reset();
        Pools.free(layout);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        font.setUseIntegerPositions(ints);
    }

    /** Draw all current build requests. Does not draw the beam effect, only the positions. */
    public void drawBuildRequests(){
        for(BuildRequest request : getPlaceQueue()){
            if(getCurrentRequest() == request) continue;

            if(request.breaking){
                Block block = world.tile(request.x, request.y).target().block();

                //draw removal request
                Lines.stroke(2f, Palette.removeBack);

                float rad = Mathf.absin(Time.time(), 7f, 1f) + block.size * tilesize / 2f - 1;
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
                float rad = Mathf.absin(Time.time(), 7f, 1f) - 1.5f + request.block.size * tilesize / 2f;

                //draw place request
                Lines.stroke(1f, Palette.accentBack);

                Lines.square(
                request.x * tilesize + request.block.offset(),
                request.y * tilesize + request.block.offset() - 1,
                rad);

                Draw.color();

                Draw.rect(request.block.icon(Icon.full),
                        request.x * tilesize + request.block.offset(),
                        request.y * tilesize + request.block.offset(), rad*2, rad*2, request.rotation * 90);


                Draw.color(Palette.accent);

                Lines.square(
                request.x * tilesize + request.block.offset(),
                request.y * tilesize + request.block.offset(),
                rad);
            }
        }

        Draw.reset();
    }

    //endregion

    //region update methods

    @Override
    public void update(){
        hitTime -= Time.delta();

        if(Float.isNaN(x) || Float.isNaN(y)){
            velocity.set(0f, 0f);
            x = 0;
            y = 0;
            setDead(true);
        }

        if(netServer.isWaitingForPlayers()){
            setDead(true);
        }

        if(!isDead() && isOutOfBounds()){
            destructTime += Time.delta();

            if(destructTime >= boundsCountdown){
                kill();
            }
        }else{
            destructTime = 0f;
        }

        if(isDead()){
            isBoosting = false;
            boostHeat = 0f;
            updateRespawning();
            return;
        }else{
            spawner = null;
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
                Effects.effect(Fx.unitLand, tile.floor().liquidColor == null ? tile.floor().color : tile.floor().color, x, y, tile.floor().isLiquid ? 1f : 0.5f);
            }
            mech.onLand(this);
            achievedFlight = false;
        }

        if(!isLocal){
            interpolate();
            updateBuilding(); //building happens even with non-locals
            status.update(this); //status effect updating also happens with non locals for effect purposes
            updateVelocityStatus(); //velocity too, for visual purposes

            if(Net.server()){
                updateShooting(); //server simulates player shooting
            }
            return;
        }else{
            //unlock mech when used
            data.unlockContent(mech);
        }

        if(mobile){
            updateFlying();
        }else{
            updateMech();
        }

        updateBuilding();

        if(!mech.flying){
            clampPosition();
        }
    }

    protected void updateMech(){
        Tile tile = world.tileWorld(x, y);

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
        if(!Core.input.keyDown(Binding.gridMode)){
            movement.y += ya * speed;
            movement.x += xa * speed;
        }

        Vector2 vec = Core.input.mouseWorld(control.input(playerIndex).getMouseX(), control.input(playerIndex).getMouseY());
        pointerX = vec.x;
        pointerY = vec.y;
        updateShooting();

        movement.limit(speed).scl(Time.delta());

        if(!ui.chatfrag.chatOpen()){
            velocity.add(movement.x, movement.y);
        }
        float prex = x, prey = y;
        updateVelocityStatus();
        moved = dst(prex, prey) > 0.001f;

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
        if(Units.invalidateTarget(target, this) && !(target instanceof TileEntity && ((TileEntity) target).damaged() && target.getTeam() == team &&
        mech.canHeal && dst(target) < getWeapon().bullet.range())){
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

            if(dst(moveTarget) < 2f){
                if(tapping){
                    Tile tile = ((TileEntity) moveTarget).tile;
                    tile.block().tapped(tile, this);
                }

                moveTarget = null;
            }
        }else{
            moveTarget = null;
        }

        movement.set(targetX - x, targetY - y).limit(isBoosting && !mech.flying ? mech.boostSpeed : mech.speed);
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

        isBoosting = EntityQuery.collisions().overlapsTile(rect) || dst(targetX, targetY) > 85f;

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
        if(!isBuilding() && getMineTile() == null){

            //autofire: mobile only!
            if(mobile){
                if(target == null){
                    isShooting = false;
                    if(Core.settings.getBool("autotarget")){
                        target = Units.getClosestTarget(team, x, y, getWeapon().bullet.range());

                        if(mech.canHeal && target == null){
                            target = Geometry.findClosest(x, y, world.indexer.getDamaged(Team.blue));
                            if(target != null && dst(target) > getWeapon().bullet.range()){
                                target = null;
                            }else if(target != null){
                                target = ((Tile) target).entity;
                            }
                        }

                        if(target != null){
                            setMineTile(null);
                        }
                    }
                }else if(target.isValid() || (target instanceof TileEntity && ((TileEntity) target).damaged() && target.getTeam() == team &&
                mech.canHeal && dst(target) < getWeapon().bullet.range())){
                    //rotate toward and shoot the target
                    if(mech.turnCursor){
                        rotation = Mathf.slerpDelta(rotation, angleTo(target), 0.2f);
                    }

                    Vector2 intercept =
                    Predict.intercept(x, y, target.getX(), target.getY(), target.velocity().x - velocity.x, target.velocity().y - velocity.y, getWeapon().bullet.speed);

                    pointerX = intercept.x;
                    pointerY = intercept.y;

                    updateShooting();
                    isShooting = true;
                }

            }else if(isShooting()){
                Vector2 vec = Core.input.mouseWorld(control.input(playerIndex).getMouseX(),
                control.input(playerIndex).getMouseY());
                pointerX = vec.x;
                pointerY = vec.y;

                updateShooting();
            }
        }
    }

    //endregion

    //region utility methods

    /** Resets all values of the player. */
    public void reset(){
        resetNoAdd();

        add();
    }

    public void resetNoAdd(){
        status.clear();
        team = Team.blue;
        item.amount = 0;
        placeQueue.clear();
        dead = true;
        target = null;
        moveTarget = null;
        health = maxHealth();
        boostHeat = drownTime = hitTime = 0f;
        mech = (isMobile ? Mechs.starterMobile : Mechs.starterDesktop);
        placeQueue.clear();
    }

    public boolean isShooting(){
        return isShooting && (!isBoosting || mech.flying);
    }

    public void updateRespawning(){

        if(spawner != null && spawner.isValid()){
            spawner.updateSpawning(this);
        }else if(!netServer.isWaitingForPlayers()){
            if(lastSpawner != null && lastSpawner.isValid()){
                this.spawner = lastSpawner;
            }else if(getClosestCore() != null){
                this.spawner = (SpawnerTrait)getClosestCore();
            }
        }
    }

    public void beginRespawning(SpawnerTrait spawner){
        this.spawner = spawner;
        this.lastSpawner = spawner;
        this.dead = true;
    }

    public void endRespawning(){
        spawner = null;
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
        buffer.writeByte(Pack.byteValue(isAdmin) | (Pack.byteValue(dead) << 1) | (Pack.byteValue(isBoosting) << 2));
        buffer.writeInt(Color.rgba8888(color));
        buffer.writeByte(mech.id);
        buffer.writeInt(mining == null ? -1 : mining.pos());
        buffer.writeInt(spawner == null ? noSpawner : spawner.getTile().pos());
        buffer.writeShort((short) (baseRotation * 2));

        writeBuilding(buffer);
    }

    @Override
    public void read(DataInput buffer) throws IOException{
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

        interpolator.read(lastx, lasty, x, y, rotation, baseRotation);
        rotation = lastrot;

        if(isLocal){
            x = lastx;
            y = lasty;
        }else{
            mining = world.tile(mine);
            isBoosting = boosting;
            Tile tile = world.tile(spawner);
            if(tile != null && tile.entity instanceof SpawnerTrait){
                this.spawner = (SpawnerTrait)tile.entity;
            }else{
                this.spawner = null;
            }
        }
    }

    //endregion
}
