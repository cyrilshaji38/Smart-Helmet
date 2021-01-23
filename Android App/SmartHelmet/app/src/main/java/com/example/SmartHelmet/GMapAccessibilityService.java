package com.example.SmartHelmet;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import java.io.ByteArrayOutputStream;

public class GMapAccessibilityService extends NotificationListenerService{

    String temptitle = "";

    /*
     These are the package names of the apps. for which we want to
     listen the notifications
  */
    private static final class ApplicationPackageNames {
        public static final String GMAP_PACK_NAME = "com.google.android.apps.maps";
    }

    /*
        These are the return codes we use in the method which intercepts
        the notifications, to decide whether we should do something or not
     */
    public static final class InterceptedNotificationCode {
        public static final int GMAP_CODE = 3;
        public static final int OTHER_NOTIFICATIONS_CODE = 4; // We ignore all notification with code == 4
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onNotificationPosted(StatusBarNotification sbn){

        int notificationCode = matchNotificationCode(sbn);
        Bundle extras = sbn.getNotification().extras;



        if(notificationCode != InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE)
        {
            String title = extras.getString("android.title");
            String distance = "", bitmapArrowPass = "";

            assert title != null;
            if (!title.equals(temptitle)) {
                temptitle = title;
                distance = "*" + title;
                for (int i = 0; i < 2; i++) {
                    if (title.charAt(i) >= '0' && title.charAt(i) <= '9' && title.contains("-")) {
                        distance = title.substring(0, title.indexOf("-") - 1);
                        distance = distance.replace("m", "%");
                        distance = distance.replace("k", "#");
                    }
                }

                if(title.equals("Rerouting..."))
                    bitmapArrowPass = ")";
                else {
                    Icon arrow;
                    arrow = (Icon) extras.get("android.largeIcon");
                    if (arrow == null) {
                        bitmapArrowPass = "&";
                    }
                    else {
                        //Convert Icon to Bitmap
                        Bitmap bitmapArrow;
                        if (arrow.loadDrawable(this) instanceof BitmapDrawable) {
                            bitmapArrow = ((BitmapDrawable) arrow.loadDrawable(this)).getBitmap();
                        } else {
                            bitmapArrow = Bitmap.createBitmap(arrow.loadDrawable(this).getIntrinsicWidth(),
                                    arrow.loadDrawable(this).getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                        }

                        //Convert Bitmap to Byte Array
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmapArrow.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        byte[] bitmapArrowByteArray = stream.toByteArray();

                        //Assign each Byte Array to symbol for arrow
                        if (bitmapArrowByteArray[945] == -126)
                            bitmapArrowPass = "^";
                        else if (bitmapArrowByteArray[993] == -126)
                            bitmapArrowPass = "^";
                        else if (bitmapArrowByteArray[993] == -86)
                            bitmapArrowPass = ">";
                        else if (bitmapArrowByteArray[993] == 66)
                            bitmapArrowPass = "<";
                        else if (bitmapArrowByteArray[993] == -85)
                            bitmapArrowPass = "$";
                        else if ((bitmapArrowByteArray[993] == -66) || (bitmapArrowByteArray[993] == -81))
                            bitmapArrowPass = "&";
                        else
                            bitmapArrowPass = "(";
                    }
                }
            }

            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)) {
                Intent intent = new Intent("com.example.ssa_ezra.SmartHelmet");
                intent.putExtra("distance", distance);
                intent.putExtra("text", title);
                intent.putExtra("arrow", bitmapArrowPass);
                sendBroadcast(intent);
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn){
        int notificationCode = matchNotificationCode(sbn);

        if(notificationCode != InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE) {

            StatusBarNotification[] activeNotifications = this.getActiveNotifications();

            if(activeNotifications != null && activeNotifications.length > 0) {
                for (StatusBarNotification activeNotification : activeNotifications) {
                    if (notificationCode == matchNotificationCode(activeNotification)) {
                        Intent intent = new Intent("com.example.ssa_ezra.SmartHelmet");
                        intent.putExtra("Notification Code", notificationCode);
                        sendBroadcast(intent);
                        break;
                    }
                }
            }
        }
    }

    private int matchNotificationCode(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        if(packageName.equals(ApplicationPackageNames.GMAP_PACK_NAME)){
            return(InterceptedNotificationCode.GMAP_CODE);
        }
        else{
            return(InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE);
        }
    }
}