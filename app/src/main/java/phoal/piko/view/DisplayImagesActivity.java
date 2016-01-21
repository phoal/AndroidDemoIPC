package phoal.piko.view;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import phoal.piko.R;
import phoal.piko.common.Utils;
import phoal.piko.model.ServiceModel;
import phoal.piko.utils.loader.ImageLoader;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

/**
 * An Activity that displays all the images that have been downloaded
 * and processed.
 */
public class DisplayImagesActivity extends AppCompatActivity {
    private final String TAG = "DisplayImagesActivity";
    private static final long ALARM_INTERVAL = 10000L;
    private static final long INITIAL_INTERVAL = 10000L;
    static final int REQUEST_TAKE_PHOTO = 33;
    static final int REQUEST_URI = 34;
    public static final int ADJUST_PARAM = 50;
    public static final int ADJUST = 6;
    public static final int xWIDTH = 50;
    public static final String PATH = "path";
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private AlarmManager mAlarmManager;
    private Intent mReminderReceiverIntent;
    private PendingIntent mReminderReceiverPendingIntent;
    /**
     * Name of the Intent Action that wills start this Activity.
     */
    public static String ACTION_DISPLAY_IMAGES =
        "android.intent.action.DISPLAY_IMAGES";

    /**
     * The column width to use for the GridView.
     */
    private int mColWidth;

    /**
     * The number of columns to use in the GridView.
     */
    private int mNumCols;

    /**
     * A reasonable column width.
     */
    public static final int COL_WIDTH = 300;

    /**
     * The adapter responsible for loading the results into the GridView.
     */
    private ImageAdapter imageAdapter;

    /**
     * The file path in external storage storing images to display
     */
    private File currentFile;
    
    /**
     * ImageLoader used to load images in the background
     */
    private ImageLoader mLoader;

    // Keeps track of Type of Service to do asynchronous download
    private int serviceType = ServiceModel.STARTED_SERVICE;

    /**
     * Creates the activity and generates a button for each filter
     * applied to the images. These buttons load change the
     * imageAdapter's source to a new directory, from which it will
     * load images into the GridView.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a Grid layout manager
        mLayoutManager = new GridLayoutManager(this, getCols());//, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Initialize the image loader
        mLoader = new ImageLoader(getResources().getDrawable(R.drawable.loading));
        /**
         * specify an adapter *** mLayoutManager MUST be set up first *** SEE getCols()
          */
        imageAdapter = new ImageAdapter(this, mLoader, mColWidth);
        imageAdapter.setImages(true);
        mRecyclerView.setAdapter(imageAdapter);

        // Get Alarm ready to schedule.
        setUpAlarm();
        // Cancel any alarms set last session.
        mAlarmManager.cancel(mReminderReceiverPendingIntent);
    }

    @Override
    protected void onPause() {
         // Set the repeating alarm whenever App is removed from foreground.
         scheduleAlarm();
         super.onPause();
    }

    /**
     * Set up App Bar
     */
    @Override
    public boolean onCreateOptionsMenu (Menu menu) {
        // Inflate the menu items for use in the action bar
        getMenuInflater().inflate(R.menu.thumb_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // Handle camera icon click
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.cam_item: {
                takeImage();
                return true;
            }
            case R.id.download: {
                getUri(serviceType);
                return true;
            }
            case R.id.delete: {
                Utils.showToast(DisplayImagesActivity.this, "All files will be permanently deleted!!!");
                imageAdapter.showAlert(-1);
                return true;
            }
            case R.id.asynctask : {
                serviceType = ServiceModel.ASYNCTASK;
                getUri(ServiceModel.ASYNCTASK);
                return true;
            }
            case R.id.started_service : {
                serviceType = ServiceModel.STARTED_SERVICE;
                getUri(ServiceModel.STARTED_SERVICE);
                return true;
            }
            case R.id.bound_service : {
                serviceType = ServiceModel.BOUND_SERVICE;
                getUri(ServiceModel.BOUND_SERVICE);
                return true;
            }
            case R.id.aidl_sync1 : {
                getWeather(ServiceModel.AIDL_SYNC);
                return true;
            }case R.id.aidl_async1 : {
                getWeather(ServiceModel.AIDL_ASYNC);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // DEALING WITH THE PHOTO FILES

    // Get the selfie image from camera.
    private void takeImage() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            Uri photoUri = null;
            try {
                photoUri = createImageUri();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoUri != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }
    // Create a unique name
    private Uri createImageUri() throws IOException {
        // Create an image file name with 2 parts: Unique_Id + jpg.
        //  .jpg will be used to identify them as images by a FileFilter.
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
       Log.i(TAG, timeStamp);
        File image = File.createTempFile(
            timeStamp,  /* prefix */
            ".jpg",         /* suffix */
            new File(ServiceModel.DIR_PATH.getPath())    /* directory */
        );

        // Save a file reference for display use.
        currentFile = image;
        return Uri.fromFile(image);
    }

    /**
     * Use DownloadActivity to download an image
     * @param service = serviceType to download with
     */
    private void getUri(int service) {
        Intent intent = new Intent(this, DownloadActivity.class);
        intent.putExtra(ServiceModel.SERVICE_TYPE, service);
        intent.putExtra(DownloadActivity.DATA_MODE, DownloadActivity.IMAGE_MODE);
        startActivityForResult(intent, REQUEST_URI);

    }

    /**
     * Use DownloadActivity to get weather data
     */
    private void getWeather(int serviceType) {
        Intent intent = new Intent(this, DownloadActivity.class);
        intent.putExtra(DownloadActivity.DATA_MODE, DownloadActivity.WEATHER_MODE);
        intent.putExtra(ServiceModel.SERVICE_TYPE, serviceType);
        startActivity(intent);
    }

    // Handle the return from the camera - get the DownloaderTask to retrieve file.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO: {
                if (resultCode == Activity.RESULT_OK) imageAdapter.addImage(new ImageAdapter.Image(currentFile));
                else currentFile.delete();
            }
            case REQUEST_URI: {
                if (resultCode == Activity.RESULT_OK) imageAdapter.setImages(false);
            }
        }
    }
    /**
     * Factory method that returns an implicit Intent for displaying
     * images.
     */
    public static Intent makeIntent(Uri directoryPathname) {
        return new Intent(ACTION_DISPLAY_IMAGES)
            .setDataAndType(directoryPathname,
                            "image/*");
    }

    /**
     * Configures the GridView with an appropriate column number and
     * width based on the screen size.
     */
    private void configureGridView(GridView imageGrid) {
        // Retrieve the Screen dimensions.
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        // Calculate appropriate values.
        mNumCols = size.x / COL_WIDTH;
        mColWidth = size.x / mNumCols;

        // Configure the GridView with dynamic values.
        imageGrid.setColumnWidth(mColWidth);
        imageGrid.setNumColumns(mNumCols);

        //((ImageAdapter) imageGrid.getAdapter()).setColWidth(mColWidth);
    }
    private int getCols() {
        // Retrieve the Screen dimensions.
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        // Calculate appropriate values.
        mNumCols = size.x / COL_WIDTH;
        mColWidth = size.x / mNumCols;
        return mNumCols;

    }

    /**
     * Schedules a repeating alarm which fires every ALARM_INTERVAL - the first one is fired AFTER
     * the INITIAL_INTERVAL has elapsed;
     */
    private  void scheduleAlarm() {
        mAlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + INITIAL_INTERVAL,
                ALARM_INTERVAL,
                mReminderReceiverPendingIntent);
    }
    // Set up alarm
    private void setUpAlarm() {
        // Get the AlarmManager Service
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        // Create an Intent to broadcast to the AlarmNotificationReceiver
        mReminderReceiverIntent = new Intent(DisplayImagesActivity.this,
                ReminderReceiver.class);

        // Create a PendingIntent that holds the mReminderReceiverIntent
        mReminderReceiverPendingIntent = PendingIntent.getBroadcast(
                DisplayImagesActivity.this, 0, mReminderReceiverIntent, 0);
    }
    /**
     * @class ImageAdapter
     *
     * @brief The Adapter that loads the Images into the Layout's GridView.
     */
    public class ImageAdapter1 extends BaseAdapter {
    	/**
    	 * File path of the directory holding the images to display
    	 */
        private String mFilePath = null;

        /**
         * The Context of the application
         */
        private Context mContext;

        /**
         * The padding each image will have around it
         */
        private int mPadding = 0;

        /**
         * The image files being displayed
         */
        private File[] mBitmapFiles;
        
        /**
         * Creates the ImageAdapter in the given context.
         */
        public ImageAdapter1(Context c) {
            mContext = c;
            mBitmapFiles = new File[] {};
        }
        
        /**
         * Returns the count of bitmaps in the list.
         */
        @Override
        public int getCount() {
            return mBitmapFiles.length;
        }

        /**
         * Returns the bitmap at the given position.
         */
        @Override
        public Object getItem(int position) {
            return mBitmapFiles[position];
        }

        /**
         * Returns the given position as the Id of the bitmap. This works
         * because the bitmaps are stored in a sequential manner.
         */
        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * Returns the view. This method is necessary for filling the
         * GridView appropriately.
         */
        @Override
        public View getView(final int position,
                            View convertView,
                            ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(mContext);

                // Set configuration properties of the ImageView
                imageView.setLayoutParams(new GridView.LayoutParams(mColWidth,
                                                                    mColWidth));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(mPadding,
                                     mPadding,
                                     mPadding,
                                     mPadding);

                // Implement onClick to start an a SwipeListDisplay at
                // the current image.
                imageView.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(DisplayImagesActivity.this,
                                ViewPagerActivity.class).setData(Uri.parse(mFilePath)).putExtra(
                                ViewPagerActivity.CURRENT_IMAGE_POSITION, position));
                        }
                    });
            } else 
                imageView = (ImageView) convertView;

            // Load the image in the background
            mLoader.loadAndDisplayImage(imageView, 
                                        mBitmapFiles[position]
                                            .getAbsolutePath(), 
                                        mColWidth);
            return imageView;
        }

        /**
         * Maximum width of a column.
         */
        private int mColWidth = 100;

        /**
         * Set the maximum width of a column.
         */
        public void setColWidth(int w) {
            if (w > 0)
                mColWidth = w;
        }
        /**
         * Resets the bitmaps of the GridView to the ones found at the
         * given filterPath.
         */
        private void setBitmaps(String filterPath) {
            mFilePath = filterPath;
            mBitmapFiles = new File(filterPath).listFiles();

            notifyDataSetChanged();
        }
        private void addUri(Uri uri) {

            notifyDataSetChanged();
        }
    }
    public ImageAdapter getImageAdapter() {return imageAdapter;}
}
