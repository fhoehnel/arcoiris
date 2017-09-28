package android.webfilesys.de.webfilesysblog;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.location.Location;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends ActionBarActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DatePickerDialog.OnDateSetListener {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_EXTERNAL_STORAGE = 1;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_INTERNET = 3;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_LOCATION = 4;

    private static final int PLACE_PICKER_REQUEST = 10;

    private static final String LAT_LONG_FORMAT = "##0.0###";

    private static final String PICTURE_TEMP_FILE_NAME = "tempBlogPicture.jpg";

    private static final int MAX_IMG_SIZE = 1280;

    protected static final int REQUEST_PICK_IMAGE = 1;
    protected static final int REQUEST_PICK_CROP_IMAGE = 2;

    private static final String PREFERENCES_FILE = "de.webfilesysblog";

    private static final String PREF_SERVER_URL = "serverUrl";
    private static final String PREF_USERID = "userid";

    // URL of the blog server (incl. context root path of the blog webapp)
    private static final String SERVER_URL_DEFAULT = "http://www.webfilesys.de/blog";
    // private static final String SERVER_URL_DEFAULT = "http://192.168.2.102:8080";

    private static final int POPUP_ABOUT_WIDTH = 280;
    private static final int POPUP_ABOUT_HEIGHT = 220;

    private static final int POPUP_SEND_STATUS_WIDTH = 280;
    private static final int POPUP_SEND_STATUS_HEIGHT = 260;

    private Button sendPostButton;
    private Button sendPublishButton;
    private Button geoLocationButton;
    private Button changeLocationButton;
    private Button clearLocationButton;
    private ImageView blogPicImageView;

    private boolean offline = false;

    private String serverUrl;
    private String userid;
    private String password;

    private Uri pictureUri = null;

    private Uri picUriFromIntent = null;

    private String picturePath = null;

    private PopupWindow aboutPopup = null;

    private PopupWindow sendStatusPopup = null;

    private LatLng selectedLocation = null;

    SupportMapFragment mMapFragment = null;

    private GoogleMap googleMap = null;

    private Button mapSelectionOkButton = null;

    private View blogFormLayout = null;

    private View mapLayout = null;

    private View sendStatusView = null;

    private DecimalFormat latLongFormat;

    private GoogleApiClient googleApiClient;

    private Location lastLocation;

    private Date selectedDate = null;

    float latitudeFromExif;
    float longitudeFromExif;

    private boolean networkAvailabilityThreadRunning = false;

    private boolean currentNetworkStatus = false;

    private boolean stopNetworkQueryThread;

    /**
     * key: serverURL + '~' + userid
     * value: password
     */
    private HashMap<String, String> checkedLogins = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_ACCESS_EXTERNAL_STORAGE);
            Log.d("webfilesysblog", "missing permissions for accessing storage granted");
        }

        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.INTERNET}, MY_PERMISSIONS_REQUEST_ACCESS_INTERNET);
            Log.d("webfilesysblog", "missing permissions granted for internet access");
        }

        Intent intent = getIntent();
        String intentAction = intent.getAction();
        String intentType = intent.getType();

        if (Intent.ACTION_SEND.equals(intentAction) && intentType != null) {
            if (intentType.startsWith("image/")) {
                picUriFromIntent = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            }
        }

        latLongFormat = new DecimalFormat(LAT_LONG_FORMAT);

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        if (checkedLogins == null) {
            checkedLogins = new HashMap<String, String>();
        }

        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);

        serverUrl = prefs.getString("serverUrl", null);
        userid = prefs.getString("userid", null);
        if ((serverUrl == null) || (userid == null) || (password == null)) {
            showSettings();
        } else {
            showBlogForm();
        }
    }

    protected void requestLocation() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(60000);
        mLocationRequest.setFastestInterval(30000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
    }

    @Override
    protected void onStart() {
        googleApiClient.connect();
        stopNetworkQueryThread = false;

        if (!networkAvailabilityThreadRunning) {
            Button saveSettingsButton = (Button) findViewById(R.id.save_settings_button);
            if (saveSettingsButton != null) {
                new QueryNetworkAvailabilityTask(saveSettingsButton).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else if (sendPostButton != null) {
                new QueryNetworkAvailabilityTask(sendPostButton).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                Log.w("webfilesysblog", "QueryNetworkAvailabilityTask not started because view not found");
            }
        }

        super.onStart();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        stopNetworkQueryThread = true;
        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint) {

        if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_LOCATION);
            Log.d("webfilesysblog", "missing permissions granted for accessing location");
        }

        lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (lastLocation == null) {
            Log.d("webfilesysblog", "could not determine last location");
        } else {
            Log.d("webfilesysblog", "got last location: " + lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
        }
    }

    @Override
    public void onConnectionSuspended (int cause) {
        Log.d("webfilesysblog", "location services disconneted");
    }

    @Override
    public void onConnectionFailed (ConnectionResult result) {
        Log.d("webfilesysblog", "connection to location services failed");
    }

    private void showBlogForm() {
        boolean viewJustCreated = false;

        if (blogFormLayout == null) {
            LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            blogFormLayout = inflater.inflate(R.layout.blog_post_form, null);
            viewJustCreated = true;
        }

        setContentView(blogFormLayout);

        if (viewJustCreated) {
            blogPicImageView = (ImageView) findViewById(R.id.image);

            Button pickImageButton = (Button) findViewById(R.id.pick_image_button);
            pickImageButton.setOnClickListener(mButtonListener);

            sendPostButton = (Button) findViewById(R.id.send_post_button);
            sendPostButton.setOnClickListener(mButtonListener);
            sendPostButton.setVisibility(View.GONE);

            sendPublishButton = (Button) findViewById(R.id.send_publish_button);
            sendPublishButton.setOnClickListener(mButtonListener);
            sendPublishButton.setVisibility(View.GONE);

            geoLocationButton = (Button) findViewById(R.id.select_geo_location);
            geoLocationButton.setOnClickListener(mButtonListener);

            EditText descrText = (EditText) findViewById(R.id.description);
            descrText.setHorizontallyScrolling(false);
            descrText.setMaxLines(40);

            if (picUriFromIntent != null) {
                handlePictureSelection(picUriFromIntent);
                picUriFromIntent = null;
            }
        }

        if (offline) {
            sendPostButton.setText(R.string.buttonSaveOffline);
            sendPublishButton.setText(R.string.buttonSavePublish);
        } else {
            sendPostButton.setText(R.string.buttonSendPost);
            sendPublishButton.setText(R.string.buttonSendPublish);
        }
    }

    private View.OnClickListener mButtonListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            switch (v.getId()) {

                case R.id.pick_image_button:
                    Intent pickImageIntent = new Intent(
                            Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pickImageIntent, REQUEST_PICK_IMAGE);
                    break;
                case R.id.send_publish_button:
                case R.id.send_post_button:
                    EditText descrText = (EditText) findViewById(R.id.description);
                    if (descrText.getText().length() == 0) {
                        Toast toast = Toast.makeText(getApplicationContext(), R.string.missingDescription, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    } else {
                        if (v.getId() == R.id.send_publish_button) {
                            if (offline) {
                                queueEntryOffline(true);
                            } else {
                                new PostToBlogTask(v, true).execute();
                            }
                        } else {
                            if (offline) {
                                queueEntryOffline(false);
                            } else {
                                new PostToBlogTask(v, false).execute();
                            }
                        }
                    }
                    break;
                case R.id.select_geo_location:
                    if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                            (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_LOCATION);
                        Log.d("webfilesysblog", "missing permissions granted for accessing location");
                    }
                    // intentionally no break here!
                case R.id.change_geo_location:
                    requestLocation();

                    if (mapLayout == null) {
                        LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        mapLayout = inflater.inflate(R.layout.google_map, null);

                        ((MapFragment) getFragmentManager().findFragmentById(R.id.mapFragment)).getMapAsync(MainActivity.this);
                    } else {
                        setContentView(mapLayout);
                    }

                    break;
                case R.id.mapSelectionOk:
                    setContentView(blogFormLayout);

                    if (selectedLocation != null) {
                        Button selectLocationButton = (Button) findViewById(R.id.select_geo_location);
                        selectLocationButton.setVisibility(View.GONE);

                        TextView latitudeText = (TextView) findViewById(R.id.selectedLocLatitude);
                        latitudeText.setText(latLongFormat.format(selectedLocation.latitude));

                        TextView longitudeText = (TextView) findViewById(R.id.selectedLocLongitude);
                        longitudeText.setText(latLongFormat.format(selectedLocation.longitude));

                        View selectedLocationView = (View) findViewById(R.id.selectedLocation);
                        selectedLocationView.setVisibility(View.VISIBLE);

                        changeLocationButton = (Button) findViewById(R.id.change_geo_location);
                        changeLocationButton.setOnClickListener(mButtonListener);

                        clearLocationButton = (Button) findViewById(R.id.clear_geo_location);
                        clearLocationButton.setOnClickListener(mButtonListener);
                    }

                    break;
                case R.id.clear_geo_location:
                    selectedLocation = null;

                    View selectedLocationView = (View) findViewById(R.id.selectedLocation);
                    selectedLocationView.setVisibility(View.GONE);

                    Button selectLocationButton = (Button) findViewById(R.id.select_geo_location);
                    selectLocationButton.setVisibility(View.VISIBLE);
                    break;
            }
        }
    };

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        if (lastLocation != null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 15));
        } else {
            // fallback: Dresden, Germany
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(51.0499, 13.7378), 10));
        }

        googleMap.getUiSettings().setZoomControlsEnabled(true);

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                googleMap.clear();

                MarkerOptions options = new MarkerOptions().position( latLng );

                options.icon(BitmapDescriptorFactory.defaultMarker());
                googleMap.addMarker( options );

                selectedLocation = new LatLng(latLng.latitude, latLng.longitude);
            }
        });

        setContentView(mapLayout);

        if (mapSelectionOkButton == null) {
            mapSelectionOkButton = (Button) findViewById(R.id.mapSelectionOk);
            mapSelectionOkButton.setOnClickListener(mButtonListener);
        }
    }

    private void showPictureLayout() {
        View placeholderLayout = findViewById(R.id.placeholder_layout);
        placeholderLayout.setVisibility(View.GONE);

        View pictureLayout = findViewById(R.id.picture_layout);
        pictureLayout.setVisibility(View.VISIBLE);
    }

    private void hidePictureLayout() {
        View placeholderLayout = findViewById(R.id.placeholder_layout);
        placeholderLayout.setVisibility(View.VISIBLE);

        View pictureLayout = findViewById(R.id.picture_layout);
        pictureLayout.setVisibility(View.GONE);
    }

    private void saveScaledImage(Bitmap scaledBitmap) {

        picturePath = Environment.getExternalStorageDirectory() + "/" + PICTURE_TEMP_FILE_NAME;

        FileOutputStream fos = null;
        BufferedOutputStream buffOut = null;

        try {
            File thumbnailFile = new File(picturePath);
            fos = new FileOutputStream(thumbnailFile);
            buffOut = new BufferedOutputStream(fos);
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 95, buffOut);
            buffOut.flush();
        } catch (Exception ex) {
            Log.e("webfilesysblog", "failed to create scaled version of image", ex);
        } finally {
            if (buffOut != null) {
                try {
                    buffOut.close();
                } catch (Exception ex2) {
                }
            }
        }

        // scaledBitmap.recycle();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Log.d("webfilesysblog", "onActivityResult start");

        switch (requestCode) {

            case REQUEST_PICK_IMAGE:
                if (RESULT_OK == resultCode) {
                    blogPicImageView.setImageDrawable(null);

                    Uri selectedPicUri = intent.getData();
                    if (selectedPicUri != null) {
                        handlePictureSelection(selectedPicUri);
                    }
                }
                break;
        }
    }

    private void handlePictureSelection(Uri selectedPicUri) {
        pictureUri = selectedPicUri;
        Log.d("webfilesysblog", "selected image Uri: " + pictureUri);

        picturePath = null;

        Bitmap bitmap;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), pictureUri);

            int rotation = 0;
            Cursor mediaCursor = getContentResolver().query(pictureUri, new String[] { "orientation", "date_added" },null, null,"date_added desc");

            if ((mediaCursor != null) && (mediaCursor.getCount() != 0)) {
                while (mediaCursor.moveToNext()){
                    rotation = mediaCursor.getInt(0);
                    Log.d("webfilesysblog", "rotation: " + rotation);
                    break;
                }
            } else {
                Log.d("webfilesysblog", "mediaCursor EMPTY");
            }

            if (mediaCursor != null) {
                mediaCursor.close();
            }

            if (rotation != 0) {
                try {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotation);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    Log.d("webfilesysblog", "bitmap rotated by " + rotation + " degrees");
                    // bitmap.recycle();
                } catch (Throwable ex) {
                    Log.e("webfilesysblog", "failed to rotate image " + pictureUri, ex);

                    // bitmap = lowMemoryRotation(bitmap);
                }
            }

            // blogPicImageView.setImageBitmap(bitmap);

            Bitmap scaledBitmap = PictureUtils.getResizedBitmap(bitmap, MAX_IMG_SIZE);

            if (scaledBitmap != bitmap) {
                // if scaledBitmap is the same size as original bitmap, a new instance is NOT created, so we must not recylcle the orig image
                bitmap.recycle();
            }

            blogPicImageView.setImageBitmap(scaledBitmap);

            saveScaledImage(scaledBitmap);

            String origImgPath = CommonUtils.getFilePathByUri(this, pictureUri);

            // Log.d("webfilesysblog", "orig img file path: " + origImgPath);

            ExifData exifData = new ExifData(origImgPath);
            LatLng gpsFromExif = exifData.getGpsLocation();

            if (gpsFromExif != null) {
                Button selectLocationButton = (Button) findViewById(R.id.select_geo_location);
                selectLocationButton.setVisibility(View.GONE);

                TextView latitudeText = (TextView) findViewById(R.id.selectedLocLatitude);
                latitudeText.setText(latLongFormat.format(gpsFromExif.latitude));

                TextView longitudeText = (TextView) findViewById(R.id.selectedLocLongitude);
                longitudeText.setText(latLongFormat.format(gpsFromExif.longitude));

                View selectedLocationView = (View) findViewById(R.id.selectedLocation);
                selectedLocationView.setVisibility(View.VISIBLE);

                changeLocationButton = (Button) findViewById(R.id.change_geo_location);
                changeLocationButton.setOnClickListener(mButtonListener);

                clearLocationButton = (Button) findViewById(R.id.clear_geo_location);
                clearLocationButton.setOnClickListener(mButtonListener);

                selectedLocation = gpsFromExif;
            }

            pictureUri = null;
        } catch (FileNotFoundException e) {
            Log.e("webfilesysblog", "failed to read image data of selected picture", e);
        } catch (IOException e) {
            Log.e("webfilesysblog", "failed to read image data of selected picture", e);
        }

        showPictureLayout();

        if (offline) {
            sendPostButton.setText(R.string.buttonSaveOffline);
            sendPublishButton.setText(R.string.buttonSavePublish);
        } else {
            sendPostButton.setText(R.string.buttonSendPost);
            sendPublishButton.setText(R.string.buttonSendPublish);
        }

        sendPostButton.setVisibility(View.VISIBLE);
        sendPublishButton.setVisibility(View.VISIBLE);
    }

    private boolean checkNetworkConnection() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        return (networkInfo != null) && networkInfo.isConnected();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_bar_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.optionSettings:
                showSettings();
                return true;

            case R.id.optionAbout:
                showAboutInfo();
                return true;

            case R.id.optionExit:
                System.exit(0);
                return true;

            case R.id.optionSendQueued:
                new SendQueuedOfflineEntriesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showAboutInfo() {
        View aboutView = getLayoutInflater().inflate(R.layout.popup_about, null);

        aboutPopup = new PopupWindow(aboutView);

        Button closeButton = (Button) aboutView.findViewById(R.id.aboutCloseButton);

        closeButton.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                aboutPopup.dismiss();
            }
        });

        View parentView = findViewById(R.id.scene_layout);

        float densityFactor = getResources().getDisplayMetrics().density;

        aboutPopup.showAtLocation(parentView, Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);

        // aboutPopup.update(20, parentView.getHeight() - POPUP_ABOUT_HEIGHT - 20, POPUP_ABOUT_WIDTH, POPUP_ABOUT_HEIGHT);

        aboutPopup.update(0, 0,
                (int) (POPUP_ABOUT_WIDTH * densityFactor),
                (int) (POPUP_ABOUT_HEIGHT * densityFactor));
    }

    private void showSendStatus() {
        sendStatusView = getLayoutInflater().inflate(R.layout.popup_send_progress, null);

        sendStatusPopup = new PopupWindow(sendStatusView);

        Button closeButton = (Button) sendStatusView.findViewById(R.id.statusCloseButton);

        closeButton.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                sendStatusPopup.dismiss();
            }
        });

        View parentView = findViewById(R.id.scene_layout);

        float densityFactor = getResources().getDisplayMetrics().density;

        sendStatusPopup.showAtLocation(parentView, Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);

        // aboutPopup.update(20, parentView.getHeight() - POPUP_ABOUT_HEIGHT - 20, POPUP_ABOUT_WIDTH, POPUP_ABOUT_HEIGHT);

        sendStatusPopup.update(0, 0,
                (int) (POPUP_SEND_STATUS_WIDTH * densityFactor),
                (int) (POPUP_SEND_STATUS_HEIGHT * densityFactor));
    }


    private void showSettings() {
        setContentView(R.layout.settings);

        final SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        serverUrl = prefs.getString(PREF_SERVER_URL, SERVER_URL_DEFAULT);
        userid = prefs.getString(PREF_USERID, null);

        ProgressBar authProgressBar = (ProgressBar) findViewById(R.id.authProgressBar);
        authProgressBar.setVisibility(View.GONE);

        TextView connectingMsg = (TextView) findViewById(R.id.connecting_msg);
        connectingMsg.setVisibility(View.GONE);

        EditText serverUrlInput = (EditText) findViewById(R.id.server_url);
        if (serverUrl != null) {
            serverUrlInput.setText(serverUrl, TextView.BufferType.EDITABLE);
        }

        EditText useridInput = (EditText) findViewById(R.id.userid);
        if (userid != null) {
            useridInput.setText(userid, TextView.BufferType.EDITABLE);
        }

        EditText passwordInput = (EditText) findViewById(R.id.password);
        if (password != null) {
            passwordInput.setText(password, TextView.BufferType.EDITABLE);
        }

        TextView offlineMsg = (TextView) findViewById(R.id.offlineMsg);
        Button saveSettingsButton = (Button) findViewById(R.id.save_settings_button);

        offline = false;

        if (!checkNetworkConnection()) {
            saveSettingsButton.setText(R.string.workOffline);
            offlineMsg.setVisibility(View.VISIBLE);
            offline = true;
        } else {
            saveSettingsButton.setText(R.string.buttonSaveSettings);
            offlineMsg.setVisibility(View.GONE);
        }

        currentNetworkStatus = !offline;

        saveSettingsButton.setVisibility(View.VISIBLE);

        saveSettingsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                switch (v.getId()) {
                    case R.id.save_settings_button:

                        EditText serverUrlInput = (EditText) findViewById(R.id.server_url);
                        serverUrl = serverUrlInput.getText().toString();

                        EditText useridInput = (EditText) findViewById(R.id.userid);
                        userid = useridInput.getText().toString();

                        EditText passwordInput = (EditText) findViewById(R.id.password);
                        password = passwordInput.getText().toString();

                        boolean missingParameters = false;

                        if ((serverUrl == null) || serverUrl.trim().isEmpty() ||
                            (userid == null) || userid.trim().isEmpty()) {
                            missingParameters = true;
                        } else {
                            if (!offline) {
                                if ((password == null) || password.trim().isEmpty()) {
                                    missingParameters = true;
                                }
                            }
                        }

                        if (missingParameters) {
                            Toast toast = Toast.makeText(getApplicationContext(), R.string.missingParameters, Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        } else {
                            if (serverUrl.endsWith("/")) {
                                serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
                            }

                            v.setVisibility(View.GONE);

                            if (!offline) {
                                TextView connectingMsg = (TextView) findViewById(R.id.connecting_msg);
                                connectingMsg.setVisibility(View.VISIBLE);

                                ProgressBar authProgressBar = (ProgressBar) findViewById(R.id.authProgressBar);
                                authProgressBar.setVisibility(View.VISIBLE);
                            }

                            SharedPreferences.Editor prefEditor = prefs.edit();
                            prefEditor.putString(PREF_SERVER_URL, serverUrl);
                            prefEditor.putString(PREF_USERID, userid);

                            prefEditor.commit();

                            if (!networkAvailabilityThreadRunning) {
                                new QueryNetworkAvailabilityTask(v).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            }

                            if (offline) {
                                showBlogForm();
                            } else {
                                new TestAuthenticationTask(v).execute();
                            }
                        }

                        break;
                }
            }
        });
    }

    private void queueEntryOffline(boolean publishImmediately) {

        OfflineQueueMetaDataElem metaData = new OfflineQueueMetaDataElem();

        metaData.setServerUrl(serverUrl);

        metaData.setUserid(userid);

        Date blogEntryDate = selectedDate;
        if (blogEntryDate == null) {
            blogEntryDate = new Date();
        }
        metaData.setBlogDate(blogEntryDate);

        EditText descrText = (EditText) findViewById(R.id.description);

        metaData.setBlogText(descrText.getText().toString().replace('\n', ' ').replace('\r', ' '));

        metaData.setGeoLocation(selectedLocation);

        metaData.setPublishImmediately(publishImmediately);

        OfflineQueueManager queueMgr = OfflineQueueManager.getInstance(getFilesDir());

        queueMgr.queueBlogEntry(getApplicationContext(), metaData, pictureUri, picturePath);

        // sendResultText.setText(R.string.saveSuccess);

        Toast toast = Toast.makeText(getApplicationContext(), R.string.saveSuccess, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

        EditText descriptionInput = (EditText) findViewById(R.id.description);
        descriptionInput.getText().clear();
        blogPicImageView.setImageDrawable(null);
        selectedLocation = null;
        hidePictureLayout();

        geoLocationButton.setVisibility(View.VISIBLE);

        View selectedLocationView = (View) findViewById(R.id.selectedLocation);
        selectedLocationView.setVisibility(View.GONE);
    }

    class QueryNetworkAvailabilityTask extends AsyncTask<String, Void, String> {
        int authResult;

        private View view;

        private boolean networkStatusChanged = false;

        public QueryNetworkAvailabilityTask(View v) {
            view = v;
        }

        protected String doInBackground(String... params) {

            networkAvailabilityThreadRunning = true;

            do {
                try {
                    Thread.currentThread().sleep(30000);

                    if (!stopNetworkQueryThread) {
                        boolean newNetworkStatus = checkNetworkConnection();

                        Log.d("webfilesysblog", "current network connection status: " + newNetworkStatus);

                        if (newNetworkStatus) {
                            if (!currentNetworkStatus) {
                                networkStatusChanged = true;
                            }
                        } else {
                            if (currentNetworkStatus) {
                                offline = true;
                                networkStatusChanged = true;
                            }
                        }

                        currentNetworkStatus = newNetworkStatus;
                    }
                } catch (InterruptedException iex) {
                }
            } while ((!networkStatusChanged) && (!stopNetworkQueryThread));

            networkAvailabilityThreadRunning = false;

            return "";
        }

        protected void onPostExecute(String result) {
            if (networkStatusChanged) {
                int toastText;
                if (currentNetworkStatus) {
                    toastText = R.string.backOnline;
                } else {
                    toastText = R.string.wentOffline;
                }
                Toast toast = Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }

            if (!stopNetworkQueryThread) {
                Button saveSettingsButton = (Button) findViewById(R.id.save_settings_button);
                new QueryNetworkAvailabilityTask(saveSettingsButton).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                stopNetworkQueryThread = false;
            }
        }
    }

    class TestAuthenticationTask extends AsyncTask<String, Void, String> {
        int authResult;

        private View view;

        public TestAuthenticationTask(View v) {
            view = v;
        }

        protected String doInBackground(String... params) {
            authResult = ServerCommunicator.getInstance().checkAuthentication(serverUrl, userid, password);

            return "";
        }

        protected void onPostExecute(String result) {
            if (authResult == 1) {
                String key = serverUrl + '~' + userid;
                checkedLogins.put(key, password);
                showBlogForm();
            } else if (authResult == 0) {
                Toast toast = Toast.makeText(view.getContext(), R.string.authenticationFailed, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                showSettings();
            } else if (authResult == (-1)) {
                Toast toast = Toast.makeText(view.getContext(), R.string.communicationFailure, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                showSettings();
            }
        }
    }

    class PostToBlogTask extends AsyncTask<String, Void, String> {

        private Exception exception;

        private boolean success = false;

        private boolean publishImmediately;

        private View view;

        private String blogEntryText = null;

        public PostToBlogTask(View v, boolean publishImmediately) {
            view = v;
            this.publishImmediately = publishImmediately;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            sendPostButton.setVisibility(View.GONE);
            sendPublishButton.setVisibility(View.GONE);

            EditText descrText = (EditText) findViewById(R.id.description);
            blogEntryText = descrText.getText().toString();

            showSendStatus();
        }

        protected String doInBackground(String... params) {

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            if (selectedDate == null) {
                selectedDate = new Date();
            }

            Date now = new Date();
            selectedDate.setHours(now.getHours());
            selectedDate.setMinutes(now.getMinutes());
            selectedDate.setSeconds(now.getSeconds());

            String destFileName = dateFormat.format(selectedDate) + "-" + selectedDate.getTime() + ".jpg";

            try {
                ServerCommunicator serverCommunicator = ServerCommunicator.getInstance();

                InputStream pictureIn = null;
                if (pictureUri != null) {
                    pictureIn = getApplicationContext().getContentResolver().openInputStream(pictureUri);
                } else {
                    pictureIn = new FileInputStream(new File(picturePath));
                }

                if (serverCommunicator.sendPicture(serverUrl, userid, password, destFileName, pictureIn)) {

                    if (serverCommunicator.sendDescription(serverUrl, userid, password, destFileName, blogEntryText, selectedLocation)) {

                        if (publishImmediately) {
                            if (serverCommunicator.sendPublishRequest(serverUrl, userid, password, destFileName)) {
                                success = true;
                            }
                        } else {
                            success = true;
                        }
                    }
                }
            } catch (FileNotFoundException fnfex) {
                Log.e("webfilesysblog", "picture file for blog entry not found", fnfex);
            }

            return "";
        }

        protected void onPostExecute(String result) {
            View sendProgressBar = sendStatusView.findViewById(R.id.sendProgressBar);
            sendProgressBar.setVisibility(View.INVISIBLE);

            TextView sendResultText  = (TextView) sendStatusView.findViewById(R.id.sendResult);

            if (success) {
                sendResultText.setText(R.string.postSuccess);

                EditText descriptionInput = (EditText) findViewById(R.id.description);
                descriptionInput.getText().clear();
                blogPicImageView.setImageDrawable(null);
                selectedLocation = null;
                hidePictureLayout();

                geoLocationButton.setVisibility(View.VISIBLE);

                View selectedLocationView = (View) findViewById(R.id.selectedLocation);
                selectedLocationView.setVisibility(View.GONE);
            } else {
                sendResultText.setText(R.string.postFailed);

                sendPostButton.setVisibility(View.VISIBLE);
                sendPublishButton.setVisibility(View.VISIBLE);
            }

            sendResultText.setVisibility(View.VISIBLE);

            View statusCloseButton = sendStatusView.findViewById(R.id.statusCloseButton);
            statusCloseButton.setVisibility(View.VISIBLE);
        }
    }

    class SendQueuedOfflineEntriesTask extends AsyncTask<String, Void, String> {

        private Exception exception;

        private boolean success = false;

        private int entriesSentCount = 0;

        public SendQueuedOfflineEntriesTask() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Toast toast;

            if (checkedLogins.isEmpty()) {
                toast = Toast.makeText(getApplicationContext(), R.string.notLoggedIn, Toast.LENGTH_LONG);
            } else {
                toast = Toast.makeText(getApplicationContext(), R.string.sendingQueuedEntries, Toast.LENGTH_LONG);
            }
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }

        protected String doInBackground(String... params) {

            if (!checkedLogins.isEmpty()) {
                entriesSentCount = OfflineQueueManager.getInstance(getFilesDir()).sendQueuedEntries(checkedLogins);
            }

            return "";
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            String toastText = entriesSentCount + " " + getString(R.string.queuedEntriesSent);

            Toast toast = Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    private String createBasicAuthToken() {
        String authToken = userid + ":" + password;
        String encodedToken = Base64.encodeToString(authToken.getBytes(), Base64.DEFAULT).replaceAll("\n", "");
        String encodedAuthToken = "Basic " + encodedToken;

        return encodedAuthToken;
    }

    public void showDatePickerDialog(View v) {

        long initialDate = System.currentTimeMillis();
        if (selectedDate != null) {
            initialDate = selectedDate.getTime();
        }

        DialogFragment newFragment = new DatePickerFragment();

        Bundle bundle = new Bundle();
        bundle.putLong("initialDate", initialDate);
        newFragment.setArguments(bundle);

        newFragment.show(getFragmentManager(), "datePicker");
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        Calendar pickedDate = Calendar.getInstance();
        pickedDate.set(Calendar.YEAR, year);
        pickedDate.set(Calendar.MONTH, monthOfYear);
        pickedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        selectedDate = pickedDate.getTime();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String formattedSelectedDate = dateFormat.format(selectedDate);

        Button selectDateButton = (Button) findViewById(R.id.pickDateButton);
        selectDateButton.setText(formattedSelectedDate);
    }

}
