package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.math.Interpolation;
import io.anuke.mindustry.type.Category;
import io.anuke.ucore.scene.Group;

public class BlocksFragment extends Fragment{

    @Override
    public void build(Group parent){
        parent.fill(frame -> {
            frame.bottom().left();
            for(int i = 0; i < Category.values().length; i++){

            }
        });
    }

    /**Rebuilds the whole placement menu, attempting to preserve previous state.*/
    void rebuild(){

    }

    void toggle(float t, Interpolation ip){

    }
}