package arc.scene.actions;

import arc.struct.Seq;
import arc.scene.Action;
import arc.scene.Element;
import arc.util.pooling.Pool;

/**
 * Executes a number of actions at the same time.
 * @author Nathan Sweet
 */
public class ParallelAction extends Action{
    Seq<Action> actions = new Seq<>(4);
    private boolean complete;

    public ParallelAction(){
    }

    public ParallelAction(Action action1){
        addAction(action1);
    }

    public ParallelAction(Action action1, Action action2){
        addAction(action1);
        addAction(action2);
    }

    public ParallelAction(Action action1, Action action2, Action action3){
        addAction(action1);
        addAction(action2);
        addAction(action3);
    }

    public ParallelAction(Action action1, Action action2, Action action3, Action action4){
        addAction(action1);
        addAction(action2);
        addAction(action3);
        addAction(action4);
    }

    public ParallelAction(Action action1, Action action2, Action action3, Action action4, Action action5){
        addAction(action1);
        addAction(action2);
        addAction(action3);
        addAction(action4);
        addAction(action5);
    }

    @Override
    public boolean act(float delta){
        if(complete) return true;
        complete = true;
        Pool pool = getPool();
        setPool(null); // Ensure this action can't be returned to the pool while executing.
        try{
            Seq<Action> actions = this.actions;
            for(int i = 0, n = actions.size; i < n && actor != null; i++){
                Action currentAction = actions.get(i);
                if(currentAction.getActor() != null && !currentAction.act(delta)) complete = false;
                if(actor == null) return true; // This action was removed.
            }
            return complete;
        }finally{
            setPool(pool);
        }
    }

    @Override
    public void restart(){
        complete = false;
        Seq<Action> actions = this.actions;
        for(int i = 0, n = actions.size; i < n; i++)
            actions.get(i).restart();
    }

    @Override
    public void reset(){
        super.reset();
        actions.clear();
    }

    public void addAction(Action action){
        actions.add(action);
        if(actor != null) action.setActor(actor);
    }

    @Override
    public void setActor(Element actor){
        Seq<Action> actions = this.actions;
        for(int i = 0, n = actions.size; i < n; i++)
            actions.get(i).setActor(actor);
        super.setActor(actor);
    }

    public Seq<Action> getActions(){
        return actions;
    }

    public String toString(){
        StringBuilder buffer = new StringBuilder(64);
        buffer.append(super.toString());
        buffer.append('(');
        Seq<Action> actions = this.actions;
        for(int i = 0, n = actions.size; i < n; i++){
            if(i > 0) buffer.append(", ");
            buffer.append(actions.get(i));
        }
        buffer.append(')');
        return buffer.toString();
    }
}
