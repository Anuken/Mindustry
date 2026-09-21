package mindustry.world.blocks;

public interface ShieldProvider{
    /** @return whether the shield was able to absorb the explosion; this should apply damage to the shield if true is returned. */
    boolean absorbExplosion(float x, float y, float damage);
    /** @return maximum (not current size!) half-size of the shield square bounding box */
    float getShieldBounds();
}
