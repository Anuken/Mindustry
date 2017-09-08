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
