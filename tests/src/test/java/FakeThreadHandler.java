import com.badlogic.gdx.Gdx;
import io.anuke.mindustry.core.ThreadHandler;
import io.anuke.ucore.core.Timers;

/** Fake thread handler which produces a new frame each time getFrameID is called and always provides a delta of 1. */
public class FakeThreadHandler extends ThreadHandler{
    private int fakeFrameId = 0;

    FakeThreadHandler(){
        super();

        Timers.setDeltaProvider(() -> 1.0f);
    }
    @Override
    public long getFrameID(){
        return ++fakeFrameId;
    }

}
