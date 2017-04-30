package io.anuke.moment.entities;

import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.util.Angles;

public enum Weapon{
	blaster(15, BulletType.shot){
		{
			unlocked = true;
		}
	};
	public float reload;
	public BulletType type;
	public boolean unlocked;
	
	private Weapon(float reload, BulletType type){
		this.reload = reload;
		this.type = type;
	}

	public void shoot(Player p){
		bullet(p, p.x, p.y);
	}
	
	void bullet(Entity owner, float x, float y){
		new Bullet(type, owner,  x, y, Angles.mouseAngle(owner.x, owner.y)).add();
	}
}
