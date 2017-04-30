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
	small(1.5f, 1){
		public void draw(Bullet b){
			Draw.color("orange");
			Draw.rect("bullet", b.x, b.y, b.angle());
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
