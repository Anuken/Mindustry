package mindustry.core;

import arc.math.*;
import arc.util.*;

/** Simple per-frame time counter. */
public enum PerfCounter{
    frame,
    update,
    entityUpdate,
    ui,
    render;

    public static final PerfCounter[] all = values();

    static final int meanWindow = 30;
    static final int refreshTimeMillis = 500;

    private long valueRefreshTime;
    private float refreshValue;

    private long beginTime;
    private boolean began = false;
    private WindowedMean mean = new WindowedMean(meanWindow);

    public void begin(){
        began = true;
        beginTime = Time.nanos();
    }

    public void end(){
        if(!began) return;
        began = false;
        mean.add(Time.timeSinceNanos(beginTime));
    }

    /** Value with a periodic refresh interval applied, to prevent jittery UI. */
    public float valueMs(){
        if(Time.timeSinceMillis(valueRefreshTime) > refreshTimeMillis){
            refreshValue = rawValueMs();
            valueRefreshTime = Time.millis();
        }
        return refreshValue;
    }

    /** Raw value without a refresh interval. This will be unstable. */
    public float rawValueMs(){
        return mean.rawMean() / Time.nanosPerMilli;
    }

    public long rawValueNs(){
        return (long)mean.rawMean();
    }
}
