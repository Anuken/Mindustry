package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.ui.layout.Unit;

import static io.anuke.mindustry.Vars.state;

public class BackgroundFragment implements Fragment {

    @Override
    public void build() {

        Core.scene.table().addRect((a, b, w, h) -> {
            Draw.color();

            TextureRegion back = Draw.region("background");
            float backscl = Math.max(Gdx.graphics.getWidth() / (float)back.getRegionWidth() * 1.5f, Unit.dp.scl(5f));

            Draw.alpha(0.7f);
            Core.batch.draw(back, w/2 - back.getRegionWidth()*backscl/2 +240f, h/2 - back.getRegionHeight()*backscl/2 + 250f,
                    back.getRegionWidth()*backscl, back.getRegionHeight()*backscl);

            float logoscl = (int)Unit.dp.scl(7);
            TextureRegion logo = Core.skin.getRegion("logotext");
            float logow = logo.getRegionWidth()*logoscl;
            float logoh = logo.getRegionHeight()*logoscl;

            Draw.color();
            Core.batch.draw(logo, w/2 - logow/2, h - logoh + 15, logow, logoh);
        }).visible(() -> state.is(State.menu)).grow();
    }
}
