package arc.scene.flabel;

import arc.func.*;
import arc.struct.*;
import arc.util.*;

import java.util.regex.*;

/** Utility class to parse tokens from a {@link FLabel}. */
class FParser{
    private static String resetReplacement;
    // Handles separating the parameters from the effect name
    static final Pattern parameterParser = Pattern.compile("(\\w+)(?:=([;:?=^_ #-'*-..\\w]+))?", Pattern.CASE_INSENSITIVE);

    /** Parses all tokens from the given {@link FLabel}. */
    static void parseTokens(FLabel label){
        // Compile patterns if necessary
        if(resetReplacement == null || FConfig.dirtyEffectMaps){
            resetReplacement = getResetReplacement();
            FConfig.dirtyEffectMaps = false;
        }

        // Adjust and check markup color
        if(label.forceMarkupColor) label.getFontCache().getFont().getData().markupEnabled = true;

        // Remove any previous entries
        label.tokenEntries.clear();

        // Parse all tokens with text replacements, namely color and var.
        parseReplacements(label);

        //wait / event / speed / effect start / effect end

        // Parse all regular tokens and properly register them
        parseRegularTokens(label);

        //remove everything

        // Parse color markups and register SKIP tokens
        stripTokens(label);

        // Sort token entries
        label.tokenEntries.sort();
        label.tokenEntries.reverse();
    }

    private static void baseParse(FLabel label, TokenHandler replacer){
        StringBuilder text = label.getText();
        StringBuilder result = new StringBuilder();
        result.ensureCapacity(text.length());

        int[] lastIndex = {0};
        int[] afterIndex = {0};

        //TODO this is broken with nested tokens, e.g. {[red]death}
        parseAllTokens(label, false, (from, to) -> {
            String replacement = text.charAt(from - 1) == '{' ? replacer.handle(text.substring(from, to), from + afterIndex[0]) : "[" + text.substring(from, to) + "]";
            afterIndex[0] -= (to - from + 2);

            //append prev text
            result.append(text.subSequence(lastIndex[0], from - 1));

            if(replacement == null){
                //no variable or text with this name, just append everything
                result.append("{").append(text.subSequence(from, to)).append("}");
            }else{
                //otherwise append the replaced text
                result.append(replacement);
            }

            lastIndex[0] = to + 1;
        });

        //append remaining text
        result.append(text.subSequence(lastIndex[0], text.length()));

        //update label text
        label.setText(result);
    }

    private static void parseReplacements(FLabel label){
        baseParse(label, (text, index) -> {
            String replacement = null;

            if(text.length() > 1 && text.charAt(1) == '$'){ //variable
                String varname = text.substring(1);
                if(label.getTypingListener() != null){
                    replacement = label.getTypingListener().replaceVariable(varname);
                }

                // If replacement is null, get value from maps.
                if(replacement == null){
                    replacement = label.getVariables().get(varname);
                }

                // If replacement is still null, get value from global scope
                if(replacement == null){
                    replacement = FConfig.globalVars.get(varname);
                }
            }else if(text.equals("/color")){ //end color
                replacement = "[#" + label.getClearColor().toString() + "]";
            }else if(text.equals("reset")){ //reset
                replacement = resetReplacement + label.getDefaultToken();
            }

            return replacement;
        });
    }

    private static void parseRegularTokens(FLabel label){
        baseParse(label, (text, index) -> {
            float floatValue = 0;
            String stringValue = null;
            FEffect effect = null;
            int indexOffset = 0;

            Matcher params = parameterParser.matcher(text);
            String paramsString = params.find() ? params.group(2) : null;
            boolean hasParams = paramsString != null;
            String textNoParams = hasParams ? text.substring(0, text.indexOf('=')) : text;

            TokenCategory tokenCategory = TokenCategory.event;
            InternalToken tmpToken = InternalToken.fromName(textNoParams);
            if(tmpToken == null){
                if(FConfig.effects.containsKey(textNoParams)){
                    tokenCategory = TokenCategory.effectStart;
                }else if(!textNoParams.isEmpty() && FConfig.effects.containsKey(textNoParams.substring(1))){
                    tokenCategory = TokenCategory.effectEnd;
                }
            }else{
                tokenCategory = tmpToken.category;
            }

            switch(tokenCategory){
                case wait:
                    floatValue = hasParams ? Strings.parseFloat(paramsString.split(";")[0], FConfig.defaultWaitValue) : FConfig.defaultWaitValue;
                    break;
                case event:
                    //use the entire parameter list as a string
                    //if there isn't any params, use the raw token text (usually {event})
                    stringValue = hasParams ? paramsString : text;
                    indexOffset = -1;
                    break;
                case speed:
                    switch(textNoParams){
                        case "speed":
                            floatValue = FConfig.defaultSpeedPerChar / (hasParams ? Strings.parseFloat(paramsString.split(";")[0], 1f) : 1f);
                            break;
                        case "slower":
                            floatValue = FConfig.defaultSpeedPerChar / 0.500f;
                            break;
                        case "slow":
                            floatValue = FConfig.defaultSpeedPerChar / 0.667f;
                            break;
                        case "normal":
                            floatValue = FConfig.defaultSpeedPerChar;
                            break;
                        case "fast":
                            floatValue = FConfig.defaultSpeedPerChar / 2.000f;
                            break;
                        case "faster":
                            floatValue = FConfig.defaultSpeedPerChar / 4.000f;
                            break;
                    }
                    break;
                case effectStart:
                    effect = FConfig.effects.get(textNoParams).get();
                    try{
                        if(paramsString != null) effect.applyParams(paramsString.split(";"));
                    }catch(Exception e){
                        //if parsing fails for a parameter, stop parsing entirely
                        //any parameters successfully parsed beforehand will stay
                    }
                    effect.endToken = "/" + textNoParams;
                    break;
                case effectEnd:
                    break;
            }

            TokenEntry entry = new TokenEntry(textNoParams, tokenCategory, index + indexOffset - 1, floatValue, stringValue);
            entry.effect = effect;
            label.tokenEntries.add(entry);

            return "{" + text + "}";
        });
    }

    private static void parseAllTokens(FLabel label, boolean square, Intc2 handler){
        StringBuilder text = label.getText();

        for(int i = 0; i < text.length(); i++){
            char c = text.charAt(i);
            if(c == '\\'){
                //escaped token, skip and continue
                i ++;
                continue;
            }

            char end = (c == '[' ? ']' : c == '{' ? '}' : '_');
            if(end != '_'){
                for(int j = i + 1; j < text.length(); j++){
                    //nested tokens, do not parse
                    if(text.charAt(j) == c){
                        break;
                    }else if(text.charAt(j) == end){
                        //found token end!
                        handler.get(i + 1, j);
                        i = j;
                        break;
                    }
                }
            }
        }
    }

    private static void stripTokens(FLabel label){
        baseParse(label, (text, index) -> "");

        //must be a square token
        parseAllTokens(label, true, (from, to) -> {});
    }

    /** Returns the replacement string intended to be used on {RESET} tokens. */
    private static String getResetReplacement(){
        Seq<String> tokens = new Seq<>();
        FConfig.effects.keys().toSeq(tokens);
        tokens.replace(m -> "/" + m);
        tokens.add("clear");
        tokens.add("normal");

        StringBuilder sb = new StringBuilder();
        for(String token : tokens){
            sb.append("{").append(token).append('}');
        }
        return sb.toString();
    }

    private interface TokenHandler{
        String handle(String string, int position);
    }

    enum InternalToken{
        wait(TokenCategory.wait),
        speed(TokenCategory.speed),
        slower(TokenCategory.speed),
        slow(TokenCategory.speed),
        normal(TokenCategory.speed),
        fast(TokenCategory.speed),
        faster(TokenCategory.speed),
        color(TokenCategory.color),
        clearcolor(TokenCategory.color),
        endcolor(TokenCategory.color),
        var(TokenCategory.variable),
        event(TokenCategory.event),
        reset(TokenCategory.reset),
        skip(TokenCategory.skip);

        final String name;
        final TokenCategory category;

        static final InternalToken[] all = values();

        InternalToken(TokenCategory category){
            this.name = name();
            this.category = category;
        }

        @Override
        public String toString(){
            return name;
        }

        static InternalToken fromName(String name){
            if(name != null){
                for(InternalToken token : all){
                    if(name.equalsIgnoreCase(token.name)){
                        return token;
                    }
                }
            }
            return null;
        }
    }

    public enum TokenCategory{
        wait,
        speed,
        color,
        variable,
        event,
        reset,
        skip,
        effectStart,
        effectEnd
    }

    /** Container representing a token, parsed parameters and its position in text. */
    static class TokenEntry implements Comparable<TokenEntry>{
        String token;
        TokenCategory category;
        int index;
        float floatValue;
        String stringValue;
        FEffect effect;

        TokenEntry(String token, TokenCategory category, int index, float floatValue, String stringValue){
            this.token = token;
            this.category = category;
            this.index = index;
            this.floatValue = floatValue;
            this.stringValue = stringValue;
        }

        @Override
        public int compareTo(TokenEntry o){
            return Integer.compare(index, o.index);
        }

    }
}
