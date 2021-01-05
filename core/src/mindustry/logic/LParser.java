package mindustry.logic;

import arc.struct.*;

public class LParser{
    Seq<LStatement> statements = new Seq<>();
    char[] chars;
    int position;

    LParser(String text){
        this.chars = text.toCharArray();
    }

    public static Seq<LStatement> parse(String text){
        return new LParser(text).parse();
    }

    void comment(){
        //read until \n or eof
        while(position < chars.length && chars[position++] != '\n');
    }

    void statement(){
        //read jump
        if(chars[position] == '['){

        }

        while(position < chars.length){
            char c = chars[position ++];

            //reached end of line, bail out.
            if(c == '\n') break;

            if(c == '#'){
                comment();
                break;
            }
        }
    }

    Seq<LStatement> parse(){
        while(position < chars.length){
            statement();
        }
        //TODO
        return statements;
    }

}
