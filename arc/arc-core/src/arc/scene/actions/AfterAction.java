package arc.scene.actions;

import arc.struct.Seq;
import arc.scene.Action;
import arc.scene.Element;

/**
 * Executes an action only after all other actions on the actor at the time this action's target was set have finished.
 * @author Nathan Sweet
 */
public class AfterAction extends DelegateAction{
    private Seq<Action> waitForActions = new Seq<>(false, 4);

    @Override
    public void setTarget(Element target){
        if(target != null) waitForActions.addAll(target.getActions());
        super.setTarget(target);
    }

    @Override
    public void restart(){
        super.restart();
        waitForActions.clear();
    }

    @Override
    protected boolean delegate(float delta){
        Seq<Action> currentActions = target.getActions();
        if(currentActions.size == 1) waitForActions.clear();
        for(int i = waitForActions.size - 1; i >= 0; i--){
            Action action = waitForActions.get(i);
            int index = currentActions.indexOf(action, true);
            if(index == -1) waitForActions.remove(i);
        }
        return waitForActions.size <= 0 && action.act(delta);
    }
}
