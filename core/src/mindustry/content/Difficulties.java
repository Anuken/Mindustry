package mindustry.content;

import java.util.List;
import java.util.ArrayList;

import mindustry.game.Difficulty;

public class Difficulties {

    public static Difficulty casual, easy, normal, hard, eradication;

    private static List<Difficulty> difficulties;

    public static void load(){
        difficulties = new ArrayList<>();
        //TODO these need tweaks    
        casual = new Difficulty(0.75f, 0.5f, 2f, "casual");
        easy = new Difficulty(1f, 0.75f, 1.5f, "easy");
        normal = new Difficulty(1f, 1f, 1f, "normal");
        hard = new Difficulty(1.25f, 1.5f, 0.8f, "hard");
        eradication = new Difficulty(1.5f, 2f, 0.6f, "eradication");
    }

    public static void addDifficulties(Difficulty...difficulties){
        for(Difficulty difficulty : difficulties){
            Difficulties.difficulties.add(difficulty);
        }

    }

    public static List<Difficulty> getDifficulties(){
        return Difficulties.difficulties;
    }
}
