package io.anuke.mindustry.entities.traits;

import io.anuke.arc.graphics.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Any TileEntity that implements this interface can display a Force Projector-like projection over other tiles.
 * This respects the Animate Shields option and doesn't display edges on overlapping shields of the same type.
 */
public interface ProjectorTrait extends Entity, DrawTrait{
	
	Map<String, List<ProjectorTrait>> projectorSets = new HashMap<>();
	
	static void onAddProjector(ProjectorTrait entity){
		projectorSets.computeIfAbsent(entity.projectorSet(), key -> new ArrayList<>());
		projectorSets.get(entity.projectorSet()).add(entity);
	}
	
	static void onRemoveProjector(ProjectorTrait entity){
		projectorSets.get(entity.projectorSet()).remove(entity);
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
	 *
	 * @return The color of this projector's projection.
	 */
	Color accent();
	
	/**
	 * Returns a string identifier for this type of projector. Projectors with the same projectorSet won't render edges
	 * between overlapping projectors.
	 *
	 * @return An identifier for this type of projector.
	 */
	String projectorSet();
}
