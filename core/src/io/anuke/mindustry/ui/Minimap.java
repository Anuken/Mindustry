package io.anuke.mindustry.ui;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.input.KeyCode;
import io.anuke.arc.scene.Element;
import io.anuke.arc.scene.event.ClickListener;
import io.anuke.arc.scene.event.InputEvent;
import io.anuke.arc.scene.event.InputListener;
import io.anuke.arc.scene.event.Touchable;
import io.anuke.arc.scene.ui.layout.Container;
import io.anuke.arc.scene.ui.layout.Unit;

import static io.anuke.mindustry.Vars.*;

public class Minimap extends Container<Element>{

    public Minimap(){
        background("pane");
        float margin = 5f;
        touchable(Touchable.enabled);

        addChild(new Element(){
            {
                setSize(Unit.dp.scl(140f));
            }

            @Override
            public void act(float delta){
                setPosition(margin, margin);

                super.act(delta);
            }

            @Override
            public void draw(){
                if(renderer.minimap.getRegion() == null) return;

                Draw.rect(renderer.minimap.getRegion(), x + width/2f, y + height/2f, width, height);

                if(renderer.minimap.getTexture() != null){
                    renderer.minimap.drawEntities(x, y, width, height);
                }
            }
        });

        size(140f);
        margin(margin);

        addListener(new InputListener(){

            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountx, float amounty){
                renderer.minimap.zoomBy(amounty);
                return true;
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                if(mobile){
                    renderer.minimap.zoomBy(Core.input.deltaY(pointer) / 12f / Unit.dp.scl(1f));
                }
            }
        });

        addListener(new ClickListener(){
            {
                tapSquareSize = Unit.dp.scl(11f);
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
            }

            @Override
            public void clicked(InputEvent event, float x, float y){
                ui.minimap.show();
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
