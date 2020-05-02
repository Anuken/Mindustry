package mindustry.ai.ai.utils;

/** All units are in seconds. */
public class Timepiece{
    public static float time;
    public static float deltaTime;

    public void update(float deltaTime){
        Timepiece.deltaTime = deltaTime;
        Timepiece.time = time + deltaTime;
    }

}
