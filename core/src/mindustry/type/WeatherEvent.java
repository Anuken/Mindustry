package mindustry.type;

import mindustry.ctype.Content;
import mindustry.ctype.ContentType;

//currently unimplemented, see trello for implementation plans
public class WeatherEvent extends Content{
    public final String name;

    public WeatherEvent(String name){
        this.name = name;
    }

    @Override
    public ContentType getContentType(){
        return ContentType.weather;
    }
}
