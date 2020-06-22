package mindustry.content;

import mindustry.entities.effect.Fire;
import mindustry.entities.effect.Puddle;
import mindustry.entities.type.Player;
import mindustry.ctype.ContentList;
import mindustry.type.TypeID;

public class TypeIDs implements ContentList{
    public static TypeID fire, puddle, player;

    @Override
    public void load(){
        fire = new TypeID("fire", Fire::new);
        puddle = new TypeID("puddle", Puddle::new);
        player = new TypeID("player", Player::new);
    }
}
