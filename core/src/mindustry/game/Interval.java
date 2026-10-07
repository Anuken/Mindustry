package mindustry.game;

import mindustry.*;

import java.util.*;

public class Interval{
    public float[] times;

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
        if(got) times[id] = Vars.state.time;
        return got;
    }

    public boolean check(int id, float time){
        return Vars.state.time - times[id] >= time || Vars.state.time < times[id];
    }

    public void reset(int id, float time){
        times[id] = Vars.state.time - time;
    }

    public void clear(){
        Arrays.fill(times, 0);
    }
}
