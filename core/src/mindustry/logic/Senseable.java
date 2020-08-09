package mindustry.logic;

import mindustry.ctype.*;

public interface Senseable{
    double sense(LSensor sensor);
    double sense(Content content);
}
