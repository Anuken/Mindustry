package mindustry.ai.ai.utils;


/**
 * A {@code RaycastCollisionDetector} finds the closest intersection between a ray and any object in the game world.
 * @author davebaol
 */
public interface RaycastCollisionDetector{

    /**
     * Casts the given ray to test if it collides with any objects in the game world.
     * @param ray the ray to cast.
     * @return {@code true} in case of collision; {@code false} otherwise.
     */
    boolean collides(Ray ray);

    /**
     * Find the closest collision between the given input ray and the objects in the game world. In case of collision,
     * {@code outputCollision} will contain the collision point and the normal vector of the obstacle at the point of collision.
     * @param outputCollision the output collision.
     * @param inputRay the ray to cast.
     * @return {@code true} in case of collision; {@code false} otherwise.
     */
    boolean findCollision(Collision outputCollision, Ray inputRay);
}
