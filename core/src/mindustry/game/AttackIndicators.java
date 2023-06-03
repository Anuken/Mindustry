package mindustry.game;

import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

/** Updates and stores attack indicators for the minimap. */
public class AttackIndicators{
    private static final float duration = 15f * 60f;

    private LongSeq indicators = new LongSeq(false, 16);
    private IntIntMap posToIndex = new IntIntMap();

    public LongSeq list(){
        return indicators;
    }

    public void clear(){
        indicators.clear();
        posToIndex.clear();
    }

    public void add(int x, int y){
        int pos = Point2.pack(x, y);
        int index = posToIndex.get(pos, -1);

        //there is an existing indicator...
        if(index != -1){
            //reset its time (new attack)
            indicators.items[index] = Indicator.time(indicators.items[index], 0f);
        }else{
            //new indicator created
            indicators.add(Indicator.get(pos, 0f));
            posToIndex.put(pos, indicators.size - 1);
        }
    }

    public void update(){
        long[] items = indicators.items;
        for(int i = 0; i < indicators.size; i ++){
            long l = items[i];
            items[i] = l = Indicator.time(l, Indicator.time(l) + Time.delta);

            if(Indicator.time(l) >= duration){
                //remove the indicator as it has timed out, make sure to not skip the next one
                indicators.removeIndex(i);
                posToIndex.remove(Indicator.pos(l));

                if(indicators.size > 0){
                    //relocation of head to this new index
                    posToIndex.put(Indicator.pos(items[i]), i);
                }

                i --;
            }
        }
    }

    @Struct
    class IndicatorStruct{
        int pos;
        float time;
    }
}
