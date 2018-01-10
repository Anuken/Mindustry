package io.anuke.mindustry.entities.enemies;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

import static io.anuke.mindustry.Vars.world;

public class EnemyType {

    //TODO documentation, comments
    private static byte lastid = 0;
    private static Array<EnemyType> types = new Array<>();

    public final static Color[] tierColors = { Color.valueOf("ffe451"), Color.valueOf("f48e20"), Color.valueOf("ff6757"), Color.valueOf("ff2d86") };
    public final static int maxtier = 4;
    public final static float maxIdle = 60*1.5f;
    public final static float maxIdleLife = 60f*13f; //13 seconds idle = death
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
    protected float mass = 1f;

    protected final int timerTarget = timeid ++;
    protected final int timerReload = timeid ++;

    public EnemyType(String name){
        this.id = lastid++;
        this.name = name;
        types.add(this);
    }

    public void draw(Enemy enemy){
        String region = name + "-t" + Mathf.clamp(enemy.tier, 1, 3);

        Shaders.outline.color.set(tierColors[enemy.tier - 1]);
        Shaders.outline.lighten = Mathf.clamp(enemy.hitTime/hitDuration);
        Shaders.outline.region = Draw.region(region);

        Shaders.outline.apply();

        Draw.rect(region, enemy.x, enemy.y, enemy.angle - 90);
        Draw.color();

        Graphics.flush();
        Shaders.outline.lighten = 0f;
    }

    public void drawOver(Enemy enemy){ }

    public void update(Enemy enemy){
        float lastx = enemy.x, lasty = enemy.y;
        if(enemy.hitTime > 0){
            enemy.hitTime -= Timers.delta();
        }

        move(enemy);

        enemy.velocity.set(enemy.x - lastx, enemy.y - lasty).scl(1f / Timers.delta());

        float minv = 0.07f;

        if(enemy.velocity.len() < minv && enemy.node > 0 && enemy.target == null){
            enemy.idletime += Timers.delta();
        }else{
            enemy.idletime = 0;
        }

        Tile tile = world.tileWorld(enemy.x, enemy.y);
        if(tile != null && tile.floor().liquid && tile.block() == Blocks.air){
            enemy.damage(enemy.health+1); //drown
        }

        if(Float.isNaN(enemy.angle)){
            enemy.angle = 0;
        }

        if(enemy.target == null || alwaysRotate){
            enemy.angle = Mathf.slerp(enemy.angle, enemy.velocity.angle(), rotatespeed * Timers.delta());
        }else{
            enemy.angle = Mathf.slerp(enemy.angle, enemy.angleTo(enemy.target), turretrotatespeed * Timers.delta());
        }
    }

    public void move(Enemy enemy){
        float speed = this.speed + 0.04f * enemy.tier;
        float range = this.range + enemy.tier * 5;

        if(Net.client() && Net.active()){
            enemy.inter.update(enemy); //TODO? better structure for interpolation
            return;
        }

        Tile core = Vars.control.getCore();

        if(enemy.idletime > maxIdleLife){
            enemy.onDeath();
            return;
        }

        boolean nearCore = enemy.distanceTo(core.worldx(), core.worldy()) <= range - 18f && stopNearCore;
        Vector2 vec;

        if(nearCore){
            vec = Tmp.v1.setZero();
            if(targetCore) enemy.target = core.entity;
        }else{
            vec = Vars.world.pathfinder().find(enemy);
            vec.sub(enemy.x, enemy.y).limit(speed);
        }

        Vector2 shift = Tmp.v3.setZero();
        float shiftRange = enemy.hitbox.width + 2f;
        float avoidRange = shiftRange + 4f;
        float attractRange = avoidRange + 7f;
        float avoidSpeed = this.speed/2.7f;

        Entities.getNearby(Vars.control.enemyGroup, enemy.x, enemy.y, range, en -> {
            Enemy other = (Enemy)en;
            if(other == enemy) return;
            float dst = other.distanceTo(enemy);

            if(dst < shiftRange){
                float scl = Mathf.clamp(1.4f - dst / shiftRange) * mass * 1f/mass;
                shift.add((enemy.x - other.x) * scl, (enemy.y - other.y) * scl);
            }else if(dst < avoidRange){
                Tmp.v2.set((enemy.x - other.x), (enemy.y - other.y)).setLength(avoidSpeed);
                shift.add(Tmp.v2.scl(1.1f));
            }else if(dst < attractRange && !nearCore){
                Tmp.v2.set((enemy.x - other.x), (enemy.y - other.y)).setLength(avoidSpeed);
                shift.add(Tmp.v2.scl(-1));
            }
        });

        shift.limit(1f);
        vec.add(shift.scl(0.5f));

        enemy.move(vec.x * Timers.delta(), vec.y * Timers.delta());

        updateTargeting(enemy, nearCore);
    }

    public void updateTargeting(Enemy enemy, boolean nearCore){
        if(enemy.target != null && enemy.target instanceof TileEntity && ((TileEntity)enemy.target).dead){
            enemy.target = null;
        }

        if(enemy.timer.get(timerTarget, 15) && !nearCore){
            enemy.target = Vars.world.findTileTarget(enemy.x, enemy.y, null, range, false);

            //no tile found
            if(enemy.target == null){
                enemy.target = Entities.getClosest(Vars.control.playerGroup, enemy.x, enemy.y, range, e -> true);
            }
        }else if(nearCore){
            enemy.target = Vars.control.getCore().entity;
        }

        if(enemy.target != null && bullet != null){
            updateShooting(enemy);
        }
    }

    public void updateShooting(Enemy enemy){
        float reload = this.reload / Math.max(enemy.tier / 1.5f, 1f);

        if(enemy.timer.get(timerReload, reload * Vars.multiplier)){
            shoot(enemy);
        }
    }

    public void shoot(Enemy enemy){
        enemy.shoot(bullet);
        if(shootsound != null) Effects.sound(shootsound, enemy);
    }

    public void onShoot(Enemy enemy, BulletType type, float rotation){}

    public void onDeath(Enemy enemy){
        if(Net.active() && Net.server()){
            Vars.netServer.handleEnemyDeath(enemy);
        }

        Effects.effect(Fx.explosion, enemy);
        Effects.shake(3f, 4f, enemy);
        Effects.sound("bang2", enemy);
        enemy.remove();
        enemy.dead = true;
    }

    public void removed(Enemy enemy){
        if(!enemy.dead){
            if(enemy.spawner != null){
                enemy.spawner.spawned --;
            }else{
                Vars.control.enemyDeath();
            }
        }
    }

    public static EnemyType getByID(byte id){
        return types.get(id);
    }
}
