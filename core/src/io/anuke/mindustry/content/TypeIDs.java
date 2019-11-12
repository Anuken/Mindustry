package io.anuke.mindustry.content;

import io.anuke.mindustry.entities.effect.Fire;
import io.anuke.mindustry.entities.effect.Puddle;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.ctype.ContentList;
import io.anuke.mindustry.type.TypeID;

public class TypeIDs implements ContentList{
    public static TypeID fire, puddle, player;

    @Override
    public void load(){
        fire = new TypeID("fire", Fire::new);
        puddle = new TypeID("puddle", Puddle::new);
        player = new TypeID("player", Player::new);
    }
}
