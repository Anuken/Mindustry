package arc.fx.filters;

import arc.fx.*;
import arc.fx.util.*;
import arc.util.*;

/**
 * The base class for any multi-pass filter.
 * Usually a multi-pass filter will make use of one or more single-pass filters,
 * promoting composition over inheritance.
 */
public abstract class MultipassVfxFilter implements Disposable{

    /** @see FxFilter#resize(int, int) */
    public void resize(int width, int height){

    }

    /** @see FxFilter#rebind() */
    public abstract void setParams();

    public abstract void render(PingPongBuffer pingPongBuffer);
}
