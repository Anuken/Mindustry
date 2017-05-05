package io.anuke.mindustry;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.graphics.Color;

import io.anuke.ucore.core.Draw;
import io.anuke.ucore.entities.Effect;
import io.anuke.ucore.graphics.Hue;

public class EffectLoader{
	
	public static void create(){
		
		Effect.create("place", 16, e -> {
			Draw.thickness(3f - e.ifract() * 2f);
			Draw.square(e.x, e.y, tilesize / 2f + e.ifract() * 3f);
			Draw.clear();
		});

		Effect.create("spark", 10, e -> {
			Draw.thickness(1f);
			Draw.color(Hue.mix(Color.WHITE, Color.GRAY, e.ifract()));
			Draw.spikes(e.x, e.y, e.ifract() * 5f, 2, 8);
			Draw.clear();
		});
		
		Effect.create("smelt", 10, e -> {
			Draw.thickness(1f);
			Draw.color(Hue.mix(Color.YELLOW, Color.RED, e.ifract()));
			Draw.spikes(e.x, e.y, e.ifract() * 5f, 2, 8);
			Draw.clear();
		});

		Effect.create("break", 12, e -> {
			Draw.thickness(2f);
			Draw.color(Color.WHITE, Color.GRAY, e.ifract());
			Draw.spikes(e.x, e.y, e.ifract() * 5f, 2, 5);
			Draw.clear();
		});

		Effect.create("hit", 10, e -> {
			Draw.thickness(1f);
			Draw.color(Hue.mix(Color.WHITE, Color.ORANGE, e.ifract()));
			Draw.spikes(e.x, e.y, e.ifract() * 3f, 2, 8);
			Draw.clear();
		});
		
		Effect.create("shoot", 8, e -> {
			Draw.thickness(1f);
			Draw.color(Hue.mix(Color.WHITE, Color.GOLD, e.ifract()));
			Draw.spikes(e.x, e.y, e.ifract() * 2f, 2, 5);
			Draw.clear();
		});
		
		Effect.create("shoot2", 8, e -> {
			Draw.thickness(1f);
			Draw.color(Hue.mix(Color.WHITE, Color.SKY, e.ifract()));
			Draw.spikes(e.x, e.y, e.ifract() * 2f, 1, 5);
			Draw.clear();
		});
		
		Effect.create("shoot3", 8, e -> {
			Draw.thickness(1f);
			Draw.color(Hue.mix(Color.WHITE, Color.GOLD, e.ifract()));
			Draw.spikes(e.x, e.y, e.ifract() * 2f, 1, 5);
			Draw.clear();
		});

		Effect.create("explosion", 15, e -> {
			Draw.thickness(2f);
			Draw.color(Hue.mix(Color.ORANGE, Color.GRAY, e.ifract()));
			Draw.spikes(e.x, e.y, 2f + e.ifract() * 3f, 4, 6);
			Draw.circle(e.x, e.y, 3f + e.ifract() * 3f);
			Draw.clear();
		});
		
		Effect.create("coreexplosion", 13, e -> {
			Draw.thickness(3f-e.ifract()*2f);
			Draw.color(Hue.mix(Color.ORANGE, Color.WHITE, e.ifract()));
			Draw.spikes(e.x, e.y, 5f + e.ifract() * 40f, 6, 6);
			Draw.circle(e.x, e.y, 4f + e.ifract() * 40f);
			Draw.clear();
		});
		
		Effect.create("spawn", 23, e -> {
			Draw.thickness(2f);
			Draw.color(Hue.mix(Color.DARK_GRAY, Color.SCARLET, e.ifract()));
			Draw.circle(e.x, e.y, 7f - e.ifract() * 6f);
			Draw.clear();
		});

		Effect.create("ind", 100, e -> {
			Draw.thickness(3f);
			Draw.color("royal");
			Draw.circle(e.x, e.y, 3);
			Draw.clear();
		});
		
		Effect.create("respawn", respawnduration, e -> {
			Draw.tcolor(Color.SCARLET);
			Draw.tscl(0.25f);
			Draw.text("Respawning in " + (int)((e.lifetime-e.time)/60), e.x, e.y);
			Draw.tscl(0.5f);
			Draw.clear();
		});
	}
}
