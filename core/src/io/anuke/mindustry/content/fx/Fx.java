package io.anuke.mindustry.content.fx;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Angles;

import static io.anuke.mindustry.Vars.tilesize;

public class Fx{

	
	public static final Effect

	none = new Effect(0, 0f, e->{}),

	placeBlock = new Effect(16, e -> {
		Draw.color(Palette.accent);
		Lines.stroke(3f - e.fin() * 2f);
		Lines.square(e.x, e.y, tilesize / 2f * (float)(e.data) + e.fin() * 3f);
		Draw.reset();
	}),

	breakBlock = new Effect(12, e -> {
		float data = (float)(e.data);

		Draw.color(Palette.remove);
		Lines.stroke(3f - e.fin() * 2f);
		Lines.square(e.x, e.y, tilesize / 2f * data + e.fin() * 3f);

		Angles.randLenVectors(e.id, 3 + (int)(data*3), data*2f + (tilesize * data) * e.finpow(), (x, y) -> {
			Fill.square(e.x + x, e.y + y, 1f + e.fout()*(3f+data));
		});
		Draw.reset();
	}),

	smoke = new Effect(100, e -> {
		Draw.color(Color.GRAY, new Color(0.3f, 0.3f, 0.3f, 1f), e.fin());
		float size = 7f-e.fin()*7f;
		Draw.rect("circle", e.x, e.y, size, size);
		Draw.reset();
	}),

	spawn = new Effect(23, e -> {
		Lines.stroke(2f);
		Draw.color(Color.DARK_GRAY, Color.SCARLET, e.fin());
		Lines.circle(e.x, e.y, 7f - e.fin() * 6f);
		Draw.reset();
	});
}
