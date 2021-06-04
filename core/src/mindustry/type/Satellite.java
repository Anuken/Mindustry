package mindustry.type;

//TODO
/** Any object that is orbiting a planet. */
public class Satellite{
    public transient Planet planet;

    public Satellite(Planet orbiting){
        this.planet = orbiting;
    }
}
