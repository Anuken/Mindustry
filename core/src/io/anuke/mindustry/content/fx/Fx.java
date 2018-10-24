package io.anuke.mindustry.content.fx;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.game.ContentList;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Angles;

import static io.anuke.mindustry.Vars.tilesize;

public class Fx extends FxList implements ContentList{
    public static Effect none, placeBlock, breakBlock, smoke, spawn, tapBlock, select;

    @Override
    public void load(){

        none = new Effect(0, 0f, e -> {
        });

        placeBlock = new Effect(16, e -> {
            Draw.color(Palette.accent);
            Lines.stroke(3f - e.fin() * 2f);
            Lines.square(e.x, e.y, tilesize / 2f * e.rotation + e.fin() * 3f);
            Draw.reset();
        });

        tapBlock = new Effect(12, e -> {
            Draw.color(Palette.accent);
            Lines.stroke(3f - e.fin() * 2f);
            Lines.circle(e.x, e.y, 4f + (tilesize / 1.5f * e.rotation) * e.fin());
            Draw.reset();
        });

        breakBlock = new Effect(12, e -> {
            Draw.color(Palette.remove);
            Lines.stroke(3f - e.fin() * 2f);
            Lines.square(e.x, e.y, tilesize / 2f * e.rotation + e.fin() * 3f);

            Angles.randLenVectors(e.id, 3 + (int) (e.rotation * 3), e.rotation * 2f + (tilesize * e.rotation) * e.finpow(), (x, y) -> {
                Fill.square(e.x + x, e.y + y, 1f + e.fout() * (3f + e.rotation));
            });
            Draw.reset();
        });

        select = new Effect(23, e -> {
            Draw.color(Palette.accent);
            Lines.stroke(e.fout() * 3f);
            Lines.circle(e.x, e.y, 3f + e.fin() * 14f);
            Draw.reset();
        });

        smoke = new Effect(100, e -> {
            Draw.color(Color.GRAY, Palette.darkishGray, e.fin());
            float size = 7f - e.fin() * 7f;
            Draw.rect("circle", e.x, e.y, size, size);
            Draw.reset();
        });

        spawn = new Effect(23, e -> {
            Lines.stroke(2f * e.fout());
            Draw.color(Palette.accent);
            Lines.poly(e.x, e.y, 4, 3f + e.fin() * 8f);
            Draw.reset();
        });
    }
}
