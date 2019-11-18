package io.anuke.mindustry.entities.traits;

import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;

/**
 * Any TileEntity that implements this interface can display a Force Projector-like projection over other tiles.
 * This respects the Animate Shields option, and doesn't display edges on overlapping shields of the same type.
 */
public interface ProjectorTrait extends Entity, DrawTrait{
    ArrayMap<String, Array<ProjectorTrait>> projectorSets = new ArrayMap<>();

    static void onAddProjector(ProjectorTrait entity){
        if(!projectorSets.containsKey(entity.projectorSet()))
            projectorSets.put(entity.projectorSet(), new Array<>());
        projectorSets.get(entity.projectorSet()).add(entity);
    }

    static void onRemoveProjector(ProjectorTrait entity){
        Array<ProjectorTrait> set = projectorSets.get(entity.projectorSet());
        set.remove(entity);
        if(set.isEmpty())
            projectorSets.removeKey(entity.projectorSet());
    }

    /**
     * Called when rendering this projector's projection when Animate Shields is on. Set the draw color to white and the draw opacity
     * to 0, then draw like normal.
     */
    void drawOver();

    /**
     * Called when rendering this projector's projection when Animate Shields is off. Draw as normal.
     */
    void drawSimple();

    /**
     * Gets the color of this projector's projection when Animate Shields is on.
     * @return The color of this projector's projection.
     */
    Color accent();

    /**
     * Returns a string identifier for this type of projector. Projectors with the same projectorSet won't render edges
     * between overlapping projectors.
     * @return An identifier for this type of projector.
     */
    String projectorSet();
}
