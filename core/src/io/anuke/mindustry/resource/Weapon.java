package io.anuke.mindustry.resource;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;

public enum Weapon{
	blaster(15, BulletType.shot){
		{
			unlocked = true;
		}
		
		@Override
		public void shootInternal(Player p, float x, float y, float rotation){
			super.shootInternal(p, x, y, rotation);
			Effects.effect(Fx.shoot3, x + vector.x, y+vector.y);
		}
	},
	triblaster(13, BulletType.shot, stack(Item.iron, 40)){
		
		@Override
		public void shootInternal(Player p, float x, float y, float rotation){
			float space = 12;
			
			bullet(p, x, y, rotation);
			bullet(p, x, y, rotation + space);
			bullet(p, x, y, rotation - space);
			
			Effects.effect(Fx.shoot, x + vector.x, y + vector.y);
			
		}
	},
	multigun(6, BulletType.multishot, stack(Item.iron, 60), stack(Item.steel, 20)){
		@Override
		public void shootInternal(Player p, float x, float y, float rotation){
			MathUtils.random.setSeed(Gdx.graphics.getFrameId());
			
			bullet(p, x, y, rotation + Mathf.range(8));
			
			Effects.effect(Fx.shoot2, x + vector.x, y + vector.y);
		}
	},
	flamer(5, BulletType.flame, stack(Item.steel, 60), stack(Item.iron, 120)){
		
		{
			shootsound = "flame2";
		}
		
		@Override
		public void shootInternal(Player p, float x, float y, float rotation){
			MathUtils.random.setSeed(Gdx.graphics.getFrameId());
			
			bullet(p, x, y, rotation + Mathf.range(12));
		}
	},
	railgun(40, BulletType.sniper,  stack(Item.steel, 60), stack(Item.iron, 60)){
		
		{
			shootsound = "railgun";
		}
		
		@Override
		public void shootInternal(Player p, float x, float y, float rotation){
			bullet(p, x, y, rotation);
			Effects.effect(Fx.railshoot, x + vector.x, y + vector.y);
		}
	},
	mortar(100, BulletType.shell, stack(Item.titanium, 40), stack(Item.steel, 60)){
		
		{
			shootsound = "bigshot";
		}
		
		@Override
		public void shootInternal(Player p, float x, float y, float rotation){
			bullet(p, x, y, rotation);
			Effects.effect(Fx.mortarshoot, x + vector.x, y + vector.y);
			Effects.shake(2f, 2f, p);
		}
	};
	public float reload;
	public BulletType type;
	public String shootsound = "shoot";
	public boolean unlocked;
	public ItemStack[] requirements;
	public final String description;

	Vector2 vector = new Vector2();

	public String localized(){
		return Bundles.get("weapon."+name() + ".name");
	}
	
	private Weapon(float reload, BulletType type, ItemStack... requirements){
		this.reload = reload;
		this.type = type;
		this.requirements = requirements;
		this.description = Bundles.getNotNull("weapon."+name()+".description");
	}

	void shootInternal(Player p, float x, float y, float rotation){
		bullet(p, x, y, rotation);
	}

	public void shoot(Player p, float x, float y, float angle){
		shootInternal(p, x, y, angle);

		if(Net.active() && p == Vars.player){
			Vars.netClient.handleShoot(this, x, y, angle);
		}
	}
	
	void bullet(Entity owner, float x, float y, float angle){
		vector.set(3, 0).rotate(angle);
		new Bullet(type, owner,  x + vector.x, y + vector.y, angle).add();
	}
	
	private static ItemStack stack(Item item, int amount){
		return new ItemStack(item, amount);
	}
}
