package mindustry.game;

import arc.func.*;
import arc.struct.*;
import arc.util.*;

public class MapObjectives{
    public static Seq<Prov<MapObjective>> allObjectiveTypes = Seq.with();

    public abstract class MapObjective{

        public boolean complete(){
            return false;
        }

        public void update(){

        }

        /** Reset internal state, if any. */
        public void reset(){

        }

        public @Nullable String details(){
            return null;
        }
    }
}
