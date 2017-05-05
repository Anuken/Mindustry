package io.anuke.mindustry.entities;

import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.util.Angles;

public enum Weapon{
	blaster(15, BulletType.shot, "Shoots a slow, weak bullet."){
		{
			unlocked = true;
		}
	},
	trishot(15, BulletType.shot, "Shoots 3 bullets in a spread.", stack(Item.iron, 40)){
		
		@Override
		public void shoot(Player p){
			float ang = mouseAngle(p);
			float space = 12;
			
			bullet(p, p.x, p.y, ang);
			bullet(p, p.x, p.y, ang+space);
			bullet(p, p.x, p.y, ang-space);
		}
	};
	public float reload;
	public BulletType type;
	public boolean unlocked;
	public ItemStack[] requirements;
	public String description = "no desc for you";
	
	private Weapon(float reload, BulletType type, String desc, ItemStack... requirements){
		this.reload = reload;
		this.type = type;
		this.requirements = requirements;
		this.description = desc;
	}

	public void shoot(Player p){
		bullet(p, p.x, p.y, mouseAngle(p));
	}
	
	float mouseAngle(Entity owner){
		return Angles.mouseAngle(owner.x, owner.y);
	}
	
	void bullet(Entity owner, float x, float y, float angle){
		new Bullet(type, owner,  x, y, angle).add();
	}
	
	private static ItemStack stack(Item item, int amount){
		return new ItemStack(item, amount);
	}
}
