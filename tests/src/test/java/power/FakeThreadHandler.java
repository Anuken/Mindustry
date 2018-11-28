package power;

import io.anuke.mindustry.core.ThreadHandler;
import io.anuke.ucore.core.Timers;

/** Fake thread handler which produces a new frame each time getFrameID is called and always provides a delta of 1. */
public class FakeThreadHandler extends ThreadHandler{
    private int fakeFrameId = 0;
    public static final float fakeDelta = 0.5f;

    FakeThreadHandler(){
        super();

        Timers.setDeltaProvider(() -> fakeDelta);
    }
    @Override
    public long getFrameID(){
        return ++fakeFrameId;
    }

}
