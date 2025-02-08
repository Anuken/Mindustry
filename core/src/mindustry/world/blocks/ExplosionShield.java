package mindustry.world.blocks;

//TODO: horrible API design, but I'm not sure of a better way to do this right now. please don't use this class
public interface ExplosionShield{
    /** @return whether the shield was able to absorb the explosion; this should apply damage to the shield if true is returned. */
    boolean absorbExplosion(float x, float y, float damage);
}
