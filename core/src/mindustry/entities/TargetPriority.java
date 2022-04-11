package mindustry.entities;

/** Higher priority blocks will always get targeted over those of lower priority, regardless of distance. */
public class TargetPriority{
    public static final float
    base = 0f,
    constructing = 1f,
    turret = 2f,
    core = 3f;
}
