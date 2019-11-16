package io.anuke.mindustry.content;

import io.anuke.mindustry.ctype.*;
import io.anuke.mindustry.type.*;

public class Planets implements ContentList{
    //TODO make all names
    public static Planet starter;

    @Override
    public void load(){
        starter = new Planet("//TODO"){{

        }};
    }
}
