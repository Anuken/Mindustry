package io.anuke.mindustry.type;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.gen.CallEntity;
import io.anuke.mindustry.net.In;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Translator;

public class Weapon extends Upgrade {
	/**minimum cursor distance from player, fixes 'cross-eyed' shooting.*/
	protected static float minPlayerDist = 20f;
	/**ammo type map. set with setAmmo()*/
	protected ObjectMap<Item, AmmoType> ammoMap = new ObjectMap<>();
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
	/**shoot barrel length*/
	protected float length = 3f;
	/**whether to shoot the weapons in different arms one after another, rather than all at once*/
	protected boolean roundrobin = false;
	/**translator for vector calulations*/
	protected Translator tr = new Translator();

	public TextureRegion equipRegion, region;

	protected Weapon(String name){
		super(name);
	}

	@Override
	public void load() {
		equipRegion = Draw.region(name + "-equip");
		region = Draw.region(name);
	}

	public void update(Player p, boolean left, float pointerX, float pointerY){
		int t = left ? Player.timerShootLeft : Player.timerShootRight;
		int t2 = !left ? Player.timerShootLeft : Player.timerShootRight;
		if(p.inventory.hasAmmo() && p.timer.get(t, reload)){
			if(roundrobin){
				p.timer.reset(t2, reload/2f);
			}

			tr.set(pointerX, pointerY).sub(p.x, p.y);
			if(tr.len() < minPlayerDist) tr.setLength(minPlayerDist);

			float cx = tr.x + p.x, cy = tr.y + p.y;

			float ang = tr.angle();
			tr.trns(ang - 90, 4f * Mathf.sign(left), length + 1f);

			shoot(p, p.x + tr.x, p.y + tr.y, Angles.angle(p.x + tr.x, p.y + tr.y, cx, cy), left);
		}
	}

	public float getRecoil(Player player, boolean left){
		return 1f-Mathf.clamp(player.timer.getTime(left ? Player.timerShootLeft : Player.timerShootRight)/reload);
	}

	public float getReload(){
		return reload;
	}

	public void shoot(Player p, float x, float y, float angle, boolean left){
		CallEntity.onShootWeapon(p, this, x, y, angle, left);
		p.inventory.useAmmo();
	}

	public AmmoType getAmmoType(Item item){
		return ammoMap.get(item);
	}

	protected void setAmmo(AmmoType... types){
		for(AmmoType type : types){
			ammoMap.put(type.item, type);
		}
	}
	
	void bullet(Unit owner, float x, float y, float angle){
		tr.trns(angle, 3f);
		Bullet.create(owner.inventory.getAmmo().bullet, owner, x + tr.x, y + tr.y, angle);
	}

	@Remote(targets = Loc.both, called = Loc.both, in = In.entities, unreliable = true, forward = true)
	public static void onShootWeapon(Player player, Weapon weapon, float x, float y, float rotation, boolean left){
		Angles.shotgun(weapon.shots, weapon.spacing, rotation, f -> weapon.bullet(player, x, y, f + Mathf.range(weapon.inaccuracy)));

		AmmoType type = player.inventory.getAmmo();

		weapon.tr.trns(rotation + 180f, type.recoil);

		player.getVelocity().add(weapon.tr);

		weapon.tr.trns(rotation, 3f);

		Effects.shake(weapon.shake, weapon.shake, x, y);
		Effects.effect(weapon.ejectEffect, x, y, rotation * -Mathf.sign(left));
		Effects.effect(type.shootEffect, x + weapon.tr.x, y + weapon.tr.y, rotation, player);
		Effects.effect(type.smokeEffect, x + weapon.tr.x, y + weapon.tr.y, rotation, player);

		//reset timer for remote players
		player.timer.get(left ? Player.timerShootLeft : Player.timerShootRight, weapon.reload);
	}
}
