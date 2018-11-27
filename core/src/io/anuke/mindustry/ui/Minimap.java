package io.anuke.mindustry.ui;

import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.ui.layout.Table;

import static io.anuke.mindustry.Vars.*;

public class Minimap extends Table{

    public Minimap(){
        super("pane");

        margin(5);

        TextureRegion r = new TextureRegion();

        Element elem = new Element(){
            @Override
            public void draw(){
                if(renderer.minimap.getRegion() == null) return;

                Draw.crect(renderer.minimap.getRegion(), x, y, width, height);

                if(renderer.minimap.getTexture() != null){
                    renderer.minimap.drawEntities(x, y, width, height);
                }

                if(showFog){
                    renderer.fog.getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

                    r.setRegion(renderer.minimap.getRegion());
                    float pad = renderer.fog.getPadding();

                    float px = r.getU() * world.width() + pad;
                    float py = r.getV() * world.height() + pad;
                    float px2 = r.getU2() * world.width() + pad;
                    float py2 = r.getV2() * world.height() + pad;

                    r.setTexture(renderer.fog.getTexture());
                    r.setU(px / (world.width() + pad*2f));
                    r.setV(1f - py / (world.height() + pad*2f));
                    r.setU2(px2 / (world.width() + pad*2f));
                    r.setV2(1f - py2 / (world.height() + pad*2f));

                    Graphics.shader(Shaders.fog);
                    Draw.crect(r, x, y, width, height);
                    Graphics.shader();

                    renderer.fog.getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
                }
            }
        };

        addListener(new InputListener(){
            public boolean scrolled(InputEvent event, float x, float y, int amount){
                renderer.minimap.zoomBy(amount);
                return true;
            }
        });

        elem.update(() -> {

            Element e = Core.scene.hit(Graphics.mouse().x, Graphics.mouse().y, true);
            if(e != null && e.isDescendantOf(this)){
                Core.scene.setScrollFocus(this);
            }else if(Core.scene.getScrollFocus() == this){
                Core.scene.setScrollFocus(null);
            }
        });

        add(elem).size(140f, 140f);
    }
}
