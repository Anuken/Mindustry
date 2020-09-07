package mindustry.ui.dialogs;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import mindustry.gen.*;

import static mindustry.Vars.renderer;

public class MinimapDialog extends BaseDialog{

    public MinimapDialog(){
        super("$minimap");
        setFillParent(true);

        shown(this::setup);

        addCloseButton();
        shouldPause = true;
        titleTable.remove();
        onResize(this::setup);
    }

    void setup(){
        cont.clear();

        cont.table(Tex.pane,t -> {
            t.rect((x, y, width, height) -> {
                if(renderer.minimap.getRegion() == null) return;
                Draw.color(Color.white);
                Draw.alpha(parentAlpha);
                Draw.rect(renderer.minimap.getRegion(), x + width / 2f, y + height / 2f, width, height);

                if(renderer.minimap.getTexture() != null){
                    renderer.minimap.drawEntities(x, y, width, height);
                }
            }).grow();
        }).size(Math.min(Core.graphics.getWidth() / 1.1f, Core.graphics.getHeight() / 1.3f) / Scl.scl(1f)).padTop(-20f);

        cont.addListener(new InputListener(){
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountx, float amounty){
                renderer.minimap.zoomBy(amounty);
                return true;
            }
        });

        cont.addListener(new ElementGestureListener(){
            float lzoom = -1f;

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                lzoom = renderer.minimap.getZoom();
            }

            @Override
            public void zoom(InputEvent event, float initialDistance, float distance){
                if(lzoom < 0){
                    lzoom = renderer.minimap.getZoom();
                }
                renderer.minimap.setZoom(initialDistance / distance * lzoom);
            }
        });

        Core.app.post(() -> Core.scene.setScrollFocus(cont));
    }
}
