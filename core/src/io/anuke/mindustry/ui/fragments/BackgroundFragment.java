package io.anuke.mindustry.ui.fragments;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.scene.Group;
import io.anuke.arc.scene.ui.layout.Unit;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.graphics.Shaders;

import static io.anuke.mindustry.Vars.state;

public class BackgroundFragment extends Fragment{

    @Override
    public void build(Group parent){
        Core.scene.table().addRect((a, b, w, h) -> {
            Draw.colorl(0.1f);
            Fill.crect(0, 0, w, h);
            Draw.shader(Shaders.menu);
            Fill.crect(0, 0, w, h);
            Draw.shader();

            boolean portrait = Core.graphics.getWidth() < Core.graphics.getHeight();
            float logoscl = (int)Unit.dp.scl(1);
            TextureRegion logo = Core.atlas.find("logotext");
            float logow = logo.getWidth() * logoscl;
            float logoh = logo.getHeight() * logoscl;

            Draw.color();
            Draw.rect(logo, (int)(w / 2), (int)(h - 10 - logoh - Unit.dp.scl(portrait ? 30f : 0)) + logoh / 2, logow, logoh);
        }).visible(() -> state.is(State.menu)).grow();
    }
}
