package io.anuke.moment.entities;

import com.badlogic.gdx.graphics.Color;

import io.anuke.ucore.core.Draw;
import io.anuke.ucore.entities.Effects;

public enum BulletType{
	stone(1.5f, 2){
		public void draw(Bullet b){
			Draw.color("gray");
			Draw.square(b.x, b.y, 1f);
			Draw.clear();
		}
	},
	iron(1.7f, 2){
		public void draw(Bullet b){
			Draw.color("gray");
			Draw.rect("bullet", b.x, b.y, b.angle());
			Draw.clear();
		}
	},
	sniper(3f, 17){
		public void draw(Bullet b){
			Draw.color("light gray");
			Draw.rect("bullet", b.x, b.y, b.angle());
			Draw.clear();
		}
	},
	small(1.5f, 1){
		public void draw(Bullet b){
			Draw.color("orange");
			Draw.rect("bullet", b.x, b.y, b.angle());
			Draw.clear();
		}
	},
	smallfast(1.6f, 2){
		Color color = new Color(0x8b5ec9ff);
		public void draw(Bullet b){
			Draw.color(color);
			Draw.rect("bullet", b.x, b.y, b.angle());
			Draw.clear();
		}
	},
	flame(0.6f, 4){
		public void draw(Bullet b){
			Draw.color(Color.YELLOW, Color.SCARLET, b.time/lifetime);
			float size = 6f-b.time/lifetime*5f;
			Draw.rect("circle", b.x, b.y, size, size);
			Draw.clear();
		}
	},
	flameshot(0.5f, 3){
		public void draw(Bullet b){
			Draw.color(Color.ORANGE, Color.SCARLET, b.time/lifetime);
			float size = 6f-b.time/lifetime*5f;
			Draw.rect("circle", b.x, b.y, size, size);
			Draw.clear();
		}
	},
	shot(2.4f, 2){
		{lifetime=40;}
		public void draw(Bullet b){
			Draw.color(Color.GOLD);
			Draw.rect("bullet", b.x, b.y, b.angle());
			Draw.clear();
		}
	};;
	public float speed;
	public int damage;
	public float lifetime = 60;
	
	private BulletType(float speed, int damage){
		this.speed = speed;
		this.damage = damage;
	}
	
	public void collide(Bullet b){
		Effects.effect("hit", b);
	}
	public void draw(Bullet b){}
}
