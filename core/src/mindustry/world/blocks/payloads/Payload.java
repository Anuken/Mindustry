package mindustry.world.blocks.payloads;

public interface Payload{

    /** draws this payload at a position. */
    void draw(float x, float y, float rotation);

    /** @return whether this payload was dumped. */
    default boolean dump(float x, float y, float rotation){
        return false;
    }
}
