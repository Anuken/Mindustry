package arc.graphics.g2d;

import arc.graphics.font.*;
import arc.struct.Seq;
import arc.struct.FloatSeq;
import arc.graphics.Color;
import arc.graphics.Colors;
import arc.graphics.font.Font.FontData;
import arc.graphics.font.Font.Glyph;
import arc.util.Align;
import arc.util.pooling.Pool;
import arc.util.pooling.Pool.Poolable;
import arc.util.pooling.Pools;

/**
 * Stores {@link GlyphRun runs} of glyphs for a piece of text. The text may contain newlines and color markup tags.
 * @author Nathan Sweet
 * @author davebaol
 * @author Alexander Dorokhov
 */
public class GlyphLayout implements Poolable{
    public final Seq<GlyphRun> runs = new Seq<>();
    private final Seq<Color> colorStack = new Seq<>(4);
    public boolean ignoreMarkup = false;
    public float width, height;

    /** Creates an empty GlyphLayout. */
    public GlyphLayout(){
    }

    public GlyphLayout(boolean ignoreMarkup){
        this.ignoreMarkup = ignoreMarkup;
    }

    public GlyphLayout(Font font, CharSequence str){
        setText(font, str);
    }

    public GlyphLayout(Font font, CharSequence str, Color color, float targetWidth, int halign, boolean wrap){
        setText(font, str, color, targetWidth, halign, wrap);
    }

    public GlyphLayout(Font font, CharSequence str, int start, int end, Color color, float targetWidth, int halign,
                       boolean wrap, String truncate){
        setText(font, str, start, end, color, targetWidth, halign, wrap, truncate);
    }

    public static GlyphLayout obtain(){
        return Pools.obtain(GlyphLayout.class, GlyphLayout::new);
    }

    public void free(){
        Pools.free(this);
    }

    public void setText(Font font, CharSequence str){
        setText(font, str, 0, str.length(), font.getColor(), 0, Align.left, false, null);
    }

    public void setText(Font font, CharSequence str, Color color, float targetWidth, int halign, boolean wrap){
        setText(font, str, 0, str.length(), color, targetWidth, halign, wrap, null);
    }

    /**
     * @param color The default color to use for the text (the BitmapFont {@link Font#getColor() color} is not used). If
     * {@link FontData#markupEnabled} is true, color markup tags in the specified string may change the color for
     * portions of the text.
     * @param halign Horizontal alignment of the text, see {@link Align}.
     * @param targetWidth The width used for alignment, line wrapping, and truncation. May be zero if those features are not used.
     * @param truncate If not null and the width of the glyphs exceed targetWidth, the glyphs are truncated and the glyphs for the
     * specified truncate string are placed at the end. Empty string can be used to truncate without adding glyphs.
     * Truncate should not be used with text that contains multiple lines. Wrap is ignored if truncate is not null.
     */
    public void setText(Font font, CharSequence str, int start, int end, Color color, float targetWidth, int halign,
                        boolean wrap, String truncate){

        FontData fontData = font.data;

        if(truncate != null)
            wrap = true; // Causes truncate code to run, doesn't actually cause wrapping.
        else if(targetWidth <= fontData.spaceXadvance * 3) //
            wrap = false; // Avoid one line per character, which is very inefficient.

        boolean markupEnabled = fontData.markupEnabled && !ignoreMarkup;

        Pool<GlyphRun> glyphRunPool = Pools.get(GlyphRun.class, GlyphRun::new);
        Seq<GlyphRun> runs = this.runs;
        glyphRunPool.freeAll(runs);
        runs.clear();

        float x = 0, y = 0, width = 0;
        int lines = 0, blankLines = 0;
        Glyph lastGlyph = null;

        Seq<Color> colorStack = this.colorStack;
        Color nextColor = color;
        colorStack.add(color);
        Pool<Color> colorPool = Pools.get(Color.class, Color::new);

        int runStart = start;
        outer:
        while(true){
            // Each run is delimited by newline or left square bracket.
            int runEnd = -1;
            boolean newline = false;
            if(start == end){
                if(runStart == end) break; // End of string with no run to process, we're done.
                runEnd = end; // End of string, process last run.
            }else{
                switch(str.charAt(start++)){
                    case '\n':
                        // End of line.
                        runEnd = start - 1;
                        newline = true;
                        break;
                    case '[':
                        // Possible color tag.
                        if(markupEnabled){
                            int length = parseColorMarkup(str, start, end, colorPool);
                            if(length >= 0){
                                runEnd = start - 1;
                                start += length + 1;
                                nextColor = colorStack.peek();
                            }else if(length == -2){
                                start++; // Skip first of "[[" escape sequence.
                                continue outer;
                            }
                        }
                        break;
                }
            }

            if(runEnd != -1){
                runEnded:
                if(runEnd != runStart){ // Eg, when a color tag is at text start or a line is "\n".
                    // Store the run that has ended.
                    GlyphRun run = glyphRunPool.obtain();
                    run.color.set(color);
                    boolean had = fontData.markupEnabled;
                    if(ignoreMarkup) fontData.markupEnabled = false;
                    fontData.getGlyphs(run, str, runStart, runEnd, lastGlyph);
                    fontData.markupEnabled = had;

                    if(run.glyphs.size == 0){
                        glyphRunPool.free(run);
                        break runEnded;
                    }
                    if(lastGlyph != null){ // Move back the width of the last glyph from the previous run.
                        x -= lastGlyph.fixedWidth ? lastGlyph.xadvance * fontData.scaleX
                        : (lastGlyph.width + lastGlyph.xoffset) * fontData.scaleX - fontData.padRight;
                    }
                    lastGlyph = run.glyphs.peek();
                    run.x = x;
                    run.y = y;
                    if(newline || runEnd == end) adjustLastGlyph(fontData, run);
                    runs.add(run);

                    float[] xAdvances = run.xAdvances.items;
                    int n = run.xAdvances.size;
                    if(!wrap){ // No wrap or truncate.
                        float runWidth = 0;
                        for(int i = 0; i < n; i++)
                            runWidth += xAdvances[i];
                        x += runWidth;
                        run.width = runWidth;
                        break runEnded;
                    }

                    // Wrap or truncate.
                    x += xAdvances[0];
                    run.width = xAdvances[0];
                    if(n < 1) break runEnded;
                    x += xAdvances[1];
                    run.width += xAdvances[1];
                    for(int i = 2; i < n; i++){
                        Glyph glyph = run.glyphs.get(i - 1);
                        float glyphWidth = (glyph.width + glyph.xoffset) * fontData.scaleX - fontData.padRight;
                        if(x + glyphWidth <= targetWidth){
                            // Glyph fits.
                            x += xAdvances[i];
                            run.width += xAdvances[i];
                            continue;
                        }

                        if(truncate != null){
                            // Truncate.
                            truncate(fontData, run, targetWidth, truncate, i, glyphRunPool);
                            x = run.x + run.width;
                            break outer;
                        }

                        // Wrap.
                        int wrapIndex = fontData.getWrapIndex(run.glyphs, i);
                        if((run.x == 0 && wrapIndex == 0) // Require at least one glyph per line.
                        || wrapIndex >= run.glyphs.size){ // Wrap at least the glyph that didn't fit.
                            wrapIndex = i - 1;
                        }
                        GlyphRun next;
                        if(wrapIndex == 0){ // Move entire run to next line.
                            next = run;
                            run.width = 0;

                            // Remove leading whitespace.
                            for(int glyphCount = run.glyphs.size; wrapIndex < glyphCount; wrapIndex++)
                                if(!fontData.isWhitespace((char)run.glyphs.get(wrapIndex).id)) break;
                            if(wrapIndex > 0){
                                run.glyphs.removeRange(0, wrapIndex - 1);
                                run.xAdvances.removeRange(1, wrapIndex);
                            }
                            run.xAdvances.set(0, -run.glyphs.first().xoffset * fontData.scaleX - fontData.padLeft);

                            if(runs.size > 1){ // Previous run is now at the end of a line.
                                // Remove trailing whitespace and adjust last glyph.
                                GlyphRun previous = runs.get(runs.size - 2);
                                int lastIndex = previous.glyphs.size - 1;
                                for(; lastIndex > 0; lastIndex--){
                                    Glyph g = previous.glyphs.get(lastIndex);
                                    if(!fontData.isWhitespace((char)g.id)) break;
                                    previous.width -= previous.xAdvances.get(lastIndex + 1);
                                }
                                previous.glyphs.truncate(lastIndex + 1);
                                previous.xAdvances.truncate(lastIndex + 2);
                                adjustLastGlyph(fontData, previous);
                                width = Math.max(width, previous.x + previous.width);
                            }
                        }else{
                            next = wrap(fontData, run, glyphRunPool, wrapIndex, i);
                            width = Math.max(width, run.x + run.width);
                            if(next == null){ // All wrapped glyphs were whitespace.
                                x = 0;
                                y += fontData.down;
                                lines++;
                                lastGlyph = null;
                                break;
                            }
                            runs.add(next);
                        }

                        // Start the loop over with the new run on the next line.
                        n = next.xAdvances.size;
                        xAdvances = next.xAdvances.items;
                        x = xAdvances[0];
                        if(n > 1) x += xAdvances[1];
                        next.width += x;
                        y += fontData.down;
                        lines++;
                        next.x = 0;
                        next.y = y;
                        i = 1;
                        run = next;
                        lastGlyph = null;
                    }
                }

                if(newline){
                    // Next run will be on the next line.
                    width = Math.max(width, x);
                    x = 0;
                    float down = fontData.down;
                    if(runEnd == runStart){ // Blank line.
                        down *= fontData.blankLineScale;
                        blankLines++;
                    }else
                        lines++;
                    y += down;
                    lastGlyph = null;
                }

                runStart = start;
                color = nextColor;
            }
        }
        width = Math.max(width, x);

        for(int i = 1, n = colorStack.size; i < n; i++)
            colorPool.free(colorStack.get(i));
        colorStack.clear();

        // Align runs to center or right of targetWidth.
        if((halign & Align.left) == 0){ // Not left aligned, so must be center or right aligned.
            boolean center = (halign & Align.center) != 0;
            float lineWidth = 0, lineY = Integer.MIN_VALUE;
            int lineStart = 0, n = runs.size;
            for(int i = 0; i < n; i++){
                GlyphRun run = runs.get(i);
                if(run.y != lineY){
                    lineY = run.y;
                    float shift = targetWidth - lineWidth;
                    if(center) shift /= 2;
                    while(lineStart < i)
                        runs.get(lineStart++).x += shift;
                    lineWidth = 0;
                }
                lineWidth = Math.max(lineWidth, run.x + run.width);
            }
            float shift = targetWidth - lineWidth;
            if(center) shift /= 2;
            while(lineStart < n)
                runs.get(lineStart++).x += shift;
        }

        this.width = width;
        this.height = fontData.capHeight - lines * fontData.down - blankLines * fontData.down * fontData.blankLineScale;
    }

    /** @param truncate May be empty string. */
    private void truncate(FontData fontData, GlyphRun run, float targetWidth, String truncate, int widthIndex,
                          Pool<GlyphRun> glyphRunPool){

        // Determine truncate string size.
        GlyphRun truncateRun = glyphRunPool.obtain();
        boolean had = fontData.markupEnabled;
        if(ignoreMarkup) fontData.markupEnabled = false;
        fontData.getGlyphs(truncateRun, truncate, 0, truncate.length(), null);
        fontData.markupEnabled = had;
        float truncateWidth = 0;
        if(truncateRun.xAdvances.size > 0){
            adjustLastGlyph(fontData, truncateRun);
            for(int i = 1, n = truncateRun.xAdvances.size; i < n; i++) // Skip first for tight bounds.
                truncateWidth += truncateRun.xAdvances.get(i);
        }
        targetWidth -= truncateWidth;

        // Determine visible glyphs.
        int count = 0;
        float width = run.x;
        while(count < run.xAdvances.size){
            float xAdvance = run.xAdvances.get(count);
            width += xAdvance;
            if(width > targetWidth){
                run.width = width - run.x - xAdvance;
                break;
            }
            count++;
        }

        if(count > 1){
            // Some run glyphs fit, append truncate glyphs.
            run.glyphs.truncate(count - 1);
            run.xAdvances.truncate(count);
            adjustLastGlyph(fontData, run);
            if(truncateRun.xAdvances.size > 0)
                run.xAdvances.addAll(truncateRun.xAdvances, 1, truncateRun.xAdvances.size - 1);
        }else{
            // No run glyphs fit, use only truncate glyphs.
            run.glyphs.clear();
            run.xAdvances.clear();
            run.xAdvances.addAll(truncateRun.xAdvances);
            if(truncateRun.xAdvances.size > 0) run.width += truncateRun.xAdvances.get(0);
        }
        run.glyphs.addAll(truncateRun.glyphs);
        run.width += truncateWidth;

        glyphRunPool.free(truncateRun);
    }

    /**
     * Breaks a run into two runs at the specified wrapIndex.
     * @return May be null if second run is all whitespace.
     */
    private GlyphRun wrap(FontData fontData, GlyphRun first, Pool<GlyphRun> glyphRunPool, int wrapIndex, int widthIndex){
        Seq<Glyph> glyphs2 = first.glyphs; // Starts with all the glyphs.
        int glyphCount = first.glyphs.size;
        FloatSeq xAdvances2 = first.xAdvances; // Starts with all the xAdvances.

        // Skip whitespace before the wrap index.
        int firstEnd = wrapIndex;
        for(; firstEnd > 0; firstEnd--)
            if(!fontData.isWhitespace((char)glyphs2.get(firstEnd - 1).id)) break;

        // Skip whitespace after the wrap index.
        int secondStart = wrapIndex;
        for(; secondStart < glyphCount; secondStart++)
            if(!fontData.isWhitespace((char)glyphs2.get(secondStart).id)) break;

        // Increase first run width up to the end index.
        while(widthIndex < firstEnd)
            first.width += xAdvances2.get(widthIndex++);

        // Reduce first run width by the wrapped glyphs that have contributed to the width.
        for(int n = firstEnd + 1; widthIndex > n; )
            first.width -= xAdvances2.get(--widthIndex);

        // Copy wrapped glyphs and xAdvances to second run.
        // The second run will contain the remaining glyph data, so swap instances rather than copying.
        GlyphRun second = null;
        if(secondStart < glyphCount){
            second = glyphRunPool.obtain();
            second.color.set(first.color);

            Seq<Glyph> glyphs1 = second.glyphs; // Starts empty.
            glyphs1.addAll(glyphs2, 0, firstEnd);
            glyphs2.removeRange(0, secondStart - 1);
            first.glyphs = glyphs1;
            second.glyphs = glyphs2;

            FloatSeq xAdvances1 = second.xAdvances; // Starts empty.
            xAdvances1.addAll(xAdvances2, 0, firstEnd + 1);
            xAdvances2.removeRange(1, secondStart); // Leave first entry to be overwritten by next line.
            xAdvances2.set(0, -glyphs2.first().xoffset * fontData.scaleX - fontData.padLeft);
            first.xAdvances = xAdvances1;
            second.xAdvances = xAdvances2;
        }else{
            // Second run is empty, just trim whitespace glyphs from end of first run.
            glyphs2.truncate(firstEnd);
            xAdvances2.truncate(firstEnd + 1);
        }

        if(firstEnd == 0){
            // If the first run is now empty, remove it.
            glyphRunPool.free(first);
            runs.pop();
        }else
            adjustLastGlyph(fontData, first);

        return second;
    }

    /** Adjusts the xadvance of the last glyph to use its width instead of xadvance. */
    private void adjustLastGlyph(FontData fontData, GlyphRun run){
        Glyph last = run.glyphs.peek();
        if(last.fixedWidth) return;
        float width = (last.width + last.xoffset) * fontData.scaleX - fontData.padRight;
        run.width += width - run.xAdvances.peek(); // Can cause the run width to be > targetWidth, but the problem is minimal.
        run.xAdvances.set(run.xAdvances.size - 1, width);
    }

    private int parseColorMarkup(CharSequence str, int start, int end, Pool<Color> colorPool){
        if(start == end) return -1; // String ended with "[".
        switch(str.charAt(start)){
            case '#':
                // Parse hex color RRGGBBAA where AA is optional and defaults to 0xFF if less than 6 chars are used.
                int colorInt = 0;
                for(int i = start + 1; i < end; i++){
                    char ch = str.charAt(i);
                    if(ch == ']'){
                        if(i < start + 2 || i > start + 9 || i == start + 8) break; // Illegal number of hex digits.
                        if(i - start <= 7){ // RRGGBB or fewer chars.
                            for(int ii = 0, nn = 9 - (i - start); ii < nn; ii++)
                                colorInt = colorInt << 4;
                            colorInt |= 0xff;
                        }
                        Color color = colorPool.obtain();
                        colorStack.add(color);
                        color.rgba8888(colorInt);
                        return i - start;
                    }
                    if(ch >= '0' && ch <= '9') colorInt = colorInt * 16 + (ch - '0');
                    else if(ch >= 'a' && ch <= 'f') colorInt = colorInt * 16 + (ch - ('a' - 10));
                    else if(ch >= 'A' && ch <= 'F') colorInt = colorInt * 16 + (ch - ('A' - 10));
                    else break; // Unexpected character in hex color.
                }
                return -1;
            case '[': // "[[" is an escaped left square bracket.
                return -2;
            case ']': // "[]" is a "pop" color tag.
                if(colorStack.size > 1) colorPool.free(colorStack.pop());
                return 0;
        }
        // Parse named color.
        for(int i = start + 1; i < end; i++){
            char ch = str.charAt(i);
            if(ch != ']') continue;
            Color namedColor = Colors.get(str.subSequence(start, i).toString());
            if(namedColor == null) return -1; // Unknown color name.
            Color color = colorPool.obtain();
            colorStack.add(color);
            color.set(namedColor);
            return i - start;
        }
        return -1; // Unclosed color tag.
    }

    @Override
    public void reset(){
        Pools.get(GlyphRun.class, GlyphRun::new).freeAll(runs);
        runs.clear();

        ignoreMarkup = false;
        width = 0;
        height = 0;
    }

    public String toString(){
        if(runs.size == 0) return "";
        StringBuilder buffer = new StringBuilder(128);
        buffer.append(width);
        buffer.append('x');
        buffer.append(height);
        buffer.append('\n');
        for(int i = 0, n = runs.size; i < n; i++){
            buffer.append(runs.get(i).toString());
            buffer.append('\n');
        }
        buffer.setLength(buffer.length() - 1);
        return buffer.toString();
    }

    /**
     * Stores glyphs and positions for a piece of text which is a single color and does not span multiple lines.
     * @author Nathan Sweet
     */
    public static class GlyphRun implements Poolable{
        public final Color color = new Color();
        public Seq<Glyph> glyphs = new Seq<>();
        /**
         * Contains glyphs.size+1 entries: First entry is X offset relative to the drawing position. Subsequent entries are the X
         * advance relative to previous glyph position. Last entry is the width of the last glyph.
         */
        public FloatSeq xAdvances = new FloatSeq();
        public float x, y, width;

        public void reset(){
            glyphs.clear();
            xAdvances.clear();
            width = 0;
        }

        public String toString(){
            StringBuilder buffer = new StringBuilder(glyphs.size);
            Seq<Glyph> glyphs = this.glyphs;
            for(int i = 0, n = glyphs.size; i < n; i++){
                Glyph g = glyphs.get(i);
                buffer.append((char)g.id);
            }
            buffer.append(", #");
            buffer.append(color);
            buffer.append(", ");
            buffer.append(x);
            buffer.append(", ");
            buffer.append(y);
            buffer.append(", ");
            buffer.append(width);
            return buffer.toString();
        }
    }
}
