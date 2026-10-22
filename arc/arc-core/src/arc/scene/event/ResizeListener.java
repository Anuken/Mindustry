package arc.scene.event;

public class ResizeListener implements EventListener{

    @Override
    public boolean handle(SceneEvent event){
        if(event instanceof SceneResizeEvent){
            //always returns false, because resizing is global.
            resized();
        }
        return false;
    }

    public void resized(){

    }
}
