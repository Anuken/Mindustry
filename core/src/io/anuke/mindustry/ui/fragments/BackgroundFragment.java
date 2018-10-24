package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.ui.layout.Unit;

import static io.anuke.mindustry.Vars.state;

public class BackgroundFragment extends Fragment{

    @Override
    public void build(Group parent){

        Core.scene.table().addRect((a, b, w, h) -> {
            Draw.colorl(0.1f);
            Fill.crect(0, 0, w, h);
            Draw.color(Palette.accent);
            Graphics.shader(Shaders.menu);
            Fill.crect(0, 0, w, h);
            Graphics.shader();
            Draw.color();

            boolean portrait = Gdx.graphics.getWidth() < Gdx.graphics.getHeight();
            float logoscl = (int) Unit.dp.scl(7) * (portrait ? 5f / 7f : 1f);
            TextureRegion logo = Core.skin.getRegion("logotext");
            float logow = logo.getRegionWidth() * logoscl;
            float logoh = logo.getRegionHeight() * logoscl;

            Draw.color();
            Core.batch.draw(logo, (int) (w / 2 - logow / 2), (int) (h - logoh + 15 - Unit.dp.scl(portrait ? 30f : 0)), logow, logoh);
        }).visible(() -> state.is(State.menu)).grow();
    }
}
