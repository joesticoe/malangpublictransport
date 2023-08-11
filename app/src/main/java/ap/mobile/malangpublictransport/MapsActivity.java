package ap.mobile.malangpublictransport;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Debug;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.transition.TransitionManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ap.mobile.malangpublictransport.base.GraphTransport;
import ap.mobile.malangpublictransport.base.Interchange;
import ap.mobile.malangpublictransport.base.Line;
import ap.mobile.malangpublictransport.base.PointTransport;
import ap.mobile.malangpublictransport.base.RouteTransport;
import ap.mobile.malangpublictransport.details.Itinerary;
import ap.mobile.malangpublictransport.details.ItineraryAdapter;
import ap.mobile.malangpublictransport.dijkstra.DijkstraTask;
import ap.mobile.malangpublictransport.dijkstra.DijkstraTransport;
import ap.mobile.malangpublictransport.utilities.CDM;
import ap.mobile.malangpublictransport.utilities.Helper;
import ap.mobile.malangpublictransport.utilities.MapUtilities;
import ap.mobile.malangpublictransport.utilities.Service;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener,
        View.OnClickListener, Service.IServiceInterface, GraphTask.IGraphTask,
        OnSuccessListener<Location>,
        RouteAdapter.RouteAdapterItemClickListener,
        DijkstraTask.DijkstraTaskListener, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private GraphTransport graph;

    private LatLng userLocation;

    private Set<Polyline> polylines = new HashSet<>();
    private Set<Marker> markers     = new HashSet<>();

    private ArrayList<Marker> sourceMarkers = new ArrayList<>();
    private ArrayList<Marker> destinationMarkers = new ArrayList<>();
    private ArrayList<RouteTransport> routeTransports = new ArrayList<>();
    private MaterialDialog resultDialog;
    private View selectedRouteCardContainer;
    private Marker markerSource, markerDestination; // user location, and clicked destination
    private Marker startMarker, endMarker; // start marker of route, end marker of route
    private RouteTransport selectedRouteTransport;
    private ItineraryAdapter itineraryAdapter;
    private MaterialDialog dijkstraDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        CDM.cost = Double.valueOf(
                PreferenceManager.getDefaultSharedPreferences(this)
                        .getString("pref_cost", String.valueOf(CDM.cost)));


        setContentView(R.layout.activity_maps);
        this.selectedRouteCardContainer = this.findViewById(R.id.card_container);
        this.selectedRouteCardContainer.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
        this.selectedRouteCardContainer.findViewById(R.id.bt_show_route_detail).setOnClickListener(this);

        ((SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map))
                .getMapAsync(this);

        this.mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


    }

    private void showMemoryInfo() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        // Memory information in bytes
        long totalMemory = memoryInfo.totalMem;
        long usedMemory = memoryInfo.totalMem - memoryInfo.availMem;
        long availableMemory = memoryInfo.availMem;

        // Convert bytes to megabytes
        long totalMemoryMB = totalMemory / (1024 * 1024);
        long usedMemoryMB = usedMemory / (1024 * 1024);
        long availableMemoryMB = availableMemory / (1024 * 1024);

        // Display the memory information
        String memoryInfoStr = "Total Memory: " + totalMemoryMB + " MB\n"
                + "Used Memory: " + usedMemoryMB + " MB\n"
                + "Available Memory: " + availableMemoryMB + " MB";

        Log.d("MemoryInfo", memoryInfoStr);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.action_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                this.startActivity(i);
                break;
            case R.id.action_recalculate:
                if (this.markerDestination != null &&
                        this.markerSource != null &&
                        this.graph != null)
                    this.onMapLongClick(this.markerDestination.getPosition());
                else {
                    if(this.markerDestination == null) {
                        new MaterialDialog.Builder(this)
                                .content("Can not find route.\nDestination location has not been set.\nSet destination by long-clicking a location on the map.")
                                .title("Information")
                                .positiveText("OK")
                                .show();
                        return true;
                    } else if(this.markerSource == null) {
                        Snackbar.make(findViewById(android.R.id.content),
                                "Sorry, your current location has not yet be found",
                                Snackbar.LENGTH_LONG).show();
                        return true;
                    }
                    return false;
                }
                break;
        }
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.mMap = googleMap;

        this.mMap.setOnMapLongClickListener(this);
        this.mMap.setOnInfoWindowClickListener(this);
        this.mMap.setOnMarkerClickListener(this);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, 99);

            return;
        }
        this.moveToCurrentLocation();

        Log.d("lokasi awal", "");
//start disini
        long waktuMulai = System.currentTimeMillis();

// Perform the task for which you want to measure the time here.

        // load graph raw data
        Service.getManagedPoints(this, this);
//end disini
        long waktuSelesai = System.currentTimeMillis();
        long waktu = waktuSelesai - waktuMulai;
        Log.d("Time", "waktu: " + waktu + " milliseconds");
        Toast.makeText(this, waktu +" milliseconds", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 99) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED)
                this.moveToCurrentLocation();
        }
    }

    private void moveToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            this.mFusedLocationClient.getLastLocation().addOnSuccessListener(this);
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {

        showMemoryInfo();
        LatLng latlong = new LatLng(-7.949296561786135,112.61467047035694);

        Log.d("graph", String.valueOf(graph));
        if (this.graph == null) {
            Toast.makeText(this, "Graph is not ready yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (this.markerDestination != null) this.markerDestination.remove();
        Log.d("lokasi destinasi", String.valueOf(this.markerDestination));
        Log.d("lokasi latlong2", String.valueOf(latLng));
        //new LatLng(-7.949296561786135,112.61467047035694);

        this.markerDestination = MapUtilities.drawMarker(
                this.mMap, latLng, BitmapDescriptorFactory.HUE_GREEN, "Destination", "Tap to show route\nto this location");

        DijkstraTransport.Priority priority = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("pref_priority", true) ?
                DijkstraTransport.Priority.COST :
                DijkstraTransport.Priority.DISTANCE;

        DijkstraTask dijkstraTask = new DijkstraTask(
                this.graph,
                this.userLocation,
                latLng,
                priority,
                CDM.getDistance(this),
                this);
        dijkstraTask.execute();


        this.dijkstraDialog = new MaterialDialog.Builder(this)
                .content("Calculating routes...")
                .progress(false, 0, true)
                .show();

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if(marker.getPosition().latitude == markerDestination.getPosition().latitude &&
                marker.getPosition().longitude == markerDestination.getPosition().longitude) {
            if(markerDestination.isInfoWindowShown()) {
                if (this.routeTransports != null) {
                    this.onDijkstraComplete(this.routeTransports);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if(marker.getPosition().latitude == markerDestination.getPosition().latitude &&
                marker.getPosition().longitude == markerDestination.getPosition().longitude) {
            if(this.routeTransports != null) {
                this.onDijkstraComplete(this.routeTransports);
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.selected_route:
                if(!this.resultDialog.isShowing()) {
                    this.resultDialog.show();
                    ScrollView cardContainer = this.findViewById(R.id.route_detail_container);
                    cardContainer.removeAllViews();

                    TransitionManager.beginDelayedTransition((ViewGroup) this.selectedRouteCardContainer.getParent());
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) this.selectedRouteCardContainer.getLayoutParams();
                    lp.gravity = Gravity.BOTTOM | Gravity.CENTER;
                    this.selectedRouteCardContainer.setLayoutParams(lp);

                }
                break;
            case R.id.bt_show_route_detail:

                TransitionManager.beginDelayedTransition((ViewGroup) this.selectedRouteCardContainer.getParent());
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) this.selectedRouteCardContainer.getLayoutParams();
                lp.gravity = Gravity.TOP | Gravity.CENTER;
                this.selectedRouteCardContainer.setLayoutParams(lp);

                ScrollView cardContainer = this.findViewById(R.id.route_detail_container);

                // toggle detail if already displayed
                if(cardContainer.findViewById(R.id.rvItinerary) != null) {
                    this.itineraryAdapter.setSteps(new ArrayList<Itinerary.Step>());
                    this.itineraryAdapter.notifyDataSetChanged();
                    cardContainer.removeAllViews();

                    TransitionManager.beginDelayedTransition((ViewGroup) this.selectedRouteCardContainer.getParent());
                    lp = (FrameLayout.LayoutParams) this.selectedRouteCardContainer.getLayoutParams();
                    lp.gravity = Gravity.BOTTOM | Gravity.CENTER;
                    this.selectedRouteCardContainer.setLayoutParams(lp);

                    return;
                }

                Itinerary itineraries = new Itinerary(
                        this.markerSource.getPosition(),
                        this.markerDestination.getPosition(),
                        this.selectedRouteTransport);

                itineraries.processItineraries();

                if(this.itineraryAdapter == null)
                    this.itineraryAdapter = new ItineraryAdapter(itineraries.getSteps());
                else this.itineraryAdapter.setSteps(itineraries.getSteps());

                @SuppressLint("InflateParams")
                View itineraryView = LayoutInflater.from(this).inflate(R.layout.fragment_route_detail, null, false);

                RecyclerView rvItinerary = itineraryView.findViewById(R.id.rvItinerary);
                rvItinerary.setLayoutManager(new LinearLayoutManager(this));
                rvItinerary.setAdapter(this.itineraryAdapter);


                cardContainer.removeAllViews();
                cardContainer.addView(itineraryView);

                break;
        }
    }

    @Override
    public void onPointsObtained(ArrayList<Line> lines, ArrayList<Interchange> interchanges) {
        new GraphTask(lines, interchanges, this).execute();
    }

    @Override
    public void onPointsRequestError(String error) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onGraphGenerated(Set<PointTransport> points) {
        this.graph = new GraphTransport();
        this.graph.setTransportPoints(points);
        Toast.makeText(this, "Graph generated from " + points.size() + " points,",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSuccess(Location location) {
        if(location == null) return;
        this.userLocation = new LatLng(location.getLatitude(), location.getLongitude());
        Log.d("lokasi latlong", String.valueOf(userLocation));
        this.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f));
        if(this.markerSource != null) this.markerSource.remove();
        this.markerSource = this.mMap.addMarker(new MarkerOptions()
                .position(this.userLocation)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        Log.d("lokasi baru", String.valueOf(this.markerSource));
    }

    @Override
    public void onItemClick(RouteTransport routeTransport) {

        this.selectedRouteTransport = routeTransport;

        if(this.polylines.size() > 0) for (Polyline p : this.polylines) p.remove();
        if(this.markers.size() > 0) for(Marker m: this.markers) m.remove();

        if(this.resultDialog != null && this.resultDialog.isShowing())
            this.resultDialog.dismiss();

        this.showSelectedRoute(routeTransport);
        this.drawPath(routeTransport.getPath());

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (PointTransport point: routeTransport.getPath())
            builder.include(point.getLatLng());

        LatLngBounds bounds = builder.build();

        int padding = Helper.toPx(128); // offset from edges of the map in pixels
        this.mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));

        Animation fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        this.selectedRouteCardContainer.startAnimation(fadeInAnimation);

    }

    private void showSelectedRoute(RouteTransport routeTransport) {
        TextView name = (this.selectedRouteCardContainer.findViewById(R.id.route_name));
        TextView distance = (this.selectedRouteCardContainer.findViewById(R.id.route_distance));
        TextView price = (this.selectedRouteCardContainer.findViewById(R.id.route_price));

        this.selectedRouteCardContainer.findViewById(R.id.selected_route).setOnClickListener(this);

        name.setText(routeTransport.getNames());
        distance.setText(Helper.humanReadableDistane(routeTransport.getDistanceReadable()));
        String priceLabel = "Rp " + String.format(Locale.getDefault(), "%,.0f",
                routeTransport.getTotalPrice()).replace(",", ".");
        price.setText(priceLabel);

        if(this.startMarker != null) this.startMarker.remove();
        if(this.endMarker != null) this.endMarker.remove();

        this.startMarker = MapUtilities.drawInterchangeMarker(this.mMap, routeTransport.getSource().getLatLng());
        this.endMarker = MapUtilities.drawInterchangeMarker(this.mMap, routeTransport.getDestination().getLatLng());

    }

    private void drawPath(List<PointTransport> path) {

        PolylineOptions startWalkingPolylineOptions = MapUtilities.getWalkingPolylineOptions();
        startWalkingPolylineOptions.add(markerSource.getPosition()).add(this.startMarker.getPosition());
        this.polylines.add(this.mMap.addPolyline(startWalkingPolylineOptions));

        PolylineOptions polylineOptions = new PolylineOptions().width(10);

        PointTransport prevPoint = null;
        for (PointTransport currentPoint : path) {
            if (prevPoint == null)
                polylineOptions.color(currentPoint.getColor());

            if (prevPoint != null && currentPoint.getIdLine() != prevPoint.getIdLine()) {

                // finish the polyline
                Polyline route = this.mMap.addPolyline(polylineOptions);
                this.polylines.add(route);

                // draw interchange markers
                this.markers.add(MapUtilities.drawInterchangeMarker(this.mMap, prevPoint.getLatLng()));
                this.markers.add(MapUtilities.drawInterchangeMarker(this.mMap, currentPoint.getLatLng()));

                // draw interchange walking paths
                PolylineOptions transferWalkingPolylineOptions = MapUtilities.getWalkingPolylineOptions();
                transferWalkingPolylineOptions.add(prevPoint.getLatLng()).add(currentPoint.getLatLng());
                this.polylines.add(this.mMap.addPolyline(transferWalkingPolylineOptions));

                // start next line polyline
                polylineOptions = new PolylineOptions().width(10).color(currentPoint.getColor());
            }

            // add current point
            polylineOptions.add(new LatLng(currentPoint.lat(), currentPoint.lng()));
            prevPoint = currentPoint;
        }

        this.polylines.add(this.mMap.addPolyline(polylineOptions));

        assert prevPoint != null;
        this.markers.add(MapUtilities.drawInterchangeMarker(this.mMap, prevPoint.getLatLng()));

        PolylineOptions endWalkingPolylineOptions = MapUtilities.getWalkingPolylineOptions();
        endWalkingPolylineOptions.add(markerDestination.getPosition()).add(this.endMarker.getPosition());
        this.polylines.add(this.mMap.addPolyline(endWalkingPolylineOptions));

    }

    @Override
    public void onDijkstraProgress(DijkstraTask.DijkstraReport report) {
        if(this.dijkstraDialog != null && this.dijkstraDialog.isShowing()) {
            this.dijkstraDialog.setMaxProgress(report.total);
            this.dijkstraDialog.setProgress(report.progress);
        }
    }

    @Override
    public void onDijkstraComplete(ArrayList<RouteTransport> routes) {

        if (this.dijkstraDialog != null && this.dijkstraDialog.isShowing())
            this.dijkstraDialog.dismiss();

        this.routeTransports = routes;

        if (this.polylines.size() > 0) for (Polyline p : this.polylines) p.remove();
        if (this.markers.size() > 0) for (Marker m : this.markers) m.remove();

        for (Marker m : this.sourceMarkers) m.remove();
        for (Marker m : this.destinationMarkers) m.remove();

        this.sourceMarkers.clear();
        this.destinationMarkers.clear();

        if (this.startMarker != null) this.startMarker.remove();
        if (this.endMarker != null) this.endMarker.remove();

        RouteAdapter routeAdapter = new RouteAdapter(this.routeTransports, this);
      //  Log.d("start marker", String.valueOf(this.startMarker));
        //Log.d("end marker", String.valueOf(this.endMarker));
        if (this.routeTransports.size() > 0) {
            this.resultDialog = new MaterialDialog.Builder(this)
                    .title("Available Routes")
                    .adapter(routeAdapter, null)
                    .positiveText("OK")
                    .show();
        } else {
            Snackbar.make(findViewById(android.R.id.content),
                    "Sorry, no possible route could be found",
                    Snackbar.LENGTH_LONG).show();
        }

        if (this.selectedRouteCardContainer.getAlpha() > 0)
            this.selectedRouteCardContainer.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));

    }

    @Override
    public void onDijkstraError(Exception ex) {
        ex.printStackTrace();
        if(this.dijkstraDialog != null && this.dijkstraDialog.isShowing())
            this.dijkstraDialog.dismiss();
    }

}
