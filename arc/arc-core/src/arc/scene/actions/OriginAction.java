package arc.scene.actions;

import arc.scene.Action;
import arc.util.Align;

public class OriginAction extends Action{

    @Override
    public boolean act(float delta){
        actor.setOrigin(Align.center);
        return true;
    }

}
