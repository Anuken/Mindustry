package mindustry.world.blocks.logic;

public abstract class AnalyzeMode{
    public AnalyzeMode(){
    }

    public static int mode(int analyzemode){
        return (int)((long)(analyzemode >>> 0) & 65535L);
    }

    public static int mode(int analyzemode, int value){
        return (int)((long)analyzemode & 65535L | (long)(value << 0));
    }

    public static int selection(int analyzemode){
        return (int)((long)(analyzemode >>> 16) & 65535L);
    }

    public static int selection(int analyzemode, int value){
        return (int)((long)analyzemode & 4294901760L | (long)(value << 16));
    }

    public static int get(int mode, int selection){
        return (int)((long)(mode << 0) & 65535L | (long)(selection << 16) & 4294901760L);
    }
}