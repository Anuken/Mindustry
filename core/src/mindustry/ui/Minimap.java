package mindustry.ui;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

public class Minimap extends Table{

    public Minimap(){
        background(Tex.pane);
        float margin = 5f;
        touchable(Touchable.enabled);

        add(new Element(){
            {
                setSize(Scl.scl(140f));
            }

            @Override
            public void act(float delta){
                setPosition(Scl.scl(margin), Scl.scl(margin));

                super.act(delta);
            }

            @Override
            public void draw(){
                if(renderer.minimap.getRegion() == null) return;
                if(!clipBegin()) return;

                Draw.rect(renderer.minimap.getRegion(), x + width / 2f, y + height / 2f, width, height);

                if(renderer.minimap.getTexture() != null){
                    renderer.minimap.drawEntities(x, y, width, height, 0.75f, false);
                }

                clipEnd();
            }
        }).size(140f);

        margin(margin);

        addListener(new InputListener(){
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountx, float amounty){
                renderer.minimap.zoomBy(amounty);
                return true;
            }
        });

        addListener(new ClickListener(){
            {
                tapSquareSize = Scl.scl(11f);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(inTapSquare()){
                    super.touchUp(event, x, y, pointer, button);
                }else{
                    pressed = false;
                    pressedPointer = -1;
                    pressedButton = null;
                    cancelled = false;
                }
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                if(!inTapSquare(x, y)){
                    invalidateTapSquare();
                }
                super.touchDragged(event, x, y, pointer);

                if(mobile){
                    float max = Math.min(world.width(), world.height()) / 16f / 2f;
                    renderer.minimap.setZoom(1f + y / height * (max - 1f));
                }
            }

            @Override
            public void clicked(InputEvent event, float x, float y){
                ui.minimapfrag.toggle();
            }
        });

        update(() -> {

            Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
            if(e != null && e.isDescendantOf(this)){
                requestScroll();
            }else if(hasScroll()){
                Core.scene.setScrollFocus(null);
            }
        });
    }
}
