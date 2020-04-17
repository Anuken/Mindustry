package mindustry.net;

import mindustry.gen.*;

/**
 * Thrown when a client sends invalid information.
 */
public class ValidateException extends RuntimeException{
    public final Playerc player;

    public ValidateException(Playerc player, String s){
        super(s);
        this.player = player;
    }
}
