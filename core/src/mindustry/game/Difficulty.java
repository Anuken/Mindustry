package mindustry.game;

import arc.*;

public class Difficulty{
    public static Difficulty[] all = {};

    public static final Difficulty
    casual = new Difficulty("casual", 0.5f, 0.5f, 2f).add(),
    easy = new Difficulty("easy", 1f, 0.75f, 1.5f).add(),
    normal = new Difficulty("normal", 1f, 1f, 1f).add(),
    hard = new Difficulty("hard", 1.25f, 1.5f, 0.8f).add(),
    eradication = new Difficulty("eradication", 1.5f, 2f, 0.6f).add();

    //TODO add more fields
    public final String name;
    public float enemyHealthMultiplier, enemySpawnMultiplier, waveTimeMultiplier;

    Difficulty(String name, float enemyHealthMultiplier, float enemySpawnMultiplier, float waveTimeMultiplier){
        this.name = name;
        this.enemySpawnMultiplier = enemySpawnMultiplier;
        this.waveTimeMultiplier = waveTimeMultiplier;
        this.enemyHealthMultiplier = enemyHealthMultiplier;
    }

    public String info(){
        String res =
        (enemyHealthMultiplier == 1f ? "" : Core.bundle.format("difficulty.enemyHealthMultiplier", percentStat(enemyHealthMultiplier)) + "\n") +
        (enemySpawnMultiplier == 1f ? "" : Core.bundle.format("difficulty.enemySpawnMultiplier", percentStat(enemySpawnMultiplier)) + "\n") +
        (waveTimeMultiplier == 1f ? "" : Core.bundle.format("difficulty.waveTimeMultiplier", percentStatNeg(waveTimeMultiplier)) + "\n");

        return res.isEmpty() ? Core.bundle.get("difficulty.nomodifiers") : res;
    }

    public String localized(){
        return Core.bundle.get("difficulty." + name);
    }

    @Override
    public String toString(){
        return name;
    }

    static String percentStat(float val){
        return ((int)(val * 100 - 100) > 0 ? "[negstat]+" : "[stat]") + (int)(val * 100 - 100) + "%[]";
    }

    static String percentStatNeg(float val){
        return ((int)(val * 100 - 100) > 0 ? "[stat]+" : "[negstat]") + (int)(val * 100 - 100) + "%[]";
    }

    Difficulty add(){
        add(this);
        return this;
    }

    public static void add(Difficulty diff){
        var prev = all;
        all = new Difficulty[all.length + 1];

        System.arraycopy(prev, 0, all, 0, prev.length);

        all[prev.length] = diff;
    }
}
