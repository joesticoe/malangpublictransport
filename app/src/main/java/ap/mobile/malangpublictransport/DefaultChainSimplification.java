package ap.mobile.malangpublictransport;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import ap.mobile.malangpublictransport.base.PointTransport;

public class DefaultChainSimplification extends ChainSimplification {

    public DefaultChainSimplification(Set<PointTransport> graphPoints) {
        this.graphPoints = graphPoints;
        this.chainCandidates = new LinkedList<>();
    }

    public void getChainHeadCandidates() {

        // Finding chain candidates...
        for (PointTransport pointTransport : this.graphPoints) {

            if(this.stops.contains(pointTransport)) continue;

            if (pointTransport.inMiddleOfChain()) {
                Set<PointTransport> prevSet = pointTransport.getPreviousTransportPoints().keySet();
                PointTransport prev = prevSet.iterator().next();
                if(!prev.inMiddleOfChain() || !this.stops.contains(prev))
                    this.chainCandidates.add(pointTransport);
            }
        }

    }

    public void doSimplify() {

        // creating subChains...
        while (chainCandidates.peek() != null) {
            PointTransport head, tail;
            PointTransport observedPoint = chainCandidates.remove();

            head = observedPoint.getPreviousTransportPoints().keySet().iterator().next();

            // detach from head
            PointTransport.TransportCost costToChain
                    = head.getAdjacentTransportPoints().get(observedPoint);
            head.getAdjacentTransportPoints().remove(observedPoint);

            LinkedHashMap<PointTransport, PointTransport.TransportCost> subChain
                    = new LinkedHashMap<>();

            // add current observed point as the beginning of the subChain
            subChain.put(observedPoint, costToChain);

            do {

                // remove detached observed point from graph
                graphPoints.remove(observedPoint);

                // get next point in chain
                Map.Entry<PointTransport, PointTransport.TransportCost> next = this.follow(observedPoint);
                if (!next.getKey().inMiddleOfChain() || this.stops.contains(next.getKey())) { // found tail
                    tail = next.getKey();
                    subChain.put(tail, next.getValue());
                    break;
                }
                subChain.put(next.getKey(), next.getValue());
                observedPoint = next.getKey();

            } while (true);

            PointTransport.TransportCost subCost = new PointTransport.TransportCost();
            for (PointTransport.TransportCost entryCost : subChain.values()) {
                subCost.distance += entryCost.distance;
                subCost.price += entryCost.price;
            }

            head.addDestination(tail, subCost);
            head.addSubChain(tail, subChain);

        }

    }
}
