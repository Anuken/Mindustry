package mindustry.maps;

public class MapException extends RuntimeException{
    public final Map map;

    public MapException(Map map, String s){
        super(s);
        this.map = map;
    }
}
