package io.anuke.mindustry.resource;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public class Weapon extends Upgrade{
	public static final Weapon

	blaster = new Weapon("blaster", 12, BulletType.shot){
		{
			effect =  Fx.laserShoot;
			length = 2f;
		}
	},
	triblaster = new Weapon("triblaster", 16, BulletType.spread){
		{
			shots = 3;
			effect = Fx.spreadShoot;
			roundrobin = true;
		}
	},
	clustergun = new Weapon("clustergun", 26f, BulletType.cluster){
		{
			effect = Fx.clusterShoot;
			inaccuracy = 17f;
			roundrobin = true;
			shots = 2;
			spacing = 0;
		}
	},
	beam = new Weapon("beam", 30f, BulletType.beamlaser){
		{
			effect = Fx.beamShoot;
			inaccuracy = 0;
			roundrobin = true;
			shake = 2f;
		}
	},
	vulcan = new Weapon("vulcan", 5, BulletType.vulcan){
		{
			effect = Fx.vulcanShoot;
			inaccuracy = 5;
			roundrobin = true;
			shake = 1f;
			inaccuracy = 4f;
		}
	},
	shockgun = new Weapon("shockgun", 36, BulletType.shockshell){
		{
			shootsound = "bigshot";
			effect = Fx.shockShoot;
			shake = 2f;
			roundrobin = true;
			shots = 7;
			inaccuracy = 15f;
			length = 3.5f;
		}
	};
	/**weapon reload in frames*/
	float reload;
	/**type of bullet shot*/
	BulletType type;
	/**sound made when shooting*/
	String shootsound = "shoot";
	/**amount of shots per fire*/
	int shots = 1;
	/**spacing in degrees between multiple shots, if applicable*/
	float spacing = 12f;
	/**inaccuracy of degrees of each shot*/
	float inaccuracy = 0f;
	/**intensity and duration of each shot's screen shake*/
	float shake = 0f;
	/**effect displayed when shooting*/
	Effect effect;
	/**shoot barrel length*/
	float length = 3f;
	/**whether to shoot the weapons in different arms one after another, rather an all at once*/
	boolean roundrobin = false;
	
	private Weapon(String name, float reload, BulletType type){
		super(name);
		this.reload = reload;
		this.type = type;
	}

	public void update(Player p, boolean left){
		if(Timers.get(p, "reload"+left, reload)){
			if(roundrobin){
				Timers.reset(p, "reload" + !left, reload/2f);
			}
			float ang = Angles.mouseAngle(p.x, p.y);
			Angles.vector.set(3f * Mathf.sign(left), length).rotate(ang - 90);
			shoot(p, p.x + Angles.x(), p.y + Angles.y(), Angles.mouseAngle(p.x + Angles.x(), p.y + Angles.y()));
		}
	}

	void shootInternal(Player p, float x, float y, float rotation){
		Angles.shotgun(shots, spacing, rotation, f -> bullet(p, x, y, f + Mathf.range(inaccuracy)));
		Angles.translation(rotation, 3f);
		if(effect != null) Effects.effect(effect, x + Angles.x(), y + Angles.y(), rotation);
		Effects.shake(shake, shake, x, y);
		Effects.sound(shootsound, x, y);
	}

	public void shoot(Player p, float x, float y, float angle){
		shootInternal(p, x, y, angle);

		if(Net.active() && p == Vars.player){
			NetEvents.handleShoot(this, x, y, angle);
		}
	}
	
	void bullet(Entity owner, float x, float y, float angle){
		Angles.translation(angle, 3f);
		new Bullet(type, owner,  x + Angles.x(), y + Angles.y(), angle).add();
	}
}
