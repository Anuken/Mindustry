package mindustry.core;

import arc.*;
import arc.struct.*;
import mindustry.gen.*;

public class Toastr implements ApplicationListener{

    public static Queue<String> queue = new Queue<>();
    private float cooldown, duration = 4f;

    @Override
    public void update(){

        if(cooldown-- > 0) return;
        if(queue.isEmpty()) return;

        cooldown = duration * 60;
        Call.onInfoToast(queue.removeFirst(), duration);
    }
}
