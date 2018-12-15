package io.anuke.mindustry.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.ui.layout.Container;

import static io.anuke.mindustry.Vars.renderer;

public class Minimap extends Container<Element>{

    public Minimap(){
        super(new Element(){
            TextureRegion r = new TextureRegion();

            @Override
            public void draw(){
                if(renderer.minimap.getRegion() == null) return;

                Draw.crect(renderer.minimap.getRegion(), x, y, width, height);

                if(renderer.minimap.getTexture() != null){
                    renderer.minimap.drawEntities(x, y, width, height);
                }
            }
        });

        background("pane");

        size(140f);
        margin(5f);

        addListener(new InputListener(){
            public boolean scrolled(InputEvent event, float x, float y, int amount){
                renderer.minimap.zoomBy(amount);
                return true;
            }
        });

        update(() -> {

            Element e = Core.scene.hit(Graphics.mouse().x, Graphics.mouse().y, true);
            if(e != null && e.isDescendantOf(this)){
                Core.scene.setScrollFocus(this);
            }else if(Core.scene.getScrollFocus() == this){
                Core.scene.setScrollFocus(null);
            }
        });
    }
}
