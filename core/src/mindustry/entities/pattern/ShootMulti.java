package mindustry.entities.pattern;

public class ShootMulti extends ShootPattern{
    public ShootPattern source;
    public ShootPattern[] dest = {};

    public ShootMulti(ShootPattern source, ShootPattern... dest){
        this.source = source;
        this.dest = dest;
    }

    public ShootMulti(){
    }

    @Override
    public void shoot(int totalShots, BulletHandler handler){
        source.shoot(totalShots, (x, y, rotation, delay) -> {
            for(var pattern : dest){
                pattern.shoot(totalShots, (x2, y2, rot2, delay2) -> {
                    handler.shoot(x + x2, y + y2, rotation + rot2, delay + delay2);
                });
            }
        });
    }
}
