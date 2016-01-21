package phoal.piko.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import phoal.piko.MVP;
import phoal.piko.R;
import phoal.piko.common.GenericActivity;
import phoal.piko.common.LifecycleLoggingActivity;
import phoal.piko.common.Utils;
import phoal.piko.model.ServiceModel;
import phoal.piko.model.aidl.WeatherData;
import phoal.piko.presenter.DataPresenter;
import phoal.piko.utils.WeatherUtils;

/**
 * This Activity shows the details of weather for a location provided
 * by the user.  It expects the intent used to start the Activity to
 * contain an extra that holds List of WeatherData objects under the
 * key "KEY_WEATHER_DATA".  Extends LifecycleLoggingActivity so its
 * its lifecycle hook methods are logged automatically.
 */
public class DisplayWeatherActivity
        extends GenericActivity<MVP.RequiredViewOps,
                                MVP.ProvidedPresenterOps,
                                DataPresenter>
        implements MVP.RequiredViewOps {
    /**
     * Custom Action used by Implicit Intent
     *  to call this Activity.
     */
    public static final String ACTION_DISPLAY_WEATHER =
        "phoal.piko.intent.action.DISPLAY_WEATHER";

    /**
     * MIME_TYPE of Weather Data
     */
    public static final String TYPE_WEATHER =
        "parcelable/weather";
	
    /**
     * Key for the List of Weather Data to be displayed
     */
    public static final String KEY_WEATHER_DATA =
        "weatherList";

    private int serviceType = ServiceModel.AIDL_ASYNC;
    private Boolean syncMode = false;
	
    /**
     * Views to hold the Weather Data from Open Weather Map API call.
     */
    private EditText mLocation;
    private Button mGo;
    private TextView mErrorMsg;
    private TextView mDateView;
    private TextView mFriendlyDateView;
    private TextView mLocationName;
    private TextView mDescriptionView;
    private TextView mCelsiusTempView;
    private TextView mFarhenheitTempView;
    private TextView mHumidityView;
    private TextView mWindView;
    private TextView mSunriseView;
    private TextView mSunsetView;
    private ImageView mIconView;

    /**
     * Factory method that makes the implicit intent another Activity
     * uses to call this Activity.
     *
     * @param weatherData
     *            List of WeatherData to be displayed.
     */
    public static Intent makeIntent(WeatherData weatherData) {
        // Create an Intent with a custom action to display
        // WeatherData.
        return new Intent(ACTION_DISPLAY_WEATHER)
                // Set MIME_TYPE to display Weather.
                .setType(TYPE_WEATHER)
                        // Store the list of WeatherData to send to the
                        // DisplayWeatherActivity.
                .putExtra(KEY_WEATHER_DATA,
                        weatherData);
    }
    
    /**
     * Hook method called when a new instance of Activity is created.
     * One time initialization code goes here, e.g., runtime
     * configuration changes.
     *
     * @param savedInstanceState Bundle object that contains saved state information.
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
		
        // Set the content view.
        setContentView(R.layout.weather_main);
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar1);
        setSupportActionBar(mToolbar);
		
        // Initialize all the View fields.
        initializeViewFields();

        // Get the intent that started this activity 
        final Intent intent = getIntent();
        // Get the ServiceType
        serviceType = intent.getIntExtra(ServiceModel.SERVICE_TYPE, ServiceModel.AIDL_ASYNC);
        syncMode = serviceType == ServiceModel.AIDL_SYNC;
        getPresenter().setServiceType(serviceType);
        // Get the Weather Data from the Intent.
        final WeatherData weatherData = intent.getParcelableExtra(KEY_WEATHER_DATA);
        if (weatherData != null )
            // The WeatherData is located in the first element of the ArrayList.
            setViewFields(weatherData);
        else
            // Show error message.
            Utils.showToast(this,
                            "Incorrect Data");
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
     * Initialize all the View fields.
     */
    private void initializeViewFields() {
        mLocation = (EditText) findViewById(R.id.locationQuery);
        mErrorMsg = (TextView) findViewById(R.id.errlabel);
        mGo = (Button) findViewById(R.id.weatherButton);
        mIconView =
            (ImageView) findViewById(R.id.detail_icon);
        mDateView =
            (TextView) findViewById(R.id.detail_date_textview);
        mFriendlyDateView =
            (TextView)findViewById(R.id.detail_day_textview);
        mLocationName =
            (TextView)findViewById(R.id.detail_locationName);
        mDescriptionView =
            (TextView)findViewById(R.id.detail_forecast_textview);
        mCelsiusTempView =
            (TextView) findViewById(R.id.detail_high_textview);
        mFarhenheitTempView=
            (TextView) findViewById(R.id.detail_low_textview);
        mHumidityView =
            (TextView)findViewById(R.id.detail_humidity_textview);
        mWindView =
            (TextView) findViewById(R.id.detail_wind_textview);
        mSunriseView =
            (TextView)findViewById(R.id.detail_sunrise_textview);
        mSunsetView =
            (TextView)findViewById(R.id.detail_sunset_textview);
    }

    /**
     * Set all the View fields from the @a weatherData.
     */
    private void setViewFields(WeatherData weatherData) {
        // Get the City and Country Name
        final String locationName = 
            weatherData.getName()
            + ", "
            + weatherData.getSys().getCountry();
            
        // Update view for Location Name
        mLocationName.setText(locationName);
    		
        // Use weather art image given by its weatherId.
        int weatherId = (int) weatherData.getWeathers().get(0).getId();
        mIconView.setImageResource
            (WeatherUtils.getArtResourceForWeatherCondition(weatherId));

        // Get user-friendly date text.
        final String dateText =
            WeatherUtils.formatCurrentDate();

        // Update views for day of week and date.
        mFriendlyDateView.setText("Today");
        mDateView.setText(dateText);
            
        // Read description and update the view.
        final String description =
            weatherData.getWeathers().get(0).getDescription();
        mDescriptionView.setText(description);
           
        // For accessibility, add a content description to the icon
        // field.
        mIconView.setContentDescription(description);
            
        // Read Sunrise time and update the view.
        final String sunriseText = 
            "Sunrise:  " + 
            WeatherUtils.formatTime(weatherData.getSys().getSunrise());
        mSunriseView.setText(sunriseText);
            
        // Read Sunset time and update the view.
        final String sunsetText = 
            "Sunset:  " 
            +
            WeatherUtils.formatTime(weatherData.getSys().getSunset());
        mSunsetView.setText(sunsetText);
             		
        // Read Temperature in Celsius and Farhenheit 
        final double temp = weatherData.getMain().getTemp();
        final String tempCelsius =
            WeatherUtils.formatTemperature(getApplicationContext(),
                                           temp,
                                           false) 
            + "C";
        final String tempFarhenheit = 
            WeatherUtils.formatTemperature(getApplicationContext(),
                                           temp, 
                                           true)
            + "F";

        // Update the Views to display Celsius and Farhenheit
        // Temperature
        mCelsiusTempView.setText(tempCelsius);
        mFarhenheitTempView.setText(tempFarhenheit);
            
        // Read humidity and update the view.
        final float humidity = weatherData.getMain().getHumidity();
        mHumidityView.setText
            (getString(R.string.format_humidity,
                       humidity));

        // Read wind speed and direction and update the view.
        final double windSpeedStr =
            weatherData.getWind().getSpeed();
        final double windDirStr =
            weatherData.getWind().getDeg();
        mWindView.setText
            (WeatherUtils.getFormattedWind(this,
                                           windSpeedStr,
                                           windDirStr));
    }

    /**
     * Set up App Bar
     */
    @Override
    public boolean onCreateOptionsMenu (Menu menu) {
        // Inflate the menu items for use in the action bar
        getMenuInflater().inflate(R.menu.weather_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // Handle camera icon click
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.aidl_sync: {
                syncMode = true;
                getPresenter().setServiceType(ServiceModel.AIDL_SYNC);
                return true;
            }
            case R.id.aidl_async: {
                syncMode = false;
                getPresenter().setServiceType(ServiceModel.AIDL_SYNC);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    // Listener method for Go button
    public void getWeather(View view) {
        final String str = mLocation.getText().toString();
        if (str == null || str.equals("")) Utils.showToast(this, "No Location Provided.");
        else {
            getPresenter().setId(str);
            getPresenter().startProcessing(null);
            //mErrorMsg.setText("Downloading via: " + getPresenter().getServiceDesc());
            Utils.showToast(this, "Download via: " + getPresenter().getServiceDesc());
            // Return focus to edit box and select all text in it
            // after query.
            mLocation.requestFocus();
            mLocation.selectAll();
        }
    }
    @Override
    public void displayResults(WeatherData weatherData,
                               String errorMessage) {
        // Only display the results if we got valid WeatherData.
        if (weatherData == null) {
            Utils.showToast(this,
                    errorMessage);
            mErrorMsg.setText(errorMessage);
        }
        else {
            setViewFields(weatherData);
            mErrorMsg.setText("");
        }
    }
    /**
     * Implement MVP RequiredViewOps
     */
    public void displayProgressBar() {}
    public void dismissProgressBar(){}
    public void displayUrls(){}
    public void reportDownloadFailure(Uri url, boolean downloadsComplete) { }
    public void displayResults(Uri directoryPathname){}
    public void disableButtons(Boolean processing){}
}
