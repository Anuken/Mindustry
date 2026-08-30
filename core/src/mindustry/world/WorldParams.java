package mindustry.world;

/** Parameters for loading or generating a world. */
public class WorldParams{
    /** For sectors: World generator seed offset. */
    public int seedOffset;
    /** For sectors: Whether to save the info once the map is generated. A value of 'false' is used for editor generation. */
    public boolean saveInfo = true;
    /** Position in packed x/y format - not array format. Overrides the core position when generating with a FileMapGenerator. 0 to disable. */
    public int corePositionOverride;
}
