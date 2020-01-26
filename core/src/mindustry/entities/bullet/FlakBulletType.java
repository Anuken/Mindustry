package mindustry.entities.bullet;

import arc.math.geom.Rect;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.entities.type.Bullet;

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
    public void update(Bullet b){
        super.update(b);
        if(b.getData() instanceof Integer) return;

        if(b.timer.get(2, 6)){
            Units.nearbyEnemies(b.getTeam(), rect.setSize(explodeRange * 2f).setCenter(b.x, b.y), unit -> {
                if(b.getData() instanceof Float) return;

                if(unit.dst(b) < explodeRange){
                    b.setData(0);
                    Time.run(5f, () -> {
                        if(b.getData() instanceof Integer){
                            b.time(b.lifetime());
                        }
                    });
                }
            });
        }
    }
}
