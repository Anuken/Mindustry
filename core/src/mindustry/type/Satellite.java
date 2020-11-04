package mindustry.type;

/** Any object that is orbiting a planet. */
public class Satellite{
    public Planet planet;

    public Satellite(Planet orbiting){
        this.planet = orbiting;
    }
}
