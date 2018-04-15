package io.anuke.mindustry.entities;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.entities.effect.DamageArea;
import io.anuke.mindustry.entities.effect.EMP;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.BaseBulletType;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.graphics.Fx.*;

public abstract class BulletType extends BaseBulletType<Bullet>{
	
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
			Draw.color(Color.GRAY);
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
			Lines.stroke(1f);
			Lines.lineAngleCenter(b.x, b.y, b.angle(), 3f);
			Draw.reset();
		}
		
		public void update(Bullet b){
			if(b.timer.get(0, 4)){
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
			Lines.circle(b.x, b.y, 4f);
			Draw.rect("circle", b.x, b.y, rad, rad);
			Draw.reset();
		}
		
		public void update(Bullet b){
			if(b.timer.get(0, 2)){
				Effects.effect(Fx.empspark, b.x + Mathf.range(2), b.y + Mathf.range(2));
			}
		}
		
		public void despawned(Bullet b){
			hit(b);
		}
		
		public void hit(Bullet b, float hitx, float hity){
			Timers.run(5f, ()-> new EMP(b.x, b.y, b.getDamage()).add());
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
			if(b.timer.get(0, 7)){
				Effects.effect(Fx.smoke, b.x + Mathf.range(2), b.y + Mathf.range(2));
			}
		}
		
		public void despawned(Bullet b){
			hit(b);
		}
		
		public void hit(Bullet b, float hitx, float hity){
			Effects.shake(3f, 3f, b);
			
			Effects.effect(Fx.shellsmoke, b);
			Effects.effect(Fx.shellexplosion, b);
			
			DamageArea.damage(!(b.owner instanceof Enemy), b.x, b.y, 25f, (int)(damage * 2f/3f));
		}
	},
	flak = new BulletType(2.9f, 8) {

		public void init(Bullet b) {
			b.velocity.scl(Mathf.random(0.6f, 1f));
		}

		public void update(Bullet b){
			if(b.timer.get(0, 7)){
				Effects.effect(Fx.smoke, b.x + Mathf.range(2), b.y + Mathf.range(2));
			}
		}

		public void draw(Bullet b) {
			Draw.color(Color.GRAY);
			Lines.stroke(3f);
			Lines.lineAngleCenter(b.x, b.y, b.angle(), 2f);
			Lines.stroke(1.5f);
			Lines.lineAngleCenter(b.x, b.y, b.angle(), 5f);
			Draw.reset();
		}

		public void hit(Bullet b, float hitx, float hity) {
			Effects.effect(shellsmoke, b);
			for(int i = 0; i < 3; i ++){
				Bullet bullet = new Bullet(flakspark, b.owner, hitx, hity, b.angle() + Mathf.range(120f));
				bullet.add();
			}
		}

		public void despawned(Bullet b) {
			hit(b, b.x, b.y);
		}
	},
	flakspark = new BulletType(2f, 2) {
		{
			drag = 0.05f;
		}

		public void init(Bullet b) {
			b.velocity.scl(Mathf.random(0.6f, 1f));
		}

		public void draw(Bullet b) {
			Draw.color(Color.LIGHT_GRAY, Color.GRAY, b.fin());
			Lines.stroke(2f - b.fin());
			Lines.lineAngleCenter(b.x, b.y, b.angle(), 2f);
			Draw.reset();
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
			if(b.timer.get(0, 4)){
				Effects.effect(Fx.smoke, b.x + Mathf.range(2), b.y + Mathf.range(2));
			}
		}
		
		public void despawned(Bullet b){
			hit(b);
		}
		
		public void hit(Bullet b, float hitx, float hity){
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
			if(b.timer.get(0, 4)){
				Effects.effect(Fx.smoke, b.x + Mathf.range(2), b.y + Mathf.range(2));
			}
		}
		
		public void despawned(Bullet b){
			hit(b);
		}
		
		public void hit(Bullet b, float hitx, float hity){
			Effects.shake(3f, 3f, b);
			
			Effects.effect(Fx.shellsmoke, b);
			Effects.effect(Fx.shockwaveSmall, b);
			
			DamageArea.damage(!(b.owner instanceof Enemy), b.x, b.y, 25f, (int)(damage * 2f/3f));
		}
	},
	blast = new BulletType(1.1f, 90){
		{
			lifetime = 0f;
			hitsize = 8f;
			speed = 0f;
		}
		
		public void despawned(Bullet b){
			hit(b);
		}
		
		public void hit(Bullet b, float hitx, float hity){
			Effects.shake(3f, 3f, b);
			
			Effects.effect(Fx.blastsmoke, b);
			Effects.effect(Fx.blastexplosion, b);

			//TODO remove translation() usage
			Angles.circleVectors(30, 6f, (nx, ny) -> {
				float ang = Mathf.atan2(nx, ny);
				Bullet o = new Bullet(blastshot, b.owner, b.x + nx, b.y + ny, ang).add();
				o.damage = b.damage/9;
			});
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
			Draw.color(Color.ORANGE);
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
	flame = new BulletType(0.7f, 5){ //for turrets
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
	flameshot = new BulletType(0.5f, 3){ //for enemies
		public void draw(Bullet b){
			Draw.color(Color.ORANGE, Color.SCARLET, b.time/lifetime);
			float size = 6f-b.time/lifetime*5f;
			Draw.rect("circle", b.x, b.y, size, size);
			Draw.reset();
		}
	},
	shot = new BulletType(2.7f, 5){
		{
			lifetime = 40;
		}

		public void draw(Bullet b){
			Draw.color(Color.WHITE, lightOrange, b.fout()/2f + 0.25f);
			Lines.stroke(1.5f);
			Lines.lineAngle(b.x, b.y, b.angle(), 3f);
			Draw.reset();
		}
	},
	spread = new BulletType(2.4f, 9) {
		{
			lifetime = 70;
		}

		public void draw(Bullet b) {
			float size = 3f - b.fin()*1f;

			Draw.color(Color.PURPLE, Color.WHITE, 0.8f);
			Lines.stroke(1f);
			Lines.circle(b.x, b.y, size);
			Draw.reset();
		}
	},
	cluster = new BulletType(4.5f, 12){
		{
			lifetime = 60;
			drag = 0.05f;
		}

		public void draw(Bullet b){
			Lines.stroke(2f);
			Draw.color(lightOrange, Color.WHITE, 0.4f);
			Lines.poly(b.x, b.y, 3, 1.6f, b.angle());
			Lines.stroke(1f);
			Draw.color(Color.WHITE, lightOrange, b.fin()/2f);
			Draw.alpha(b.fin());
			Lines.spikes(b.x, b.y, 1.5f, 2f, 6);
			Draw.reset();
		}

		public void despawned(Bullet b){
			hit(b);
		}

		public void hit(Bullet b, float hitx, float hity){
			Effects.shake(1.5f, 1.5f, b);

			Effects.effect(Fx.clusterbomb, b);

			DamageArea.damage(!(b.owner instanceof Enemy), b.x, b.y, 35f, damage);
		}
	},
    vulcan = new BulletType(4.5f, 12) {
		{
			lifetime = 50;
		}

		public void init(Bullet b) {
			Timers.reset(b, "smoke", Mathf.random(4f));
		}

		public void draw(Bullet b){
            Draw.color(lightGray);
            Lines.stroke(1f);
            Lines.lineAngleCenter(b.x, b.y, b.angle(), 2f);
            Draw.reset();
        }

        public void update(Bullet b){
            if(b.timer.get(0, 4)){
                Effects.effect(Fx.chainsmoke, b.x, b.y);
            }
        }
    },
	shockshell = new BulletType(5.5f, 11) {

		{
			drag = 0.03f;
			lifetime = 30f;
		}

		public void init(Bullet b) {
			b.velocity.scl(Mathf.random(0.5f, 1f));
		}

		public void draw(Bullet b) {
			Draw.color(Color.WHITE, Color.ORANGE, b.fin());
			Lines.stroke(2f);
			Lines.lineAngleCenter(b.x, b.y, b.angle(), b.fout()*5f);
			Draw.reset();
		}

		public void despawned(Bullet b) {
			hit(b);
		}

		public void hit(Bullet b, float hitx, float hity) {
			for(int i = 0; i < 4; i ++){
				Bullet bullet = new Bullet(scrap, b.owner, b.x, b.y, b.angle() + Mathf.range(80f));
				bullet.add();
			}
		}
	},
	scrap = new BulletType(2f, 3) {
		{
			drag = 0.06f;
			lifetime = 30f;
		}

		public void init(Bullet b) {
			b.velocity.scl(Mathf.random(0.5f, 1f));
		}

		public void draw(Bullet b) {
			Draw.color(Color.WHITE, Color.ORANGE, b.fin());
			Lines.stroke(1f);
			Lines.lineAngleCenter(b.x, b.y, b.angle(), b.fout()*4f);
			Draw.reset();
		}
	},
	beamlaser = new BulletType(0.001f, 38) {
		float length = 230f;
		{
			drawSize = length*2f+20f;
			lifetime = 15f;
		}

		public void init(Bullet b) {
			DamageArea.damageLine(b.owner, Fx.beamhit, b.x, b.y, b.angle(), length, damage);
		}

		public void draw(Bullet b) {
			float f = b.fout()*1.5f;

			Draw.color(beam);
			Draw.rect("circle", b.x, b.y, 6f*f, 6f*f);
			Lines.stroke(3f * f);
			Lines.lineAngle(b.x, b.y, b.angle(), length);

			Lines.stroke(2f * f);
            Lines.lineAngle(b.x, b.y, b.angle(), length + 6f);
			Lines.stroke(1f * f);
			Lines.lineAngle(b.x, b.y, b.angle(), length + 12f);

			Draw.color(beamLight);
			Lines.stroke(1.5f * f);
			Draw.rect("circle", b.x, b.y, 3f*f, 3f*f);
			Lines.lineAngle(b.x, b.y, b.angle(), length);
		}
	};
	
	private BulletType(float speed, int damage){
		this.speed = speed;
		this.damage = damage;
	}
	
	@Override
	public void hit(Bullet b, float hitx, float hity){
		Effects.effect(Fx.hit, hitx, hity);
	}
}
