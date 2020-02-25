package mindustry.core;

import arc.*;
import arc.struct.*;
import mindustry.gen.*;

public class Toastr implements ApplicationListener{

    public static Queue<String> queue = new Queue<>();
    private float cooldown;

    @Override
    public void update(){

        if(cooldown-- > 0) return;
        if(queue.isEmpty()) return;

        cooldown = 2.5f * 60;
        Call.onInfoToast(queue.removeFirst(), 2.5f);
    }
}
