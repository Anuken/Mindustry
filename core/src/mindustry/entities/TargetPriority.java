package mindustry.entities;

/** A higher ordinal means a higher priority. Higher priority blocks will always get targeted over those of lower priority, regardless of distance. */
public enum TargetPriority{
    base,
    turret,
    core
}
