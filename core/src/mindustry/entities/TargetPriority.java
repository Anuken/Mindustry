package mindustry.entities;

/** Higher priority blocks will always get targeted over those of lower priority, regardless of distance. */
public class TargetPriority{
    public static final float
    //nobody cares about walls
    wall = -2f,
    //transport infrastructure isn't as important as factories
    transport = -1f,
    //most blocks
    base = 0f,
    //turrets deal damage so they are more important
    turret = 1f,
    //core is always the most important thing to destroy
    core = 2f;
}
