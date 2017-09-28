package android.webfilesys.de.webfilesysblog;

import android.util.Base64;
import android.util.Log;
import android.widget.EditText;

import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by User on 27.09.2017.
 */

public class ServerCommunicator {

    private static final String LAT_LONG_FORMAT = "##0.0###";

    private static ServerCommunicator instance = null;

    private ServerCommunicator() {
    }

    public static ServerCommunicator getInstance() {
        if (instance == null) {
            instance = new ServerCommunicator();
        }
        return instance;
    }

    public int checkAuthentication(String serverUrl, String userid, String password) {
        String encodedAuthToken = createBasicAuthToken(userid, password);

        try {
            URL url = new URL(serverUrl + "/blogpost/authenticate");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", encodedAuthToken);

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                return 1;
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED){
                return 0;
            }
            return (-1);
        } catch (MalformedURLException urlEx) {
            Log.w("webfilesysblog", "invalid server URL in authentication check", urlEx);
            return (-1);
        } catch (IOException ioEx) {
            Log.w("webfilesysblog", "communication failure in authentication check", ioEx);
            return (-1);
        }
    }

    public boolean sendDescription(String serverUrl, String userid, String password, String destFileName, String descrText, LatLng gpsLocation ) {
        String response = "";

        try {
            HttpURLConnection conn = prepareUrlConnection(serverUrl + "/blogpost/description/" + destFileName, userid, password);

            OutputStream os = null;
            BufferedWriter out = null;
            PrintWriter pout = null;
            try {
                os = conn.getOutputStream();

                out = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                pout = new PrintWriter(out);

                pout.println(descrText.replace('\n', ' ').replace('\r', ' '));

                if (gpsLocation != null) {
                    DecimalFormat latLongFormat = new DecimalFormat(LAT_LONG_FORMAT);

                    pout.println(latLongFormat.format(gpsLocation.latitude));
                    pout.println(latLongFormat.format(gpsLocation.longitude));
                }

                os.flush();

            } catch (Exception ioex) {
                Log.e("webfilesysblog", "failed to post description data to blog", ioex);
            } finally {
                if (pout != null) {
                    try {
                        pout.close();
                    } catch (Exception closeEx) {
                    }
                }
                if (os != null) {
                    try {
                        os.close();
                    } catch (Exception closeEx) {
                    }
                }
            }

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }
                return true;
            }  else {
                response = "";
            }
        } catch (Exception e) {
            Log.e("webfilesysblog", "failed to send metadata via HTTP Post", e);
        }
        return false;
    }

    public boolean sendPicture(String serverUrl, String userid, String password, String destFileName, InputStream pictureIn) {
        String response = "";
        try {
            HttpURLConnection conn = prepareUrlConnection(serverUrl + "/blogpost/picture/" + destFileName, userid, password);

            OutputStream os = null;
            BufferedOutputStream buffOut = null;
            BufferedInputStream buffIn = null;
            try {
                buffIn = new BufferedInputStream(pictureIn);

                os = conn.getOutputStream();
                buffOut = new BufferedOutputStream(os);

                byte[] buff = new byte[4096];

                int count;

                while (( count = buffIn.read(buff)) >= 0 ) {
                    buffOut.write(buff, 0, count);
                }
                buffOut.flush();

            } catch (Exception ioex) {
                Log.e("webfilesysblog", "failed to post picture data to blog", ioex);
            } finally {
                if (buffIn != null) {
                    try {
                        buffIn.close();
                    } catch (Exception closeEx) {
                    }
                }
                if (buffOut != null) {
                    try {
                        buffOut.close();
                    } catch (Exception closeEx) {
                    }
                }
            }

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }
                return true;
            }  else {
                response = "";
            }
        } catch (Exception e) {
            Log.e("webfilesysblog", "failed to send picture via HTTP Post", e);
        }
        return false;
    }

    public boolean sendPublishRequest(String serverUrl, String userid, String password, String destFileName) {
        String response = "";
        try {
            HttpURLConnection conn = prepareUrlConnection(serverUrl + "/blogpost/publish/" + destFileName, userid, password);

            OutputStream os = null;
            BufferedWriter out = null;
            PrintWriter pout = null;
            try {
                os = conn.getOutputStream();

                out = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                pout = new PrintWriter(out);

                pout.println("publish immediately");

                os.flush();

            } catch (Exception ioex) {
                Log.e("webfilesysblog", "failed to post publish request", ioex);
            } finally {
                if (pout != null) {
                    try {
                        pout.close();
                    } catch (Exception closeEx) {
                    }
                }
                if (os != null) {
                    try {
                        os.close();
                    } catch (Exception closeEx) {
                    }
                }
            }

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }
                return true;
            }  else {
                response = "";
            }
        } catch (Exception e) {
            Log.e("webfilesysblog", "failed to post publish request", e);
        }
        return false;
    }

    private HttpURLConnection prepareUrlConnection(String webfilesysUrl, String userid, String password)
            throws MalformedURLException, IOException {

        String encodedAuthToken = createBasicAuthToken(userid, password);

        URL url = new URL(webfilesysUrl);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(15000);
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", encodedAuthToken);
        conn.setDoInput(true);
        conn.setDoOutput(true);

        return conn;
    }

    private String createBasicAuthToken(String userid, String password) {
        String authToken = userid + ":" + password;
        String encodedToken = Base64.encodeToString(authToken.getBytes(), Base64.DEFAULT).replaceAll("\n", "");
        String encodedAuthToken = "Basic " + encodedToken;

        return encodedAuthToken;
    }

}
