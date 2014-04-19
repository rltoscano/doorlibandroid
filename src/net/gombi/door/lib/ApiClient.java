package net.gombi.door.lib;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/** Client of the Door service. */
public class ApiClient {

  public static class ApiClientException extends Exception {
    private static final long serialVersionUID = 1L;
    public ApiClientException(String msg) { super(msg); }
    public ApiClientException(String msg, Throwable t) { super(msg, t); }
    public ApiClientException(Throwable t) { super(t); }
  }

  private static String LOG_TAG = "NET_GOMBI_DOOR_LIB_APICLIENT";
  private static String OAUTH_SCOPE = "oauth2:https://www.googleapis.com/auth/userinfo.email";

  private final Context c;
  private final HttpClient httpClient;
  private final String acctPrefName;
  private final String baseUrl;

  public ApiClient(Context c, String acctPrefName) {
    this.c = c;
    httpClient = AndroidHttpClient.newInstance("GombiDoor", c);
    this.acctPrefName = acctPrefName;
    baseUrl = "https://" + R.string.serviceHostName + "/api/";
  }

  public List<Door> listDoors(PermissionLevel level) throws ApiClientException {
    return listDoors(level, null);
  }

  public List<Door> listDoors(PermissionLevel level, String devId) throws ApiClientException {
    Log.i(LOG_TAG, "Listing doors.");
    StringBuilder sb = new StringBuilder(baseUrl + "doors");
    sb.append("?level=").append(level.getValue());
    if (devId != null) {
      sb.append("&devId=").append(devId);
    }
    HttpGet get = new HttpGet(sb.toString());
    Reader r = request(get);
    TypeToken<List<Door>> doorListType = new TypeToken<List<Door>>(){};
    return new Gson().fromJson(r, doorListType.getType());
  }

  public Door createDoor(Door d) throws ApiClientException {
    Log.i(LOG_TAG, "Creating door.");
    HttpPost post = new HttpPost(baseUrl + "doors");
    List<NameValuePair> params = new LinkedList<NameValuePair>();
    params.add(new BasicNameValuePair("displayName", d.getDisplayName()));
    params.add(new BasicNameValuePair("regId", d.getRegId()));
    params.add(new BasicNameValuePair("devId", d.getDevId()));
    try {
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (UnsupportedEncodingException e) {
      throw new ApiClientException(e);
    }
    Reader r = request(post);
    TypeToken<List<Door>> doorListType = new TypeToken<List<Door>>(){};
    List<Door> doors = new Gson().fromJson(r, doorListType.getType());
    return doors.get(0);
  }

  public Door updateDoor(Door d) throws ApiClientException {
    Log.i(LOG_TAG, "Updating door.");
    if (d.getKey() == null) {
      throw new ApiClientException("Given door has no key.");
    }
    HttpPut put = new HttpPut(baseUrl + "doors");
    List<NameValuePair> params = new LinkedList<NameValuePair>();
    params.add(new BasicNameValuePair("key", d.getKey()));
    params.add(new BasicNameValuePair("displayName", d.getDisplayName()));
    params.add(new BasicNameValuePair("regId", d.getRegId()));
    try {
      put.setEntity(new UrlEncodedFormEntity(params));
    } catch (UnsupportedEncodingException e) {
      throw new ApiClientException(e);
    }
    Reader r = request(put);
    TypeToken<List<Door>> doorListType = new TypeToken<List<Door>>(){};
    List<Door> doors = new Gson().fromJson(r, doorListType.getType());
    return doors.get(0);
  }

  public void openDoor(String doorKey) throws ApiClientException {
    Log.i(LOG_TAG, "Opening door: " + doorKey);
    HttpPost post = new HttpPost(baseUrl + "open");
    List<NameValuePair> params = new LinkedList<NameValuePair>();
    params.add(new BasicNameValuePair("doorKey", doorKey));
    try {
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (UnsupportedEncodingException e) {
      throw new ApiClientException(e);
    }
    request(post);
    return;
  }

  private Reader request(HttpRequestBase req) throws ApiClientException {
    String authToken = getOAuthToken();
    req.setHeader("Authorization", "Bearer " + authToken);
    HttpResponse response;
    try {
      response = httpClient.execute(req);
    } catch (Exception e) {
      throw new ApiClientException("Could not make HTTP request to server.", e);
    }

    int serverCode = response.getStatusLine().getStatusCode();
    if (serverCode == 200) {
      Log.v(LOG_TAG, "Successfully received response from door service.");
      try {
         return new InputStreamReader(response.getEntity().getContent());
      } catch (Exception e) {
        throw new ApiClientException(e);
      }
    } else if (serverCode == 401 || serverCode == 403) {
      // Bad token, invalidate.
      GoogleAuthUtil.invalidateToken(c, authToken);
      throw new ApiClientException("Server auth error");
    } else {
      // Unknown error, do something else.
      throw new ApiClientException("Server returned the following error code: " + serverCode);
    }
  }

  private String getOAuthToken() throws ApiClientException {
    String account = PreferenceManager.getDefaultSharedPreferences(c).getString(acctPrefName, null);
    if (account == null) {
      throw new ApiClientException("No account has been selected");
    }
    try {
      return GoogleAuthUtil.getTokenWithNotification(c, account, OAUTH_SCOPE, null);
    } catch (Exception e) {
      throw new ApiClientException("Error while getting OAuth token", e);
    }
  }
}
