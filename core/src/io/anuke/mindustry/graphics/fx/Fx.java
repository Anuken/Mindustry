package io.anuke.mindustry.graphics.fx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.graphics.Lines;

import static io.anuke.mindustry.Vars.respawnduration;
import static io.anuke.mindustry.Vars.tilesize;

public class Fx{
    public static Color lightOrange = Color.valueOf("f68021");
    public static Color lighterOrange = Color.valueOf("f6e096");
    public static Color whiteOrange = Hue.mix(lightOrange, Color.WHITE, 0.6f);
    public static Color whiteYellow = Hue.mix(Color.YELLOW, Color.WHITE, 0.6f);
    public static Color lightGray = Color.valueOf("b0b0b0");
	public static Color glowy = Color.valueOf("fdc056");
	public static Color beam = Color.valueOf("9bffbe");
	public static Color beamLight = Color.valueOf("ddffe9");
	public static Color stoneGray = Color.valueOf("8f8f8f");
	
	public static final Effect

	none = new Effect(0, 0f, e->{}),

	place = new Effect(16, e -> {
		Lines.stroke(3f - e.ifract() * 2f);
		Lines.square(e.x, e.y, tilesize / 2f + e.ifract() * 3f);
		Draw.reset();
	}),

	breakBlock = new Effect(12, e -> {
		Lines.stroke(2f);
		Draw.color(Color.WHITE, Colors.get("break"), e.ifract());
		Lines.spikes(e.x, e.y, e.ifract() * 6f, 2, 5, 90);
		Draw.reset();
	}),

	hit = new Effect(10, e -> {
		Lines.stroke(1f);
		Draw.color(Color.WHITE, Color.ORANGE, e.ifract());
		Lines.spikes(e.x, e.y, e.ifract() * 3f, 2, 8);
		Draw.reset();
	}),

	smoke = new Effect(100, e -> {
		Draw.color(Color.GRAY, new Color(0.3f, 0.3f, 0.3f, 1f), e.ifract());
		float size = 7f-e.ifract()*7f;
		Draw.rect("circle", e.x, e.y, size, size);
		Draw.reset();
	}),

	dash = new Effect(30, e -> {
		Draw.color(Color.CORAL, Color.GRAY, e.ifract());
		float size = e.fract()*4f;
		Draw.rect("circle", e.x, e.y, size, size);
		Draw.reset();
	}),

	spawn = new Effect(23, e -> {
		Lines.stroke(2f);
		Draw.color(Color.DARK_GRAY, Color.SCARLET, e.ifract());
		Lines.circle(e.x, e.y, 7f - e.ifract() * 6f);
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
