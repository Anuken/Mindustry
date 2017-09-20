package io.anuke.mindustry.entities;

import com.badlogic.gdx.graphics.Color;

import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.BaseBulletType;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public abstract class BulletType  extends BaseBulletType<Bullet>{
	public static final BulletType 
	
	none = new BulletType(0f, 0){
		public void draw(Bullet b){
			
		}
	},
	stone = new BulletType(1.5f, 2){
		public void draw(Bullet b){
			Draw.color("gray");
			Draw.square(b.x, b.y, 1f);
			Draw.reset();
		}
	},
	iron = new BulletType(1.7f, 2){
		public void draw(Bullet b){
			Draw.color("gray");
			Draw.rect("bullet", b.x, b.y, b.angle());
			Draw.reset();
		}
	},
	sniper = new BulletType(3f, 20){
		public void draw(Bullet b){
			Draw.color(Color.LIGHT_GRAY);
			Draw.rect("bullet", b.x, b.y, b.angle());
			Draw.reset();
		}
	},
	shell = new BulletType(1.1f, 110){
		{
			lifetime = 110f;
			hitsize = 8f;
		}
		public void draw(Bullet b){
			float rad = 8f;
			Draw.color(Color.GRAY);
			Draw.rect("circle", b.x, b.y, rad, rad);
			rad += Mathf.sin(Timers.time(), 3f, 1f);
			Draw.color(Color.ORANGE);
			Draw.rect("circle", b.x, b.y, rad/1.7f, rad/1.7f);
			Draw.reset();
		}
		
		public void update(Bullet b){
			if(Timers.get(b, "smoke", 7)){
				Effects.effect("smoke", b.x + Mathf.range(2), b.y + Mathf.range(2));
			}
		}
		
		public void despawned(Bullet b){
			removed(b);
		}
		
		public void removed(Bullet b){
			Effects.shake(3f, 3f, b);
			
			Effects.effect("shellsmoke", b);
			Effects.effect("shellexplosion", b);
			
			Angles.circle(25, f->{
				Angles.translation(f, 5f);
				new Bullet(shellshot, b.owner, b.x + Angles.x(), b.y + Angles.y(), f).add();
			});
		}
	},
	shellshot = new BulletType(1.5f, 6){
		{
			lifetime = 7f;
		}
		public void draw(Bullet b){
		//	Draw.color("orange");
		//	Draw.rect("bullet", b.x, b.y, b.angle());
		//	Draw.reset();
		}
	},
	small = new BulletType(1.5f, 1){
		public void draw(Bullet b){
			Draw.color("orange");
			Draw.rect("bullet", b.x, b.y, b.angle());
			Draw.reset();
		}
	},
	smallfast = new BulletType(1.6f, 2){
		Color color = new Color(0x8b5ec9ff);
		public void draw(Bullet b){
			Draw.color(color);
			Draw.rect("bullet", b.x, b.y, b.angle());
			Draw.reset();
		}
	},
	flame = new BulletType(0.6f, 4){
		public void draw(Bullet b){
			Draw.color(Color.YELLOW, Color.SCARLET, b.time/lifetime);
			float size = 6f-b.time/lifetime*5f;
			Draw.rect("circle", b.x, b.y, size, size);
			Draw.reset();
		}
	},
	flameshot = new BulletType(0.5f, 3){
		public void draw(Bullet b){
			Draw.color(Color.ORANGE, Color.SCARLET, b.time/lifetime);
			float size = 6f-b.time/lifetime*5f;
			Draw.rect("circle", b.x, b.y, size, size);
			Draw.reset();
		}
	},
	shot = new BulletType(2.4f, 2){
		{lifetime=40;}
		public void draw(Bullet b){
			Draw.color(Color.GOLD);
			Draw.rect("bullet", b.x, b.y, b.angle());
			Draw.reset();
		}
	},
	shot2 = new BulletType(2.5f, 2){
		{lifetime=40;}
		public void draw(Bullet b){
			Draw.color(Color.SKY);
			Draw.rect("bullet", b.x, b.y, b.angle());
			Draw.reset();
		}
	};
	
	private BulletType(float speed, int damage){
		this.speed = speed;
		this.damage = damage;
	}
	
	@Override
	public void removed(Bullet b){
		Effects.effect("hit", b);
	}
}
