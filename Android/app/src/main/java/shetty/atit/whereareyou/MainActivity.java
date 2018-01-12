package shetty.atit.whereareyou;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private EditText hostNameEditText;
    private EditText usernameEditText;
    private Button startStopButton;
    private TextView finalResponseTextView;
    private LocationManager locationManager;
    private LocationListener listener;
    private String hostNameValue = "10.0.2.2:9000";
    private long start = Calendar.getInstance().getTimeInMillis();
    final Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hostNameEditText = (EditText) findViewById(R.id.hostEditText);
        hostNameEditText.setText(hostNameValue);

        usernameEditText = (EditText) findViewById(R.id.usernameText);

        finalResponseTextView = (TextView) findViewById(R.id.finalResponseTextView);

        startStopButton = (Button) findViewById(R.id.startButton);

        startStopButton.setBackgroundColor(Color.GREEN);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);


        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                setRequest(location);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        };

        setStartStopButtonProperty();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 1){
            setStartStopButtonProperty();
        }
    }

    void setStartStopButtonProperty() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}
                        , 1);

            return;
        }
        // this code won't execute IF permissions are not allowed, because in the line above there is return statement.
        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(usernameEditText == null || usernameEditText.getText().length() == 0){
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("You Missed Something");
                    builder
                            .setMessage("Please enter username")
                            .setCancelable(false)
                            .setPositiveButton("Ok",new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alertDialog = builder.create();

                    alertDialog.show();
                }else {

                    if (startStopButton.getText().equals("Start")) {

                        startStopButton.setText("Stop");
                        startStopButton.setBackgroundColor(Color.RED);
                        usernameEditText.setEnabled(false);
                        hostNameEditText.setEnabled(false);
                        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        start = Calendar.getInstance().getTimeInMillis();

                        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                            //noinspection MissingPermission
                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, listener);
                        }else{
                            //noinspection MissingPermission
                            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0, listener);
                        }

                    } else {

                        locationManager.removeUpdates(listener);
                        startStopButton.setText("Start");
                        startStopButton.setBackgroundColor(Color.GREEN);
                        usernameEditText.setEnabled(true);
                        hostNameEditText.setEnabled(true);
                        getDistance();
                    }
                }
            }
        });
    }

    public void setRequest(Location loc){
        RequestQueue reqQueue = Volley.newRequestQueue(this);
        String url = "http://"+hostNameEditText.getText()+"/locationupdates";
        JSONObject jObj = new JSONObject();

        try {
            jObj.put("username", usernameEditText.getText());
            jObj.put("latitude", loc.getLatitude());
            jObj.put("longitude", loc.getLongitude());
            jObj.put("timestamp", Calendar.getInstance().getTimeInMillis());
        }catch(Exception e){
            Log.e("Cannot create json",e.getMessage());
        }
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,url,jObj,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i("Successful request",response.toString());

                        //finalResponseTextView.append(response.toString());
                    }}, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("Unsuccessful request",error.getMessage());
                            finalResponseTextView.setText("Error:"+error.getMessage());
                            finalResponseTextView.setTextColor(Color.RED);
                        }
                    });

        reqQueue.add(request);
    }

    public void getDistance(){
        {
            RequestQueue reqQueue = Volley.newRequestQueue(this);
            String url = "http://"+hostNameEditText.getText()+"/requestresult";
            JSONObject jObj = new JSONObject();

            try {
                jObj.put("timestamp", start);
                jObj.put("username", usernameEditText.getText());
            }catch(Exception e){
                Log.e("Cannot create json",e.getMessage());
            }
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,url,jObj,
                    new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                finalResponseTextView.setText(response.get("result").toString());
                            }catch(JSONException e){
                                Log.e("Error",e.getMessage());
                            }
                        }}, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    finalResponseTextView.setText(error.getMessage());
                    finalResponseTextView.setTextColor(Color.RED);
                }
            });

            reqQueue.add(request);
        }
    }
}