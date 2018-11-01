package io.anuke.ucore.scene.event;

import io.anuke.ucore.scene.Element;

import java.beans.Visibility;

public class VisibilityEvent extends Event{
    private boolean hide;

    public VisibilityEvent(){}

    public VisibilityEvent(boolean hide){
        this.hide = hide;
    }

    public boolean isHide() {
        return hide;
    }
}
