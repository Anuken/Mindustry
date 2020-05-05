package mindustry.ai.formations.patterns;

import arc.math.*;
import arc.math.geom.*;
import mindustry.ai.formations.*;

public class SquareFormation extends FormationPattern{
    public float spacing = 20;

    @Override
    public Vec3 calculateSlotLocation(Vec3 out, int slot){
        //side of each square of formation
        int side = Mathf.ceil(Mathf.sqrt(slots + 1));
        int cx = slot % side, cy = slot / side;

        //don't hog the middle spot
        if(cx == side /2 && cy == side/2 && (side%2)==1){
            slot = slots;

            cx = slot % side;
            cy = slot / side;
        }

        return out.set(cx - (side/2f - 0.5f), cy - (side/2f - 0.5f), 0).scl(spacing);
    }
}
