package mindustry.server;

import arc.*;
import arc.files.*;
import arc.util.*;
import mindustry.core.GameState.*;
import mindustry.io.*;

import static mindustry.Vars.*;

public class Autosave implements ApplicationListener{
    private Interval timer = new Interval();

    @Override
    public void update(){
        if(timer.get(60f * 60f)){
            if(state.is(State.playing)){
                Fi file = saveDirectory.child("autosave" + "." + saveExtension);

                Core.app.post(() -> {
                    SaveIO.save(file);
                });
            }
        }
    }
}
