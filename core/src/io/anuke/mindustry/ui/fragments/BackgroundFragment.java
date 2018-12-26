package io.anuke.mindustry.ui.fragments;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Fill;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.scene.Group;
import io.anuke.arc.scene.ui.layout.Unit;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;

import static io.anuke.mindustry.Vars.state;

public class BackgroundFragment extends Fragment{

    @Override
    public void build(Group parent){
        Core.scene.table().addRect((a, b, w, h) -> {
            Draw.colorl(0.1f);
            Fill.rect().set(0, 0, w, h);
            Draw.color(Palette.accent);
            Draw.shader(Shaders.menu);
            Fill.rect().set(0, 0, w, h);
            Draw.shader();
            Draw.color();

            boolean portrait = Core.graphics.getWidth() < Core.graphics.getHeight();
            float logoscl = (int) Unit.dp.scl(7) * (portrait ? 5f / 7f : 1f);
            TextureRegion logo = Core.atlas.find("logotext");
            float logow = logo.getWidth() * logoscl;
            float logoh = logo.getHeight() * logoscl;

            Draw.color();
            Draw.rect().tex(logo).set((int) (w / 2 - logow / 2), (int) (h - logoh + 15 - Unit.dp.scl(portrait ? 30f : 0)), logow, logoh);
        }).visible(() -> state.is(State.menu)).grow();
    }
}
