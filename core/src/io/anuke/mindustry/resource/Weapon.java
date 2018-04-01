package io.anuke.mindustry.resource;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.bullets.BulletType;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Translator;

public class Weapon extends Upgrade{
	/**weapon reload in frames*/
	protected float reload;
	/**type of bullet shot*/
	protected BulletType type;
	/**sound made when shooting*/
	protected String shootsound = "shoot";
	/**amount of shots per fire*/
	protected int shots = 1;
	/**spacing in degrees between multiple shots, if applicable*/
	protected float spacing = 12f;
	/**inaccuracy of degrees of each shot*/
	protected float inaccuracy = 0f;
	/**intensity and duration of each shot's screen shake*/
	protected float shake = 0f;
	/**effect displayed when shooting*/
	protected Effect effect;
	/**shoot barrel length*/
	protected float length = 3f;
	/**whether to shoot the weapons in different arms one after another, rather an all at once*/
	protected boolean roundrobin = false;
	/**translator for vector calulations*/
	protected Translator tr = new Translator();

	protected Weapon(String name, float reload, BulletType type){
		super(name);
		this.reload = reload;
		this.type = type;
	}

	public void update(Player p, boolean left){
		int t = left ? 1 : 2;
		int t2 = !left ? 1 : 2;
		if(p.timer.get(t, reload)){
			if(roundrobin){
				p.timer.reset(t2, reload/2f);
			}
			float ang = Angles.mouseAngle(p.x, p.y);
			tr.trns(ang - 90, 4f * Mathf.sign(left), length + 1f);
			shoot(p, p.x + tr.x, p.y + tr.y, Angles.mouseAngle(p.x + tr.x, p.y + tr.y));
		}
	}

	void shootInternal(Player p, float x, float y, float rotation){
		Angles.shotgun(shots, spacing, rotation, f -> bullet(p, x, y, f + Mathf.range(inaccuracy)));
		tr.trns(rotation, 3f);
		if(effect != null) Effects.effect(effect, x + tr.x, y + tr.y, rotation);
		Effects.shake(shake, shake, x, y);
		Effects.sound(shootsound, x, y);
	}

	public float getReload(){
		return reload;
	}

	public void shoot(Player p, float x, float y, float angle){
		shootInternal(p, x, y, angle);

		if(Net.active() && p == Vars.player){
			NetEvents.handleShoot(Vars.player, x, y, angle, id);
		}
	}
	
	void bullet(Unit owner, float x, float y, float angle){
		tr.trns(angle, 3f);
		new Bullet(type, owner,  x + tr.x, y + tr.y, angle).add();
	}
}
