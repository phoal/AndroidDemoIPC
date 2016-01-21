package phoal.piko.view;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
//import android.util.Log;
/**
 * Created by Phoal on 25/09/2015.
 */
public class ReminderReceiver extends BroadcastReceiver {
    // Notification ID to allow for future updates
    private static final int REMINDER_ID = 31;
    private static final String TAG = "ReminderReceiver";

    // Notification Text Elements
    private final CharSequence tickerText = "Skin Check Reminder";
    private final CharSequence contentText = "Remember To Take A Skin Check!";

    @Override
    public void onReceive(Context context, Intent intent) {

        // The Intent to be used when the user clicks on the Notification View
        Intent mNotificationIntent = new Intent(context, DisplayImagesActivity.class) ;
        mNotificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // The PendingIntent that wraps the underlying Intent
        PendingIntent mContentIntent = PendingIntent.getActivity(context, 0,
                mNotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Build the Notification
        Notification.Builder notificationBuilder = new Notification.Builder(
                context).setTicker(tickerText)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setAutoCancel(true).setContentTitle(tickerText)
                .setContentText(contentText).setContentIntent(mContentIntent);

        // Get the NotificationManager
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        // Pass the Notification to the NotificationManager:
        mNotificationManager.notify(REMINDER_ID,
                notificationBuilder.build());

        // Log occurence of notify() call
        //Log.i(TAG, "Sending notification at:"
        //        + DateFormat.getDateTimeInstance().format(new Date()));

    }
}

