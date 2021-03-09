package mindustry.logic;

import arc.struct.*;

public class LParser{
    Seq<LStatement> statements = new Seq<>();
    char[] chars;
    int pos;

    LParser(String text){
        this.chars = text.toCharArray();
    }

    public static Seq<LStatement> parse(String text){
        return new LParser(text).parse();
    }

    void comment(){
        //read until \n or eof
        while(pos < chars.length && chars[pos++] != '\n');
    }

    void label(){
        while(pos < chars.length){

        }
    }

    void statement(){
        //read jump
        if(chars[pos] == '['){

        }

        while(pos < chars.length){
            char c = chars[pos++];

            //reached end of line, bail out.
            if(c == '\n') break;

            if(c == '#'){
                comment();
                break;
            }
        }
    }

    Seq<LStatement> parse(){
        while(pos < chars.length){
            switch(chars[pos++]){
                case '\n', ' ' -> {} //skip newlines and spaces
                case '\r' -> pos ++; //skip the newline after the \r
                default -> statement();
            }
        }
        return statements;
    }

}
