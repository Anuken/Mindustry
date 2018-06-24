package io.anuke.mindustry.ui;

import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.style.TextureRegionDrawable;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.layout.Table;

import static io.anuke.mindustry.Vars.renderer;

public class Minimap extends Table {

    public Minimap(){
        super("button");

        margin(5);
        marginBottom(10);

        Image image = new Image(new TextureRegionDrawable(new TextureRegion())){
            @Override
            public void draw(Batch batch, float parentAlpha) {
                TextureRegionDrawable draw = (TextureRegionDrawable)getDrawable();
                draw.getRegion().setRegion(renderer.minimap().getRegion());
                super.draw(batch, parentAlpha);
                if(renderer.minimap().getTexture() != null){
                    renderer.minimap().drawEntities(x, y, width, height);
                }

                renderer.fog().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

                //draw.getRegion().setV(draw.getRegion().getV2());
                //draw.getRegion().setV2(v);
                draw.getRegion().setTexture(renderer.fog().getTexture());
                draw.getRegion().setV(1f - draw.getRegion().getV());
                draw.getRegion().setV2(1f - draw.getRegion().getV2());


                Graphics.shader(Shaders.fog);
                super.draw(batch, parentAlpha);
                Graphics.shader();

                renderer.fog().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
            }
        };

        addListener(new InputListener(){
            public boolean scrolled (InputEvent event, float x, float y, int amount) {
                renderer.minimap().zoomBy(amount);
                return true;
            }
        });

        image.update(() -> {

            Element e = Core.scene.hit(Graphics.mouse().x, Graphics.mouse().y, true);
            if(e != null && e.isDescendantOf(this)){
                Core.scene.setScrollFocus(this);
            }else if(Core.scene.getScrollFocus() == this){
                Core.scene.setScrollFocus(null);
            }
        });
        add(image).size(140f, 140f);
    }
}
