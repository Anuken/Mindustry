package io.anuke.mindustry.type;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.*;
import io.anuke.arc.audio.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.Effects.*;
import io.anuke.mindustry.entities.bullet.*;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.entities.type.Bullet;
import io.anuke.mindustry.gen.*;

import static io.anuke.mindustry.Vars.net;

public class Weapon{
    public String name;

    /** minimum cursor distance from player, fixes 'cross-eyed' shooting. */
    protected static float minPlayerDist = 20f;
    protected static int sequenceNum = 0;
    /** bullet shot */
    public @NonNull BulletType bullet;
    /** shell ejection effect */
    public Effect ejectEffect = Fx.none;
    /** weapon reload in frames */
    public float reload;
    /** amount of shots per fire */
    public int shots = 1;
    /** spacing in degrees between multiple shots, if applicable */
    public float spacing = 12f;
    /** inaccuracy of degrees of each shot */
    public float inaccuracy = 0f;
    /** intensity and duration of each shot's screen shake */
    public float shake = 0f;
    /** visual weapon knockback. */
    public float recoil = 1.5f;
    /** shoot barrel y offset */
    public float length = 3f;
    /** shoot barrel x offset. */
    public float width = 4f;
    /** fraction of velocity that is random */
    public float velocityRnd = 0f;
    /** whether to shoot the weapons in different arms one after another, rather than all at once */
    public boolean alternate = false;
    /** randomization of shot length */
    public float lengthRand = 0f;
    /** delay in ticks between shots */
    public float shotDelay = 0;
    /** whether shooter rotation is ignored when shooting. */
    public boolean ignoreRotation = false;

    public Sound shootSound = Sounds.pew;

    public TextureRegion region;

    protected Weapon(String name){
        this.name = name;
    }

    public Weapon(){
        //no region
        this.name = "";
    }

    @Remote(targets = Loc.server, called = Loc.both, unreliable = true)
    public static void onPlayerShootWeapon(Player player, float x, float y, float rotation, boolean left){

        if(player == null) return;
        //clients do not see their own shoot events: they are simulated completely clientside to prevent laggy visuals
        //messing with the firerate or any other stats does not affect the server (take that, script kiddies!)
        if(net.client() && player == Vars.player){
            return;
        }

        shootDirect(player, x, y, rotation, left);
    }

    @Remote(targets = Loc.server, called = Loc.both, unreliable = true)
    public static void onGenericShootWeapon(ShooterTrait shooter, float x, float y, float rotation, boolean left){
        if(shooter == null) return;
        shootDirect(shooter, x, y, rotation, left);
    }

    public static void shootDirect(ShooterTrait shooter, float offsetX, float offsetY, float rotation, boolean left){
        float x = shooter.getX() + offsetX;
        float y = shooter.getY() + offsetY;
        float baseX = shooter.getX(), baseY = shooter.getY();

        Weapon weapon = shooter.getWeapon();
        weapon.shootSound.at(x, y, Mathf.random(0.8f, 1.0f));

        sequenceNum = 0;
        if(weapon.shotDelay > 0.01f){
            Angles.shotgun(weapon.shots, weapon.spacing, rotation, f -> {
                Time.run(sequenceNum * weapon.shotDelay, () -> weapon.bullet(shooter, x + shooter.getX() - baseX, y + shooter.getY() - baseY, f + Mathf.range(weapon.inaccuracy)));
                sequenceNum++;
            });
        }else{
            Angles.shotgun(weapon.shots, weapon.spacing, rotation, f -> weapon.bullet(shooter, x, y, f + Mathf.range(weapon.inaccuracy)));
        }

        BulletType ammo = weapon.bullet;

        Tmp.v1.trns(rotation + 180f, ammo.recoil);

        shooter.velocity().add(Tmp.v1);

        Tmp.v1.trns(rotation, 3f);

        Effects.shake(weapon.shake, weapon.shake, x, y);
        Effects.effect(weapon.ejectEffect, x, y, rotation * -Mathf.sign(left));
        Effects.effect(ammo.shootEffect, x + Tmp.v1.x, y + Tmp.v1.y, rotation, shooter);
        Effects.effect(ammo.smokeEffect, x + Tmp.v1.x, y + Tmp.v1.y, rotation, shooter);

        //reset timer for remote players
        shooter.getTimer().get(shooter.getShootTimer(left), weapon.reload);
    }

    public void load(){
        region = Core.atlas.find(name + "-equip", Core.atlas.find(name, Core.atlas.find("clear")));
    }

    public void update(ShooterTrait shooter, float pointerX, float pointerY){
        for(boolean left : Mathf.booleans){
            Tmp.v1.set(pointerX, pointerY).sub(shooter.getX(), shooter.getY());
            if(Tmp.v1.len() < minPlayerDist) Tmp.v1.setLength(minPlayerDist);

            float cx = Tmp.v1.x + shooter.getX(), cy = Tmp.v1.y + shooter.getY();

            float ang = Tmp.v1.angle();
            Tmp.v1.trns(ang - 90, width * Mathf.sign(left), length + Mathf.range(lengthRand));

            update(shooter, shooter.getX() + Tmp.v1.x, shooter.getY() + Tmp.v1.y, Angles.angle(shooter.getX() + Tmp.v1.x, shooter.getY() + Tmp.v1.y, cx, cy), left);
        }
    }

    public void update(ShooterTrait shooter, float mountX, float mountY, float angle, boolean left){
        if(shooter.getTimer().get(shooter.getShootTimer(left), reload)){
            if(alternate){
                shooter.getTimer().reset(shooter.getShootTimer(!left), reload / 2f);
            }

            shoot(shooter, mountX - shooter.getX(), mountY - shooter.getY(), angle, left);
        }
    }

    public float getRecoil(ShooterTrait player, boolean left){
        return (1f - Mathf.clamp(player.getTimer().getTime(player.getShootTimer(left)) / reload)) * recoil;
    }

    public void shoot(ShooterTrait p, float x, float y, float angle, boolean left){
        if(net.client()){
            //call it directly, don't invoke on server
            shootDirect(p, x, y, angle, left);
        }else{
            if(p instanceof Player){ //players need special weapon handling logic
                Call.onPlayerShootWeapon((Player)p, x, y, angle, left);
            }else{
                Call.onGenericShootWeapon(p, x, y, angle, left);
            }
        }
    }

    void bullet(ShooterTrait owner, float x, float y, float angle){
        if(owner == null) return;

        Tmp.v1.trns(angle, 3f);
        Bullet.create(bullet,
        owner, owner.getTeam(), x + Tmp.v1.x, y + Tmp.v1.y, angle, (1f - velocityRnd) + Mathf.random(velocityRnd));
    }
}
