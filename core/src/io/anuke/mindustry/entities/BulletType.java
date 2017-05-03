package io.anuke.mindustry.entities;

import com.badlogic.gdx.graphics.Color;

import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.entities.BaseBulletType;

public abstract class BulletType  extends BaseBulletType<Bullet>{
	public static final BulletType 
	
	stone = new BulletType(1.5f, 2){
		public void draw(Bullet b){
			Draw.color("gray");
			Draw.square(b.x, b.y, 1f);
			Draw.clear();
		}
	},
	iron = new BulletType(1.7f, 2){
		public void draw(Bullet b){
			Draw.color("gray");
			Draw.rect("bullet", b.x, b.y, b.angle());
			Draw.clear();
		}
	},
	sniper = new BulletType(3f, 17){
		public void draw(Bullet b){
			Draw.color("light gray");
			Draw.rect("bullet", b.x, b.y, b.angle());
			Draw.clear();
		}
	},
	small = new BulletType(1.5f, 1){
		public void draw(Bullet b){
			Draw.color("orange");
			Draw.rect("bullet", b.x, b.y, b.angle());
			Draw.clear();
		}
	},
	smallfast = new BulletType(1.6f, 2){
		Color color = new Color(0x8b5ec9ff);
		public void draw(Bullet b){
			Draw.color(color);
			Draw.rect("bullet", b.x, b.y, b.angle());
			Draw.clear();
		}
	},
	flame = new BulletType(0.6f, 4){
		public void draw(Bullet b){
			Draw.color(Color.YELLOW, Color.SCARLET, b.time/lifetime);
			float size = 6f-b.time/lifetime*5f;
			Draw.rect("circle", b.x, b.y, size, size);
			Draw.clear();
		}
	},
	flameshot = new BulletType(0.5f, 3){
		public void draw(Bullet b){
			Draw.color(Color.ORANGE, Color.SCARLET, b.time/lifetime);
			float size = 6f-b.time/lifetime*5f;
			Draw.rect("circle", b.x, b.y, size, size);
			Draw.clear();
		}
	},
	shot = new BulletType(2.4f, 2){
		{lifetime=40;}
		public void draw(Bullet b){
			Draw.color(Color.GOLD);
			Draw.rect("bullet", b.x, b.y, b.angle());
			Draw.clear();
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
