package io.anuke.ucore.scene.event;

public class VisibilityListener implements EventListener{

    @Override
    public boolean handle(Event event) {
        if(event instanceof VisibilityEvent){
            if(((VisibilityEvent)event).isHide()){
                return hidden();
            }else{
                return shown();
            }
        }
        return false;
    }

    public boolean shown(){ return false;}
    public boolean hidden(){ return false;}
}
