package ap.mobile.malangpublictransport.details;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ap.mobile.malangpublictransport.base.Line;
import ap.mobile.malangpublictransport.base.PointTransport;
import ap.mobile.malangpublictransport.base.RouteTransport;
import ap.mobile.malangpublictransport.utilities.CDM;
import ap.mobile.malangpublictransport.utilities.Helper;

public class Itinerary {


    private RouteTransport route;
    private LatLng destination;
    private LatLng source;

    private ArrayList<Step> steps = new ArrayList<>();

    public Itinerary(LatLng source, LatLng destination, RouteTransport selectedRouteTransport) {
        this.source = source;
        this.destination = destination;
        this.route = selectedRouteTransport;
    }

    public void processItineraries() {

        StartStep start = new StartStep(this.source);
        this.steps.add(start);

        List<PointTransport> path = this.route.getPath();

        WalkStep initialWalk;
        LineStep lineStep = null;
        PointTransport prevPoint = null;

        for (PointTransport currentPoint : path) {


            if (prevPoint == null) {
                initialWalk = new WalkStep(this.source, currentPoint.getLatLng());
                this.steps.add(initialWalk);

                lineStep = new LineStep();
                lineStep.setStartPoint(currentPoint);

                Line line = new Line(
                    currentPoint.idLine, currentPoint.getLineName(),
                    currentPoint.getColor(), currentPoint.getDirection()
                );
                line.cost = 4000D;

                lineStep.setLine(line);

                prevPoint = currentPoint;
                continue;
            }

            if (currentPoint.getIdLine() != prevPoint.getIdLine()) {

                lineStep.setEndPoint(prevPoint);
                this.steps.add(lineStep);

                WalkTransferStep walkTransferStep = new WalkTransferStep(prevPoint.getLatLng(), currentPoint.getLatLng());
                this.steps.add(walkTransferStep);

                lineStep = new LineStep();
                lineStep.setStartPoint(currentPoint);
                Line line = new Line(
                        currentPoint.idLine, currentPoint.getLineName(),
                        currentPoint.getColor(), currentPoint.getDirection()
                );
                line.cost = 4000D;

                lineStep.setLine(line);

            }

            lineStep.addDistance(prevPoint, currentPoint);

            prevPoint = currentPoint;
        }

        assert lineStep != null;
        lineStep.setEndPoint(prevPoint);
        this.steps.add(lineStep);

        WalkStep walkEndStep = new WalkStep(prevPoint.getLatLng(), this.destination);
        this.steps.add(walkEndStep);

        EndStep endStep = new EndStep(this.destination);
        this.steps.add(endStep);

    }

    public class Step {

    }

    public class StartStep extends Step {

        private final LatLng location;

        StartStep(LatLng location) {
            this.location = location;
        }

        public String getLabel() {
            return "Your location";
        }
        public String getLocation() {
            return this.location.latitude + ", " + this.location.longitude;
        }
    }

    public class WalkStep extends Step {

        private final double distance;

        WalkStep(LatLng source, LatLng destination) {
            this.distance = Helper.calculateDistance(source, destination);
        }

        public String getLabel() {
            int meter = (int) (this.distance / CDM.oneMeterInDegree());
            return "Walk " + meter + " m "
                    + String.format(Locale.getDefault(), "(%.0f minutes)", Math.ceil(meter/80f));
        }

    }

    public class LineStep extends Step {

        private PointTransport startPoint;
        private PointTransport endPoint;
        private Line line;

        void setStartPoint(PointTransport startPoint) {
            this.startPoint = startPoint;
        }

        void setEndPoint(PointTransport endPoint) {
            this.endPoint = endPoint;
        }

        void setLine(Line line) {
            this.line = line;
        }

        void addDistance(PointTransport prevPoint, PointTransport currentPoint) {
            this.line.distance += Helper.calculateDistance(prevPoint, currentPoint);
        }

        public String getStartPoint() {
            return "Stop #" + this.startPoint.id;
        }

        public String getLineName() {
            return this.line.name + " to " +
                    (this.line.direction.equals("Inbound") ?
                            this.line.name.substring(0,1) :
                            this.line.name.substring(this.line.name.length()-1, this.line.name.length()));
        }

        public String getDistance() {
            return "Ride for " + Helper.humanReadableDistane(this.line.distance/CDM.oneMeterInDegree());
        }

        public String getPrice() {
            return String.format(Locale.getDefault(), "Rp %,.0f", this.line.cost).replace(",", ".");
        }

        public String getEndPoint() {
            return "Stop #" + this.endPoint.id;
        }

        public int getColor() {
            return this.line.color;
        }
    }

    public class WalkTransferStep extends Step {

        private final double distance;

        WalkTransferStep(LatLng source, LatLng destination) {
            this.distance = Helper.calculateDistance(source, destination);
        }

        public String getLabel() {
            int meter = (int) (this.distance / CDM.oneMeterInDegree());
            return "Transfer walk " + meter + " m "
                    + String.format(Locale.getDefault(), "(%.0f minutes)", Math.ceil(meter/80f));
        }

    }

    public ArrayList<Step> getSteps() {
        return this.steps;
    }

    public class EndStep extends Step {

        private final LatLng location;

        EndStep(LatLng location) {
            this.location = location;
        }

        public String getLabel() {
            return "Destination";
        }
        public String getLocation() {
            return this.location.latitude + ", " + this.location.longitude;
        }

    }
}
