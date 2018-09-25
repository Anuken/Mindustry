package io.anuke.mindustry.maps.missions;

import io.anuke.mindustry.Vars;

/**A mission that just displays some text.*/
public class MessageMission extends ActionMission{

    public MessageMission(String text){
        super(() -> {
            if(!Vars.headless){
                Vars.ui.showInfo(text);
            }
        });
    }
}
