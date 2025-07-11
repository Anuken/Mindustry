package mindustry.ui.dialogs;

import arc.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.input.*;
import arc.scene.ui.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.type.Category;
import mindustry.ui.*;
import mindustry.ui.fragments.PlacementFragment;
import mindustry.world.Block;

import java.util.regex.*;

import static mindustry.Vars.*;

public class BuildingDialog extends BaseDialog{
    private Block firstBuilding;
    private String search = "";
    private TextField searchField;
    private Runnable rebuildPane = () -> {
    };
    private Pattern ignoreSymbols = Pattern.compile("[`~!@#$%^&*()\\-_=+{}|;:'\",<.>/?]");

    public BuildingDialog(){
        super("@buildings");
        Core.assets.load("sprites/schematic-background.png", Texture.class).loaded = t -> t.setWrap(TextureWrap.repeat);

        shouldPause = true;
        addCloseButton();
        makeButtonOverlay();
        shown(this::setup);
        onResize(this::setup);
    }

    void setup(){
        search = "";

        cont.top();
        cont.clear();

        cont.table(s -> {
            s.left();
            s.image(Icon.zoom);
            searchField = s.field(search, res -> {
                search = res;
                rebuildPane.run();
            }).growX().get();
            searchField.setMessageText("@building.search");
            searchField.clicked(KeyCode.mouseRight, () -> {
                if(!search.isEmpty()){
                    search = "";
                    searchField.clearText();
                    rebuildPane.run();
                }
            });
        }).fillX().padBottom(4);

        cont.row();
        cont.pane(t -> {
            t.top();
            t.update(() -> {
                if(Core.input.keyTap(Binding.chat) && Core.scene.getKeyboardFocus() == searchField && firstBuilding != null){
                    control.input.block = firstBuilding;
                    hide();
                }
            });

            rebuildPane = () -> {
                t.clear();
                // Pre-process the search string once
                String cleanSearchString = ignoreSymbols.matcher(search.toLowerCase()).replaceAll("");

                // get all player placeable blocks
                Seq<Block> placeableBlocks = new Seq<>();
                PlacementFragment f = new PlacementFragment();
                for(Category c : Category.all){
                    Seq<Block> blocks = f.getUnlockedByCategory(c);
                    placeableBlocks.addAll(blocks);
                }

                Seq<BlockScore> scoredBlocks = new Seq<>();

                for(Block block : placeableBlocks){
                    String cleanBlockName = ignoreSymbols.matcher(block.localizedName.toLowerCase()).replaceAll("");

                    if (!cleanSearchString.isEmpty()) {
                        // Calculate LCS length
                        int lcs = lcsLength(cleanBlockName, cleanSearchString);

                        // All query characters must be found in order
                        if (lcs == cleanSearchString.length()) {
                            double score = calculateScore(cleanBlockName, cleanSearchString);
                            scoredBlocks.add(new BlockScore(block, score));
                        }
                    }
                }

                // Sort blocks by score in descending order
                scoredBlocks.sort((b1, b2) -> Double.compare(b2.score, b1.score));

                firstBuilding = null;
                int i = 0;
                for(BlockScore bs : scoredBlocks){
                    Block block = bs.block;
                    if(firstBuilding == null) firstBuilding = block; // Set the first building from the sorted list

                    Button[] sel = {null};
                    sel[0] = t.button(b -> {
                        b.top();
                        b.margin(10f);
                        b.row();
                        b.name = block.localizedName;
                        b.stack(new Image(block.uiIcon).setScaling(Scaling.fit)).size(70f).pad(10f);

                        Label label = b.add(block.localizedName).style(Styles.outlineLabel).color(Color.white).top().growX().get();
                        label.setAlignment(Align.left);
                        b.stack(label);
                    }, () -> {
                        if(sel[0].childrenPressed()) return;
                        control.input.block = block;
                        hide();
                    }).pad(4).style(Styles.flati).get();

                    sel[0].getStyle().up = Tex.pane;

                    t.row();
                }

                if(scoredBlocks.isEmpty()){
                    if(!cleanSearchString.isEmpty()){
                        t.add("@none.found");
                    }else{
                        t.add("@none").color(Color.lightGray);
                    }
                }
            };

            rebuildPane.run();
        }).grow().scrollX(false);
    }

    @Override
    public Dialog show(){
        super.show();

        if(Core.app.isDesktop() && searchField != null){
            Core.scene.setKeyboardFocus(searchField);
        }

        return this;
    }

    /**
     * Calculates the length of the Longest Common Subsequence (LCS) between two strings, not necessarily contiguously.
     * @param s1 The first string (e.g., block name).
     * @param s2 The second string (e.g., search query).
     * @return The length of the LCS.
     */
    private int lcsLength(String s1, String s2) {
        if (s1.isEmpty() || s2.isEmpty()) {
            return 0;
        }

        int m = s1.length();
        int n = s2.length();

        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = 1 + dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp[m][n];
    }

    /**
     * Calculates a score based on LCS
     * @param cleanBlockName The pre-processed block name.
     * @param cleanSearchString The pre-processed search query.
     * @return A double representing the score. Higher is better.
     */
    private double calculateScore(String cleanBlockName, String cleanSearchString) {
        // If the search string is empty, all items are equally "relevant"
        if (cleanSearchString.isEmpty()) {
            return 0.0; // They will be sorted alphabetically or by default order later
        }

        double score = 0.0;
        if (cleanBlockName.contains(cleanSearchString)) {
            score += 1000.0; // High bonus for direct substring match
            // Prioritize shorter block names among direct matches (e.g., "router" over "liquid router")
            score += (1.0 / (1.0 + Math.abs(cleanBlockName.length() - cleanSearchString.length()))) * 100;
        } else {
            score -= cleanBlockName.length() * 0.1; // Small penalty for longer block names
        }
        // This ensures longer queries inherently lead to higher scores for matching blocks
        score += lcsLength(cleanBlockName, cleanSearchString);

        return score;
    }

    // Helper class to store Block and its score for sorting
    static class BlockScore {
        Block block;
        double score;

        public BlockScore(Block block, double score) {
            this.block = block;
            this.score = score;
        }
    }
}
