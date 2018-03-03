package io.anuke.mindustry.entities.enemies;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

import static io.anuke.mindustry.Vars.*;

public class EnemyType {

    //TODO documentation, comments
    private static byte lastid = 0;
    private static Array<EnemyType> types = new Array<>();

    public final static Color[] tierColors = {
            Color.valueOf("ffe451"), Color.valueOf("f48e20"), Color.valueOf("ff6757"),
            Color.valueOf("ff2d86"), Color.valueOf("cb2dff"), Color.valueOf("362020") };
    public final static int maxtier = tierColors.length;
    public final static float maxIdleLife = 60f*2f; //2 seconds idle = death
    public final static float hitDuration = 5f;

    public final String name;
    public final byte id;

    protected int timeid;
    protected int health = 60;
    protected float hitsize = 5f;
    protected float hitsizeTile = 4f;
    protected float speed = 0.4f;
    protected float reload = 32;
    protected float range = 60;
    protected float length = 4;
    protected float rotatespeed = 0.1f;
    protected float turretrotatespeed = 0.2f;
    protected boolean alwaysRotate = false;
    protected BulletType bullet = BulletType.small;
    protected String shootsound = "enemyshoot";
    protected boolean targetCore = false;
    protected boolean stopNearCore = true;
    protected boolean targetClient = false;
    protected float mass = 1f;

    protected final int timerTarget = timeid ++;
    protected final int timerReload = timeid ++;
    protected final int timerReset = timeid ++;

    protected final Vector2 shift = new Vector2();
    protected final Vector2 move = new Vector2();
    protected final Vector2 calc = new Vector2();

    public EnemyType(String name){
        this.id = lastid++;
        this.name = name;
        types.add(this);
    }

    public void draw(Enemy enemy){
        Shaders.outline.color.set(tierColors[enemy.tier - 1]);
        Shaders.outline.lighten = Mathf.clamp(enemy.hitTime/hitDuration);
        Shaders.outline.region = enemy.region;

        Shaders.outline.apply();

        Draw.rect(enemy.region, enemy.x, enemy.y, enemy.angle - 90);
        Draw.color();

        Graphics.flush();

        if(isCalculating(enemy)){
            Draw.color(Color.SKY);
            Lines.polySeg(20, 0, 4, enemy.x, enemy.y, 11f, Timers.time() * 2f + enemy.id*52f);
            Lines.polySeg(20, 0, 4, enemy.x, enemy.y, 11f, Timers.time() * 2f + enemy.id*52f + 180f);
            Draw.color();
        }

        if(showPaths){
            Draw.tscl(0.25f);
            Draw.text((int)enemy.idletime + " " + enemy.node + " " + enemy.id + "\n" + Strings.toFixed(enemy.totalMove.x, 2) + ", "
                    + Strings.toFixed(enemy.totalMove.x, 2), enemy.x, enemy.y);
            Draw.tscl(fontscale);
        }

        Shaders.outline.lighten = 0f;
    }

    public void drawOver(Enemy enemy){ }

    public void update(Enemy enemy){
        float lastx = enemy.x, lasty = enemy.y;
        if(enemy.hitTime > 0){
            enemy.hitTime -= Timers.delta();
        }

        if(enemy.lane >= world.getSpawns().size || enemy.lane < 0) enemy.lane = 0;

        boolean waiting = enemy.lane >= world.getSpawns().size || enemy.lane < 0
                || world.getSpawns().get(enemy.lane).pathTiles == null || enemy.node <= 0;

        move(enemy);

        enemy.velocity.set(enemy.x - lastx, enemy.y - lasty).scl(1f / Timers.delta());
        enemy.totalMove.add(enemy.velocity);

        float minv = 0.07f;

        if(enemy.timer.get(timerReset, 80)){
            enemy.totalMove.setZero();
        }

        if(enemy.velocity.len() < minv && !waiting && enemy.target == null){
            enemy.idletime += Timers.delta();
        }else{
            enemy.idletime = 0;
        }

        if(enemy.timer.getTime(timerReset) > 50 && enemy.totalMove.len() < 0.2f && !waiting && enemy.target == null){
            enemy.idletime = 999999f;
        }

        Tile tile = world.tileWorld(enemy.x, enemy.y);
        if(tile != null && tile.floor().liquid && tile.block() == Blocks.air){
            enemy.damage(enemy.health+1); //drown
        }

        if(Float.isNaN(enemy.angle)){
            enemy.angle = 0;
        }

        if(enemy.target == null || alwaysRotate){
            enemy.angle = Mathf.slerpDelta(enemy.angle, enemy.velocity.angle(), rotatespeed);
        }else{
            enemy.angle = Mathf.slerpDelta(enemy.angle, enemy.angleTo(enemy.target), turretrotatespeed);
        }

        enemy.x = Mathf.clamp(enemy.x, 0, world.width() * tilesize);
        enemy.y = Mathf.clamp(enemy.y, 0, world.height() * tilesize);
    }

    public void move(Enemy enemy){
        if(Net.client()){
            enemy.interpolate();
            if(targetClient) updateTargeting(enemy, false);
            return;
        }

        float speed = this.speed + 0.04f * enemy.tier;
        float range = this.range + enemy.tier * 5;

        Tile core = world.getCore();

        if(core == null) return;

        if(enemy.idletime > maxIdleLife && enemy.node > 0){
            enemy.onDeath();
            return;
        }

        boolean nearCore = enemy.distanceTo(core.worldx(), core.worldy()) <= range - 18f && stopNearCore;
        Vector2 vec;

        if(nearCore){
            vec = move.setZero();
            if(targetCore) enemy.target = core.entity;
        }else{
            vec = world.pathfinder().find(enemy);
            vec.sub(enemy.x, enemy.y).limit(speed);
        }

        shift.setZero();
        float shiftRange = enemy.hitbox.width + 2f;
        float avoidRange = shiftRange + 4f;
        float attractRange = avoidRange + 7f;
        float avoidSpeed = this.speed/2.7f;

        Entities.getNearby(enemyGroup, enemy.x, enemy.y, range, en -> {
            Enemy other = (Enemy)en;
            if(other == enemy) return;
            float dst = other.distanceTo(enemy);

            if(dst < shiftRange){
                float scl = Mathf.clamp(1.4f - dst / shiftRange) * mass * 1f/mass;
                shift.add((enemy.x - other.x) * scl, (enemy.y - other.y) * scl);
            }else if(dst < avoidRange){
                calc.set((enemy.x - other.x), (enemy.y - other.y)).setLength(avoidSpeed);
                shift.add(calc.scl(1.1f));
            }else if(dst < attractRange && !nearCore && !isCalculating(enemy)){
                calc.set((enemy.x - other.x), (enemy.y - other.y)).setLength(avoidSpeed);
                shift.add(calc.scl(-1));
            }
        });

        shift.limit(1f);
        vec.add(shift.scl(0.5f));

        enemy.move(vec.x * Timers.delta(), vec.y * Timers.delta());

        updateTargeting(enemy, nearCore);

        behavior(enemy);
    }

    public void behavior(Enemy enemy){}

    public void updateTargeting(Enemy enemy, boolean nearCore){
        if(enemy.target != null && enemy.target instanceof TileEntity && ((TileEntity)enemy.target).dead){
            enemy.target = null;
        }

        if(enemy.timer.get(timerTarget, 15) && !nearCore){
            enemy.target = world.findTileTarget(enemy.x, enemy.y, null, range, false);

            //no tile found
            if(enemy.target == null){
                enemy.target = Entities.getClosest(playerGroup, enemy.x, enemy.y, range, e -> !((Player)e).isAndroid &&
                    !((Player)e).isDead());
            }
        }else if(nearCore){
            enemy.target = world.getCore().entity;
        }

        if(enemy.target != null && bullet != null){
            updateShooting(enemy);
        }
    }

    public void updateShooting(Enemy enemy){
        float reload = this.reload / Math.max(enemy.tier / 1.5f, 1f);

        if(enemy.timer.get(timerReload, reload)){
            shoot(enemy);
        }
    }

    public void shoot(Enemy enemy){
        enemy.shoot(bullet);
        if(shootsound != null) Effects.sound(shootsound, enemy);
    }

    public void onShoot(Enemy enemy, BulletType type, float rotation){}

    public void onDeath(Enemy enemy, boolean force){
        if(Net.server()){
            NetEvents.handleEnemyDeath(enemy);
        }

        if(!Net.client() || force) {
            Effects.effect(Fx.explosion, enemy);
            Effects.shake(3f, 4f, enemy);
            Effects.sound("bang2", enemy);
            enemy.remove();
            enemy.dead = true;
        }
    }

    public void removed(Enemy enemy){
        if(!enemy.dead){
            if(enemy.spawner != null){
                enemy.spawner.spawned --;
            }else{
                state.enemies --;
            }
        }
    }

    public boolean isCalculating(Enemy enemy){
        return enemy.node < 0 && !Net.client();
    }

    public static EnemyType getByID(byte id){
        return types.get(id);
    }
}
