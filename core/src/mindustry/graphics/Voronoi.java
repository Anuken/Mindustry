package mindustry.graphics;

import arc.math.geom.*;
import arc.struct.*;

import java.util.*;

//TODO in dire need of cleanup
public class Voronoi{
    private final static int LE = 0;
    private final static int RE = 1;

    //TODO make local
    int siteidx;
    Site[] sites;
    int nsites;
    float borderMinX, borderMaxX, borderMinY, borderMaxY;
    float ymin;
    float deltay;
    int nvertices = 0;
    int nedges;
    Site bottomsite;
    int PQcount;
    int PQmin;
    int PQhashsize;
    Halfedge[] PQhash;
    int ELhashsize;
    Halfedge[] ELhash;
    Seq<GraphEdge> allEdges;
    float minDistanceBetweenSites = 1f;

    public static Seq<GraphEdge> generate(Vec2[] values, float minX, float maxX, float minY, float maxY){
        return new Voronoi().generateVoronoi(values, minX, maxX, minY, maxY);
    }

    Seq<GraphEdge> generateVoronoi(Vec2[] values, float minX, float maxX, float minY, float maxY){
        allEdges = new Seq<>();

        nsites = values.length;

        float sn = (float)nsites + 4;
        int rtsites = (int)Math.sqrt(sn);

        sites = new Site[nsites];
        Vec2 first = values[0];
        float xmin = first.x;
        ymin = first.y;
        float xmax = first.x;
        float ymax = first.y;
        for(int i = 0; i < nsites; i++){
            sites[i] = new Site();
            sites[i].coord.set(values[i]);
            sites[i].sitenbr = i;

            if(values[i].x < xmin){
                xmin = values[i].x;
            }else if(values[i].x > xmax){
                xmax = values[i].x;
            }

            if(values[i].y < ymin){
                ymin = values[i].y;
            }else if(values[i].y > ymax){
                ymax = values[i].y;
            }
        }

        Arrays.sort(sites, (p1, p2) -> {
            Vec2 s1 = p1.coord, s2 = p2.coord;
            if(s1.y < s2.y){
                return (-1);
            }
            if(s1.y > s2.y){
                return (1);
            }
            return Float.compare(s1.x, s2.x);
        });

        deltay = ymax - ymin;
        float deltax = xmax - xmin;

        // Check bounding box inputs - if mins are bigger than maxes, swap them
        float temp;
        if(minX > maxX){
            temp = minX;
            minX = maxX;
            maxX = temp;
        }
        if(minY > maxY){
            temp = minY;
            minY = maxY;
            maxY = temp;
        }
        borderMinX = minX;
        borderMinY = minY;
        borderMaxX = maxX;
        borderMaxY = maxY;

        siteidx = 0;

        PQcount = 0;
        PQmin = 0;
        PQhashsize = 4 * rtsites;
        PQhash = new Halfedge[PQhashsize];

        for(int i2 = 0; i2 < PQhashsize; i2 += 1){
            PQhash[i2] = new Halfedge();
        }
        int i1;
        ELhashsize = 2 * rtsites;
        ELhash = new Halfedge[ELhashsize];

        for(i1 = 0; i1 < ELhashsize; i1 += 1){
            ELhash[i1] = null;
        }
        Halfedge ELleftend = newHe(null, 0);
        Halfedge ELrightend = newHe(null, 0);
        ELleftend.ELleft = null;
        ELleftend.ELright = ELrightend;
        ELrightend.ELleft = ELleftend;
        ELrightend.ELright = null;
        ELhash[0] = ELleftend;
        ELhash[ELhashsize - 1] = ELrightend;

        bottomsite = next();
        Site newsite = next();
        Halfedge lbnd;
        Vec2 newintstar = null;
        Edge e;
        while(true){
            if(PQcount != 0){
                Vec2 answer = new Vec2();

                while(PQhash[PQmin].PQnext == null){
                    PQmin += 1;
                }
                answer.x = PQhash[PQmin].PQnext.vertex.coord.x;
                answer.y = PQhash[PQmin].PQnext.ystar;
                newintstar = (answer);
            }

            Halfedge rbnd;
            Halfedge bisector;
            Site p;
            Site bot;

            if(newsite != null && (PQcount == 0 || newsite.coord.y < newintstar.y || (newsite.coord.y == newintstar.y && newsite.coord.x < newintstar.x))){
                int bucket = (int)(((newsite.coord).x - xmin) / deltax * ELhashsize);

                if(bucket < 0){
                    bucket = 0;
                }
                if(bucket >= ELhashsize){
                    bucket = ELhashsize - 1;
                }

                Halfedge he = getHash(bucket);
                if(he == null){
                    for(int i = 1; i < ELhashsize; i += 1){
                        if((he = getHash(bucket - i)) != null){
                            break;
                        }
                        if((he = getHash(bucket + i)) != null){
                            break;
                        }
                    }
                }
                if(he == ELleftend || (he != ELrightend && right(he, (newsite.coord)))){
                    do{
                        he = he.ELright;
                    }while(he != ELrightend && right(he, (newsite.coord)));
                    he = he.ELleft;
                }else{
                    do{
                        he = he.ELleft;
                    }while(he != ELleftend && !right(he, (newsite.coord)));
                }

                if(bucket > 0 && bucket < ELhashsize - 1){
                    ELhash[bucket] = he;
                }
                lbnd = he;
                rbnd = lbnd.ELright;

                bot = rightreg(lbnd);
                e = bisect(bot, newsite);

                bisector = newHe(e, LE);
                insert(lbnd, bisector);

                if((p = intersect(lbnd, bisector)) != null){
                    pqdelete(lbnd);
                    pqinsert(lbnd, p, p.coord.dst(newsite.coord));
                }
                lbnd = bisector;
                bisector = newHe(e, RE);
                insert(lbnd, bisector);

                if((p = intersect(bisector, rbnd)) != null){
                    pqinsert(bisector, p, p.coord.dst(newsite.coord));
                }
                newsite = next();
            }else if(!(PQcount == 0)){
                Halfedge curr;

                curr = PQhash[PQmin].PQnext;
                PQhash[PQmin].PQnext = curr.PQnext;
                PQcount -= 1;
                lbnd = (curr);
                Halfedge llbnd = lbnd.ELleft;
                rbnd = lbnd.ELright;
                Halfedge rrbnd = (rbnd.ELright);
                bot = leftReg(lbnd);
                Site top = rightreg(rbnd);

                Site v = lbnd.vertex;
                v.sitenbr = nvertices;
                nvertices += 1;
                endpoint(lbnd.ELedge, lbnd.ELpm, v);
                endpoint(rbnd.ELedge, rbnd.ELpm, v);
                delete(lbnd);
                pqdelete(rbnd);
                delete(rbnd);
                int pm = LE;

                if(bot.coord.y > top.coord.y){
                    Site temp1 = bot;
                    bot = top;
                    top = temp1;
                    pm = RE;
                }

                e = bisect(bot, top);
                bisector = newHe(e, pm);
                insert(llbnd, bisector);
                endpoint(e, RE - pm, v);

                if((p = intersect(llbnd, bisector)) != null){
                    pqdelete(llbnd);
                    pqinsert(llbnd, p, p.coord.dst(bot.coord));
                }

                if((p = intersect(bisector, rrbnd)) != null){
                    pqinsert(bisector, p, p.coord.dst(bot.coord));
                }
            }else{
                break;
            }
        }

        for(lbnd = (ELleftend.ELright); lbnd != ELrightend; lbnd = (lbnd.ELright)){
            e = lbnd.ELedge;
            clipLine(e);
        }

        return allEdges;
    }

    private Site next(){
        return siteidx < nsites ? sites[siteidx ++] : null;
    }

    private Edge bisect(Site s1, Site s2){
        Edge newedge = new Edge();

        // store the sites that this edge is bisecting
        newedge.reg[0] = s1;
        newedge.reg[1] = s2;
        // to begin with, there are no endpoints on the bisector - it goes to
        // infinity
        newedge.ep[0] = null;
        newedge.ep[1] = null;

        // get the difference in x dist between the sites
        float dx = s2.coord.x - s1.coord.x;
        float dy = s2.coord.y - s1.coord.y;
        // make sure that the difference in positive
        float adx = dx > 0 ? dx : -dx;
        float ady = dy > 0 ? dy : -dy;
        newedge.c = s1.coord.x * dx + s1.coord.y * dy + (dx * dx + dy * dy) * 0.5f;// get the slope of the line

        if(adx > ady){
            newedge.a = 1.0f;
            newedge.b = dy / dx;
            newedge.c /= dx;// set formula of line, with x fixed to 1
        }else{
            newedge.b = 1.0f;
            newedge.a = dx / dy;
            newedge.c /= dy;// set formula of line, with y fixed to 1
        }

        newedge.edgenbr = nedges;

        nedges += 1;
        return newedge;
    }

    private int pqbucket(Halfedge he){
        int bucket;

        bucket = (int)((he.ystar - ymin) / deltay * PQhashsize);
        if(bucket < 0){
            bucket = 0;
        }
        if(bucket >= PQhashsize){
            bucket = PQhashsize - 1;
        }
        if(bucket < PQmin){
            PQmin = bucket;
        }
        return bucket;
    }

    // push the HalfEdge into the ordered linked list of vertices
    private void pqinsert(Halfedge he, Site v, float offset){
        Halfedge last, next;

        he.vertex = v;
        he.ystar = v.coord.y + offset;
        last = PQhash[pqbucket(he)];
        while((next = last.PQnext) != null
        && (he.ystar > next.ystar || (he.ystar == next.ystar && v.coord.x > next.vertex.coord.x))){
            last = next;
        }
        he.PQnext = last.PQnext;
        last.PQnext = he;
        PQcount += 1;
    }

    // remove the HalfEdge from the list of vertices
    private void pqdelete(Halfedge he){
        Halfedge last;

        if(he.vertex != null){
            last = PQhash[pqbucket(he)];
            while(last.PQnext != he){
                last = last.PQnext;
            }

            last.PQnext = he.PQnext;
            PQcount -= 1;
            he.vertex = null;
        }
    }

    private Halfedge newHe(Edge e, int pm){
        Halfedge answer = new Halfedge();
        answer.ELedge = e;
        answer.ELpm = pm;
        answer.PQnext = null;
        answer.vertex = null;
        return answer;
    }

    private Site leftReg(Halfedge he){
        if(he.ELedge == null){
            return bottomsite;
        }
        return he.ELpm == LE ? he.ELedge.reg[LE] : he.ELedge.reg[RE];
    }

    private void insert(Halfedge lb, Halfedge newHe){
        newHe.ELleft = lb;
        newHe.ELright = lb.ELright;
        lb.ELright.ELleft = newHe;
        lb.ELright = newHe;
    }

    /*
     * This delete routine can't reclaim node, since pointers from hash table
     * may be present.
     */
    private void delete(Halfedge he){
        he.ELleft.ELright = he.ELright;
        he.ELright.ELleft = he.ELleft;
        he.deleted = true;
    }

    /* Get entry from hash table, pruning any deleted nodes */
    private Halfedge getHash(int b){
        Halfedge he;

        if(b < 0 || b >= ELhashsize){
            return (null);
        }
        he = ELhash[b];
        if(he == null || !he.deleted){
            return (he);
        }

        /* Hash table points to deleted half edge. Patch as necessary. */
        ELhash[b] = null;
        return (null);
    }

    private void clipLine(Edge e){
        float pxmin, pxmax, pymin, pymax;
        Site s1, s2;
        float x1 = 0, x2 = 0, y1 = 0, y2 = 0;

        x1 = e.reg[0].coord.x;
        x2 = e.reg[1].coord.x;
        y1 = e.reg[0].coord.y;
        y2 = e.reg[1].coord.y;

        // if the distance between the two points this line was created from is
        // less than the square root of 2, then ignore it
        if(Math.sqrt(((x2 - x1) * (x2 - x1)) + ((y2 - y1) * (y2 - y1))) < minDistanceBetweenSites){
            return;
        }
        pxmin = borderMinX;
        pxmax = borderMaxX;
        pymin = borderMinY;
        pymax = borderMaxY;

        if(e.a == 1.0 && e.b >= 0.0){
            s1 = e.ep[1];
            s2 = e.ep[0];
        }else{
            s1 = e.ep[0];
            s2 = e.ep[1];
        }

        if(e.a == 1.0){
            y1 = pymin;
            if(s1 != null && s1.coord.y > pymin){
                y1 = s1.coord.y;
            }
            if(y1 > pymax){
                y1 = pymax;
            }
            x1 = e.c - e.b * y1;
            y2 = pymax;
            if(s2 != null && s2.coord.y < pymax){
                y2 = s2.coord.y;
            }

            if(y2 < pymin){
                y2 = pymin;
            }
            x2 = (e.c) - (e.b) * y2;
            if(((x1 > pxmax) & (x2 > pxmax)) | ((x1 < pxmin) & (x2 < pxmin))){
                return;
            }
            if(x1 > pxmax){
                x1 = pxmax;
                y1 = (e.c - x1) / e.b;
            }
            if(x1 < pxmin){
                x1 = pxmin;
                y1 = (e.c - x1) / e.b;
            }
            if(x2 > pxmax){
                x2 = pxmax;
                y2 = (e.c - x2) / e.b;
            }
            if(x2 < pxmin){
                x2 = pxmin;
                y2 = (e.c - x2) / e.b;
            }
        }else{
            x1 = pxmin;
            if(s1 != null && s1.coord.x > pxmin){
                x1 = s1.coord.x;
            }
            if(x1 > pxmax){
                x1 = pxmax;
            }
            y1 = e.c - e.a * x1;
            x2 = pxmax;
            if(s2 != null && s2.coord.x < pxmax){
                x2 = s2.coord.x;
            }
            if(x2 < pxmin){
                x2 = pxmin;
            }
            y2 = e.c - e.a * x2;
            if(((y1 > pymax) & (y2 > pymax)) | ((y1 < pymin) & (y2 < pymin))){
                return;
            }
            if(y1 > pymax){
                y1 = pymax;
                x1 = (e.c - y1) / e.a;
            }
            if(y1 < pymin){
                y1 = pymin;
                x1 = (e.c - y1) / e.a;
            }
            if(y2 > pymax){
                y2 = pymax;
                x2 = (e.c - y2) / e.a;
            }
            if(y2 < pymin){
                y2 = pymin;
                x2 = (e.c - y2) / e.a;
            }
        }

        GraphEdge newEdge = new GraphEdge();
        allEdges.add(newEdge);
        newEdge.x1 = x1;
        newEdge.y1 = y1;
        newEdge.x2 = x2;
        newEdge.y2 = y2;

        newEdge.site1 = e.reg[0].sitenbr;
        newEdge.site2 = e.reg[1].sitenbr;
    }

    private void endpoint(Edge e, int lr, Site s){
        e.ep[lr] = s;
        if(e.ep[RE - lr] == null){
            return;
        }
        clipLine(e);
    }

    private boolean right(Halfedge el, Vec2 p){
        Edge e = el.ELedge;
        Site topsite = e.reg[1];
        boolean rightOf = p.x > topsite.coord.x;
        if(rightOf && el.ELpm == LE){
            return true;
        }
        if(!rightOf && el.ELpm == RE){
            return false;
        }

        boolean above;
        if(e.a == 1.0){
            float dyp = p.y - topsite.coord.y;
            float dxp = p.x - topsite.coord.x;
            boolean fast = false;
            if((!rightOf & (e.b < 0.0)) | (rightOf & (e.b >= 0.0))){
                above = dyp >= e.b * dxp;
                fast = above;
            }else{
                above = p.x + p.y * e.b > e.c;
                if(e.b < 0.0){
                    above = !above;
                }
                if(!above){
                    fast = true;
                }
            }
            if(!fast){
                float dxs = topsite.coord.x - (e.reg[0]).coord.x;
                above = e.b * (dxp * dxp - dyp * dyp) < dxs * dyp
                * (1.0 + 2.0 * dxp / dxs + e.b * e.b);
                if(e.b < 0.0){
                    above = !above;
                }
            }
        }else{
            float yl = e.c - e.a * p.x;
            float t1 = p.y - yl;
            float t2 = p.x - topsite.coord.x;
            float t3 = yl - topsite.coord.y;
            above = t1 * t1 > t2 * t2 + t3 * t3;
        }
        return ((el.ELpm == LE) == above);
    }

    private Site rightreg(Halfedge he){
        if(he.ELedge == null) return bottomsite;

        return (he.ELpm == LE ? he.ELedge.reg[RE] : he.ELedge.reg[LE]);
    }

    private Site intersect(Halfedge el1, Halfedge el2){
        Edge e1, e2, e;
        Halfedge el;
        float d, xint, yint;
        boolean right_of_site;
        Site v;

        e1 = el1.ELedge;
        e2 = el2.ELedge;
        if(e1 == null || e2 == null){
            return null;
        }

        if(e1.reg[1] == e2.reg[1]){
            return null;
        }

        d = e1.a * e2.b - e1.b * e2.a;
        if(-1.0e-10 < d && d < 1.0e-10){
            return null;
        }

        xint = (e1.c * e2.b - e2.c * e1.b) / d;
        yint = (e2.c * e1.a - e1.c * e2.a) / d;

        if((e1.reg[1].coord.y < e2.reg[1].coord.y)
        || (e1.reg[1].coord.y == e2.reg[1].coord.y && e1.reg[1].coord.x < e2.reg[1].coord.x)){
            el = el1;
            e = e1;
        }else{
            el = el2;
            e = e2;
        }

        right_of_site = xint >= e.reg[1].coord.x;
        if((right_of_site && el.ELpm == LE)
        || (!right_of_site && el.ELpm == RE)){
            return null;
        }

        v = new Site();
        v.coord.x = xint;
        v.coord.y = yint;
        return (v);
    }

    static class Site{
        Vec2 coord = new Vec2();
        int sitenbr;
    }

    static class Halfedge{
        Halfedge ELleft, ELright;
        Edge ELedge;
        boolean deleted;
        int ELpm;
        Site vertex;
        float ystar;
        Halfedge PQnext;
    }

    public static class GraphEdge{
        public float x1, y1, x2, y2;

        public int site1, site2;
    }

    static class Edge{
        float a = 0, b = 0, c = 0;
        Site[] ep = new Site[2];
        Site[] reg = new Site[2];
        int edgenbr;
    }
}
