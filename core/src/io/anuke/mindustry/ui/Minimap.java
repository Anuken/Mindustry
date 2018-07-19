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
import static io.anuke.mindustry.Vars.showFog;
import static io.anuke.mindustry.Vars.world;

public class Minimap extends Table{

    public Minimap(){
        super("button");

        margin(5);
        marginBottom(10);

        Image image = new Image(new TextureRegionDrawable(new TextureRegion())){
            @Override
            public void draw(Batch batch, float parentAlpha){
                if(renderer.minimap().getRegion() == null) return;

                TextureRegionDrawable draw = (TextureRegionDrawable) getDrawable();
                draw.getRegion().setRegion(renderer.minimap().getRegion());
                super.draw(batch, parentAlpha);
                if(renderer.minimap().getTexture() != null){
                    renderer.minimap().drawEntities(x, y, width, height);
                }

                if(showFog){
                    renderer.fog().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

                    TextureRegion r = draw.getRegion();
                    float pad = renderer.fog().getPadding();

                    float px = r.getU() * world.width() + pad;
                    float py = r.getV() * world.height() + pad;
                    float px2 = r.getU2() * world.width() + pad;
                    float py2 = r.getV2() * world.height() + pad;

                    r.setTexture(renderer.fog().getTexture());
                    r.setU(px / (world.width() + pad*2f));
                    r.setV(1f - py / (world.height() + pad*2f));
                    r.setU2(px2 / (world.width() + pad*2f));
                    r.setV2(1f - py2 / (world.height() + pad*2f));

                    //r.setV(1f - draw.getRegion().getV());
                    //r.setV2(1f - draw.getRegion().getV2());

                    //r.setU(r.getU() + renderer.fog().getPadding()/(float)(world.width() + renderer.fog().getPadding()*2) * renderer.minimap().getZoom());
                    //r.setV(r.getV() - renderer.fog().getPadding()/(float)(world.height() + renderer.fog().getPadding()*2) * renderer.minimap().getZoom());
                    //r.setU2(r.getU2() - renderer.fog().getPadding()/(float)(world.width() + renderer.fog().getPadding()*2) * renderer.minimap().getZoom());
                    //r.setV2(r.getV2() + renderer.fog().getPadding()/(float)(world.height() + renderer.fog().getPadding()*2) * renderer.minimap().getZoom());

                    Graphics.shader(Shaders.fog);
                    super.draw(batch, parentAlpha);
                    Graphics.shader();

                    renderer.fog().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
                }
            }
        };

        addListener(new InputListener(){
            public boolean scrolled(InputEvent event, float x, float y, int amount){
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
