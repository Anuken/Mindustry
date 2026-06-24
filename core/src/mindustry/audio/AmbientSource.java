package mindustry.audio;

import arc.audio.*;
import arc.math.geom.*;

public interface AmbientSource extends Position{
    boolean isValid();
    boolean shouldAmbientSound();
    float getAmbientVolume();
    Sound getAmbientSound();
}
