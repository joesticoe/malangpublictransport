package ap.mobile.malangpublictransport;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import ap.mobile.malangpublictransport.base.PointTransport;

public abstract class ChainSimplification {

    protected Queue<PointTransport> chainCandidates = new LinkedList<>();
    protected Set<PointTransport> graphPoints;
    protected Set<PointTransport> stops = new HashSet<>();

    public abstract void getChainHeadCandidates();
    public abstract void doSimplify();

    protected Map.Entry<PointTransport, PointTransport.TransportCost> follow(PointTransport observedPoint) {
        // found beginning of chain, follow
        Map<PointTransport, PointTransport.TransportCost> adjacentTransportPoints = observedPoint.getAdjacentTransportPoints();
        Map.Entry<PointTransport, PointTransport.TransportCost> next = adjacentTransportPoints.entrySet().iterator().next();
        return next;
    }

    public void addStop(PointTransport stop) {
        this.stops.add(stop);
    }

    public void addStops(Set<PointTransport> stops) {
        this.stops.addAll(stops);
    }

    public static Set<PointTransport> simplify(Set<PointTransport> graphPoints, Set<PointTransport> stops) {
        ChainSimplification cs = new DefaultChainSimplification(graphPoints);
        cs.addStops(stops);
        cs.getChainHeadCandidates();
        cs.doSimplify();
        return graphPoints;
    }

}
