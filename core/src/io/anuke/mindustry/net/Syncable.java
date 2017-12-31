package io.anuke.mindustry.net;

import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.entities.Player;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.util.Mathf;

public interface Syncable {

    public Interpolator<?> getInterpolator();

    public abstract class SyncType<T extends Entity>{
        public abstract float[] write(T entity);
        public abstract void read(T entity, float[] data);
        public abstract void update(T entity, Interpolator interpolator);

        public static final SyncType<Player> player = new SyncType<Player>() {
            @Override
            public float[] write(Player entity) {
                return new float[]{entity.x, entity.y, entity.angle, entity.health};
            }

            @Override
            public void read(Player entity, float[] data) {
                entity.getInterpolator().target.set(data[0], data[1]);
                entity.getInterpolator().targetrot = data[2];
                entity.health = (int)data[3];
            }

            @Override
            public void update(Player entity, Interpolator interpolator) {
                Interpolator i = entity.getInterpolator();
                if(i.target.dst(entity.x, entity.y) > 16){
                    entity.set(i.target.x, i.target.y);
                }

                entity.x = Mathf.lerpDelta(entity.x, i.target.x, 0.4f);
                entity.y = Mathf.lerpDelta(entity.y, i.target.y, 0.4f);
                entity.angle = Mathf.lerpAngDelta(entity.angle, i.targetrot, 0.6f);
            }
        };
    }

    public static class Interpolator<T extends Entity> {
        public SyncType<T> type;
        public Vector2 target = new Vector2();
        public float targetrot;

        public Interpolator(SyncType<T> type){
            this.type = type;
        }

        public void update(T entity){
            this.type.update(entity, this);
        }
    }
}
