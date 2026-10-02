package arc.util;

import java.util.Arrays;

public class Interval{
    float[] times;

    public Interval(int capacity){
        times = new float[capacity];
    }

    public Interval(){
        this(1);
    }

    public boolean get(float time){
        return get(0, time);
    }

    public boolean get(int id, float time){
        if(id >= times.length) throw new RuntimeException("Out of bounds! Max timer size is " + times.length + "!");

        boolean got = check(id, time);
        if(got) times[id] = Time.time;
        return got;
    }

    public boolean check(int id, float time){
        return Time.time - times[id] >= time || Time.time < times[id];
    }

    public void reset(int id, float time){
        times[id] = Time.time - time;
    }

    public void clear(){
        Arrays.fill(times, 0);
    }

    public float getTime(int id){
        return Time.time - times[id];
    }

    public float[] getTimes(){
        return times;
    }
}
