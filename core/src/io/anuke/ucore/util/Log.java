package io.anuke.ucore.util;

public class Log {
    private static boolean useColors = true;
    private static boolean disabled = false;
    private static LogHandler logger = new LogHandler();

    public static void setLogger(LogHandler log){
        logger = log;
    }

    public static void setDisabled(boolean disabled){
        Log.disabled = disabled;
    }

    public static void setUseColors(boolean colors){
        useColors = colors;
    }

    public static void info(String text, Object... args){
        if(disabled) return;
        logger.info(text, args);
    }

    public static void err(String text, Object... args){
        if(disabled) return;
        logger.err(text, args);
    }

    public static void err(Throwable th){
        if(disabled) return;
        logger.err(th);
    }

    public static void print(String text, Object... args){
        if(disabled) return;
        logger.print(text, args);
    }

    public static String format(String text, Object... args){

        for(int i = 0; i < args.length; i ++){
            text = text.replace("{" + i + "}", String.valueOf(args[i]));
        }

        if(useColors) {
            for (String color : ColorCodes.getColorCodes()) {
                text = text.replace("&" + color, ColorCodes.getColorText(color));
            }
        }else{
            for (String color : ColorCodes.getColorCodes()) {
                text = text.replace("&" + color,  "");
            }
        }
        return text;
    }

    public static class LogHandler{

        public void info(String text, Object... args){
            print("&lg&fb" + format(text, args));
        }

        public void err(String text, Object... args){
            print("&lr&fb" + format(text, args));
        }

        public void err(Throwable e){
            if(useColors) System.out.print(ColorCodes.LIGHT_RED + ColorCodes.BOLD);
            e.printStackTrace();
            if(useColors) System.out.print(ColorCodes.RESET);
        }

        public void print(String text, Object... args){
            System.out.println(format(text + "&fr", args));
        }
    }

}
