package phoal.piko.view;

import phoal.piko.MVP;
import phoal.piko.R;
import phoal.piko.common.GenericActivity;
import phoal.piko.common.Utils;
import phoal.piko.model.ServiceModel;
import phoal.piko.model.aidl.WeatherData;
import phoal.piko.presenter.DataPresenter;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * This Activity prompts the user for URLs of images to download
 * concurrently via the DataPresenter and view via the
 * DisplayImagesActivity.  It plays the role of the "View" in the
 * Model-View-Presenter (MVP) pattern.  It extends GenericActivity
 * that provides a framework to automatically handle runtime
 * configuration changes of an DataPresenter object, which plays the
 * role of the "Presenter" in the MVP pattern.  The
 * MPV.RequiredViewOps and MVP.ProvidedPresenterOps interfaces are
 * used to minimize dependencies between the View and Presenter
 * layers.
 */
public class DownloadActivity
       extends GenericActivity<MVP.RequiredViewOps,
                               MVP.ProvidedPresenterOps,
                               DataPresenter>
       implements MVP.RequiredViewOps {
    /**
     * Modes of operation
     */
    public static final String DATA_MODE = "dataMode";
    public static final int IMAGE_MODE = 11;
    public static final int WEATHER_MODE = 12;
    // Keep track of operation mode - default = 11.
    private int mode = 11;
    // Keeps track of which service user wants
    private int serviceType = ServiceModel.STARTED_SERVICE;
    /**
     * EditText field for entering the desired URL to an image.
     */
    protected EditText mUrlEditText;
    /**
     * Linear layout to store TextViews displaying URLs.
     */
    protected LinearLayout mLinearLayout;

    /**
     * Display progress to the user.
     */
    protected ProgressBar mLoadingProgressBar;
    private Button mButton2;
    private TextView mError;
    
    /**
     * Menu on main screen
     */
    protected Menu mServiceMenu;
    /**
     * Disables buttons if processing has already started.
     * Clicking a button again after downloadImages() is called will create conflicts;
     * Set mProcessing to true and disable all buttons.
     */
    private Boolean mProcessing = false;
    private ArrayList<View> buttons;

    /**
     * Hook method called when a new instance of Activity is created.
     * One time initialization code goes here, e.g., UI layout
     * initialization and initializing the GenericActivity framework.
     *
     * @param savedInstanceState
     *            Object that contains saved state information.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Perform first part of initializing the super class.
        super.onCreate(savedInstanceState);

        // Perform second part of initializing the super class,
        // passing in the DataPresenter class to instantiate/manage
        // and "this" to provide DataPresenter with the
        // MVP.RequiredViewOps instance.
        super.onCreate(DataPresenter.class, this);

        // Retrieve ServiceType and operation mode from intent
        serviceType = getIntent().getIntExtra(ServiceModel.SERVICE_TYPE, ServiceModel.STARTED_SERVICE);
        getPresenter().setServiceType(serviceType);
        mode = getIntent().getIntExtra(DATA_MODE, IMAGE_MODE);

        // Set the default layout.
        setContentView(R.layout.download_activity);

        // (Re)initialize all the View fields.
        initializeViewFields();

        Log.i(TAG, "Dougs: " + R.raw.dougs);
    }

    /**
     * Hook method called by Android when this Activity becomes
     * invisible.
     */
    @Override
    protected void onDestroy() {
        // Destroy the presenter layer, passing in whether this is
        // triggered by a runtime configuration or not.
        getPresenter().onDestroy(isChangingConfigurations());

        // Always call super class for necessary operations when
        // stopping.
        super.onDestroy();
    }

    /**
     * Initialize the View fields.
     */
    private void initializeViewFields() {
        // Store the ProgressBar in a field for fast access.
        mLoadingProgressBar =
            (ProgressBar) findViewById(R.id.progressBar_loading);
            
        // Store the EditText that holds the urls entered by the user
        // (if any).
        mUrlEditText =
            (EditText) findViewById(R.id.url);

        // Store the linear layout displaying URLs entered.
        mLinearLayout =
            (LinearLayout) findViewById(R.id.linearLayout);
        // Retrieve the url button etc.
        mButton2 = (Button) findViewById(R.id.button2);
        mError = (TextView) findViewById(R.id.errlabel1);
        // Store all buttons to be disabled once processing starts.
        buttons = new ArrayList<>(3);
        buttons.add(findViewById(R.id.downloadFabButton));
        buttons.add(findViewById(R.id.clearFabButton));
        buttons.add(findViewById(R.id.button2));

        // Set the views according to mode
        if (mode == WEATHER_MODE) setViews();
    }
    // Set the view fields according to mode
    private  void setViews() {
        if (mode == WEATHER_MODE) {
            mUrlEditText.setHint(R.string.query);
            mButton2.setText(R.string.weatherButton);
            mButton2.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // Perform action on click
                    getWeather();
                }
            });
            mError.setText("");
        } else {
            mUrlEditText.setHint(R.string.enter_url);
            mButton2.setText(R.string.add_url);
            mError.setText("");
        }
    }

    /**
     * Called by the Android Activity framework when the user presses
     * the "Download" button in the UI.
     *
     * @param view The view.
     */
    public void downloadImages(View view) {
        /**
         * MUST use APPLICATION context NOT activity context - it is ONLY safe to keep a reference to
         * ApplicationContext for duration of download without incurring memory leaks or a nulled
         * reference.
         * Passing it as a reference here avoids call backs to view from presenter during background
         * tasks during config changes.
         */
        Utils.showToast(this, "Download via: " + getPresenter().getServiceDesc());
        mError.setText("Downloading via: " + getPresenter().getServiceDesc());
        getPresenter().startProcessing(getApplicationContext());
    }

    /**
     * Add whatever URL has been entered into the text field if that
     * URL is valid when user presses the "Add URL" button in UI.
     */
    public void addUrl(View view) {

        // Get the user input (if any).
        final String url;
        final String str = mUrlEditText.getText().toString();
        if (str == null || str.equals("")) url = "http://lorempixel.com/400/200";
        else url = str;

        // Do sanity check for syntactic validity of the URL.
        if (URLUtil.isValidUrl(url)) {
            // Add valid URL to running list for download.
            getPresenter().getUrlList().add(Uri.parse(url));

            // (Re)display all the URLs.
            displayUrls();
    	} else 
            Utils.showToast(this, "Invalid URL " + url); }
    // Listener method for button2
    public void getWeather() {
        final String str = mUrlEditText.getText().toString();
        if (str == null || str.equals("")) Utils.showToast(this, "No Location Provided.");
        else {
            getPresenter().setId(str);
            getPresenter().startProcessing(null);
            Utils.showToast(this, "Download via: " + getPresenter().getServiceDesc());
            mError.setText("Downloading via: " + getPresenter().getServiceDesc());
            // Return focus to edit box and select all text in it
            // after query.
            mUrlEditText.requestFocus();
            mUrlEditText.selectAll();
        }
    }

    /**
     * Delete the previously downloaded pictures and directories when
     * user presses the "Delete Downloaded Image(s)" button in the UI.
     */
    public void deleteDownloadedImages(View view) {
        getPresenter().deleteDownloadedImages();
    }
	
    /**
     * Make the ProgressBar visible.
     */
    @Override
    public void displayProgressBar() {
        mLoadingProgressBar.setVisibility(View.VISIBLE);
    }

    /**
     * Make the ProgressBar invisible.
     */
    @Override
    public void dismissProgressBar() {
        mLoadingProgressBar.setVisibility(View.INVISIBLE);
    }

    /**
     * Handle failure to download an image.
     */
    @Override
    public void reportDownloadFailure(Uri url,
                                      boolean downloadsComplete) {
        Utils.showToast(this, "image at " + url.toString() + " failed to download!");
        mError.setText("Image at " + url.toString() + " failed to download!");
        // Remove the URL that failed from the UI.
        removeUrl(url,
                downloadsComplete);

        if (downloadsComplete)
            // Dismiss the progress bar.
            mLoadingProgressBar.setVisibility(View.INVISIBLE);
    }

    /**
     * Remove a URL that couldn't be downloaded.
     */
    private void removeUrl(Uri url,
                           boolean downloadsComplete) {
        // Check if passed URL is in the list of URLs.
        if (getPresenter().getUrlList().contains(url)) {
            // Remove the invalid URL from the list.
            getPresenter().getUrlList().remove(url);
        } else {
            // Warn caller that URL was not in the list.
            Log.w(TAG, 
                  "RemoveUrl() - passed URL ("
                  + (url == null ? "null" : url.toString())
                  + ") is not in URL list.");
        }

        // If there are no more downloads pending dismiss the progress
        // bar.
        if (downloadsComplete)
            mLoadingProgressBar.setVisibility(View.INVISIBLE);

        // (Re)display the URLs provided by the user thus far.
        displayUrls();
    }

    /**
     * Display the URLs provided by the user thus far.
     */
    @Override
    public void displayUrls() {
        if (mLinearLayout != null) {
            // First remove all URL views in the parent LinearLayout
            mLinearLayout.removeAllViews();

            // Add a each URL list entry as a text view child of the
            // parent LinearLayout.
            for (Uri url : getPresenter().getUrlList()) {
                TextView urlTextView = new TextView(this);
                urlTextView.setLayoutParams
                        (new LayoutParams(LayoutParams.WRAP_CONTENT,
                                LayoutParams.WRAP_CONTENT));
                urlTextView.setText(url.toString());
                mLinearLayout.addView(urlTextView);
            }

            // Clear the URL input view.
            mUrlEditText.setText("");
        }
    }

    /**
     * Restart the DisplayImagesActivity to display the results of the
     * download to the user.
     */
    @Override
    public void displayResults(Uri directoryPathname) {
        // Return the intent as an ok Result
        setResult(Activity.RESULT_OK);
        finish();
    }
    @Override
    public void displayResults(WeatherData weatherData,
                               String errorMessage) {
        // Only display the results if we got valid WeatherData.
        if (weatherData == null) {
            Utils.showToast(this,
                    errorMessage);
            mError.setText(errorMessage);
        }
        else {
            // Create an intent that will start an Activity to display
            // the WeatherData to the user.
            final Intent intent = new Intent(this, DisplayWeatherActivity.class);
            intent.putExtra(DisplayWeatherActivity.KEY_WEATHER_DATA, weatherData);
            intent.putExtra(ServiceModel.SERVICE_TYPE, serviceType);

            startActivity(intent);

            mError.setText("");
        }
    }
    /**
     * Disables addUrl and downloadImages buttons if processing has already started
     */
    @Override
    public void disableButtons(Boolean processing) {
        mProcessing = processing;
        for (View view : buttons) {
            view.setClickable(!processing);
            if (!processing) view.setAlpha(1.0f);
        }
    }
}
