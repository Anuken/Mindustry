package mindustry.core;

import arc.*;
import arc.math.*;
import arc.util.*;

/** Simple per-frame time counter. */
public enum PerfCounter{
    frame,
    update,
    other,
    stateUpdate,
    entityMisc,
    entityUpdate,
    buildingUpdate,
    powerUpdate,
    unitUpdate,
    bulletUpdate,
    ui,
    render;

    public static final PerfCounter[] all = values();

    public static final PerfCounter[] displayedCounters = {
        powerUpdate,
        buildingUpdate,
        entityMisc,
        bulletUpdate,
        unitUpdate,
        render,
        ui,
        stateUpdate,
        other
    };

    static final int meanWindow = 120;
    static final int refreshTimeMillis = 100;

    private long lastUpdateFrame = -1;
    private long valueRefreshTime;
    private float refreshValue;

    private long beginTime;
    private long partValue;
    private boolean began = false;
    private WindowedMean mean = new WindowedMean(meanWindow);

    public void add(long nanos){
        mean.add(nanos);
        lastUpdateFrame = Core.graphics.getFrameId();
    }

    public void begin(){
        lastUpdateFrame = Core.graphics.getFrameId();
        began = true;
        beginTime = Time.nanos();
    }

    public void end(){
        end(0);
    }

    public void end(long subtract){
        if(!began) return;
        lastUpdateFrame = Core.graphics.getFrameId();
        began = false;
        mean.add(Time.timeSinceNanos(beginTime) - subtract);
    }

    public void checkUpdate(){
        if(lastUpdateFrame < Core.graphics.getFrameId()){
            mean.add(0f);
        }
    }

    public void beginPart(){
        began = true;
        beginTime = Time.nanos();
    }

    public void endPart(){
        if(!began) return;
        began = false;
        partValue += Time.timeSinceNanos(beginTime);
    }

    public void finishParts(){
        lastUpdateFrame = Core.graphics.getFrameId();
        mean.add(partValue);
        partValue = 0;
    }

    /** Value with a periodic refresh interval applied, to prevent jittery UI. */
    public float valueMs(){
        if(Time.timeSinceMillis(valueRefreshTime) > refreshTimeMillis){
            refreshValue = rawValueMs();
            valueRefreshTime = Time.millis();
        }
        return refreshValue;
    }

    public long latestValueNs(){
        return (long)mean.latest();
    }

    /** Raw value without a refresh interval. This will be unstable. */
    public float rawValueMs(){
        return mean.rawMean() / Time.nanosPerMilli;
    }

    public long rawValueNs(){
        return (long)mean.rawMean();
    }
}
