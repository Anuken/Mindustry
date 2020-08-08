package mindustry.world.modules;

import arc.math.*;
import arc.util.*;
import arc.util.ArcAnnotate.*;
import arc.util.io.*;
import mindustry.type.*;

import java.util.*;

import static mindustry.Vars.content;

public class LiquidModule extends BlockModule{
    private static final int windowSize = 3, updateInterval = 60;
    private static final Interval flowTimer = new Interval(2);
    private static final float pollScl = 20f;

    private float[] liquids = new float[content.liquids().size];
    private float total;
    private Liquid current = content.liquid(0);
    private float smoothLiquid;

    private @Nullable WindowedMean flow;
    private float lastAdded, currentFlowRate;

    public void update(boolean showFlow){
        smoothLiquid = Mathf.lerpDelta(smoothLiquid, currentAmount(), 0.1f);
        if(showFlow){
            if(flowTimer.get(1, pollScl)){

                if(flow == null) flow = new WindowedMean(windowSize);
                flow.add(lastAdded);
                lastAdded = 0;
                if(currentFlowRate < 0 || flowTimer.get(updateInterval)){
                    currentFlowRate = flow.hasEnoughData() ? flow.mean() / pollScl : -1f;
                }
            }
        }else{
            currentFlowRate = -1f;
            flow = null;
        }
    }

    /** @return current liquid's flow rate in u/s; any value < 0 means 'not ready'. */
    public float getFlowRate(){
        return currentFlowRate * 60;
    }

    public float smoothAmount(){
        return smoothLiquid;
    }

    /** @return total amount of liquids. */
    public float total(){
        return total;
    }

    /** Last received or loaded liquid. Only valid for liquid modules with 1 type of liquid. */
    public Liquid current(){
        return current;
    }

    public void reset(Liquid liquid, float amount){
        Arrays.fill(liquids, 0f);
        liquids[liquid.id] = amount;
        total = amount;
        current = liquid;
    }

    public float currentAmount(){
        return liquids[current.id];
    }

    public float get(Liquid liquid){
        return liquids[liquid.id];
    }

    public void clear(){
        total = 0;
        Arrays.fill(liquids, 0);
    }

    public void add(Liquid liquid, float amount){
        liquids[liquid.id] += amount;
        total += amount;
        current = liquid;

        if(flow != null){
            lastAdded += Math.max(amount, 0);
        }
    }

    public void remove(Liquid liquid, float amount){
        add(liquid, -amount);
    }

    public void each(LiquidConsumer cons){
        for(int i = 0; i < liquids.length; i++){
            if(liquids[i] > 0){
                cons.accept(content.liquid(i), liquids[i]);
            }
        }
    }

    public float sum(LiquidCalculator calc){
        float sum = 0f;
        for(int i = 0; i < liquids.length; i++){
            if(liquids[i] > 0){
                sum += calc.get(content.liquid(i), liquids[i]);
            }
        }
        return sum;
    }

    @Override
    public void write(Writes write){
        byte amount = 0;
        for(float liquid : liquids){
            if(liquid > 0) amount++;
        }

        write.b(amount); //amount of liquids

        for(int i = 0; i < liquids.length; i++){
            if(liquids[i] > 0){
                write.b(i); //liquid ID
                write.f(liquids[i]); //item amount
            }
        }
    }

    @Override
    public void read(Reads read){
        Arrays.fill(liquids, 0);
        total = 0f;
        byte count = read.b();

        for(int j = 0; j < count; j++){
            int liquidid = read.b();
            float amount = read.f();
            liquids[liquidid] = amount;
            if(amount > 0){
                current = content.liquid(liquidid);
            }
            this.total += amount;
        }
    }

    public interface LiquidConsumer{
        void accept(Liquid liquid, float amount);
    }

    public interface LiquidCalculator{
        float get(Liquid liquid, float amount);
    }
}
