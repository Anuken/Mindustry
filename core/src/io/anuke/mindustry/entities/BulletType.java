package io.anuke.mindustry.entities;

import com.badlogic.gdx.graphics.Color;

import io.anuke.mindustry.entities.effect.DamageArea;
import io.anuke.mindustry.entities.effect.EMP;
import io.anuke.mindustry.entities.effect.Fx;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.BaseBulletType;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public abstract class BulletType  extends BaseBulletType<Bullet>{
	static Color glowy = Color.valueOf("fdc056");
	static Color lightGold = Hue.mix(Color.GOLD, Color.WHITE, 0.4f);
	static Color lightRed = Hue.mix(Color.WHITE, Color.FIREBRICK, 0.1f);
	static Color lightOrange = Color.valueOf("f68021");
	static Color whiteOrange = Hue.mix(lightOrange, Color.WHITE, 0.6f);
	static Color whiteYellow = Hue.mix(Color.YELLOW, Color.WHITE, 0.6f);
	
	public static final BulletType 
	
	none = new BulletType(0f, 0){
		public void draw(Bullet b){}
	},
	stone = new BulletType(1.5f, 2){
		public void draw(Bullet b){
			Draw.colorl(0.64f);
			Draw.rect("blank", b.x, b.y, 2f, 2f);
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
	chain = new BulletType(2f, 8){
		public void draw(Bullet b){
			Draw.color(whiteOrange);
			Draw.rect("chainbullet", b.x, b.y, b.angle());
			Draw.reset();
		}
	},
	sniper = new BulletType(3f, 25){
		public void draw(Bullet b){
			Draw.color(Color.LIGHT_GRAY);
			Draw.thick(1f);
			Draw.lineAngleCenter(b.x, b.y, b.angle(), 3f);
			Draw.reset();
		}
		
		public void update(Bullet b){
			if(Timers.get(b, "smoke", 4)){
				Effects.effect(Fx.railsmoke, b.x, b.y);
			}
		}
	},
	emp = new BulletType(1.6f, 8){
		{
			lifetime = 50f;
			hitsize = 6f;
		}
		
		public void draw(Bullet b){
			float rad = 6f + Mathf.sin(Timers.time(), 5f, 2f);
			
			Draw.color(Color.SKY);
			Draw.circle(b.x, b.y, 4f);
			Draw.rect("circle", b.x, b.y, rad, rad);
			Draw.reset();
		}
		
		public void update(Bullet b){
			if(Timers.get(b, "smoke", 2)){
				Effects.effect(Fx.empspark, b.x + Mathf.range(2), b.y + Mathf.range(2));
			}
		}
		
		public void despawned(Bullet b){
			removed(b);
		}
		
		public void removed(Bullet b){
			Timers.run(5f, ()->{
				new EMP(b.x, b.y, b.getDamage()).add();
			});
			Effects.effect(Fx.empshockwave, b);
			Effects.shake(3f, 3f, b);
		}
	},
	//TODO better visuals for shell
	shell = new BulletType(1.1f, 60){
		{
			lifetime = 110f;
			hitsize = 11f;
		}
		
		public void draw(Bullet b){
			float rad = 8f;
			Draw.color(Color.ORANGE);
			Draw.color(Color.GRAY);
			Draw.rect("circle", b.x, b.y, rad, rad);
			rad += Mathf.sin(Timers.time(), 3f, 1f);
			Draw.color(Color.ORANGE);
			Draw.rect("circle", b.x, b.y, rad/1.7f, rad/1.7f);
			Draw.reset();
		}
		
		public void update(Bullet b){
			if(Timers.get(b, "smoke", 7)){
				Effects.effect(Fx.smoke, b.x + Mathf.range(2), b.y + Mathf.range(2));
			}
		}
		
		public void despawned(Bullet b){
			removed(b);
		}
		
		public void removed(Bullet b){
			Effects.shake(3f, 3f, b);
			
			Effects.effect(Fx.shellsmoke, b);
			Effects.effect(Fx.shellexplosion, b);
			
			DamageArea.damage(!(b.owner instanceof Enemy), b.x, b.y, 25f, (int)(damage * 2f/3f));
		}
	},
	titanshell = new BulletType(1.8f, 38){
		{
			lifetime = 70f;
			hitsize = 15f;
		}
		
		public void draw(Bullet b){
			Draw.color(whiteOrange);
			Draw.rect("titanshell", b.x, b.y, b.angle());
			Draw.reset();
		}
		
		public void update(Bullet b){
			if(Timers.get(b, "smoke", 4)){
				Effects.effect(Fx.smoke, b.x + Mathf.range(2), b.y + Mathf.range(2));
			}
		}
		
		public void despawned(Bullet b){
			removed(b);
		}
		
		public void removed(Bullet b){
			Effects.shake(3f, 3f, b);
			
			Effects.effect(Fx.shellsmoke, b);
			Effects.effect(Fx.shockwaveSmall, b);
			
			DamageArea.damage(!(b.owner instanceof Enemy), b.x, b.y, 50f, (int)(damage * 2f/3f));
		}
	},
	yellowshell = new BulletType(1.2f, 20){
		{
			lifetime = 60f;
			hitsize = 11f;
		}
		
		public void draw(Bullet b){
			Draw.color(whiteYellow);
			Draw.rect("titanshell", b.x, b.y, b.angle());
			Draw.reset();
		}
		
		public void update(Bullet b){
			if(Timers.get(b, "smoke", 4)){
				Effects.effect(Fx.smoke, b.x + Mathf.range(2), b.y + Mathf.range(2));
			}
		}
		
		public void despawned(Bullet b){
			removed(b);
		}
		
		public void removed(Bullet b){
			Effects.shake(3f, 3f, b);
			
			Effects.effect(Fx.shellsmoke, b);
			Effects.effect(Fx.shockwaveSmall, b);
			
			DamageArea.damage(!(b.owner instanceof Enemy), b.x, b.y, 25f, (int)(damage * 2f/3f));
		}
	},
	blast = new BulletType(1.1f, 80){
		{
			lifetime = 0f;
			hitsize = 8f;
			speed = 0f;
		}
		
		public void despawned(Bullet b){
			removed(b);
		}
		
		public void removed(Bullet b){
			Effects.shake(3f, 3f, b);
			
			Effects.effect(Fx.blastsmoke, b);
			Effects.effect(Fx.blastexplosion, b);
			
			Angles.circle(30, f->{
				Angles.translation(f, 6f);
				Bullet o = new Bullet(blastshot, b.owner, b.x + Angles.x(), b.y + Angles.y(), f).add();
				o.damage = b.damage/9;
			});
		}

		public void draw(Bullet b){}
	},
	shellshot = new BulletType(1.5f, 6){
		{
			lifetime = 7f;
		}
		public void draw(Bullet b){}
	},
	blastshot = new BulletType(1.6f, 6){
		{
			lifetime = 7f;
		}
		public void draw(Bullet b){}
	},
	small = new BulletType(1.5f, 2){
		public void draw(Bullet b){
			Draw.color(glowy);
			Draw.rect("shot", b.x, b.y, b.angle() - 45);
			Draw.reset();
		}
	},
	smallSlow = new BulletType(1.2f, 2){
		public void draw(Bullet b){
			Draw.color("orange");
			Draw.rect("shot", b.x, b.y, b.angle() - 45);
			Draw.reset();
		}
	},
	purple = new BulletType(1.6f, 2){
		Color color = new Color(0x8b5ec9ff);
		
		public void draw(Bullet b){
			Draw.color(color);
			Draw.rect("bullet", b.x, b.y, b.angle());
			Draw.reset();
		}
	},
	flame = new BulletType(0.6f, 5){
		public void draw(Bullet b){
			Draw.color(Color.YELLOW, Color.SCARLET, b.time/lifetime);
			float size = 6f-b.time/lifetime*5f;
			Draw.rect("circle", b.x, b.y, size, size);
			Draw.reset();
		}
	},
	plasmaflame = new BulletType(0.8f, 17){
		{
			lifetime = 65f;
		}
		public void draw(Bullet b){
			Draw.color(Color.valueOf("efa66c"), Color.valueOf("72deaf"), b.time/lifetime);
			float size = 7f-b.time/lifetime*6f;
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
	shot = new BulletType(2.4f, 4){
		{lifetime = 40;}
		public void draw(Bullet b){
			Draw.color(lightGold);
			Draw.rect("bullet", b.x, b.y, b.angle());
			Draw.reset();
		}
	},
	multishot = new BulletType(2.5f, 3){
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
		Effects.effect(Fx.hit, b);
	}
}
