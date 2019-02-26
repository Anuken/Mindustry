package io.anuke.mindustry.ui;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.scene.Element;
import io.anuke.arc.scene.event.InputEvent;
import io.anuke.arc.scene.event.InputListener;
import io.anuke.arc.scene.ui.layout.Container;

import static io.anuke.mindustry.Vars.renderer;

public class Minimap extends Container<Element>{

    public Minimap(){
        super(new Element(){

            @Override
            public void draw(){
                if(renderer.minimap.getRegion() == null) return;

                Draw.rect(renderer.minimap.getRegion(), x + width/2f, y + height/2f, width, height);

                if(renderer.minimap.getTexture() != null){
                    renderer.minimap.drawEntities(x, y, width, height);
                }
            }
        });

        background("pane");

        size(140f);
        margin(5f);

        addListener(new InputListener(){
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountx, float amounty){
                renderer.minimap.zoomBy(amounty);
                return true;
            }
        });

        update(() -> {

            Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
            if(e != null && e.isDescendantOf(this)){
                Core.scene.setScrollFocus(this);
            }else if(Core.scene.getScrollFocus() == this){
                Core.scene.setScrollFocus(null);
            }
        });
    }
}
