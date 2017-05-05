package io.anuke.mindustry.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public enum Weapon{
	blaster(15, BulletType.shot, "Shoots a slow, weak bullet."){
		{
			unlocked = true;
		}
		
		@Override
		public void shoot(Player p){
			super.shoot(p);
			Effects.effect("shoot3", p.x + vector.x, p.y+vector.y);
		}
	},
	trishot(13, BulletType.shot, "Shoots 3 bullets in a spread.", stack(Item.iron, 40)){
		
		@Override
		public void shoot(Player p){
			float ang = mouseAngle(p);
			float space = 12;
			
			bullet(p, p.x, p.y, ang);
			bullet(p, p.x, p.y, ang+space);
			bullet(p, p.x, p.y, ang-space);
			
			Effects.effect("shoot", p.x + vector.x, p.y+vector.y);
			
		}
	},
	multigun(6, BulletType.shot2, "Shoots inaccurate bullets with a high\nrate of fire.", stack(Item.iron, 60), stack(Item.steel, 20)){
		@Override
		public void shoot(Player p){
			float ang = mouseAngle(p);
			MathUtils.random.setSeed(Gdx.graphics.getFrameId());
			
			bullet(p, p.x, p.y, ang + Mathf.range(8));
			
			Effects.effect("shoot2", p.x + vector.x, p.y+vector.y);
		}
	},
	flamethrower(5, BulletType.flame, "Shoots a stream of fire.", stack(Item.steel, 60), stack(Item.coal, 60)){
		
		{
			shootsound = "flame2";
		}
		
		@Override
		public void shoot(Player p){
			float ang = mouseAngle(p);
			//????
			MathUtils.random.setSeed(Gdx.graphics.getFrameId());
			
			bullet(p, p.x, p.y, ang + Mathf.range(12));
		}
	};
	float reload;
	BulletType type;
	public String shootsound = "shoot";
	public boolean unlocked;
	public ItemStack[] requirements;
	public String description = "no desc for you";
	
	Vector2 vector = new Vector2();
	
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
		vector.set(3, 0).rotate(mouseAngle(owner));
		new Bullet(type, owner,  x+vector.x, y+vector.y, angle).add();
	}
	
	private static ItemStack stack(Item item, int amount){
		return new ItemStack(item, amount);
	}
}
