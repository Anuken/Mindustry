package io.anuke.ucore.scene.actions;

import io.anuke.ucore.function.Callable;
import io.anuke.ucore.scene.Action;

public class CallAction extends Action{
    public Callable call;
    public boolean called = false;

    @Override
    public boolean act(float delta) {
        if(!called) call.run();
        called = true;
        return true;
    }

    @Override
    public void reset() {
        called = false;
    }
}
