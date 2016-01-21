package phoal.piko.model;

import android.content.Context;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.List;

import phoal.piko.MVP;
import phoal.piko.common.GenericAsyncTask;
import phoal.piko.common.GenericAsyncTaskOps;
import phoal.piko.common.GenericServiceConnection;
import phoal.piko.model.ModelImpl;
import phoal.piko.model.aidl.WeatherCall;
import phoal.piko.model.aidl.WeatherData;
import phoal.piko.model.aidl.WeatherRequest;
import phoal.piko.model.aidl.WeatherResults;
import phoal.piko.model.services.WeatherServiceAsync;
import phoal.piko.model.services.WeatherServiceSync;

/**
 * Created by phoal on 8/01/2016.
 *
 * This class implements the downloading of weather data using an
 * AIDL two-way call (Synchronous) to a Service.  It plays the role of the "Concrete
 * Implementor" in the Bridge pattern.
 */

public class AidlImplSync extends ModelImpl implements GenericAsyncTaskOps<String, Void, WeatherData> {

    /**
     * Debugging tag used by the Android logger.
     */
    /**
     * Debugging tag used by the Android logger.
     */
    protected final static String TAG = AidlImplSync.class.getSimpleName();

    /**
     * A WeakReference used to access methods in the Presenter layer.
     * The WeakReference enables garbage collection.
     */
    protected WeakReference<MVP.RequiredPresenterOps> mPresenter;

    /**
     * Location we're trying to get current weather for.
     */
    private String mLocation;
    /**
     * This GenericServiceConnection is used to receive results after
     * binding to the WeatherServiceSync Service using bindService().
     */
    private GenericServiceConnection<WeatherCall> mServiceConnectionSync;

    /**
     * TWO-WAY AIDL calls are SYNCHRONOUS FROM THE CALLING THREAD EVEN IF THE CALL IS TO ANOTHER
     * PROCESS EXECUTING IN ANOTHER THREAD. Use an AsyncTask so the main thread is not blocked.
     *
     */
    private GenericAsyncTask<String,
            Void,
            WeatherData,
            AidlImplSync> mAsyncTask;
    /**
     * Hook method called when a new WeatherModel instance is created
     * to initialize the ServiceConnections and bind to the WeatherService*.
     *
     * @param presenter
     *            A reference to the Presenter layer.
     */
    @Override
    public void onCreate(MVP.RequiredPresenterOps presenter) {
        // Set the WeakReference.
        mPresenter = new WeakReference<>(presenter);

        // TODO -- you fill in here to initialize the WeatherService*.

        // Initialize the GenericServiceConnection objects.
        mServiceConnectionSync = new GenericServiceConnection<WeatherCall>(WeatherCall.class);
        // Bind to the services.
        bindService();
    }

    /**
     * Hook method called to shutdown the Presenter layer.
     */
    @Override
    public void onDestroy(boolean isChangingConfigurations) {
        // Don't bother unbinding the service if we're simply changing configurations.
        if (isChangingConfigurations)
            Log.d(TAG, "Simply changing configurations, no need to destroy the Service");
        else unbindService();
    }
    /**
     * Initiate the service binding protocol.
     */
    @Override
    protected void bindService() {
        Log.d(TAG, "calling bindService()");

       if (mServiceConnectionSync.getInterface() == null)
            // Use ApplicationContext here since binding can extend across lifecycle of many Activities
            mPresenter.get().getApplicationContext().bindService
                    (WeatherServiceSync.makeIntent(mPresenter.get()
                            // Prefer ActivityContext for Intent constructor since it is a one-off call on
                            // behalf of current Activity
                            .getActivityContext()), mServiceConnectionSync, Context.BIND_AUTO_CREATE);
    }
    @Override
    public void startProcess(Context context, Uri url, String id) {
        // Check to see if there's already a call in progress.
        if (mAsyncTask != null ) {}
        else {
            // Create and execute the AsyncTask to expand the weather
            // without blocking the caller.
            mAsyncTask = new GenericAsyncTask<>(this);
            mAsyncTask.execute(id);
        }
    }
    /**
     * Retrieve the expanded weather results via a synchronous two-way
     * method call, which runs in a background thread to avoid
     * blocking the UI thread.
     */
    public WeatherData doInBackground(String... locations) {
        mLocation = locations[0];
        return getWeatherSync(mLocation);
    }

    /**
     * Display the results in the UI Thread.
     */
    public void onPostExecute(WeatherData weatherData) {
        /**
         * This runs on the Main thread so the presenter's method can be called directly to access the view.
         */

        String msg = "No weather data for location \""
                + mLocation
                + "\" found";
        if (weatherData != null && weatherData.getMessage() != null) {
            msg = weatherData.getMessage();
            mPresenter.get().displayResults(null, msg);
        } else  mPresenter.get().displayResults(weatherData, msg);

        mAsyncTask = null;
    }
    /**
     * Initiate the service unbinding protocol.
     */
    @Override
    protected void unbindService() {
        Log.d(TAG, "calling unbindService()");
        // Unbind the Async Service if it is connected.
        if (mServiceConnectionSync.getInterface() != null)
            mPresenter.get().getApplicationContext().unbindService(mServiceConnectionSync);
    }
    /**
     * Initiate the synchronous weather lookup.
     */
    public WeatherData getWeatherSync(String location) {
        try {
            final WeatherCall weatherCall =
                    mServiceConnectionSync.getInterface();

            if (weatherCall != null) {
                // Invoke a two-way AIDL call, which blocks the caller.
                List<WeatherData> resultList = weatherCall.getCurrentWeather(location);
                // resultList.get(0) may contain a WeatherData object with an error message - the
                // Presenter will check for this.
                return (resultList == null || resultList.size() == 0) ? null : resultList.get(0);
            }
            else
                Log.d(TAG, "mWeatherCall was null.");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }
}


