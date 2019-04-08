package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.input.KeyCode;
import io.anuke.arc.scene.event.InputEvent;
import io.anuke.arc.scene.event.InputListener;
import io.anuke.arc.scene.ui.layout.Unit;

import static io.anuke.mindustry.Vars.mobile;
import static io.anuke.mindustry.Vars.renderer;

public class MinimapDialog extends FloatingDialog{

    public MinimapDialog(){
        super("$minimap");
        setFillParent(false);

        shown(this::setup);

        addCloseButton();
        shouldPause = true;
    }

    void setup(){
        cont.clear();
        float size = Math.min(Core.graphics.getWidth(), Core.graphics.getHeight()) / Unit.dp.scl(1f) / 1.3f;

        cont.table("pane", t -> {
            t.addRect((x, y, width, height) -> {
                if(renderer.minimap.getRegion() == null) return;
                Draw.color(Color.WHITE);
                Draw.rect(renderer.minimap.getRegion(), x + width / 2f, y + height / 2f, width, height);

                if(renderer.minimap.getTexture() != null){
                    renderer.minimap.drawEntities(x, y, width, height);
                }
            }).grow();
        }).size(size);

        cont.addListener(new InputListener(){
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
                    renderer.minimap.zoomBy(Core.input.deltaY(pointer) / 30f / Unit.dp.scl(1f));
                }
            }
        });

        Core.app.post(() -> Core.scene.setScrollFocus(cont));
    }
}
