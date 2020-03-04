package mindustry.type;

import arc.util.ArcAnnotate.*;

/** Any object that is orbiting a planet. */
public class Satellite{
    public @NonNull Planet planet;

    public Satellite(@NonNull Planet orbiting){
        this.planet = orbiting;
    }
}
