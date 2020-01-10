package mindustry.net;

import arc.*;
import arc.struct.*;
import mindustry.entities.type.*;
import mindustry.game.EventType.*;

public class Ascension{
    private Queue<Player> ascendants = new Queue<>();
    private ObjectSet<Player> suplicants = new ObjectSet<>();

    public Ascension(){
        Events.on(PlayerJoin.class, event -> ascendants.addLast(event.player));

        Events.on(PlayerLeave.class, event -> {
            ascendants.remove(event.player);
            if(suplicants.contains(event.player)) suplicants.remove(event.player);
        });
    }

    public boolean highlord(Player player){
        if(ascendants.isEmpty()) return false;

        return ascendants.first() == player || suplicants.contains(player);
    }

    public void declare(Player player){
        if (!suplicants.contains(player)) suplicants.add(player);
    }
}

