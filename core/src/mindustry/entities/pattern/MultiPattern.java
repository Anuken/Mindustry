package mindustry.entities.pattern;

public class MultiPattern extends ShotPattern{
    public ShotPattern source;
    public ShotPattern[] dest = {};

    public MultiPattern(ShotPattern source, ShotPattern... dest){
        this.source = source;
        this.dest = dest;
    }

    public MultiPattern(){
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
