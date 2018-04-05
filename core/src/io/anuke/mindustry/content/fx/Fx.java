package io.anuke.mindustry.content.fx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;

import static io.anuke.mindustry.Vars.respawnduration;
import static io.anuke.mindustry.Vars.tilesize;

public class Fx{
	public static Color beam = Color.valueOf("9bffbe");
	public static Color stoneGray = Color.valueOf("8f8f8f");
	
	public static final Effect

	none = new Effect(0, 0f, e->{}),

	place = new Effect(16, e -> {
		Lines.stroke(3f - e.fin() * 2f);
		Lines.square(e.x, e.y, tilesize / 2f + e.fin() * 3f);
		Draw.reset();
	}),

	breakBlock = new Effect(12, e -> {
		Lines.stroke(2f);
		Draw.color(Color.WHITE, Colors.get("break"), e.fin());
		Lines.spikes(e.x, e.y, e.fin() * 6f, 2, 5, 90);
		Draw.reset();
	}),

	smoke = new Effect(100, e -> {
		Draw.color(Color.GRAY, new Color(0.3f, 0.3f, 0.3f, 1f), e.fin());
		float size = 7f-e.fin()*7f;
		Draw.rect("circle", e.x, e.y, size, size);
		Draw.reset();
	}),

	dash = new Effect(30, e -> {
		Draw.color(Color.CORAL, Color.GRAY, e.fin());
		float size = e.fout()*4f;
		Draw.rect("circle", e.x, e.y, size, size);
		Draw.reset();
	}),

	spawn = new Effect(23, e -> {
		Lines.stroke(2f);
		Draw.color(Color.DARK_GRAY, Color.SCARLET, e.fin());
		Lines.circle(e.x, e.y, 7f - e.fin() * 6f);
		Draw.reset();
	}),

	respawn = new Effect(respawnduration, e -> {
		Draw.tcolor(Color.SCARLET);
		Draw.tscl(0.25f);
		Draw.text("Respawning in " + (int)((e.lifetime-e.time)/60), e.x, e.y);
		Draw.tscl(0.5f);
		Draw.reset();
	});
}
