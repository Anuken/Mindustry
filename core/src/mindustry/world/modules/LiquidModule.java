package mindustry.world.modules;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.type.*;

import java.util.*;

import static mindustry.Vars.*;

public class LiquidModule extends BlockModule{
    private static final int windowSize = 3;
    private static final Interval flowTimer = new Interval(2);
    private static final float pollScl = 20f;

    private static WindowedMean[] cacheFlow;
    private static float[] cacheSums;
    private static float[] displayFlow;
    private static final Bits cacheBits = new Bits();

    private float[] liquids = new float[content.liquids().size];
    private Liquid current = content.liquid(0);

    private @Nullable WindowedMean[] flow;

    public void updateFlow(){
        if(flowTimer.get(1, pollScl)){
            if(flow == null){
                if(cacheFlow == null || cacheFlow.length != liquids.length){
                    cacheFlow = new WindowedMean[liquids.length];
                    for(int i = 0; i < liquids.length; i++){
                        cacheFlow[i] = new WindowedMean(windowSize);
                    }
                    cacheSums = new float[liquids.length];
                    displayFlow = new float[liquids.length];
                }else{
                    for(int i = 0; i < liquids.length; i++){
                        cacheFlow[i].reset();
                    }
                    Arrays.fill(cacheSums, 0);
                    cacheBits.clear();
                }

                Arrays.fill(displayFlow, -1);

                flow = cacheFlow;
            }

            boolean updateFlow = flowTimer.get(30);

            for(int i = 0; i < liquids.length; i++){
                flow[i].add(cacheSums[i]);
                if(cacheSums[i] > 0){
                    cacheBits.set(i);
                }
                cacheSums[i] = 0;

                if(updateFlow){
                    displayFlow[i] = flow[i].hasEnoughData() ? flow[i].mean() / pollScl : -1;
                }
            }
        }
    }

    public void stopFlow(){
        flow = null;
    }

    /** @return current liquid's flow rate in u/s; any value < 0 means 'not ready'. */
    public float getFlowRate(Liquid liquid){
        return flow == null ? -1f : displayFlow[liquid.id] * 60;
    }

    public boolean hasFlowLiquid(Liquid liquid){
        return flow != null && cacheBits.get(liquid.id);
    }

    /** Last received or loaded liquid. Only valid for liquid modules with 1 type of liquid. */
    public Liquid current(){
        return current;
    }

    public void reset(Liquid liquid, float amount){
        Arrays.fill(liquids, 0f);
        liquids[liquid.id] = amount;
        current = liquid;
    }

    public void set(Liquid liquid, float amount){
        if(amount >= liquids[current.id]){
            current = liquid;
        }
        liquids[liquid.id] = amount;
    }

    public float currentAmount(){
        return liquids[current.id];
    }

    public float get(Liquid liquid){
        return liquids[liquid.id];
    }

    public void clear(){
        Arrays.fill(liquids, 0);
    }

    public void add(Liquid liquid, float amount){
        liquids[liquid.id] += amount;
        current = liquid;

        if(flow != null){
            cacheSums[liquid.id] += Math.max(amount, 0);
        }
    }

    public void handleFlow(Liquid liquid, float amount){
        if(flow != null){
            cacheSums[liquid.id] += Math.max(amount, 0);
        }
    }

    public void remove(Liquid liquid, float amount){
        //cap to prevent negative removal
        add(liquid, Math.max(-amount, -liquids[liquid.id]));
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
        int amount = 0;
        for(float liquid : liquids){
            if(liquid > 0) amount++;
        }

        write.s(amount); //amount of liquids

        for(int i = 0; i < liquids.length; i++){
            if(liquids[i] > 0){
                write.s(i); //liquid ID
                write.f(liquids[i]); //liquid amount
            }
        }
    }

    @Override
    public void read(Reads read, boolean legacy){
        Arrays.fill(liquids, 0);
        int count = legacy ? read.ub() : read.s();

        for(int j = 0; j < count; j++){
            Liquid liq = content.liquid(legacy ? read.ub() : read.s());
            float amount = read.f();
            if(liq != null){
                int liquidid = liq.id;
                liquids[liquidid] = amount;
                if(amount > liquids[current.id]){
                    current = liq;
                }
            }
        }
    }

    public interface LiquidConsumer{
        void accept(Liquid liquid, float amount);
    }

    public interface LiquidCalculator{
        float get(Liquid liquid, float amount);
    }
}
