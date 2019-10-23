package io.anuke.mindustry.entities.type;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.arc.util.pooling.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.input.*;
import io.anuke.mindustry.io.*;
import io.anuke.mindustry.net.Administration.*;
import io.anuke.mindustry.net.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.type.TypeID;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.ui.Cicon;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

public class Player extends Unit implements BuilderMinerTrait, ShooterTrait{
    public static final int timerSync = 2;
    public static final int timerAbility = 3;
    public static final int timerTransfer = 4;
    private static final int timerShootLeft = 0;
    private static final int timerShootRight = 1;
    private static final float liftoffBoost = 0.2f;

    private static final Rectangle rect = new Rectangle();

    //region instance variables

    public float baseRotation;
    public float pointerX, pointerY;
    public String name = "noname";
    public @Nullable
    String uuid, usid;
    public boolean isAdmin, isTransferring, isShooting, isBoosting, isMobile, isTyping, isBuilding = true;
    public float boostHeat, shootHeat, destructTime;
    public boolean achievedFlight;
    public Color color = new Color();
    public Mech mech = Mechs.starter;
    public SpawnerTrait spawner, lastSpawner;
    public int respawns;

    public @Nullable NetConnection con;
    public boolean isLocal = false;
    public Interval timer = new Interval(6);
    public TargetTrait target;
    public TargetTrait moveTarget;

    public @Nullable String lastText;
    public float textFadeTime;

    private float walktime, itemtime;
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
    }

    @Override
    public float getDamageMultipler(){
        return status.getDamageMultiplier() * state.rules.playerDamageMultiplier;
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
        velocity.setZero();
        boostHeat = 1f;
        achievedFlight = true;
        rotation = 90f;
        baseRotation = 90f;
        dead = false;
        spawner = null;
        respawns --;
        Sounds.respawn.at(tile);

        setNet(tile.drawx(), tile.drawy());
        clearItem();
        heal();
    }

    @Override
    public TypeID getTypeID(){
        return TypeIDs.player;
    }

    @Override
    public void move(float x, float y){
        if(!mech.flying){
            collisions.move(this, x, y);
        }else{
            moveBy(x, y);
        }
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
        return mech.icon(Cicon.full);
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
        return mech.health * state.rules.playerHealthMultiplier;
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
    public void damage(float amount){
        hitTime = hitDuration;
        if(!net.client()){
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
    public Queue<BuildRequest> buildQueue(){
        return placeQueue;
    }

    @Override
    public String toString(){
        return "Player{" + name + ", mech=" + mech.name + ", id=" + id + ", local=" + isLocal + ", " + x + ", " + y + "}";
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
        return isLocal ? Float.MAX_VALUE : 40 + placeDistance;
    }

    @Override
    public void drawShadow(float offsetX, float offsetY){
        float scl = mech.flying ? 1f : boostHeat / 2f;

        Draw.rect(getIconRegion(), x + offsetX * scl, y + offsetY * scl, rotation - 90);
    }

    @Override
    public void draw(){
        if(dead) return;

        if(!movement.isZero() && moved && !state.isPaused()){
            walktime += movement.len() * getFloorOn().speedMultiplier * 2f;
            baseRotation = Mathf.slerpDelta(baseRotation, movement.angle(), 0.13f);
        }

        float ft = Mathf.sin(walktime, 6f, 2f) * (1f - boostHeat);

        Floor floor = getFloorOn();

        Draw.color();
        Draw.mixcol(Color.white, hitTime / hitDuration);

        if(!mech.flying){
            if(floor.isLiquid){
                Draw.color(Color.white, floor.color, 0.5f);
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
            Draw.color(Color.white, floor.color, drownTime);
        }else{
            Draw.color(Color.white);
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

        Draw.reset();
    }

    @Override
    public void drawStats(){
        Draw.color(Color.black, team.color, healthf() + Mathf.absin(Time.time(), healthf() * 5f, 1f - healthf()));
        Draw.rect(getPowerCellRegion(), x + Angles.trnsx(rotation, mech.cellTrnsY, 0f), y + Angles.trnsy(rotation, mech.cellTrnsY, 0f), rotation - 90);
        Draw.reset();
        drawBackItems(itemtime, isLocal);
    }

    @Override
    public void drawOver(){
        if(dead) return;

        if(isBuilding()){
            if(!state.isPaused() && isBuilding){
                drawBuilding();
            }
        }else{
            drawMining();
        }
    }

    @Override
    public void drawUnder(){
        if(dead) return;

        float size = mech.engineSize * (mech.flying ? 1f : boostHeat);
        Draw.color(mech.engineColor);
        Fill.circle(x + Angles.trnsx(rotation + 180, mech.engineOffset), y + Angles.trnsy(rotation + 180, mech.engineOffset),
        size + Mathf.absin(Time.time(), 2f, size / 4f));

        Draw.color(Color.white);
        Fill.circle(x + Angles.trnsx(rotation + 180, mech.engineOffset - 1f), y + Angles.trnsy(rotation + 180, mech.engineOffset - 1f),
        (size + Mathf.absin(Time.time(), 2f, size / 4f)) / 2f);
        Draw.color();
    }

    public void drawName(){
        BitmapFont font = Fonts.def;
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        final float nameHeight = 11;
        final float textHeight = 15;

        boolean ints = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);
        font.getData().setScale(0.25f / Scl.scl(1f));
        layout.setText(font, name);

        if(!isLocal){
            Draw.color(0f, 0f, 0f, 0.3f);
            Fill.rect(x, y + nameHeight - layout.height / 2, layout.width + 2, layout.height + 3);
            Draw.color();
            font.setColor(color);
            font.draw(name, x, y + nameHeight, 0, Align.center, false);

            if(isAdmin){
                float s = 3f;
                Draw.color(color.r * 0.5f, color.g * 0.5f, color.b * 0.5f, 1f);
                Draw.rect(Core.atlas.find("icon-admin-badge"), x + layout.width / 2f + 2 + 1, y + nameHeight - 1.5f, s, s);
                Draw.color(color);
                Draw.rect(Core.atlas.find("icon-admin-badge"), x + layout.width / 2f + 2 + 1, y + nameHeight - 1f, s, s);
            }
        }

        if(Core.settings.getBool("playerchat") && ((textFadeTime > 0 && lastText != null) || isTyping)){
            String text = textFadeTime <= 0 || lastText == null ? "[LIGHT_GRAY]" + Strings.animated(Time.time(), 4, 15f, ".") : lastText;
            float width = 100f;
            float visualFadeTime = 1f - Mathf.curve(1f - textFadeTime, 0.9f);
            font.setColor(1f, 1f, 1f, textFadeTime <= 0 || lastText == null ? 1f : visualFadeTime);

            layout.setText(font, text, Color.white, width, Align.bottom, true);

            Draw.color(0f, 0f, 0f, 0.3f * (textFadeTime <= 0 || lastText == null  ? 1f : visualFadeTime));
            Fill.rect(x, y + textHeight + layout.height - layout.height/2f, layout.width + 2, layout.height + 3);
            font.draw(text, x - width/2f, y + textHeight + layout.height, width, Align.center, true);
        }

        Draw.reset();
        Pools.free(layout);
        font.getData().setScale(1f);
        font.setColor(Color.white);
        font.setUseIntegerPositions(ints);
    }

    /** Draw all current build requests. Does not draw the beam effect, only the positions. */
    public void drawBuildRequests(){
        if(!isLocal) return;

        for(BuildRequest request : buildQueue()){
            if(request.progress > 0.01f || (buildRequest() == request && request.initialized && (dst(request.x * tilesize, request.y * tilesize) <= placeDistance || state.isEditor()))) continue;

            request.animScale = 1f;
            if(request.breaking){
                control.input.drawBreaking(request);
            }else{
                request.block.drawRequest(request, control.input.allRequests(),
                    Build.validPlace(getTeam(), request.x, request.y, request.block, request.rotation) || control.input.requestMatches(request));
            }
        }

        Draw.reset();
    }

    //endregion

    //region update methods

    @Override
    public void updateMechanics(){
        if(isBuilding){
            updateBuilding();
        }

        //mine only when not building
        if(buildRequest() == null){
            updateMining();
        }
    }

    @Override
    public void update(){
        hitTime -= Time.delta();
        textFadeTime -= Time.delta() / (60 * 5);
        itemtime = Mathf.lerpDelta(itemtime, Mathf.num(item.amount > 0), 0.1f);

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

        if(!isDead() && isFlying()){
            loops.play(Sounds.thruster, this, Mathf.clamp(velocity.len() * 2f) * 0.3f);
        }

        BuildRequest request = buildRequest();
        if(isBuilding() && isBuilding && request.tile() != null && (request.tile().withinDst(x, y, placeDistance) || state.isEditor())){
            loops.play(Sounds.build, request.tile(), 0.75f);
        }

        if(isDead()){
            isBoosting = false;
            boostHeat = 0f;
            if(respawns > 0 || !state.rules.limitedRespawns){
                updateRespawning();
            }
            return;
        }else{
            spawner = null;
        }

        if(isLocal || net.server()){
            avoidOthers();
        }

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
                Effects.effect(Fx.unitLand, tile.floor().color, x, y, tile.floor().isLiquid ? 1f : 0.5f);
            }
            mech.onLand(this);
            achievedFlight = false;
        }

        if(!isLocal){
            interpolate();
            updateMechanics(); //building happens even with non-locals
            status.update(this); //status effect updating also happens with non locals for effect purposes
            updateVelocityStatus(); //velocity too, for visual purposes

            if(net.server()){
                updateShooting(); //server simulates player shooting
            }
            return;
        }else if(world.isZone()){
            //unlock mech when used
            data.unlockContent(mech);
        }

        if(control.input instanceof MobileInput){
            updateTouch();
        }else{
            updateKeyboard();
        }

        isTyping = ui.chatfrag.chatOpen();

        updateMechanics();

        if(!mech.flying){
            clampPosition();
        }
    }

    protected void updateKeyboard(){
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
        if(!(Core.scene.getKeyboardFocus() instanceof TextField)){
            movement.y += ya * speed;
            movement.x += xa * speed;
        }

        Vector2 vec = Core.input.mouseWorld(control.input.getMouseX(), control.input.getMouseY());
        pointerX = vec.x;
        pointerY = vec.y;
        updateShooting();

        movement.limit(speed).scl(Time.delta());

        if(!ui.chatfrag.chatOpen()){
            velocity.add(movement.x, movement.y);
        }else{
            isShooting = false;
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
                float angle = control.input.mouseAngle(x, y);
                this.rotation = Mathf.slerpDelta(this.rotation, angle, 0.1f * baseLerp);
            }
        }
    }

    protected void updateShooting(){
        if(!state.isEditor() && isShooting() && mech.canShoot(this)){
            if(!mech.turnCursor){
                //shoot forward ignoring cursor
                mech.weapon.update(this, x + Angles.trnsx(rotation, 1f), y + Angles.trnsy(rotation, 1f));
            }else{
                mech.weapon.update(this, pointerX, pointerY);
            }
        }
    }

    protected void updateTouch(){
        if(Units.invalidateTarget(target, this) &&
            !(target instanceof TileEntity && ((TileEntity)target).damaged() && target.isValid() && target.getTeam() == team && mech.canHeal && dst(target) < getWeapon().bullet.range() && !(((TileEntity)target).block instanceof BuildBlock))){
            target = null;
        }

        if(state.isEditor()){
            target = null;
        }

        float targetX = Core.camera.position.x, targetY = Core.camera.position.y;
        float attractDst = 15f;
        float speed = isBoosting && !mech.flying ? mech.boostSpeed : mech.speed;

        if(moveTarget != null && !moveTarget.isDead()){
            targetX = moveTarget.getX();
            targetY = moveTarget.getY();
            boolean tapping = moveTarget instanceof TileEntity && moveTarget.getTeam() == team;
            attractDst = 0f;

            if(tapping){
                velocity.setAngle(angleTo(moveTarget));
            }

            if(dst(moveTarget) <= 2f * Time.delta()){
                if(tapping && !isDead()){
                    Tile tile = ((TileEntity)moveTarget).tile;
                    tile.block().tapped(tile, this);
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
        if(!isBuilding() && getMineTile() == null){

            //autofire
            if(target == null){
                isShooting = false;
                if(Core.settings.getBool("autotarget")){
                    target = Units.closestTarget(team, x, y, getWeapon().bullet.range(), u -> u.getTeam() != Team.derelict, u -> u.getTeam() != Team.derelict);

                    if(mech.canHeal && target == null){
                        target = Geometry.findClosest(x, y, indexer.getDamaged(Team.sharded));
                        if(target != null && dst(target) > getWeapon().bullet.range()){
                            target = null;
                        }else if(target != null){
                            target = ((Tile)target).entity;
                        }
                    }

                    if(target != null){
                        setMineTile(null);
                    }
                }
            }else if(target.isValid() || (target instanceof TileEntity && ((TileEntity)target).damaged() && target.getTeam() == team &&
            mech.canHeal && dst(target) < getWeapon().bullet.range())){
                //rotate toward and shoot the target
                if(mech.turnCursor){
                    rotation = Mathf.slerpDelta(rotation, angleTo(target), 0.2f);
                }

                Vector2 intercept = Predict.intercept(this, target, getWeapon().bullet.speed);

                pointerX = intercept.x;
                pointerY = intercept.y;

                updateShooting();
                isShooting = true;
            }

        }
    }

    //endregion

    //region utility methods

    public void sendMessage(String text){
        if(isLocal){
            if(Vars.ui != null){
                Log.info("add " + text);
                Vars.ui.chatfrag.addMessage(text, null);
            }
        }else{
            Call.sendMessage(con, text, null, null);
        }
    }

    public void sendMessage(String text, Player from){
        sendMessage(text, from, NetClient.colorizeName(from.id, from.name));
    }

    public void sendMessage(String text, Player from, String fromName){
        if(isLocal){
            if(Vars.ui != null){
                Vars.ui.chatfrag.addMessage(text, fromName);
            }
        }else{
            Call.sendMessage(con, text, fromName, from);
        }
    }

    public PlayerInfo getInfo(){
        if(uuid == null){
            throw new IllegalArgumentException("Local players cannot be traced and do not have info.");
        }else{
            return netServer.admins.getInfo(uuid);
        }
    }

    /** Resets all values of the player. */
    public void reset(){
        resetNoAdd();

        add();
    }

    public void resetNoAdd(){
        status.clear();
        team = Team.sharded;
        item.amount = 0;
        placeQueue.clear();
        dead = true;
        lastText = null;
        isBuilding = true;
        textFadeTime = 0f;
        target = null;
        moveTarget = null;
        isShooting = isBoosting = isTransferring = isTyping = false;
        spawner = lastSpawner = null;
        health = maxHealth();
        mining = null;
        boostHeat = drownTime = hitTime = 0f;
        mech = Mechs.starter;
        placeQueue.clear();
        respawns = state.rules.respawns;
    }

    public boolean isShooting(){
        return isShooting && (boostHeat < 0.1f || mech.flying) && mining == null;
    }

    public void updateRespawning(){

        if(state.isEditor()){
            //instant respawn at center of map.
            set(world.width() * tilesize/2f, world.height() * tilesize/2f);
            setDead(false);
        }else if(spawner != null && spawner.isValid()){
            spawner.updateSpawning(this);
        }else if(!netServer.isWaitingForPlayers()){
            if(!net.client()){
                if(lastSpawner != null && lastSpawner.isValid()){
                    this.spawner = lastSpawner;
                }else if(getClosestCore() != null){
                    this.spawner = (SpawnerTrait)getClosestCore();
                }
            }
        }else if(getClosestCore() != null){
            set(getClosestCore().getX(), getClosestCore().getY());
        }
    }

    public void beginRespawning(SpawnerTrait spawner){
        this.spawner = spawner;
        this.lastSpawner = spawner;
        this.dead = true;
        setNet(spawner.getX(), spawner.getY());
        spawner.updateSpawning(this);
    }

    //endregion

    //region read and write methods

    @Override
    public byte version(){
        return 0;
    }

    @Override
    public void writeSave(DataOutput stream) throws IOException{
        stream.writeBoolean(isLocal);

        if(isLocal){
            stream.writeByte(mech.id);
            stream.writeInt(lastSpawner == null ? noSpawner : lastSpawner.getTile().pos());
            super.writeSave(stream, false);
        }
    }

    @Override
    public void readSave(DataInput stream, byte version) throws IOException{
        boolean local = stream.readBoolean();

        if(local){
            byte mechid = stream.readByte();
            int spawner = stream.readInt();
            Tile stile = world.tile(spawner);
            Player player = headless ? this : Vars.player;
            player.readSaveSuper(stream, version);
            player.mech = content.getByID(ContentType.mech, mechid);
            player.dead = false;
            if(stile != null && stile.entity instanceof SpawnerTrait){
                player.lastSpawner = (SpawnerTrait)stile.entity;
            }
        }
    }

    private void readSaveSuper(DataInput stream, byte version) throws IOException{
        super.readSave(stream, version);

        add();
    }

    @Override
    public void write(DataOutput buffer) throws IOException{
        super.writeSave(buffer, !isLocal);
        TypeIO.writeStringData(buffer, name);
        buffer.writeByte(Pack.byteValue(isAdmin) | (Pack.byteValue(dead) << 1) | (Pack.byteValue(isBoosting) << 2) | (Pack.byteValue(isTyping) << 3)| (Pack.byteValue(isBuilding) << 4));
        buffer.writeInt(Color.rgba8888(color));
        buffer.writeByte(mech.id);
        buffer.writeInt(mining == null ? noSpawner : mining.pos());
        buffer.writeInt(spawner == null || !spawner.hasUnit(this) ? noSpawner : spawner.getTile().pos());
        buffer.writeShort((short)(baseRotation * 2));

        writeBuilding(buffer);
    }

    @Override
    public void read(DataInput buffer) throws IOException{
        float lastx = x, lasty = y, lastrot = rotation, lastvx = velocity.x, lastvy = velocity.y;

        super.readSave(buffer, version());

        name = TypeIO.readStringData(buffer);
        byte bools = buffer.readByte();
        isAdmin = (bools & 1) != 0;
        dead = (bools & 2) != 0;
        boolean boosting = (bools & 4) != 0;
        isTyping = (bools & 8) != 0;
        boolean building = (bools & 16) != 0;
        color.set(buffer.readInt());
        mech = content.getByID(ContentType.mech, buffer.readByte());
        int mine = buffer.readInt();
        int spawner = buffer.readInt();
        float baseRotation = buffer.readShort() / 2f;

        readBuilding(buffer, !isLocal);

        interpolator.read(lastx, lasty, x, y, rotation, baseRotation);
        rotation = lastrot;
        x = lastx;
        y = lasty;

        if(isLocal){
            velocity.x = lastvx;
            velocity.y = lastvy;
        }else{
            mining = world.tile(mine);
            isBuilding = building;
            isBoosting = boosting;
        }

        Tile tile = world.tile(spawner);
        if(tile != null && tile.entity instanceof SpawnerTrait){
            this.spawner = (SpawnerTrait)tile.entity;
        }else{
            this.spawner = null;
        }
    }

    //endregion
}
