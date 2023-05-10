package mindustry.ui.fragments;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class MinimapFragment{
    private boolean shown;
    float panx, pany, zoom = 1f, lastZoom = -1;
    private float baseSize = Scl.scl(5f);
    public Element elem;

    protected Rect getRectBounds(){
        float
            w = Core.graphics.getWidth(),
            h = Core.graphics.getHeight(),
            ratio = renderer.minimap.getTexture() == null ? 1f : (float)renderer.minimap.getTexture().height / renderer.minimap.getTexture().width,
            size = baseSize * zoom * world.width();

        return Tmp.r1.set(w/2f + panx*zoom - size/2f, h/2f + pany*zoom - size/2f * ratio, size, size * ratio);
    }

    public void build(Group parent){
        elem = parent.fill((x, y, w, h) -> {
            w = Core.graphics.getWidth();
            h = Core.graphics.getHeight();
            float size = baseSize * zoom * world.width();

            Draw.color(Color.black);
            Fill.crect(0, 0, w, h);

            if(renderer.minimap.getTexture() != null){
                Draw.color();
                float ratio = (float)renderer.minimap.getTexture().height / renderer.minimap.getTexture().width;
                TextureRegion reg = Draw.wrap(renderer.minimap.getTexture());
                Draw.rect(reg, w/2f + panx*zoom, h/2f + pany*zoom, size, size * ratio);

                Rect bounds = getRectBounds();
                renderer.minimap.drawEntities(bounds.x, bounds.y, bounds.width, bounds.height, zoom, true);
            }

            Draw.reset();
        });

        elem.visible(() -> shown);
        elem.update(() -> {
            if(!ui.chatfrag.shown()){
                elem.requestKeyboard();
                elem.requestScroll();
            }
            elem.setFillParent(true);
            elem.setBounds(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());

            if(Core.input.keyTap(Binding.menu)){
                shown = false;
            }
        });
        elem.touchable = Touchable.enabled;

        elem.addListener(new ElementGestureListener(){

            @Override
            public void zoom(InputEvent event, float initialDistance, float distance){
                if(lastZoom < 0){
                    lastZoom = zoom;
                }

                zoom = Mathf.clamp(distance / initialDistance * lastZoom, 0.25f, 10f);
            }

            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY){
                if(event.keyCode != KeyCode.mouseRight){
                    panx += deltaX / zoom;
                    pany += deltaY / zoom;
                }else{
                    panTo(x, y);
                }
            }

            @Override
            public void touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                super.touchDown(event, x, y, pointer, button);
                if(button == KeyCode.mouseRight){
                    panTo(x, y);
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                lastZoom = zoom;
            }
        });

        elem.addListener(new InputListener(){

            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY){
                zoom = Mathf.clamp(zoom - amountY / 10f * zoom, 0.25f, 10f);
                return true;
            }
        });

        parent.fill(t -> {
            t.setFillParent(true);
            t.visible(() -> shown);
            t.update(() -> t.setBounds(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight()));

            t.add("@minimap").style(Styles.outlineLabel).pad(10f);
            t.row();
            t.add().growY();
            t.row();
            t.button("@back", Icon.leftOpen, () -> shown = false).size(220f, 60f).pad(10f);
        });
    }

    public void panTo(float relativeX, float relativeY){
        Rect r = getRectBounds();
        Tmp.v1.set(relativeX, relativeY).sub(r.x, r.y).scl(1f / r.width, 1f / r.height).scl(world.unitWidth(), world.unitHeight());
        control.input.panCamera(Tmp.v1.clamp(-tilesize/2f, -tilesize/2f, world.unitWidth() + tilesize/2f, world.unitHeight() + tilesize/2f));
    }

    public boolean shown(){
        return shown;
    }

    public void hide(){
        shown = false;
    }

    public void toggle(){
        if(renderer.minimap.getTexture() != null){
            float size = baseSize * zoom * world.width();
            float ratio = (float)renderer.minimap.getTexture().height / renderer.minimap.getTexture().width;
            float px = player.dead() ? Core.camera.position.x : player.x, py = player.dead() ? Core.camera.position.y : player.y;
            panx = (size/2f - px / (world.width() * tilesize) * size) / zoom;
            pany = (size*ratio/2f - py / (world.height() * tilesize) * size*ratio) / zoom;
        }

        shown = !shown;
    }
}
