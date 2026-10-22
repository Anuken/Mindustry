package arc.util;

/** Keeps track of X actions in Y units of time. */
public class Ratekeeper{
    public int occurences;
    public long lastTime;

    /**
     * @return whether an action is allowed.
     * @param spacing the spacing between action chunks in milliseconds
     * @param cap the maximum amount of actions per chunk
     * */
    public boolean allow(long spacing, int cap){
        if(Time.timeSinceMillis(lastTime) > spacing){
            occurences = 0;
            lastTime = Time.millis();
        }

        occurences ++;
        return occurences <= cap;
    }

    public void reset(){
        occurences = 0;
        lastTime = 0;
    }
}
