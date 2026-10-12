package arc.scene.event;

public class VisibilityEvent extends SceneEvent{
    private boolean hide;

    public VisibilityEvent(){
    }

    public VisibilityEvent(boolean hide){
        this.hide = hide;
    }

    public boolean isHide(){
        return hide;
    }
}
