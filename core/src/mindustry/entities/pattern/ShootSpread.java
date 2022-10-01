package mindustry.entities.pattern;

public class ShootSpread extends ShootPattern{
    /** spread between bullets, in degrees. */
    public float spread = 5f;

    public ShootSpread(int shots, float spread){
        this.shots = shots;
        this.spread = spread;
    }

    public ShootSpread(){
    }

    @Override
    public void shoot(int totalShots, BulletHandler handler){
        for(int i = 0; i < shots; i++){
            float angleOffset = i * spread - (shots - 1) * spread / 2f;
            handler.shoot(0, 0, angleOffset, firstShotDelay + shotDelay * i);
        }
    }
}
