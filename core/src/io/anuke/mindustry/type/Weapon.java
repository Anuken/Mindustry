package io.anuke.mindustry.type;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.traits.ShooterTrait;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Translator;

public class Weapon extends Content{
    public final String name;

    /**minimum cursor distance from player, fixes 'cross-eyed' shooting.*/
    protected static float minPlayerDist = 20f;
    /**ammo type map. set with setAmmo()*/
    protected AmmoType ammo;
    /**shell ejection effect*/
    protected Effect ejectEffect = Fx.none;
    /**weapon reload in frames*/
    protected float reload;
    /**amount of shots per fire*/
    protected int shots = 1;
    /**spacing in degrees between multiple shots, if applicable*/
    protected float spacing = 12f;
    /**inaccuracy of degrees of each shot*/
    protected float inaccuracy = 0f;
    /**intensity and duration of each shot's screen shake*/
    protected float shake = 0f;
    /**visual weapon knockback.*/
    protected float recoil = 1.5f;
    /**shoot barrel y offset*/
    protected float length = 3f;
    /**shoot barrel x offset.*/
    protected float width = 4f;
    /**fraction of velocity that is random*/
    protected float velocityRnd = 0f;
    /**whether to shoot the weapons in different arms one after another, rather than all at once*/
    protected boolean roundrobin = false;
    /**translator for vector calulations*/
    protected Translator tr = new Translator();

    public TextureRegion equipRegion, region;

    protected Weapon(String name){
        this.name = name;
    }

    @Remote(targets = Loc.server, called = Loc.both, unreliable = true)
    public static void onPlayerShootWeapon(Player player, float x, float y, float rotation, boolean left){
        if(player == null) return;
        //clients do not see their own shoot events: they are simulated completely clientside to prevent laggy visuals
        //messing with the firerate or any other stats does not affect the server (take that, script kiddies!)
        if(Net.client() && player == Vars.players[0]){
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

        Weapon weapon = shooter.getWeapon();

        Angles.shotgun(weapon.shots, weapon.spacing, rotation, f -> weapon.bullet(shooter, x, y, f + Mathf.range(weapon.inaccuracy)));
        AmmoType ammo = weapon.ammo;

        weapon.tr.trns(rotation + 180f, ammo.recoil);

        shooter.getVelocity().add(weapon.tr);

        weapon.tr.trns(rotation, 3f);

        Effects.shake(weapon.shake, weapon.shake, x, y);
        Effects.effect(weapon.ejectEffect, x, y, rotation * -Mathf.sign(left));
        Effects.effect(ammo.shootEffect, x + weapon.tr.x, y + weapon.tr.y, rotation, shooter);
        Effects.effect(ammo.smokeEffect, x + weapon.tr.x, y + weapon.tr.y, rotation, shooter);

        //reset timer for remote players
        shooter.getTimer().get(shooter.getShootTimer(left), weapon.reload);
    }

    @Override
    public void load(){
        equipRegion = Draw.region(name + "-equip");
        region = Draw.region(name);
    }

    @Override
    public ContentType getContentType(){
        return ContentType.weapon;
    }

    public AmmoType getAmmo(){
        return ammo;
    }

    public void update(ShooterTrait shooter, float pointerX, float pointerY){
        update(shooter, true, pointerX, pointerY);
        update(shooter, false, pointerX, pointerY);
    }

    private void update(ShooterTrait shooter, boolean left, float pointerX, float pointerY){
        if(shooter.getTimer().get(shooter.getShootTimer(left), reload)){
            if(roundrobin){
                shooter.getTimer().reset(shooter.getShootTimer(!left), reload / 2f);
            }

            tr.set(pointerX, pointerY).sub(shooter.getX(), shooter.getY());
            if(tr.len() < minPlayerDist) tr.setLength(minPlayerDist);

            float cx = tr.x + shooter.getX(), cy = tr.y + shooter.getY();

            float ang = tr.angle();
            tr.trns(ang - 90, width * Mathf.sign(left), length);

            shoot(shooter, tr.x, tr.y, Angles.angle(shooter.getX() + tr.x, shooter.getY() + tr.y, cx, cy), left);
        }
    }

    public float getRecoil(ShooterTrait player, boolean left){
        return (1f - Mathf.clamp(player.getTimer().getTime(player.getShootTimer(left)) / reload)) * recoil;
    }

    public float getRecoil(){
        return recoil;
    }

    public float getReload(){
        return reload;
    }

    public void shoot(ShooterTrait p, float x, float y, float angle, boolean left){
        if(Net.client()){
            //call it directly, don't invoke on server
            shootDirect(p, x, y, angle, left);
        }else{
            if(p instanceof Player){ //players need special weapon handling logic
                Call.onPlayerShootWeapon((Player) p, x, y, angle, left);
            }else{
                Call.onGenericShootWeapon(p, x, y, angle, left);
            }
        }
    }

    void bullet(ShooterTrait owner, float x, float y, float angle){
        if(owner == null) return;

        tr.trns(angle, 3f);
        Bullet.create(ammo.bullet,
                owner, owner.getTeam(), x + tr.x, y + tr.y, angle, (1f - velocityRnd) + Mathf.random(velocityRnd));
    }
}
