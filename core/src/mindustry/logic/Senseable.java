package mindustry.logic;

import mindustry.ctype.*;

public interface Senseable{
    double sense(LAccess sensor);
    double sense(Content content);
}
