package io.anuke.mindustry.resource;

import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Bits;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Translator;

public class Weapon extends Upgrade{
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

	protected Weapon(String name){
		super(name);
	}

	public void update(Player p, boolean left){
		int t = left ? 1 : 2;
		int t2 = !left ? 1 : 2;
		if(p.inventory.hasAmmo() && p.timer.get(t, reload)){
			if(roundrobin){
				p.timer.reset(t2, reload/2f);
			}

			tr.set(Graphics.mouseWorld()).sub(p.x, p.y);
			if(tr.len() < minPlayerDist) tr.setLength(minPlayerDist);

			float cx = tr.x + p.x, cy = tr.y + p.y;

			float ang = tr.angle();
			tr.trns(ang - 90, 4f * Mathf.sign(left), length + 1f);

			shoot(p, p.x + tr.x, p.y + tr.y, Angles.angle(p.x + tr.x, p.y + tr.y, cx, cy), left);
		}
	}

	public float getRecoil(Player player, boolean left){
		return 1f-Mathf.clamp(player.timer.getTime(left ? 1 : 2)/reload);
	}

	public float getReload(){
		return reload;
	}

	public void shoot(Player p, float x, float y, float angle, boolean left){
		shootInternal(p, x, y, angle, left);

		if(Net.active() && p == Vars.player){
			NetEvents.handleShoot(Vars.player, x, y, angle, Bits.packShort(id, (byte)(left ? 1 : 0)));
		}

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

	void shootInternal(Player p, float x, float y, float rotation, boolean left){
		Angles.shotgun(shots, spacing, rotation, f -> bullet(p, x, y, f + Mathf.range(inaccuracy)));
		tr.trns(rotation, 3f);

		AmmoType type = p.inventory.getAmmo();

		Effects.shake(shake, shake, x, y);
		Effects.effect(ejectEffect, x, y, rotation * -Mathf.sign(left));
		Effects.effect(type.shootEffect, x + tr.x, y + tr.y, rotation, p);
		Effects.effect(type.smokeEffect, x + tr.x, y + tr.y, rotation, p);
	}
	
	void bullet(Unit owner, float x, float y, float angle){
		tr.trns(angle, 3f);
		Bullet.create(owner.inventory.getAmmo().bullet, owner, x + tr.x, y + tr.y, angle);
	}
}
