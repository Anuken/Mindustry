package mindustry.logic;

import arc.struct.*;

public class LParser{
    char[] chars;
    int position;

    LParser(String text){
        this.chars = text.toCharArray();
    }

    public static Seq<LStatement> parse(String text){
        return new LParser(text).parse();
    }

    Seq<LStatement> parse(){
        Seq<LStatement> statements = new Seq<>();
        //TODO
        return statements;
    }

}
