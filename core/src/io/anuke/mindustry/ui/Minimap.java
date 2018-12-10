package io.anuke.mindustry.ui;

import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.event.InputEvent;
import io.anuke.ucore.scene.event.InputListener;
import io.anuke.ucore.scene.ui.layout.Table;

import static io.anuke.mindustry.Vars.renderer;

public class Minimap extends Table{

    public Minimap(){
        super("pane");

        margin(5);

        Element elem = new Element(){
            @Override
            public void draw(){
                if(renderer.minimap.getRegion() == null) return;

                Draw.crect(renderer.minimap.getRegion(), x, y, width, height);

                if(renderer.minimap.getTexture() != null){
                    renderer.minimap.drawEntities(x, y, width, height);
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
