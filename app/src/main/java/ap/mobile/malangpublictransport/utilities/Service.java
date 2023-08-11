package ap.mobile.malangpublictransport.utilities;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import ap.mobile.malangpublictransport.MapsActivity;
import ap.mobile.malangpublictransport.R;
import ap.mobile.malangpublictransport.base.Interchange;
import ap.mobile.malangpublictransport.base.Line;
import ap.mobile.malangpublictransport.base.PointTransport;

/**
 * Created by Aryo on 29/08/2017.
 */

public class Service {

    public interface IServiceInterface {
        void onPointsObtained(ArrayList<Line> lines, ArrayList<Interchange> interchanges);
        void onPointsRequestError(String error);
    }


    public static void getManagedPoints(Context context, final IServiceInterface callback) {
        String rawManagedPointsJson = readRawString(context, R.raw.managedpoints);

        String link = "http://amwgr.com/managedpoints.json";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, link,
                new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        ArrayList<Line> lines = new ArrayList<>();
                        JSONObject json = new JSONObject(response);
                        JSONArray lineJSON = json.getJSONArray("lines");
                        Log.d("line name", String.valueOf(lineJSON));
                        for (int i = 0; i < lineJSON.length(); i++) {
                            JSONObject lineArray = lineJSON.getJSONObject(i);
                            Line line = new Line();
                            line.id = Integer.valueOf(lineArray.getString("idline"));
                            line.name = lineArray.getString("name");
                            line.direction = lineArray.getString("direction");
                            line.color = Color.parseColor(lineArray.getString("color"));


                            JSONArray pathArray = lineArray.getJSONArray("path");


                            LinkedList<PointTransport> path = new LinkedList<>();

                            for (int j = 0; j < pathArray.length(); j++) {
                                JSONObject pointJson = pathArray.getJSONObject(j);

                                PointTransport point = new PointTransport(
                                        pointJson.getString("idpoint"),
                                        Double.valueOf(pointJson.getString("lat")),
                                        Double.valueOf(pointJson.getString("lng")),
                                        pointJson.getString("stop").equals("1"),
                                        line.id,
                                        line.name,
                                        line.direction,
                                        "#" + Integer.toHexString(line.color),
                                        Integer.valueOf(pointJson.getString("sequence")),
                                        null,
                                        null
                                );
                                path.add(point);
                            }
                            line.path = path;
                            lines.add(line);
                        }
                        ArrayList<Interchange> interchanges = new ArrayList<>();

                        JSONArray interArray = json.getJSONArray("interchanges");

                        for (int h = 0; h < interArray.length(); h++) {

                            JSONObject interchangeJson = interArray.getJSONObject(h);

                            Interchange interchange = new Interchange();
                            interchange.idInterchange = interchangeJson.getString("idinterchange");
                            interchange.name = interchangeJson.getString("name");

                            Set<String> pointIds = new HashSet<>();

                            JSONArray pointsJson = interchangeJson.getJSONArray("points");

                            for (int k = 0; k < pointsJson.length(); k++) {
                                JSONObject pointJson = pointsJson.getJSONObject(k);
                                PointTransport point = new PointTransport();
                                point.id = pointJson.getString("idpoint");
                                pointIds.add(point.id);
                            }

                            interchange.pointIds.addAll(pointIds);
                            interchanges.add(interchange);
                        }
                        if (callback != null)
                            callback.onPointsObtained(lines, interchanges);
                    } catch (JSONException e){
                        e.printStackTrace();
                        if(callback!= null)
                            callback.onPointsRequestError(e.getMessage());
                    }
                }
            }, volleyError -> Toast.makeText(
                        context,
                        volleyError.getMessage(),
                        Toast.LENGTH_SHORT)
                .show());
            RequestQueue requestQueue = Volley.newRequestQueue(context);
            requestQueue.add(stringRequest);



//        try {
//            JSONObject response = new JSONObject(rawManagedPointsJson);
//            JSONArray linesJson = response.getJSONArray("lines");
//
//            ArrayList<Line> lines = new ArrayList<>();
//
//            for(int i=0; i<linesJson.length();i++) {
//                JSONObject lineJson = linesJson.getJSONObject(i);
//                /* "idline": "1", "name": "AL", "direction": "O", "color": "#FF0000", "path": [] */
//                Line line = new Line();
//                line.id = Integer.valueOf(lineJson.getString("idline"));
//                line.name = lineJson.getString("name");
//                line.direction = lineJson.getString("direction");
//                line.color = Color.parseColor(lineJson.getString("color"));
//
//                JSONArray pathJson = lineJson.getJSONArray("path");
//                Log.d("line name", line.name);
//
//                LinkedList<PointTransport> path = new LinkedList<>();
//
//                for(int j = 0; j<pathJson.length(); j++) {
//                    JSONObject pointJson = pathJson.getJSONObject(j);
//                    /*
//                        String id, double lat, double lng, boolean stop, int idLine,
//                        String lineName, String direction, String color, int sequence,
//                        String adjacentPoints, String interchanges
//                     */
//                    PointTransport point = new PointTransport(
//                            pointJson.getString("idpoint"),
//                            Double.valueOf(pointJson.getString("lat")),
//                            Double.valueOf(pointJson.getString("lng")),
//                            pointJson.getString("stop").equals("1"),
//                            line.id,
//                            line.name,
//                            line.direction,
//                            "#"+Integer.toHexString(line.color),
//                            Integer.valueOf(pointJson.getString("sequence")),
//                            null,
//                            null
//                    );
//                    path.add(point);
//                }
//                line.path = path;
//                lines.add(line);
//
//            }
//
//            ArrayList<Interchange> interchanges = new ArrayList<>();
//
//            JSONArray interchangesJson = response.getJSONArray("interchanges");
//
//            for(int i = 0; i<interchangesJson.length(); i++) {
//
//                JSONObject interchangeJson = interchangesJson.getJSONObject(i);
//
//                Interchange interchange = new Interchange();
//                interchange.idInterchange = interchangeJson.getString("idinterchange");
//                interchange.name = interchangeJson.getString("name");
//
//                Set<String> pointIds = new HashSet<>();
//
//                JSONArray pointsJson = interchangeJson.getJSONArray("points");
//
//                for(int j=0; j<pointsJson.length(); j++) {
//                    JSONObject pointJson = pointsJson.getJSONObject(j);
//                    PointTransport point = new PointTransport();
//                    /*
//                    "idline": "1",
//                    "idpoint": "717",
//                    "sequence": "281",
//                    "stop": "1",
//                    "idinterchange": "4"
//                     */
//                    point.id = pointJson.getString("idpoint");
//                    pointIds.add(point.id);
//                }
//
//                interchange.pointIds.addAll(pointIds);
//                interchanges.add(interchange);
//
//            }
//
//            if(callback!=null)
//                callback.onPointsObtained(lines, interchanges);
//        } catch (JSONException e) {
//            e.printStackTrace();
//            if(callback!= null)
//                callback.onPointsRequestError(e.getMessage());
//        }

    }

    private static String readRawString(Context context, int rawResourceId) {
        InputStream inputStream = context.getResources().openRawResource(rawResourceId);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            int i = inputStream.read();
            while (i != -1) {
                byteArrayOutputStream.write(i);
                i = inputStream.read();
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteArrayOutputStream.toString();
    }

}

    /*
    public static void getPoints(Context context, final IServiceInterface callback) {
        InputStream inputStream = context.getResources().openRawResource(R.raw.points);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try {
            int i = inputStream.read();
            while (i != -1) {
                byteArrayOutputStream.write(i);
                i = inputStream.read();
            }
            inputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        String json = byteArrayOutputStream.toString();

        ArrayList<PointTransport> points = new ArrayList<>();

        // Display the first 500 characters of the response string.
        //mTextView.setText("Response is: "+ response.substring(0,500));
        //Toast.makeText(MapsActivity.this, "Response is: "+ response.substring(0,100), Toast.LENGTH_SHORT).show();
        try {
            JSONArray response = new JSONArray(json);
            //{"id":"637","lat":"-7.9346600068216","lng":"112.65868753195","l":"1","n":"AL","d":"O","s":"0","a":"1526","i":null}
            for(int i=0;i<response.length();i++) {
                JSONObject jsonPoint = response.getJSONObject(i);
                String id = jsonPoint.getString("id");
                String lat = jsonPoint.getString("lat");
                String lng = jsonPoint.getString("lng");
                String stop = jsonPoint.getString("st");
                String idLine = jsonPoint.getString("l");
                String lineName = jsonPoint.getString("n");
                String direction = jsonPoint.getString("d");
                String color = jsonPoint.getString("c");
                String sequence = jsonPoint.getString("s");
                String adjacentPoints = jsonPoint.getString("a");
                String interchanges = jsonPoint.getString("i");

                if(adjacentPoints.equals("null")) adjacentPoints = null;
                if(interchanges.equals("null")) interchanges = null;

                PointTransport point = new PointTransport(id, Double.valueOf(lat), Double.valueOf(lng), Boolean.valueOf(stop),
                        Integer.valueOf(idLine), lineName, direction, color,
                        Integer.valueOf(sequence), adjacentPoints, interchanges);
                points.add(point);
            }
            if(callback!=null)
                callback.onPointsObtained(points);
        } catch (JSONException e) {
            e.printStackTrace();
            if(callback!= null)
                callback.onPointsRequestError(e.getMessage());
        }

    }
    public static void getPoints(Context context, RequestQueue requestQueue, final IServiceInterface callback) {
        String url = CDM.getApiBaseUrl(context) + "/get-points-json.php";

        JsonArrayRequest pointsJsonRequest = new JsonArrayRequest(Request.Method.GET, url, null,
            new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {

                    ArrayList<PointTransport> points = new ArrayList<>();

                    // Display the first 500 characters of the response string.
                    //mTextView.setText("Response is: "+ response.substring(0,500));
                    //Toast.makeText(MapsActivity.this, "Response is: "+ response.substring(0,100), Toast.LENGTH_SHORT).show();
                    try {
                        //{"id":"637","lat":"-7.9346600068216","lng":"112.65868753195","l":"1","n":"AL","d":"O","s":"0","a":"1526","i":null}
                        for(int i=0;i<response.length();i++) {
                            JSONObject jsonPoint = response.getJSONObject(i);
                            String id = jsonPoint.getString("id");
                            String lat = jsonPoint.getString("lat");
                            String lng = jsonPoint.getString("lng");
                            String stop = jsonPoint.getString("st");
                            String idLine = jsonPoint.getString("l");
                            String lineName = jsonPoint.getString("n");
                            String direction = jsonPoint.getString("d");
                            String color = jsonPoint.getString("c");
                            String sequence = jsonPoint.getString("s");
                            String adjacentPoints = jsonPoint.getString("a");
                            String interchanges = jsonPoint.getString("i");

                            if(adjacentPoints.equals("null")) adjacentPoints = null;
                            if(interchanges.equals("null")) interchanges = null;

                            PointTransport point = new PointTransport(id, Double.valueOf(lat), Double.valueOf(lng), Boolean.valueOf(stop),
                                    Integer.valueOf(idLine), lineName, direction, color,
                                    Integer.valueOf(sequence), adjacentPoints, interchanges);
                            points.add(point);
                        }
                        if(callback!=null)
                            callback.onPointsObtained(points);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        if(callback!= null)
                            callback.onPointsRequestError(e.getMessage());
                    }

                }
            }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(callback!= null)
                    callback.onPointsRequestError(error.getMessage());
            }
        });
        requestQueue.add(pointsJsonRequest);
    }
    */