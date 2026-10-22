package arc.scene.ui.layout;

import arc.func.*;
import arc.scene.*;

public class Spacer extends Element{
    Floatp widthFunc, heightFunc;

    public Spacer(Floatp widthFunc, Floatp heightFunc){
        this.widthFunc = widthFunc;
        this.heightFunc = heightFunc;

        width = Scl.scl(widthFunc.get());
        height = Scl.scl(heightFunc.get());
    }

    @Override
    public void act(float delta){
        super.act(delta);

        float w = Scl.scl(widthFunc.get()), h = Scl.scl(heightFunc.get());
        if(w != width || h != height){
            width = w;
            height = h;
            invalidateHierarchy();
        }
    }

    @Override
    public float getPrefHeight(){
        return Scl.scl(heightFunc.get());
    }

    @Override
    public float getPrefWidth(){
        return Scl.scl(widthFunc.get());
    }
}
