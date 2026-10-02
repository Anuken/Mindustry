package arc.audio;

import arc.*;
import arc.util.*;

import static arc.audio.Soloud.*;

public abstract class AudioSource implements Disposable{
    protected long handle;

    protected int maxConcurrent, concurrentGroup;
    protected float minInterruptAbsolute, minInterruptFraction;
    protected float priority;

    public boolean valid(){
        return handle != 0;
    }

    public void setFilter(int index, @Nullable AudioFilter filter){
        if(handle == 0) return;
        sourceFilter(handle, index, filter == null ? 0 : filter.handle);
    }

    public void setFilter(@Nullable AudioFilter filter){
        setFilter(0, filter);
    }

    protected void setParamsAfterLoad(){
        if(priority != 0f) setPriority(priority);
        if(maxConcurrent != 0) setMaxConcurrent(maxConcurrent);
        if(concurrentGroup != 0) setConcurrentGroup(concurrentGroup);
        if(minInterruptFraction != 0f){
            setMinConcurrentInterruptFraction(minInterruptAbsolute, minInterruptFraction);
        }else if(minInterruptAbsolute != 0f){
            setMinConcurrentInterrupt(minInterruptAbsolute);
        }
    }

    /** Sets the priority of this source. Sources with higher priorities will not get cut off by those of lower priorities. */
    public void setPriority(float priority){
        this.priority = priority;
        if(handle == 0) return;
        sourcePriority(handle, priority);
    }

    /** Sets the priority of this source. Sources with higher priorities will not get cut off by those of lower priorities. */
    public void setMaxConcurrent(int max){
        this.maxConcurrent = max;
        if(handle == 0) return;
        sourceMaxConcurrent(handle, max);
    }

    /** Sets the group ID of this source, for which maxConcurrent will be enforced. If unset, a unique group will be created for this sound.  */
    public void setConcurrentGroup(int group){
        this.concurrentGroup = group;
        if(handle == 0) return;
        sourceConcurrentGroup(handle, group);
    }

    /** Sets the minimum playtime (in seconds) that a sound must have in order to be interrupted when its concurrent limit is reached. */
    public void setMinConcurrentInterrupt(float seconds){
        minInterruptAbsolute = seconds;
        minInterruptFraction = 0f;
        if(handle == 0) return;
        sourceMinConcurrentInterrupt(handle, seconds);
    }

    /** Sets the minimum playtime (in seconds) that a sound must have in order to be interrupted when its concurrent limit is reached. This is a fraction of length. */
    public void setMinConcurrentInterruptFraction(float min, float fraction){
        minInterruptFraction = fraction;
        minInterruptAbsolute = min;
        if(handle == 0) return;
        sourceMinConcurrentInterrupt(handle, Math.min(min, getLength() * fraction));
    }

    /** @return number of currently playing instances */
    public int countPlaying(){
        if(handle == 0) return  0;
        return Core.audio.countPlaying(this);
    }

    public void setSingleInstance(boolean singleInstance){
        if(handle == 0 || !Core.audio.initialized) return;
        sourceSingleInstance(handle, singleInstance);
    }

    public void stop(){
        if(handle == 0) return;
        sourceStop(handle);
    }

    public abstract float getLength();

    /** @return true if this is a lazily loaded source (only loaded once played) */
    public boolean isLazy(){
        return false;
    }

    @Override
    public void dispose(){
        if(handle != 0) sourceDestroy(handle);
        handle = 0;
    }
}
