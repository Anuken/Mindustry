package io.anuke.mindustry.ui.fragments;

import io.anuke.arc.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.scene.*;
import io.anuke.arc.scene.event.*;

/** Fades in a black overlay.*/
public class FadeInFragment extends Fragment{
    private static final float duration = 40f;
    float time = 0f;

    @Override
    public void build(Group parent){
        parent.addChild(new Element(){
            {
                setFillParent(true);
                touchable(Touchable.disabled);
            }

             @Override
             public void draw(){
                Draw.color(0f, 0f, 0f, Mathf.clamp(1f - time));
                Fill.crect(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
                Draw.color();
             }

            @Override
            public void act(float delta){
                super.act(delta);
                time += 1f / duration;
                if(time > 1){
                    remove();
                }
            }
        });
    }
}
