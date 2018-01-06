package io.anuke.mindustry.resource;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;

public class Weapon extends Upgrade{
	public static final Weapon

	blaster = new Weapon("blaster", 15, BulletType.shot){
		{
			effect =  Fx.shoot3;
		}
	},
	triblaster = new Weapon("blaster", 13, BulletType.shot){
		{
			shots = 3;
			effect = Fx.shoot;
		}
	},
	multigun = new Weapon("blaster", 6, BulletType.multishot){
		{
			effect = Fx.shoot2;
			inaccuracy = 8f;
		}
	},
	flamer = new Weapon("blaster", 5, BulletType.flame){
		{
			shootsound = "flame2";
			inaccuracy = 12f;
		}
	},
	railgun = new Weapon("blaster", 40, BulletType.sniper){
		{
			shootsound = "railgun";
			effect = Fx.railshoot;
		}
	},
	mortar = new Weapon("blaster", 100, BulletType.shell){
		{
			shootsound = "bigshot";
			effect = Fx.mortarshoot;
			shake = 2f;
		}
	};
	float reload;
	BulletType type;
	String shootsound = "shoot";
	int shots = 1;
	float inaccuracy = 0f;
	float shake = 0f;
	Effect effect;

	public final String description;
	public final String name;
	
	private Weapon(String name, float reload, BulletType type){
		this.reload = reload;
		this.type = type;
		this.name = name;
		this.description = Bundles.getNotNull("weapon."+name+".description");
	}

	public void update(Player p){
		if(Timers.get(p, "reload", reload)){
			shoot(p, p.x, p.y, Angles.mouseAngle(p.x, p.y));
		}
	}


	void shootInternal(Player p, float x, float y, float rotation){
		Angles.shotgun(shots, 12f, rotation, f -> bullet(p, x, y, f + Mathf.range(inaccuracy)));
		Angles.translation(rotation, 3f);
		if(effect != null) Effects.effect(effect, x + Angles.x(), y + Angles.y());
		Effects.shake(shake, shake, x, y);
		Effects.sound(shootsound, x, y);
	}

	void shoot(Player p, float x, float y, float angle){
		shootInternal(p, x, y, angle);

		if(Net.active() && p == Vars.player){
			Vars.netClient.handleShoot(this, x, y, angle);
		}
	}
	
	void bullet(Entity owner, float x, float y, float angle){
		Angles.translation(angle, 3f);
		new Bullet(type, owner,  x + Angles.x(), y + Angles.y(), angle).add();
	}

	public String localized(){
		return Bundles.get("weapon."+name + ".name");
	}
	
	private static ItemStack stack(Item item, int amount){
		return new ItemStack(item, amount);
	}
}
