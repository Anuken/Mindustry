package mindustry.world.modules;

import arc.math.*;

public class ThroughputModule{

    public WindowedMean window = new WindowedMean(60 * 10);
    public float i;

    public void update(){
        window.addValue(i);
        i = 0;
    }
}
