package arc.scene.event;

public class VisibilityListener implements EventListener{

    @Override
    public boolean handle(SceneEvent event){
        if(event instanceof VisibilityEvent){
            if(((VisibilityEvent)event).isHide()){
                return hidden();
            }else{
                return shown();
            }
        }
        return false;
    }

    public boolean shown(){
        return false;
    }

    public boolean hidden(){
        return false;
    }
}
