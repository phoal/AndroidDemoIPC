package phoal.piko.model;

import android.content.Context;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.List;

import phoal.piko.MVP;
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

public class AidlImplAsync extends ModelImpl {
    /**
     * Flag indicating whether CONCURRENT REQUESTS are allowed.
     */
    private Boolean allowConcurrentRequests = false;
    /**
     * Debugging tag used by the Android logger.
     */
    /**
     * Debugging tag used by the Android logger.
     */
    protected final static String TAG =
            AidlImplAsync.class.getSimpleName();

    /**
     * A WeakReference used to access methods in the Presenter layer.
     * The WeakReference enables garbage collection.
     */
    protected WeakReference<MVP.RequiredPresenterOps> mPresenter;

    /**
     * Location we're trying to get current weather for.
     */
    private String mLocation;

    // TODO -- define ServiceConnections to connect to the WeatherServiceSync.
    /**
     * This GenericServiceConnection is used to receive results after
     * binding to the WeatherServiceAsync Service using bindService().
     */
    private GenericServiceConnection<WeatherRequest> mServiceConnectionAsync;


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

        mServiceConnectionAsync = new GenericServiceConnection<WeatherRequest>(WeatherRequest.class);

        // Bind to the services.
        bindService();
    }

    /**
     * Hook method called to shutdown the Presenter layer.
     */
    @Override
    public void onDestroy(boolean isChangingConfigurations) {
        // Don't bother unbinding the service if we're simply changing
        // configurations.
        if (isChangingConfigurations)
            Log.d(TAG,
                    "Simply changing configurations, no need to destroy the Service");
        else
            unbindService();
    }

    /**
     * The implementation of the WeatherResults AIDL Interface, which
     * will be passed to the Weather Web service using the
     * WeatherRequest.getCurrentWeather() method.
     *
     * This implementation of WeatherResults.Stub plays the role of
     * Invoker in the Broker Pattern since it dispatches the upcall to
     * sendResults().
     */
    private final WeatherResults.Stub mWeatherResults =
            new WeatherResults.Stub() {
                /**
                 * This method is invoked by the WeatherServiceAsync to
                 * return the results back.
                 */
                @Override
                public void sendResults(final WeatherData weatherResults)
                        throws RemoteException {
                    /**
                     * Pass the results back to the Presenter's displayResults() method which calls
                     * the view - IT MUST BE CALLED ON UI THREAD.
                     * These are sent from another thread ( usually the aidl thread pool ).
                     * This ModelImpl was created on the UI Thread so it's handler will run on it.
                      */

                    // TODO -- you fill in here.
                    post(new Runnable() {
                        public void run() {
                            mPresenter.get().displayResults(weatherResults, null);
                        }
                    });
                }

                /**
                 * This method is invoked by the WeatherServiceAsync to
                 * return error results back - NOTE THE NEED FOR A UI THREAD HANDLER.
                 */
                @Override
                public void sendError(final String reason)
                        throws RemoteException {
                    // Pass the results back to the Presenter's
                    // displayResults() method.
                    // TODO -- you fill in here.
                    post(new Runnable() {
                        public void run() {
                            mPresenter.get().displayResults(null, reason);
                        }
                    });
                }
            };

    /**
     * Initiate the service binding protocol.
     */
    protected void bindService() {
        Log.d(TAG,
                "calling bindService()");

        // Launch the Weather Bound Services if they aren't already
        // running via a call to bindService(), which binds this
        // activity to the WeatherService* if they aren't already
        // bound.

        // TODO -- you fill in here.

        if (mServiceConnectionAsync.getInterface() == null)
            // Use ApplicationContext here since binding can extend across lifecycle of many Activities
            mPresenter.get().getApplicationContext().bindService
                    (WeatherServiceAsync.makeIntent(mPresenter.get()
                            // Prefer ActivityContext for Intent constructor since it is a one-off
                            // call on behalf of current Activity
                            .getActivityContext()), mServiceConnectionAsync, Context.BIND_AUTO_CREATE);
    }

    /**
     * Initiate the service unbinding protocol.
     */
    protected void unbindService() {
        Log.d(TAG,
                "calling unbindService()");

        // TODO -- you fill in here to unbind from the WeatherService*.

        // Unbind the Async Service if it is connected.
        if (mServiceConnectionAsync.getInterface() != null)
            mPresenter.get().getApplicationContext().unbindService(mServiceConnectionAsync);
    }

    /**
     * Initiate the asynchronous weather lookup.
     *
     * The Async Service is set up to handle concurrent requests either through the ExecutorService
     * set up in onCreate which will handle requests if it's running in the same process OR
     * the AIDL thread pool if it's in a different process.
     * However for a given location, requests only need to be actioned every 10sec, so to save resources
     * concurrent requests are prevented.
     *
     * ALSO  the view is currently set up to only handle one request at a time.
     *
     * CONCURRENT REQUESTS CAN BE ALLOWED BY SETTING allowConcurrentRequests TO TRUE.
     */
    public boolean getWeatherAsync(String location) {
        // TODO -- you fill in here.

        // Get a reference to the AcronymRequest interface.
        final WeatherRequest weatherRequest  = mServiceConnectionAsync.getInterface();

        if (weatherRequest != null) {
            try {
                // Invoke a one-way AIDL call that doesn't block the
                // caller.  Results are returned via the sendResults()
                // or sendError() methods of the WeatherResults
                // callback object, which runs in a Thread from the
                // Thread pool managed by the Binder framework.
                weatherRequest.getCurrentWeather(location, mWeatherResults);
            } catch (RemoteException e) {
                Log.e(TAG,
                        "RemoteException:"
                                + e.getMessage());
            }
        } else
            Log.d(TAG,
                    "WeatherRequest was null.");
        return true;
    }
    @Override
    public void startProcess(Context context, Uri url, String id) {
       getWeatherAsync(id);
    }
}
