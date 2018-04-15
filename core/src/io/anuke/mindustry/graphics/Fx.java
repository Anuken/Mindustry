package io.anuke.mindustry.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.graphics.Shapes;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.respawnduration;
import static io.anuke.mindustry.Vars.tilesize;

public class Fx{
	public static Color lightRed = Hue.mix(Color.WHITE, Color.FIREBRICK, 0.1f);
    public static Color lightOrange = Color.valueOf("f68021");
    public static Color lighterOrange = Color.valueOf("f6e096");
    public static Color whiteOrange = Hue.mix(lightOrange, Color.WHITE, 0.6f);
    public static Color whiteYellow = Hue.mix(Color.YELLOW, Color.WHITE, 0.6f);
    public static Color lightGray = Color.valueOf("b0b0b0");
	public static Color glowy = Color.valueOf("fdc056");
	public static Color beam = Color.valueOf("9bffbe");
	public static Color beamLight = Color.valueOf("ddffe9");
	
	public static final Effect
	
	generatorexplosion = new Effect(28, 40f, e -> {
		Angles.randLenVectors(e.id, 16, 10f + e.fout()*8f, (x, y)->{
			float size = e.fin()*12f + 1f;
			Draw.color(Color.WHITE, lightOrange, e.fout());
			Draw.rect("circle", e.x + x, e.y + y, size, size);
			Draw.reset();
		});
	}),
	
	reactorsmoke = new Effect(17, e -> {
		Angles.randLenVectors(e.id, 4, e.fout()*8f, (x, y)->{
			float size = 1f+e.fin()*5f;
			Draw.color(Color.LIGHT_GRAY, Color.GRAY, e.fout());
			Draw.rect("circle", e.x + x, e.y + y, size, size);
			Draw.reset();
		});
	}),
	
	nuclearsmoke = new Effect(40, e -> {
		Angles.randLenVectors(e.id, 4, e.fout()*13f, (x, y)->{
			float size = e.finpow()*4f;
			Draw.color(Color.LIGHT_GRAY, Color.GRAY, e.fout());
			Draw.rect("circle", e.x + x, e.y + y, size, size);
			Draw.reset();
		});
	}),
	
	nuclearcloud = new Effect(90, 200f, e -> {
		Angles.randLenVectors(e.id, 10, e.finpow()*90f, (x, y)->{
			float size = e.fin()*14f;
			Draw.color(Color.LIME, Color.GRAY, e.fout());
			Draw.rect("circle", e.x + x, e.y + y, size, size);
			Draw.reset();
		});
	}),
	
	chainshot = new Effect(9f, e -> {
		Draw.color(Color.WHITE, lightOrange, e.fout());
		Lines.stroke(e.fin()*4f);
		Lines.lineAngle(e.x, e.y, e.rotation, e.fin()*7f);
		Lines.stroke(e.fin()*2f);
		Lines.lineAngle(e.x, e.y, e.rotation, e.fin()*10f);
		Draw.reset();
	}),
	
	mortarshot = new Effect(10f, e -> {
		Draw.color(Color.WHITE, Color.DARK_GRAY, e.fout());
		Lines.stroke(e.fin()*6f);
		Lines.lineAngle(e.x, e.y, e.rotation, e.fin()*10f);
		Lines.stroke(e.fin()*5f);
		Lines.lineAngle(e.x, e.y, e.rotation, e.fin()*14f);
		Lines.stroke(e.fin()*1f);
		Lines.lineAngle(e.x, e.y, e.rotation, e.fin()*16f);
		Draw.reset();
	}),
	
	railshot = new Effect(9f, e -> {
		Draw.color(Color.WHITE, Color.DARK_GRAY, e.fout());
		Lines.stroke(e.fin()*5f);
		Lines.lineAngle(e.x, e.y, e.rotation, e.fin()*8f);
		Lines.stroke(e.fin()*4f);
		Lines.lineAngle(e.x, e.y, e.rotation, e.fin()*12f);
		Lines.stroke(e.fin()*1f);
		Lines.lineAngle(e.x, e.y, e.rotation, e.fin()*14f);
		Draw.reset();
	}),
	
	titanshot = new Effect(12f, e -> {
		Draw.color(Color.WHITE, lightOrange, e.fout());
		Lines.stroke(e.fin()*7f);
		Lines.lineAngle(e.x, e.y, e.rotation, e.fin()*12f);
		Lines.stroke(e.fin()*4f);
		Lines.lineAngle(e.x, e.y, e.rotation, e.fin()*16f);
		Lines.stroke(e.fin()*2f);
		Lines.lineAngle(e.x, e.y, e.rotation, e.fin()*18f);
		Draw.reset();
	}),
	
	largeCannonShot = new Effect(11f, e -> {
		Draw.color(Color.WHITE, whiteYellow, e.fout());
		Lines.stroke(e.fin()*6f);
		Lines.lineAngle(e.x, e.y, e.rotation, e.fin()*12f);
		Lines.stroke(e.fin()*3f);
		Lines.lineAngle(e.x, e.y, e.rotation, e.fin()*16f);
		Lines.stroke(e.fin()*1f);
		Lines.lineAngle(e.x, e.y, e.rotation, e.fin()*18f);
		Draw.reset();
	}),
	
	shockwave = new Effect(10f, 80f, e -> {
		Draw.color(Color.WHITE, Color.LIGHT_GRAY, e.fout());
		Lines.stroke(e.fin()*2f + 0.2f);
		Lines.circle(e.x, e.y, e.fout()*28f);
		Draw.reset();
	}),
	
	nuclearShockwave = new Effect(10f, 200f, e -> {
		Draw.color(Color.WHITE, Color.LIGHT_GRAY, e.fout());
		Lines.stroke(e.fin()*3f + 0.2f);
		Lines.poly(e.x, e.y, 40, e.fout()*140f);
		Draw.reset();
	}),
	
	shockwaveSmall = new Effect(10f, e -> {
		Draw.color(Color.WHITE, Color.LIGHT_GRAY, e.fout());
		Lines.stroke(e.fin()*2f + 0.1f);
		Lines.circle(e.x, e.y, e.fout()*15f);
		Draw.reset();
	}),
	
	empshockwave = new Effect(7f, e -> {
		Draw.color(Color.WHITE, Color.SKY, e.fout());
		Lines.stroke(e.fin()*2f);
		Lines.circle(e.x, e.y, e.fout()*40f);
		Draw.reset();
	}),
	
	empspark = new Effect(13, e -> {
		Angles.randLenVectors(e.id, 7, 1f + e.fout()*12f, (x, y)->{
			float len = 1f+e.fin()*6f;
			Draw.color(Color.SKY);
			Lines.lineAngle(e.x + x, e.y + y, Mathf.atan2(x, y), len);
			Draw.reset();
		});
	}),
	
	redgeneratespark = new Effect(18, e -> {
		Angles.randLenVectors(e.id, 5, e.fout()*8f, (x, y)->{
			float len = e.fin()*4f;
			Draw.color(Color.valueOf("fbb97f"), Color.GRAY, e.fout());
			//Draw.alpha(e.fin());
			Draw.rect("circle", e.x + x, e.y + y, len, len);
			Draw.reset();
		});
	}),
	
	generatespark = new Effect(18, e -> {
		Angles.randLenVectors(e.id, 5, e.fout()*8f, (x, y)->{
			float len = e.fin()*4f;
			Draw.color(Color.valueOf("d2b29c"), Color.GRAY, e.fout());
					//Draw.alpha(e.fin());
			Draw.rect("circle", e.x + x, e.y + y, len, len);
			Draw.reset();
		});
	}),

	fuelburn = new Effect(23, e -> {
		Angles.randLenVectors(e.id, 5, e.fout()*9f, (x, y)->{
			float len = e.fin()*4f;
			Draw.color(Color.LIGHT_GRAY, Color.GRAY, e.fout());
			//Draw.alpha(e.fin());
			Draw.rect("circle", e.x + x, e.y + y, len, len);
			Draw.reset();
		});
	}),
	
	laserspark = new Effect(14, e -> {
		Angles.randLenVectors(e.id, 8, 1f + e.fout()*11f, (x, y)->{
			float len = 1f+e.fin()*5f;
			Draw.color(Color.WHITE, Color.CORAL, e.fout());
			Draw.alpha(e.fout()/1.3f);
			Lines.lineAngle(e.x + x, e.y + y, Mathf.atan2(x, y), len);
			Draw.reset();
		});
	}),
	
	shellsmoke = new Effect(20, e -> {
		Angles.randLenVectors(e.id, 8, 3f + e.fout()*17f, (x, y)->{
			float size = 2f+e.fin()*5f;
			Draw.color(Color.LIGHT_GRAY, Color.DARK_GRAY, e.fout());
			Draw.rect("circle", e.x + x, e.y + y, size, size);
			Draw.reset();
		});
	}),
	
	blastsmoke = new Effect(26, e -> {
		Angles.randLenVectors(e.id, 12, 1f + e.fout()*23f, (x, y)->{
			float size = 2f+e.fin()*6f;
			Draw.color(Color.LIGHT_GRAY, Color.DARK_GRAY, e.fout());
			Draw.rect("circle", e.x + x, e.y + y, size, size);
			Draw.reset();
		});
	}),
	
	lava = new Effect(18, e -> {
		Angles.randLenVectors(e.id, 3, 1f + e.fout()*10f, (x, y)->{
			float size = e.finpow()*4f;
			Draw.color(Color.ORANGE, Color.GRAY, e.fout());
			Draw.rect("circle", e.x + x, e.y + y, size, size);
			Draw.reset();
		});
	}),
	
	lavabubble = new Effect(45f, e -> {
		Draw.color(Color.ORANGE);
		float scl = 0.35f;
		Lines.stroke(1f - Mathf.clamp(e.fout() - (1f-scl)) * (1f/scl));
		Lines.circle(e.x, e.y, e.fout()*4f);
		Draw.reset();
	}),
	
	oilbubble = new Effect(64f, e -> {
		Draw.color(Color.DARK_GRAY);
		float scl = 0.25f;
		Lines.stroke(1f - Mathf.clamp(e.fout() - (1f-scl)) * (1f/scl));
		Lines.circle(e.x, e.y, e.fout()*3f);
		Draw.reset();
	}),
	
	shellexplosion = new Effect(9, e -> {
		Lines.stroke(2f - e.fout()*1.7f);
		Draw.color(Color.WHITE, Color.LIGHT_GRAY, e.fout());
		Lines.circle(e.x, e.y, 3f + e.fout() * 9f);
		Draw.reset();
	}),
	
	blastexplosion = new Effect(14, e -> {
		Lines.stroke(1.2f - e.fout());
		Draw.color(Color.WHITE, lightOrange, e.fout());
		Lines.circle(e.x, e.y, 1.5f + e.fout() * 9f);
		Draw.reset();
	}),
	
	place = new Effect(16, e -> {
		Lines.stroke(3f - e.fout() * 2f);
		Lines.square(e.x, e.y, tilesize / 2f + e.fout() * 3f);
		Draw.reset();
	}),
	
	dooropen = new Effect(10, e -> {
		Lines.stroke(e.fin() * 1.6f);
		Lines.square(e.x, e.y, tilesize / 2f + e.fout() * 2f);
		Draw.reset();
	}),
	
	doorclose= new Effect(10, e -> {
		Lines.stroke(e.fin() * 1.6f);
		Lines.square(e.x, e.y, tilesize / 2f + e.fin() * 2f);
		Draw.reset();
	}),
	
	dooropenlarge = new Effect(10, e -> {
		Lines.stroke(e.fin() * 1.6f);
		Lines.square(e.x, e.y, tilesize + e.fout() * 2f);
		Draw.reset();
	}),
			
	doorcloselarge = new Effect(10, e -> {
		Lines.stroke(e.fin() * 1.6f);
		Lines.square(e.x, e.y, tilesize + e.fin() * 2f);
		Draw.reset();
	}),
	
	purify = new Effect(10, e -> {
		Draw.color(Color.ROYAL, Color.GRAY, e.fout());
		Lines.stroke(2f);
		Lines.spikes(e.x, e.y, e.fout() * 4f, 2, 6);
		Draw.reset();
	}),
	
	purifyoil = new Effect(10, e -> {
		Draw.color(Color.BLACK, Color.GRAY, e.fout());
		Lines.stroke(2f);
		Lines.spikes(e.x, e.y, e.fout() * 4f, 2, 6);
		Draw.reset();
	}),
	
	purifystone = new Effect(10, e -> {
		Draw.color(Color.ORANGE, Color.GRAY, e.fout());
		Lines.stroke(2f);
		Lines.spikes(e.x, e.y, e.fout() * 4f, 2, 6);
		Draw.reset();
	}),
	
	generate = new Effect(11, e -> {
		Draw.color(Color.ORANGE, Color.YELLOW, e.fout());
		Lines.stroke(1f);
		Lines.spikes(e.x, e.y, e.fout() * 5f, 2, 8);
		Draw.reset();
	}),

	spark = new Effect(10, e -> {
		Lines.stroke(1f);
		Draw.color(Color.WHITE, Color.GRAY, e.fout());
		Lines.spikes(e.x, e.y, e.fout() * 5f, 2, 8);
		Draw.reset();
	}),
	
	sparkbig = new Effect(11, e -> {
		Lines.stroke(1f);
		Draw.color(lightRed, Color.GRAY, e.fout());
		Lines.spikes(e.x, e.y, e.fout() * 5f, 2.3f, 8);
		Draw.reset();
	}),
	
	smelt = new Effect(10, e -> {
		Lines.stroke(1f);
		Draw.color(Color.YELLOW, Color.RED, e.fout());
		Lines.spikes(e.x, e.y, e.fout() * 5f, 1f, 8);
		Draw.reset();
	}),

	breakBlock = new Effect(12, e -> {
		Lines.stroke(2f);
		Draw.color(Color.WHITE, Colors.get("break"), e.fout());
		Lines.spikes(e.x, e.y, e.fout() * 6f, 2, 5, 90);
		Draw.reset();
	}),

	hit = new Effect(10, e -> {
		Lines.stroke(1f);
		Draw.color(Color.WHITE, Color.ORANGE, e.fout());
		Lines.spikes(e.x, e.y, e.fout() * 3f, 2, 8);
		Draw.reset();
	}),
	
	laserhit = new Effect(10, e -> {
		Lines.stroke(1f);
		Draw.color(Color.WHITE, Color.SKY, e.fout());
		Lines.spikes(e.x, e.y, e.fout() * 2f, 2, 6);
		Draw.reset();
	}),
	
	shieldhit = new Effect(9, e -> {
		Lines.stroke(1f);
		Draw.color(Color.WHITE, Color.SKY, e.fout());
		Lines.spikes(e.x, e.y, e.fout() * 5f, 2, 6);
		Lines.stroke(4f*e.fin());
		Lines.circle(e.x, e.y, e.fout()*14f);
		Draw.reset();
	}),
	
	laserShoot = new Effect(8, e -> {
		Draw.color(Color.WHITE, lightOrange, e.fout());
		Shapes.lineShot(e.x, e.y, e.rotation, 3, e.fin(), 6f, 2f, 0.8f);
		Draw.reset();
	}),

	spreadShoot = new Effect(12, e -> {
		Draw.color(Color.WHITE, Color.PURPLE, e.fout());
		Shapes.lineShot(e.x, e.y, e.rotation, 3, e.fin(), 9f, 3.5f, 0.8f);
		Draw.reset();
	}),

	clusterShoot = new Effect(12, e -> {
		Draw.color(Color.WHITE, lightOrange, e.fout());
		Shapes.lineShot(e.x, e.y, e.rotation, 3, e.fin(), 10f, 2.5f, 0.7f);
		Draw.reset();
	}),
	
	vulcanShoot = new Effect(8, e -> {
		Draw.color(lighterOrange, lightOrange, e.fout());
		Shapes.lineShot(e.x, e.y, e.rotation, 3, e.fin(), 10f, 2f, 0.7f);
		Draw.reset();
	}),
	
	shockShoot = new Effect(8, e -> {
		Draw.color(Color.WHITE, Color.ORANGE, e.fout());
		Shapes.lineShot(e.x, e.y, e.rotation, 3, e.fin(), 14f, 4f, 0.8f);
		Draw.reset();
	}),

	beamShoot = new Effect(8, e -> {
		Draw.color(beamLight, beam, e.fout());
		Shapes.lineShot(e.x, e.y, e.rotation - 70, 3, e.fin(), 12f, 1f, 0.5f);
		Shapes.lineShot(e.x, e.y, e.rotation + 70, 3, e.fin(), 12f, 1f, 0.5f);
		Draw.reset();
	}),

    beamhit = new Effect(8, e -> {
        Draw.color(beamLight, beam, e.fout());
        Lines.stroke(e.fin()*3f+0.5f);
        Lines.circle(e.x, e.y, e.fout()*8f);
        Lines.spikes(e.x, e.y, e.fout()*6f, 2f, 4, 45);
        Draw.reset();
    }),

	titanExplosion = new Effect(11, 48f, e -> {
		Lines.stroke(2f*e.fin()+0.5f);
		Draw.color(Color.WHITE, Color.DARK_GRAY, e.finpow());
		Lines.circle(e.x, e.y, 5f + e.finpow() * 8f);
		
		Draw.color(e.fout() < 0.5f ? whiteOrange : Color.DARK_GRAY);
		float rad = e.fin()*10f + 5f;
		Angles.randLenVectors(e.id, 5, 9f, (x, y)->{
			Draw.rect("circle2", e.x + x, e.y + y, rad, rad);
		});
		
		Draw.reset();
	}),

	explosion = new Effect(11, e -> {
		Lines.stroke(2f*e.fin()+0.5f);
		Draw.color(Color.WHITE, Color.DARK_GRAY, e.finpow());
		Lines.circle(e.x, e.y, 5f + e.finpow() * 6f);
		
		Draw.color(e.fout() < 0.5f ? Color.WHITE : Color.DARK_GRAY);
		float rad = e.fin()*10f + 5f;
		Angles.randLenVectors(e.id, 5, 8f, (x, y)->{
			Draw.rect("circle2", e.x + x, e.y + y, rad, rad);
		});
		
		Draw.reset();
	}),
	
	
	blockexplosion = new Effect(13, e -> {
		Angles.randLenVectors(e.id+1, 8, 5f + e.fout()*11f, (x, y)->{
			float size = 2f+e.fin()*8f;
			Draw.color(Color.LIGHT_GRAY, Color.DARK_GRAY, e.fout());
			Draw.rect("circle", e.x + x, e.y + y, size, size);
			Draw.reset();
		});
		
		Lines.stroke(2f*e.fin()+0.4f);
		Draw.color(Color.WHITE, Color.ORANGE, e.finpow());
		Lines.circle(e.x, e.y, 2f + e.finpow() * 9f);
		
		Draw.color(e.fout() < 0.5f ? Color.WHITE : Color.DARK_GRAY);
		float rad = e.fin()*10f + 2f;
		Angles.randLenVectors(e.id, 5, 8f, (x, y)->{
			Draw.rect("circle2", e.x + x, e.y + y, rad, rad);
		});
		
		Draw.reset();
	}),

	clusterbomb = new Effect(10f, e -> {
		Draw.color(Color.WHITE, lightOrange, e.fout());
		Lines.stroke(e.fin()*1.5f);
		Lines.poly(e.x, e.y, 4, e.fin()*8f);
		Lines.circle(e.x, e.y, e.fout()*14f);
		Draw.reset();
	}),
	
	coreexplosion = new Effect(13, e -> {
		Lines.stroke(3f-e.fout()*2f);
		Draw.color(Color.ORANGE, Color.WHITE, e.fout());
		Lines.spikes(e.x, e.y, 5f + e.fout() * 40f, 6, 6);
		Lines.circle(e.x, e.y, 4f + e.fout() * 40f);
		Draw.reset();
	}),
	
	smoke = new Effect(100, e -> {
		Draw.color(Color.GRAY, new Color(0.3f, 0.3f, 0.3f, 1f), e.fout());
		float size = 7f-e.fout()*7f;
		Draw.rect("circle", e.x, e.y, size, size);
		Draw.reset();
	}),
	
	railsmoke = new Effect(30, e -> {
		Draw.color(Color.LIGHT_GRAY, Color.WHITE, e.fout());
		float size = e.fin()*4f;
		Draw.rect("circle", e.x, e.y, size, size);
		Draw.reset();
	}),

	chainsmoke = new Effect(30, e -> {
		Draw.color(lightGray);
		float size = e.fin()*4f;
		Draw.rect("circle", e.x, e.y, size, size);
		Draw.reset();
	}),
	
	dashsmoke = new Effect(30, e -> {
		Draw.color(Color.CORAL, Color.GRAY, e.fout());
		float size = e.fin()*4f;
		Draw.rect("circle", e.x, e.y, size, size);
		Draw.reset();
	}),
	
	spawn = new Effect(23, e -> {
		Lines.stroke(2f);
		Draw.color(Color.DARK_GRAY, Color.SCARLET, e.fout());
		Lines.circle(e.x, e.y, 7f - e.fout() * 6f);
		Draw.reset();
	}),
	
	respawn = new Effect(respawnduration, e -> {
		Draw.tcolor(Color.SCARLET);
		Draw.tscl(0.25f);
		Draw.text("Respawning in " + (int)((e.lifetime-e.time)/60), e.x, e.y);
		Draw.tscl(0.5f);
		Draw.reset();
	}),
	transfer = new Effect(20, e -> {
		Draw.color(Color.SCARLET, Color.CLEAR, e.fin());
		Lines.square(e.x, e.y, 4);
		Lines.lineAngle(e.x, e.y, e.rotation, 5f);
		Draw.reset();
	});
}
