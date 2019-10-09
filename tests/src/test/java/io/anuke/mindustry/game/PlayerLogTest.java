package io.anuke.mindustry.game;

import io.anuke.arc.collection.ObjectSet;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.ContentLoader;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.world.Tile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for PlayerLog.
 */
public class PlayerLogTest{
    private static final int TIMESTAMP = 1234;
    private static final String PLAYER_NAME = "Bob";
    private static final String TILE1_NAME = "Steam Gen";
    private static final String TILE2_NAME = "Power Node";
    private static final short IN_XY = 20;
    private static final short OUT_XY = 21;
    private static final int LOG_SIZE = 16;
    private static final String[] SEARCH_ALL = { };
    private static final Tile TILE_1 = new Tile(IN_XY, IN_XY);
    private static final Tile TILE_2 = new Tile(OUT_XY, OUT_XY);;

    private StringBuilder sb;
    private int now;
    private ObjectSet<String> players;
    private Player player;
    private PlayerEvent event;
    private PlayerLog log;

    @BeforeEach
    public void setUp(){
        sb = new StringBuilder();
        now = 3333;
        players = new ObjectSet<>();
        players.add(PLAYER_NAME);
        players.add("Sally");
        players.add("Billy");
        Vars.content = new ContentLoader();
        Vars.content.createContent();
        player = new Player() { };
        event = createEvent(IN_XY);
        log = new PlayerLog(LOG_SIZE, 4);
    }

    private LogFilter createFilter(String... args){
        return new LogFilter(2, args, players, player);
    }

    private PlayerEvent createEvent(short xy){
        return new PlayerEvent(TIMESTAMP, PLAYER_NAME, PlayerLog.Action.linked, TILE1_NAME, xy, xy);
    }

    private PlayerEvent createEvent(short xy1, short xy2){
        return new PlayerEvent(TIMESTAMP, PLAYER_NAME, PlayerLog.Action.linked, TILE1_NAME, xy1, xy1, TILE2_NAME, xy2, xy2);
    }

    @Test
    public void append_Single_Succeeds(){
        assertEquals("00:34:59: (Bob) linked Steam Gen@20,20\n", event.append(sb, now).toString());
    }

    @Test
    public void append_Double_Succeeds(){
        final PlayerEvent event = createEvent(IN_XY, IN_XY);
        assertEquals("00:34:59: (Bob) linked Steam Gen@20,20 Power Node@20,20\n", event.append(sb, now).toString());
    }

    @Test
    public void LogFilter_NoArgs_Succeeds(){
        assertDoesNotThrow(() -> createFilter());
    }

    @Test
    public void LogFilter_InvalidAction_Throws(){
        final Exception e = assertThrows(IllegalArgumentException.class, () -> createFilter("-action"));
        assertEquals("Action must be one of: constructed, deconstructed, linked, unlinked, configured.", e.getMessage());
    }

    @Test
    public void LogFilter_InvalidPage_Throws(){
        final Exception e = assertThrows(IllegalArgumentException.class, () -> createFilter("bob", "-3"));
        assertEquals("Page must be > 0.", e.getMessage());
    }

    @Test
    public void LogFilter_InvalidPlayer_Throws(){
        final Exception e = assertThrows(IllegalArgumentException.class, () -> createFilter("ann"));
        assertEquals("Player ann not found.", e.getMessage());
    }

    @Test
    public void LogFilter_MultiplePlayer_Throws(){
        final Exception e = assertThrows(IllegalArgumentException.class, () -> createFilter("ly"));
        assertEquals("Did you mean: Sally, Billy?", e.getMessage());
    }

    @Test
    public void LogFilter_ExcessAlphaArg_Throws(){
        final Exception e = assertThrows(IllegalArgumentException.class, () -> createFilter("ill", "huh?"));
        assertEquals("Too many arguments.", e.getMessage());
    }

    @Test
    public void LogFilter_ExcessNumberArg_Throws(){
        final Exception e = assertThrows(IllegalArgumentException.class, () -> createFilter("4", "2"));
        assertEquals("Too many arguments.", e.getMessage());
    }

    @Test
    public void matches_Default_True(){
        assertTrue(createFilter().matches(event));
    }

    @Test
    public void matches_OutOfScope_False(){
        assertFalse(createFilter(".").matches(createEvent(OUT_XY)));
    }

    @Test
    public void matches_SecondInScope_True(){
        assertTrue(createFilter(".").matches(createEvent(OUT_XY, IN_XY)));
    }

    @Test
    public void matches_BothOutOfScope_False(){
        assertFalse(createFilter(".").matches(createEvent(OUT_XY, OUT_XY)));
    }

    @Test
    public void matches_Action_False(){
        assertFalse(createFilter("-deconstructed").matches(event));
    }

    @Test
    public void matches_Player_True(){
        assertTrue(createFilter(PLAYER_NAME).matches(event));
    }

    @Test
    public void matches_Player_False(){
        final LogFilter filter = createFilter("sally");
        assertFalse(filter.matches(event));
    }

    @Test
    public void matches_Page1_False(){
        final LogFilter filter = createFilter();
        assertTrue(filter.matches(event));
        assertTrue(filter.matches(event));
        assertFalse(filter.matches(event));
    }

    @Test
    public void matches_Page2_True(){
        final LogFilter filter = createFilter("2");
        assertFalse(filter.matches(event));
        assertFalse(filter.matches(event));
        assertTrue(filter.matches(event));
    }

    @Test
    public void summary_Succeeds(){
        final LogFilter filter = createFilter();
        filter.matches(event);
        filter.matches(event);
        filter.matches(event);
        assertEquals("Showing page 1 of 2\n", filter.summary());
    }

    @Test
    public void PlayerLog_SmallSize_Throws(){
        final Exception e = assertThrows(IllegalArgumentException.class, () -> new PlayerLog(15, 2));
        assertEquals("Size must be at least 16", e.getMessage());
    }

    @Test
    public void PlayerLog_SmallPageSize_Throws(){
        final Exception e = assertThrows(IllegalArgumentException.class, () -> new PlayerLog(16, 17));
        assertEquals("LogSize: 16 < PageSize: 17", e.getMessage());
    }

    @Test
    public void clear_Succeeds(){
        log.record(null, PlayerLog.Action.unlinked, TILE_1, null);
        log.clear();
        assertEquals("No results.", log.search(SEARCH_ALL, player));
    }

    @Test
    public void record_NullPlayerSingleTile_Succeeds(){
        log.record(null, PlayerLog.Action.unlinked, TILE_1, null);
        assertEquals("Showing page 1 of 1\n00:00:00: (<unknown>) unlinked air@20,20\n", log.search(SEARCH_ALL, player));
    }

    @Test
    public void record_PlayerDoubleTile_Succeeds(){
        log.record(player, PlayerLog.Action.unlinked, TILE_1, TILE_2);
        assertEquals("Showing page 1 of 1\n00:00:00: (noname) unlinked air@20,20 air@21,21\n", log.search(SEARCH_ALL, player));
    }

    @Test
    public void record_WrapLog_Succeeds(){
        Time.setDeltaProvider(() -> 270f);
        for(int i = -4; i != LOG_SIZE; ++i){
            log.record(player, PlayerLog.Action.unlinked, TILE_1, TILE_2);
            Time.update();
        }
        assertEquals(
            "Showing page 3 of 4\n" +
            "00:00:41: (noname) unlinked air@20,20 air@21,21\n" +
            "00:00:45: (noname) unlinked air@20,20 air@21,21\n" +
            "00:00:50: (noname) unlinked air@20,20 air@21,21\n" +
            "00:00:54: (noname) unlinked air@20,20 air@21,21\n", log.search(new String[]{"3"}, player));
    }

    @Test
    public void search_Empty_Succeeds(){
        assertEquals("No results.", log.search(SEARCH_ALL, player));
    }

    @Test
    public void search_Error_Succeeds(){
        assertEquals("Player tammy not found.", log.search(new String[]{"tammy"}, player));
    }
}
