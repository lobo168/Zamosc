package app.thecity.fcm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.Map;

import app.thecity.ActivityNewsInfoDetails;
import app.thecity.ActivityPlaceDetail;
import app.thecity.ActivitySplash;
import app.thecity.R;
import app.thecity.data.AppConfig;
import app.thecity.data.Constant;
import app.thecity.data.DatabaseHandler;
import app.thecity.data.SharedPref;
import app.thecity.model.FcmNotif;
import app.thecity.model.NewsInfo;
import app.thecity.model.Place;
import app.thecity.utils.PermissionUtil;

public class FcmMessagingService extends FirebaseMessagingService {

    private static int VIBRATION_TIME = 500; // in millisecond
    private SharedPref sharedPref;
    private int retry_count = 0;
    private ImageLoader imgloader = ImageLoader.getInstance();

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        sharedPref = new SharedPref(this);
        sharedPref.setFcmRegId(s);
        sharedPref.setOpenAppCounter(SharedPref.MAX_OPEN_COUNTER);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        sharedPref = new SharedPref(this);

        sharedPref.setRefreshPlaces(true);
        if (imgloader.isInited() && AppConfig.REFRESH_IMG_NOTIF) {
            imgloader.clearDiskCache();
            imgloader.clearMemoryCache();
        }

        if (sharedPref.getNotification() && PermissionUtil.isStorageGranted(this)) {
            if (remoteMessage.getData().size() == 0) return;
            Map<String, String> data = remoteMessage.getData();
            final FcmNotif fcmNotif = new FcmNotif();
            fcmNotif.title = data.get("title");
            fcmNotif.content = data.get("content");
            fcmNotif.type = data.get("type");

            // load data place if exist
            String place_str = data.get("place");
            fcmNotif.place = place_str != null ? new Gson().fromJson(place_str, Place.class) : null;

            // load data news_info if exist
            String news_str = data.get("news");
            fcmNotif.news = news_str != null ? new Gson().fromJson(news_str, NewsInfo.class) : null;

            loadRetryImageFromUrl(fcmNotif, new CallbackImageNotif() {
                @Override
                public void onSuccess(Bitmap bitmap) {
                    displayNotificationIntent(fcmNotif, bitmap);
                }

                @Override
                public void onFailed() {
                    displayNotificationIntent(fcmNotif, null);
                }
            });
        }
    }

    private void displayNotificationIntent(FcmNotif fcmNotif, Bitmap bitmap) {
        playRingtoneVibrate(this);
        Intent intent = new Intent(this, ActivitySplash.class);

        if (fcmNotif.place != null) {
            intent = ActivityPlaceDetail.navigateBase(this, fcmNotif.place, true);
        } else if (fcmNotif.news != null) {
            new DatabaseHandler(this).refreshTableNewsInfo();
            intent = ActivityNewsInfoDetails.navigateBase(this, fcmNotif.news, true);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        String channelId = getString(R.string.default_notification_channel_id);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setContentTitle(fcmNotif.title);
        builder.setContentText(fcmNotif.content);
        builder.setDefaults(Notification.DEFAULT_LIGHTS);
        builder.setAutoCancel(true);
        builder.setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            builder.setPriority(Notification.PRIORITY_HIGH);
        }

        if (bitmap != null) {
            builder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap).setSummaryText(fcmNotif.content));
        } else {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(fcmNotif.content));
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
        int unique_id = (int) System.currentTimeMillis();
        notificationManager.notify(unique_id, builder.build());
    }

    private void playRingtoneVibrate(Context context) {
        try {
            // play vibration
            if (sharedPref.getVibration()) {
                ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(VIBRATION_TIME);
            }
            RingtoneManager.getRingtone(context, Uri.parse(sharedPref.getRingtone())).play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRetryImageFromUrl(final FcmNotif fcmNotif, final CallbackImageNotif callback) {
        String url = "";
        if (fcmNotif.place != null) {
            url = Constant.getURLimgPlace(fcmNotif.place.image);
        } else if (fcmNotif.news != null) {
            url = Constant.getURLimgNews(fcmNotif.news.image);
        } else {
            callback.onFailed();
            return;
        }
        loadImageFromUrl(url, new CallbackImageNotif() {
            @Override
            public void onSuccess(Bitmap bitmap) {
                callback.onSuccess(bitmap);
            }

            @Override
            public void onFailed() {
                Log.e("onFailed", "on Failed");
                if (retry_count <= Constant.LOAD_IMAGE_NOTIF_RETRY) {
                    retry_count++;
                    loadRetryImageFromUrl(fcmNotif, callback);
                } else {
                    callback.onFailed();
                }
            }
        });
    }

    private void loadImageFromUrl(String url, final CallbackImageNotif callback) {
        imgloader.loadImage(url, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                callback.onSuccess(loadedImage);
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                callback.onFailed();
                super.onLoadingFailed(imageUri, view, failReason);
            }
        });
    }

    private interface CallbackImageNotif {
        void onSuccess(Bitmap bitmap);

        void onFailed();
    }
}
