package arc.packer;

import arc.math.*;
import arc.packer.TexturePacker.*;
import arc.struct.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Packs pages of images using the maximal rectangles bin packing algorithm by Jukka Jylänki. A brute force binary search is
 * used to pack into the smallest bin possible.
 * @author Nathan Sweet
 */
public class MaxRectsPacker implements Packer{
    final Settings settings;
    /** Below this many rects, packing is so quick that handing work to other threads costs more than it saves. */
    private static final int parallelThreshold = 48;

    private final FreeRectChoiceHeuristic[] methods = FreeRectChoiceHeuristic.values();
    /** One instance per heuristic, so the heuristics can be evaluated at the same time without sharing any state. */
    private final MaxRects[] maxRects = new MaxRects[methods.length];
    private final Sort sort = new Sort();
    /** Where progress is printed. Null means System.out. */
    PrintStream log;

    private final Comparator<Rect> rectComparator = new Comparator<Rect>(){
        @Override
        public int compare(Rect o1, Rect o2){
            return Rect.getAtlasName(o1.name, settings.flattenPaths).compareTo(Rect.getAtlasName(o2.name, settings.flattenPaths));
        }
    };

    public MaxRectsPacker(Settings settings){
        this.settings = settings;
        for(int i = 0; i < maxRects.length; i++) maxRects[i] = new MaxRects();
        if(settings.minWidth > settings.maxWidth) throw new RuntimeException("Page min width cannot be higher than max width.");
        if(settings.minHeight > settings.maxHeight)
            throw new RuntimeException("Page min height cannot be higher than max height.");
    }

    private PrintStream out(){
        return log == null ? System.out : log;
    }

    @Override
    public Seq<Page> pack(Seq<Rect> inputRects){
        int n = inputRects.size;
        for(int i = 0; i < n; i++){
            Rect rect = inputRects.get(i);
            rect.width += settings.paddingX;
            rect.height += settings.paddingY;
        }

        if(settings.fast){
            if(settings.rotation){
                // Sort by longest side if rotation is enabled.
                sort.sort(inputRects, (o1, o2) -> {
                    int n1 = Math.max(o1.width, o1.height);
                    int n2 = Math.max(o2.width, o2.height);
                    return n2 - n1;
                });
            }else{
                // Sort only by width (largest to smallest) if rotation is disabled.
                sort.sort(inputRects, (o1, o2) -> o2.width - o1.width);
            }
        }

        Seq<Page> pages = new Seq<>();
        while(inputRects.size > 0){
            Page result = packPage(inputRects);
            pages.add(result);
            inputRects = result.remainingRects;
        }
        return pages;

    }

    private Page packPage(Seq<Rect> inputRects){
        int paddingX = settings.paddingX, paddingY = settings.paddingY;
        float maxWidth = settings.maxWidth, maxHeight = settings.maxHeight;
        boolean edgePadX = false, edgePadY = false;
        if(settings.edgePadding){
            if(settings.duplicatePadding){
                maxWidth -= paddingX;
                maxHeight -= paddingY;
            }else{
                maxWidth -= paddingX * 2;
                maxHeight -= paddingY * 2;
            }
            edgePadX = paddingX > 0;
            edgePadY = paddingY > 0;
        }

        // Find min size.
        int minWidth = Integer.MAX_VALUE, minHeight = Integer.MAX_VALUE;
        for(int i = 0, nn = inputRects.size; i < nn; i++){
            Rect rect = inputRects.get(i);
            int width = rect.width - paddingX, height = rect.height - paddingY;
            minWidth = Math.min(minWidth, width);
            minHeight = Math.min(minHeight, height);
            if(settings.rotation){
                if((width > maxWidth || height > maxHeight) && (width > maxHeight || height > maxWidth)){
                    String paddingMessage = (edgePadX || edgePadY) ? (" and edge padding " + paddingX + "*2," + paddingY + "*2") : "";
                    throw new RuntimeException("Image does not fit with max page size " + settings.maxWidth + "x" + settings.maxHeight
                    + paddingMessage + ": " + rect.name + "[" + width + "," + height + "]");
                }
            }else{
                if(width > maxWidth){
                    String paddingMessage = edgePadX ? (" and X edge padding " + paddingX + "*2") : "";
                    throw new RuntimeException("Image does not fit with max page width " + settings.maxWidth + paddingMessage + ": "
                    + rect.name + "[" + width + "," + height + "]");
                }
                if(height > maxHeight){
                    String paddingMessage = edgePadY ? (" and Y edge padding " + paddingY + "*2") : "";
                    throw new RuntimeException("Image does not fit in max page height " + settings.maxHeight + paddingMessage + ": "
                    + rect.name + "[" + width + "," + height + "]");
                }
            }
        }
        minWidth = Math.max(minWidth, settings.minWidth);
        minHeight = Math.max(minHeight, settings.minHeight);

        // BinarySearch uses the max size. Rects are packed with right and top padding, so the max size is increased to match.
        // After packing the padding is subtracted from the page size.
        int adjustX = paddingX, adjustY = paddingY;
        if(settings.edgePadding){
            if(settings.duplicatePadding){
                adjustX -= paddingX;
                adjustY -= paddingY;
            }else{
                adjustX -= paddingX * 2;
                adjustY -= paddingY * 2;
            }
        }

        if(!settings.silent) out().print("| Packing");

        //the width/height searches revisit many of the same sizes, and packing at a size is a pure function of that size
        Map<Long, Page> cache = new HashMap<>();

        // Find the minimal page size that fits all rects.
        Page bestResult = null;
        if(settings.square){
            int minSize = Math.max(minWidth, minHeight);
            int maxSize = Math.min(settings.maxWidth, settings.maxHeight);
            BinarySearch sizeSearch = new BinarySearch(minSize, maxSize, settings.fast ? 25 : 15, settings.pot,
            settings.multipleOfFour);
            int size = sizeSearch.reset(), i = 0;
            while(size != -1){
                Page result = packAtSizeCached(cache, size + adjustX, size + adjustY, inputRects);
                if(!settings.silent){
                    if(++i % 70 == 0) out().println();
                    out().print(".");
                }
                bestResult = getBest(bestResult, result);
                size = sizeSearch.next(result == null);
            }
            if(!settings.silent) out().println();
            // Rects don't fit on one page. Fill a whole page and return.
            if(bestResult == null) bestResult = packAtSize(false, maxSize + adjustX, maxSize + adjustY, inputRects);
            sort.sort(bestResult.outputRects, rectComparator);
            bestResult.width = Math.max(bestResult.width, bestResult.height) - paddingX;
            bestResult.height = Math.max(bestResult.width, bestResult.height) - paddingY;
            return bestResult;
        }else{
            BinarySearch widthSearch = new BinarySearch(minWidth, settings.maxWidth, settings.fast ? 25 : 15, settings.pot,
            settings.multipleOfFour);
            BinarySearch heightSearch = new BinarySearch(minHeight, settings.maxHeight, settings.fast ? 25 : 15, settings.pot,
            settings.multipleOfFour);
            int width = widthSearch.reset(), i = 0;
            int height = settings.square ? width : heightSearch.reset();
            while(true){
                Page bestWidthResult = null;
                while(width != -1){
                    Page result = packAtSizeCached(cache, width + adjustX, height + adjustY, inputRects);
                    if(!settings.silent){
                        if(++i % 70 == 0) out().println();
                        out().print(".");
                    }
                    bestWidthResult = getBest(bestWidthResult, result);
                    width = widthSearch.next(result == null);
                    if(settings.square) height = width;
                }
                bestResult = getBest(bestResult, bestWidthResult);
                if(settings.square) break;
                height = heightSearch.next(bestWidthResult == null);
                if(height == -1) break;
                width = widthSearch.reset();
            }
            if(!settings.silent) out().println();
            // Rects don't fit on one page. Fill a whole page and return.
            if(bestResult == null)
                bestResult = packAtSize(false, settings.maxWidth + adjustX, settings.maxHeight + adjustY, inputRects);
            sort.sort(bestResult.outputRects, rectComparator);
            bestResult.width -= paddingX;
            bestResult.height -= paddingY;
            return bestResult;
        }
    }

    /** Same as {@link #packAtSize(boolean, int, int, Seq)} with fully = true, but remembers results per size. */
    private Page packAtSizeCached(Map<Long, Page> cache, int width, int height, Seq<Rect> inputRects){
        Long key = ((long)width << 32) | (height & 0xffffffffL);
        Page result = cache.get(key);
        if(result == null && !cache.containsKey(key)){
            result = packAtSize(true, width, height, inputRects);
            cache.put(key, result);
        }
        return result;
    }

    /**
     * @param fully If true, the only results that pack all rects will be considered. If false, all results are considered, not
     * all rects may be packed.
     */
    private Page packAtSize(boolean fully, int width, int height, Seq<Rect> inputRects){
        int count = methods.length;
        Page[] results = new Page[count];

        if(Tasks.parallel() && inputRects.size >= parallelThreshold){
            //each heuristic is independent and only reads the input rects
            Seq<FutureTask<Page>> tasks = new Seq<>(count);
            for(int i = 0; i < count; i++){
                final int index = i;
                tasks.add(Tasks.submit(() -> packWithMethod(index, fully, width, height, inputRects)));
            }
            for(int i = 0; i < count; i++){
                results[i] = Tasks.join(tasks.get(i));
            }
        }else{
            for(int i = 0; i < count; i++){
                results[i] = packWithMethod(i, fully, width, height, inputRects);
            }
        }

        //combine in a fixed order so the outcome doesn't depend on thread timing
        Page bestResult = null;
        for(int i = 0; i < count; i++){
            if(results[i] != null) bestResult = getBest(bestResult, results[i]);
        }
        return bestResult;
    }

    /** @return the page produced by a single heuristic, or null if it doesn't qualify. */
    private Page packWithMethod(int index, boolean fully, int width, int height, Seq<Rect> inputRects){
        FreeRectChoiceHeuristic method = methods[index];
        MaxRects maxRects = this.maxRects[index];
        maxRects.init(width, height, method == FreeRectChoiceHeuristic.ContactPointRule);
        Page result;
        if(!settings.fast){
            result = maxRects.pack(inputRects, method);
        }else{
            int nn = inputRects.size;
            int ii = 0;
            while(ii < nn){
                if(maxRects.insert(inputRects.get(ii), method) == null) break;
                ii++;
            }
            if(ii < nn){
                //a rect didn't fit
                if(fully) return null;

                Seq<Rect> remaining = new Seq<>(nn - ii);
                while(ii < nn) remaining.add(inputRects.get(ii++));
                result = maxRects.getResult();
                result.remainingRects = remaining;
            }else{
                result = maxRects.getResult();
                result.remainingRects = new Seq<>();
            }
        }
        if(fully && result.remainingRects.size > 0) return null;
        if(result.outputRects.size == 0) return null;
        return result;
    }

    private Page getBest(Page result1, Page result2){
        if(result1 == null) return result2;
        if(result2 == null) return result1;
        return result1.occupancy > result2.occupancy ? result1 : result2;
    }

    static class BinarySearch{
        final boolean pot, mod4;
        final int min, max, fuzziness;
        int low, high, current;

        public BinarySearch(int min, int max, int fuzziness, boolean pot, boolean mod4){
            if(pot){
                this.min = (int)(Math.log(Mathf.nextPowerOfTwo(min)) / Math.log(2));
                this.max = (int)(Math.log(Mathf.nextPowerOfTwo(max)) / Math.log(2));
            }else if(mod4){
                this.min = min % 4 == 0 ? min : min + 4 - (min % 4);
                this.max = max % 4 == 0 ? max : max + 4 - (max % 4);
            }else{
                this.min = min;
                this.max = max;
            }
            this.fuzziness = pot ? 0 : fuzziness;
            this.pot = pot;
            this.mod4 = mod4;
        }

        public int reset(){
            low = min;
            high = max;
            current = (low + high) >>> 1;
            if(pot) return (int)Math.pow(2, current);
            if(mod4) return current % 4 == 0 ? current : current + 4 - (current % 4);
            return current;
        }

        public int next(boolean result){
            if(low >= high) return -1;
            if(result)
                low = current + 1;
            else
                high = current - 1;
            current = (low + high) >>> 1;
            if(Math.abs(low - high) < fuzziness) return -1;
            if(pot) return (int)Math.pow(2, current);
            if(mod4) return current % 4 == 0 ? current : current + 4 - (current % 4);
            return current;
        }
    }

    /**
     * Maximal rectangles bin packing algorithm. Adapted from this C++ public domain source:
     * http://clb.demon.fi/projects/even-more-rectangle-bin-packing
     * @author Jukka Jyl�nki
     * @author Nathan Sweet
     */
    class MaxRects{
        private int binWidth, binHeight;
        private final Seq<Rect> usedRectangles = new Seq<>();
        private final Seq<Rect> freeRectangles = new Seq<>();
        private final Seq<Rect> rectanglesToCheckWhenPruning = new Seq<>();
        /** Scratch space for pruneFreeList, indexed like freeRectangles. */
        private boolean[] pruneMarks = new boolean[64];

        /**
         * Used rects indexed by each of their four edge coordinates, only maintained for the contact point heuristic. This lets
         * the contact score look at just the rects that can touch a candidate instead of scanning all of them.
         */
        private boolean trackEdges;
        private Seq<Rect>[] byLeft, byRight, byBottom, byTop;

        /** @param trackEdges must be true if the contact point heuristic is going to be used. */
        @SuppressWarnings("unchecked")
        public void init(int width, int height, boolean trackEdges){
            binWidth = width;
            binHeight = height;

            usedRectangles.clear();
            freeRectangles.clear();
            rectanglesToCheckWhenPruning.clear();
            Rect n = new Rect();
            n.x = 0;
            n.y = 0;
            n.width = width;
            n.height = height;
            freeRectangles.add(n);

            this.trackEdges = trackEdges;
            if(trackEdges){
                if(byLeft == null || byLeft.length < width + 1){
                    byLeft = new Seq[width + 1];
                    byRight = new Seq[width + 1];
                }else{
                    Arrays.fill(byLeft, null);
                    Arrays.fill(byRight, null);
                }
                if(byBottom == null || byBottom.length < height + 1){
                    byBottom = new Seq[height + 1];
                    byTop = new Seq[height + 1];
                }else{
                    Arrays.fill(byBottom, null);
                    Arrays.fill(byTop, null);
                }
            }
        }

        private void markUsed(Rect rect){
            usedRectangles.add(rect);
            if(trackEdges){
                index(byLeft, rect.x, rect);
                index(byRight, rect.x + rect.width, rect);
                index(byBottom, rect.y, rect);
                index(byTop, rect.y + rect.height, rect);
            }
        }

        private void index(Seq<Rect>[] edges, int coordinate, Rect rect){
            if(coordinate < 0 || coordinate >= edges.length) return; //can't touch anything inside the bin
            Seq<Rect> list = edges[coordinate];
            if(list == null) list = edges[coordinate] = new Seq<>(4);
            list.add(rect);
        }

        /** Packs a single image. Order is defined externally. */
        public Rect insert(Rect rect, FreeRectChoiceHeuristic method){
            Rect newNode = scoreRect(rect, method);
            if(newNode.height == 0) return null;

            int numRectanglesToProcess = freeRectangles.size;
            for(int i = 0; i < numRectanglesToProcess; ++i){
                if(splitFreeNode(freeRectangles.get(i), newNode)){
                    freeRectangles.remove(i);
                    --i;
                    --numRectanglesToProcess;
                }
            }

            pruneFreeList();

            Rect bestNode = new Rect();
            bestNode.set(rect);
            bestNode.score1 = newNode.score1;
            bestNode.score2 = newNode.score2;
            bestNode.x = newNode.x;
            bestNode.y = newNode.y;
            bestNode.width = newNode.width;
            bestNode.height = newNode.height;
            bestNode.rotated = newNode.rotated;

            markUsed(bestNode);
            return bestNode;
        }

        /** For each rectangle, packs each one then chooses the best and packs that. Slow! */
        public Page pack(Seq<Rect> rects, FreeRectChoiceHeuristic method){
            rects = new Seq<>(rects);
            while(rects.size > 0){
                int bestRectIndex = -1;
                Rect bestNode = new Rect();
                bestNode.score1 = Integer.MAX_VALUE;
                bestNode.score2 = Integer.MAX_VALUE;

                // Find the next rectangle that packs best.
                for(int i = 0; i < rects.size; i++){
                    Rect newNode = scoreRect(rects.get(i), method);
                    if(newNode.score1 < bestNode.score1 || (newNode.score1 == bestNode.score1 && newNode.score2 < bestNode.score2)){
                        bestNode.set(rects.get(i));
                        bestNode.score1 = newNode.score1;
                        bestNode.score2 = newNode.score2;
                        bestNode.x = newNode.x;
                        bestNode.y = newNode.y;
                        bestNode.width = newNode.width;
                        bestNode.height = newNode.height;
                        bestNode.rotated = newNode.rotated;
                        bestRectIndex = i;
                    }
                }

                if(bestRectIndex == -1) break;

                placeRect(bestNode);
                rects.remove(bestRectIndex);
            }

            Page result = getResult();
            result.remainingRects = rects;
            return result;
        }

        public Page getResult(){
            int w = 0, h = 0;
            for(int i = 0; i < usedRectangles.size; i++){
                Rect rect = usedRectangles.get(i);
                w = Math.max(w, rect.x + rect.width);
                h = Math.max(h, rect.y + rect.height);
            }
            Page result = new Page();
            result.outputRects = new Seq<>(usedRectangles);
            result.occupancy = getOccupancy();
            result.width = w;
            result.height = h;
            return result;
        }

        private void placeRect(Rect node){
            int numRectanglesToProcess = freeRectangles.size;
            for(int i = 0; i < numRectanglesToProcess; i++){
                if(splitFreeNode(freeRectangles.get(i), node)){
                    freeRectangles.remove(i);
                    --i;
                    --numRectanglesToProcess;
                }
            }

            pruneFreeList();

            markUsed(node);
        }

        private Rect scoreRect(Rect rect, FreeRectChoiceHeuristic method){
            int width = rect.width;
            int height = rect.height;
            int rotatedWidth = height - settings.paddingY + settings.paddingX;
            int rotatedHeight = width - settings.paddingX + settings.paddingY;
            boolean rotate = rect.canRotate && settings.rotation;

            Rect newNode = null;
            switch(method){
                case BestShortSideFit:
                    newNode = findPositionForNewNodeBestShortSideFit(width, height, rotatedWidth, rotatedHeight, rotate);
                    break;
                case BottomLeftRule:
                    newNode = findPositionForNewNodeBottomLeft(width, height, rotatedWidth, rotatedHeight, rotate);
                    break;
                case ContactPointRule:
                    newNode = findPositionForNewNodeContactPoint(width, height, rotatedWidth, rotatedHeight, rotate);
                    newNode.score1 = -newNode.score1; // Reverse since we are minimizing, but for contact point score bigger is better.
                    break;
                case BestLongSideFit:
                    newNode = findPositionForNewNodeBestLongSideFit(width, height, rotatedWidth, rotatedHeight, rotate);
                    break;
                case BestAreaFit:
                    newNode = findPositionForNewNodeBestAreaFit(width, height, rotatedWidth, rotatedHeight, rotate);
                    break;
            }

            // Cannot fit the current rectangle.
            if(newNode.height == 0){
                newNode.score1 = Integer.MAX_VALUE;
                newNode.score2 = Integer.MAX_VALUE;
            }

            return newNode;
        }

        // / Computes the ratio of used surface area.
        private float getOccupancy(){
            int usedSurfaceArea = 0;
            for(int i = 0; i < usedRectangles.size; i++)
                usedSurfaceArea += usedRectangles.get(i).width * usedRectangles.get(i).height;
            return (float)usedSurfaceArea / (binWidth * binHeight);
        }

        private Rect findPositionForNewNodeBottomLeft(int width, int height, int rotatedWidth, int rotatedHeight, boolean rotate){
            Rect bestNode = new Rect();

            bestNode.score1 = Integer.MAX_VALUE; // best y, score2 is best x

            for(int i = 0, n = freeRectangles.size; i < n; i++){
                Rect free = freeRectangles.get(i);
                // Try to place the rectangle in upright (non-rotated) orientation.
                if(free.width >= width && free.height >= height){
                    int topSideY = free.y + height;
                    if(topSideY < bestNode.score1 || (topSideY == bestNode.score1 && free.x < bestNode.score2)){
                        bestNode.x = free.x;
                        bestNode.y = free.y;
                        bestNode.width = width;
                        bestNode.height = height;
                        bestNode.score1 = topSideY;
                        bestNode.score2 = free.x;
                        bestNode.rotated = false;
                    }
                }
                if(rotate && free.width >= rotatedWidth && free.height >= rotatedHeight){
                    int topSideY = free.y + rotatedHeight;
                    if(topSideY < bestNode.score1 || (topSideY == bestNode.score1 && free.x < bestNode.score2)){
                        bestNode.x = free.x;
                        bestNode.y = free.y;
                        bestNode.width = rotatedWidth;
                        bestNode.height = rotatedHeight;
                        bestNode.score1 = topSideY;
                        bestNode.score2 = free.x;
                        bestNode.rotated = true;
                    }
                }
            }
            return bestNode;
        }

        private Rect findPositionForNewNodeBestShortSideFit(int width, int height, int rotatedWidth, int rotatedHeight,
                                                            boolean rotate){
            Rect bestNode = new Rect();
            bestNode.score1 = Integer.MAX_VALUE;

            for(int i = 0, n = freeRectangles.size; i < n; i++){
                Rect free = freeRectangles.get(i);
                // Try to place the rectangle in upright (non-rotated) orientation.
                if(free.width >= width && free.height >= height){
                    int leftoverHoriz = Math.abs(free.width - width);
                    int leftoverVert = Math.abs(free.height - height);
                    int shortSideFit = Math.min(leftoverHoriz, leftoverVert);
                    int longSideFit = Math.max(leftoverHoriz, leftoverVert);

                    if(shortSideFit < bestNode.score1 || (shortSideFit == bestNode.score1 && longSideFit < bestNode.score2)){
                        bestNode.x = free.x;
                        bestNode.y = free.y;
                        bestNode.width = width;
                        bestNode.height = height;
                        bestNode.score1 = shortSideFit;
                        bestNode.score2 = longSideFit;
                        bestNode.rotated = false;
                    }
                }

                if(rotate && free.width >= rotatedWidth && free.height >= rotatedHeight){
                    int flippedLeftoverHoriz = Math.abs(free.width - rotatedWidth);
                    int flippedLeftoverVert = Math.abs(free.height - rotatedHeight);
                    int flippedShortSideFit = Math.min(flippedLeftoverHoriz, flippedLeftoverVert);
                    int flippedLongSideFit = Math.max(flippedLeftoverHoriz, flippedLeftoverVert);

                    if(flippedShortSideFit < bestNode.score1
                    || (flippedShortSideFit == bestNode.score1 && flippedLongSideFit < bestNode.score2)){
                        bestNode.x = free.x;
                        bestNode.y = free.y;
                        bestNode.width = rotatedWidth;
                        bestNode.height = rotatedHeight;
                        bestNode.score1 = flippedShortSideFit;
                        bestNode.score2 = flippedLongSideFit;
                        bestNode.rotated = true;
                    }
                }
            }

            return bestNode;
        }

        private Rect findPositionForNewNodeBestLongSideFit(int width, int height, int rotatedWidth, int rotatedHeight,
                                                           boolean rotate){
            Rect bestNode = new Rect();

            bestNode.score2 = Integer.MAX_VALUE;

            for(int i = 0, n = freeRectangles.size; i < n; i++){
                Rect free = freeRectangles.get(i);
                // Try to place the rectangle in upright (non-rotated) orientation.
                if(free.width >= width && free.height >= height){
                    int leftoverHoriz = Math.abs(free.width - width);
                    int leftoverVert = Math.abs(free.height - height);
                    int shortSideFit = Math.min(leftoverHoriz, leftoverVert);
                    int longSideFit = Math.max(leftoverHoriz, leftoverVert);

                    if(longSideFit < bestNode.score2 || (longSideFit == bestNode.score2 && shortSideFit < bestNode.score1)){
                        bestNode.x = free.x;
                        bestNode.y = free.y;
                        bestNode.width = width;
                        bestNode.height = height;
                        bestNode.score1 = shortSideFit;
                        bestNode.score2 = longSideFit;
                        bestNode.rotated = false;
                    }
                }

                if(rotate && free.width >= rotatedWidth && free.height >= rotatedHeight){
                    int leftoverHoriz = Math.abs(free.width - rotatedWidth);
                    int leftoverVert = Math.abs(free.height - rotatedHeight);
                    int shortSideFit = Math.min(leftoverHoriz, leftoverVert);
                    int longSideFit = Math.max(leftoverHoriz, leftoverVert);

                    if(longSideFit < bestNode.score2 || (longSideFit == bestNode.score2 && shortSideFit < bestNode.score1)){
                        bestNode.x = free.x;
                        bestNode.y = free.y;
                        bestNode.width = rotatedWidth;
                        bestNode.height = rotatedHeight;
                        bestNode.score1 = shortSideFit;
                        bestNode.score2 = longSideFit;
                        bestNode.rotated = true;
                    }
                }
            }
            return bestNode;
        }

        private Rect findPositionForNewNodeBestAreaFit(int width, int height, int rotatedWidth, int rotatedHeight,
                                                       boolean rotate){
            Rect bestNode = new Rect();

            bestNode.score1 = Integer.MAX_VALUE; // best area fit, score2 is best short side fit

            for(int i = 0, n = freeRectangles.size; i < n; i++){
                Rect free = freeRectangles.get(i);
                int areaFit = free.width * free.height - width * height;

                // Try to place the rectangle in upright (non-rotated) orientation.
                if(free.width >= width && free.height >= height){
                    int leftoverHoriz = Math.abs(free.width - width);
                    int leftoverVert = Math.abs(free.height - height);
                    int shortSideFit = Math.min(leftoverHoriz, leftoverVert);

                    if(areaFit < bestNode.score1 || (areaFit == bestNode.score1 && shortSideFit < bestNode.score2)){
                        bestNode.x = free.x;
                        bestNode.y = free.y;
                        bestNode.width = width;
                        bestNode.height = height;
                        bestNode.score2 = shortSideFit;
                        bestNode.score1 = areaFit;
                        bestNode.rotated = false;
                    }
                }

                if(rotate && free.width >= rotatedWidth && free.height >= rotatedHeight){
                    int leftoverHoriz = Math.abs(free.width - rotatedWidth);
                    int leftoverVert = Math.abs(free.height - rotatedHeight);
                    int shortSideFit = Math.min(leftoverHoriz, leftoverVert);

                    if(areaFit < bestNode.score1 || (areaFit == bestNode.score1 && shortSideFit < bestNode.score2)){
                        bestNode.x = free.x;
                        bestNode.y = free.y;
                        bestNode.width = rotatedWidth;
                        bestNode.height = rotatedHeight;
                        bestNode.score2 = shortSideFit;
                        bestNode.score1 = areaFit;
                        bestNode.rotated = true;
                    }
                }
            }
            return bestNode;
        }

        // / Returns 0 if the two intervals i1 and i2 are disjoint, or the length of their overlap otherwise.
        private int commonIntervalLength(int i1start, int i1end, int i2start, int i2end){
            if(i1end < i2start || i2end < i1start) return 0;
            return Math.min(i1end, i2end) - Math.max(i1start, i2start);
        }

        /** Total length of the contacts between a candidate placement and everything already placed. */
        private int contactPointScoreNode(int x, int y, int width, int height){
            int score = 0;

            if(x == 0 || x + width == binWidth) score += height;
            if(y == 0 || y + height == binHeight) score += width;

            int right = x + width, top = y + height;

            //rects touching the left or right side of the candidate have an edge at exactly x or x + width
            score += touchingY(edge(byLeft, right), y, top, Integer.MIN_VALUE);
            score += touchingY(edge(byRight, x), y, top, right); //those already found by the call above aren't counted twice

            //same for the bottom and top side
            score += touchingX(edge(byBottom, top), x, right, Integer.MIN_VALUE);
            score += touchingX(edge(byTop, y), x, right, top);
            return score;
        }

        private Seq<Rect> edge(Seq<Rect>[] edges, int coordinate){
            return coordinate < 0 || coordinate >= edges.length ? null : edges[coordinate];
        }

        /** Overlap of the rects' y ranges with [y1, y2], skipping rects whose x is skipX. */
        private int touchingY(Seq<Rect> rects, int y1, int y2, int skipX){
            if(rects == null) return 0;
            int score = 0;
            for(int i = 0, n = rects.size; i < n; i++){
                Rect rect = rects.get(i);
                if(rect.x == skipX) continue;
                score += commonIntervalLength(rect.y, rect.y + rect.height, y1, y2);
            }
            return score;
        }

        /** Overlap of the rects' x ranges with [x1, x2], skipping rects whose y is skipY. */
        private int touchingX(Seq<Rect> rects, int x1, int x2, int skipY){
            if(rects == null) return 0;
            int score = 0;
            for(int i = 0, n = rects.size; i < n; i++){
                Rect rect = rects.get(i);
                if(rect.y == skipY) continue;
                score += commonIntervalLength(rect.x, rect.x + rect.width, x1, x2);
            }
            return score;
        }

        private Rect findPositionForNewNodeContactPoint(int width, int height, int rotatedWidth, int rotatedHeight,
                                                        boolean rotate){

            Rect bestNode = new Rect();
            bestNode.score1 = -1; // best contact score

            Seq<Rect> freeRectangles = this.freeRectangles;
            for(int i = 0, n = freeRectangles.size; i < n; i++){
                // Try to place the rectangle in upright (non-rotated) orientation.
                Rect free = freeRectangles.get(i);
                if(free.width >= width && free.height >= height){
                    int score = contactPointScoreNode(free.x, free.y, width, height);
                    if(score > bestNode.score1){
                        bestNode.x = free.x;
                        bestNode.y = free.y;
                        bestNode.width = width;
                        bestNode.height = height;
                        bestNode.score1 = score;
                        bestNode.rotated = false;
                    }
                }
                if(rotate && free.width >= rotatedWidth && free.height >= rotatedHeight){
                    int score = contactPointScoreNode(free.x, free.y, rotatedWidth, rotatedHeight);
                    if(score > bestNode.score1){
                        bestNode.x = free.x;
                        bestNode.y = free.y;
                        bestNode.width = rotatedWidth;
                        bestNode.height = rotatedHeight;
                        bestNode.score1 = score;
                        bestNode.rotated = true;
                    }
                }
            }
            return bestNode;
        }

        private boolean splitFreeNode(Rect freeNode, Rect usedNode){
            // Test with SAT if the rectangles even intersect.
            if(usedNode.x >= freeNode.x + freeNode.width || usedNode.x + usedNode.width <= freeNode.x
            || usedNode.y >= freeNode.y + freeNode.height || usedNode.y + usedNode.height <= freeNode.y) return false;

            if(usedNode.x < freeNode.x + freeNode.width && usedNode.x + usedNode.width > freeNode.x){
                // New node at the top side of the used node.
                if(usedNode.y > freeNode.y && usedNode.y < freeNode.y + freeNode.height){
                    Rect newNode = new Rect(freeNode);
                    newNode.height = usedNode.y - newNode.y;
                    freeRectangles.add(newNode);
                    rectanglesToCheckWhenPruning.add(newNode);
                }

                // New node at the bottom side of the used node.
                if(usedNode.y + usedNode.height < freeNode.y + freeNode.height){
                    Rect newNode = new Rect(freeNode);
                    newNode.y = usedNode.y + usedNode.height;
                    newNode.height = freeNode.y + freeNode.height - (usedNode.y + usedNode.height);
                    freeRectangles.add(newNode);
                    rectanglesToCheckWhenPruning.add(newNode);
                }
            }

            if(usedNode.y < freeNode.y + freeNode.height && usedNode.y + usedNode.height > freeNode.y){
                // New node at the left side of the used node.
                if(usedNode.x > freeNode.x && usedNode.x < freeNode.x + freeNode.width){
                    Rect newNode = new Rect(freeNode);
                    newNode.width = usedNode.x - newNode.x;
                    freeRectangles.add(newNode);
                    rectanglesToCheckWhenPruning.add(newNode);
                }

                // New node at the right side of the used node.
                if(usedNode.x + usedNode.width < freeNode.x + freeNode.width){
                    Rect newNode = new Rect(freeNode);
                    newNode.x = usedNode.x + usedNode.width;
                    newNode.width = freeNode.x + freeNode.width - (usedNode.x + usedNode.width);
                    freeRectangles.add(newNode);
                    rectanglesToCheckWhenPruning.add(newNode);
                }
            }

            return true;
        }

        private void pruneFreeList(){
            Seq<Rect> toCheck = rectanglesToCheckWhenPruning;
            if(toCheck.size == 0) return;

            Seq<Rect> free = freeRectangles;
            int freeSize = free.size;
            if(pruneMarks.length < freeSize) pruneMarks = new boolean[Math.max(freeSize, pruneMarks.length * 2)];
            boolean[] marks = pruneMarks;
            Arrays.fill(marks, 0, freeSize, false);

            boolean any = false;
            for(int c = 0, cn = toCheck.size; c < cn; c++){
                Rect checking = toCheck.get(c);
                for(int i = 0; i < freeSize; i++){
                    Rect rect = free.get(i);
                    if(rect == checking) continue;
                    if(isContainedIn(rect, checking)){
                        marks[i] = true;
                        any = true;
                    }
                }
            }

            toCheck.clear();

            if(!any) return;

            //remove marked rects in place, keeping the order of the rest
            int kept = 0;
            for(int i = 0; i < freeSize; i++){
                if(!marks[i]) free.set(kept++, free.get(i));
            }
            free.truncate(kept);
        }

        private boolean isContainedIn(Rect a, Rect b){
            return a.x >= b.x && a.y >= b.y && a.x + a.width <= b.x + b.width && a.y + a.height <= b.y + b.height;
        }
    }

    public enum FreeRectChoiceHeuristic{
        /** BSSF: Positions the rectangle against the short side of a free rectangle into which it fits the best. */
        BestShortSideFit,
        /** BLSF: Positions the rectangle against the long side of a free rectangle into which it fits the best. */
        BestLongSideFit,
        /** BAF: Positions the rectangle into the smallest free rect into which it fits. */
        BestAreaFit,
        /** BL: Does the Tetris placement. */
        BottomLeftRule,
        /** CP: Choosest the placement where the rectangle touches other rects as much as possible. */
        ContactPointRule
    }
}
