package arc.graphics.g2d;

import arc.struct.Seq;
import arc.math.Mathf;

/**
 * <p>
 * An Animation stores a list of objects representing an animated sequence, e.g. for running or jumping. Each
 * object in the Animation is called a key frame, and multiple key frames make up the animation.
 * <p>
 * The animation's type is the class representing a frame of animation. For example, a typical 2D animation could be made
 * up of {@link arc.graphics.g2d.TextureRegion TextureRegions} and would be specified as:
 * <p><code>Animation&lt;TextureRegion&gt; myAnimation = new Animation&lt;TextureRegion&gt;(...);</code>
 * @author mzechner
 */
public class Animation<T>{
    /** Length must not be modified without updating {@link #animationDuration}. See {@link #setKeyFrames(T[])}. */
    T[] keyFrames;
    private float frameDuration;
    private float animationDuration;
    private int lastFrameNumber;
    private float lastStateTime;
    private PlayMode playMode = PlayMode.normal;

    /**
     * Constructor, storing the frame duration and key frames.
     * @param frameDuration the time between frames in seconds.
     * @param keyFrames the objects representing the frames. If this Array is type-aware, {@link #getKeyFrames()} can return the
     * correct type of array. Otherwise, it returns an Object[].
     */
    @SuppressWarnings("unchecked")
    public Animation(float frameDuration, Seq<? extends T> keyFrames){
        this.frameDuration = frameDuration;
        Class arrayType = keyFrames.items.getClass().getComponentType();
        T[] frames = (T[])java.lang.reflect.Array.newInstance(arrayType, keyFrames.size);
        for(int i = 0, n = keyFrames.size; i < n; i++){
            frames[i] = keyFrames.get(i);
        }
        setKeyFrames(frames);
    }

    /**
     * Constructor, storing the frame duration and key frames.
     * @param frameDuration the time between frames in seconds.
     * @param keyFrames the objects representing the frames. If this Array is type-aware, {@link #getKeyFrames()} can
     * return the correct type of array. Otherwise, it returns an Object[].
     */
    public Animation(float frameDuration, Seq<? extends T> keyFrames, PlayMode playMode){
        this(frameDuration, keyFrames);
        setPlayMode(playMode);
    }

    /**
     * Constructor, storing the frame duration and key frames.
     * @param frameDuration the time between frames in seconds.
     * @param keyFrames the objects representing the frames.
     */
    public Animation(float frameDuration, T... keyFrames){
        this.frameDuration = frameDuration;
        setKeyFrames(keyFrames);
    }

    /**
     * Returns a frame based on the so called state time. This is the amount of seconds an object has spent in the
     * state this Animation instance represents, e.g. running, jumping and so on. The mode specifies whether the animation is
     * looping or not.
     * @param stateTime the time spent in the state represented by this animation.
     * @param looping whether the animation is looping or not.
     * @return the frame of animation for the given state time.
     */
    public T getKeyFrame(float stateTime, boolean looping){
        // we set the play mode by overriding the previous mode based on looping
        // parameter value
        PlayMode oldPlayMode = playMode;
        if(looping && (playMode == PlayMode.normal || playMode == PlayMode.reversed)){
            if(playMode == PlayMode.normal)
                playMode = PlayMode.loop;
            else
                playMode = PlayMode.loopReversed;
        }else if(!looping && !(playMode == PlayMode.normal || playMode == PlayMode.reversed)){
            if(playMode == PlayMode.loopReversed)
                playMode = PlayMode.reversed;
            else
                playMode = PlayMode.loop;
        }

        T frame = getKeyFrame(stateTime);
        playMode = oldPlayMode;
        return frame;
    }

    /**
     * Returns a frame based on the so called state time. This is the amount of seconds an object has spent in the
     * state this Animation instance represents, e.g. running, jumping and so on using the mode specified by
     * {@link #setPlayMode(PlayMode)} method.
     * @return the frame of animation for the given state time.
     */
    public T getKeyFrame(float stateTime){
        int frameNumber = getKeyFrameIndex(stateTime);
        return keyFrames[frameNumber];
    }

    /**
     * Returns the current frame number.
     * @return current frame number
     */
    public int getKeyFrameIndex(float stateTime){
        if(keyFrames.length == 1) return 0;

        int frameNumber = (int)(stateTime / frameDuration);
        switch(playMode){
            case normal:
                frameNumber = Math.min(keyFrames.length - 1, frameNumber);
                break;
            case loop:
                frameNumber = frameNumber % keyFrames.length;
                break;
            case loopPingPong:
                frameNumber = frameNumber % ((keyFrames.length * 2) - 2);
                if(frameNumber >= keyFrames.length)
                    frameNumber = keyFrames.length - 2 - (frameNumber - keyFrames.length);
                break;
            case loopRandom:
                int lastFrameNumber = (int)((lastStateTime) / frameDuration);
                if(lastFrameNumber != frameNumber){
                    frameNumber = Mathf.random(keyFrames.length - 1);
                }else{
                    frameNumber = this.lastFrameNumber;
                }
                break;
            case reversed:
                frameNumber = Math.max(keyFrames.length - frameNumber - 1, 0);
                break;
            case loopReversed:
                frameNumber = frameNumber % keyFrames.length;
                frameNumber = keyFrames.length - frameNumber - 1;
                break;
        }

        lastFrameNumber = frameNumber;
        lastStateTime = stateTime;

        return frameNumber;
    }

    /**
     * Returns the keyframes[] array where all the frames of the animation are stored.
     * @return The keyframes[] field. This array is an Object[] if the animation was instantiated with an Array that was not
     * type-aware.
     */
    public T[] getKeyFrames(){
        return keyFrames;
    }

    protected void setKeyFrames(T... keyFrames){
        this.keyFrames = keyFrames;
        this.animationDuration = keyFrames.length * frameDuration;
    }

    /** Returns the animation play mode. */
    public PlayMode getPlayMode(){
        return playMode;
    }

    /**
     * Sets the animation play mode.
     * @param playMode The animation {@link PlayMode} to use.
     */
    public void setPlayMode(PlayMode playMode){
        this.playMode = playMode;
    }

    /**
     * Whether the animation would be finished if played without looping (PlayMode#NORMAL), given the state time.
     * @return whether the animation is finished.
     */
    public boolean isAnimationFinished(float stateTime){
        int frameNumber = (int)(stateTime / frameDuration);
        return keyFrames.length - 1 < frameNumber;
    }

    /** @return the duration of a frame in seconds */
    public float getFrameDuration(){
        return frameDuration;
    }

    /**
     * Sets duration a frame will be displayed.
     * @param frameDuration in seconds
     */
    public void setFrameDuration(float frameDuration){
        this.frameDuration = frameDuration;
        this.animationDuration = keyFrames.length * frameDuration;
    }

    /** @return the duration of the entire animation, number of frames times frame duration, in seconds */
    public float getAnimationDuration(){
        return animationDuration;
    }

    /** Defines possible playback modes for an {@link Animation}. */
    public enum PlayMode{
        normal,
        reversed,
        loop,
        loopReversed,
        loopPingPong,
        loopRandom,
    }
}
