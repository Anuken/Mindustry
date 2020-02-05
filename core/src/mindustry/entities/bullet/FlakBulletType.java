package mindustry.entities.bullet;

import arc.math.geom.Rect;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.gen.*;

public class FlakBulletType extends BasicBulletType{
    protected static Rect rect = new Rect();
    protected float explodeRange = 30f;

    public FlakBulletType(float speed, float damage){
        super(speed, damage, "shell");
        splashDamage = 15f;
        splashDamageRadius = 34f;
        hitEffect = Fx.flakExplosionBig;
        bulletWidth = 8f;
        bulletHeight = 10f;
    }

    public FlakBulletType(){
        this(1f, 1f);
    }

    @Override
    public void update(Bulletc b){
        super.update(b);
        if(b.data() instanceof Integer) return;

        if(b.timer(2, 6)){
            Units.nearbyEnemies(b.team(), rect.setSize(explodeRange * 2f).setCenter(b.x(), b.y()), unit -> {
                if(b.data() instanceof Float) return;

                if(unit.dst(b) < explodeRange){
                    b.data(0);
                    Time.run(5f, () -> {
                        if(b.data() instanceof Integer){
                            b.time(b.lifetime());
                        }
                    });
                }
            });
        }
    }
}
