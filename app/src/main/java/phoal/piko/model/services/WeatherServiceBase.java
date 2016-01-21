package phoal.piko.model.services;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import phoal.piko.common.ExecutorServiceTimeoutCache;
import phoal.piko.common.GenericSingleton;
import phoal.piko.common.LifecycleLoggingService;
import phoal.piko.model.aidl.WeatherData;
import phoal.piko.model.aidl.WeatherDataJsonParser;
import android.util.Log;

/**
 * This is the super class for both WeatherServiceSync and
 * WeatherServiceAsync.  It factors out fields and methods that are
 * shared by both Service implementations.
 */
public class WeatherServiceBase 
       extends LifecycleLoggingService {
    /**
     * Appid needed to access the service.  TODO -- fill in with your Appid.
     */
    private final String mAppid = "c93c173bfe54bbe2e9045ee1ca9a29f1";
    // private final String mAppid ="9";
    /**
     * URL to the Weather Service web service.
     */
    private String mWeatherServiceURL =
        "http://api.openweathermap.org/data/2.5/weather?&APPID="
        + mAppid + "&q=";

    /**
     * Default timeout is 10 seconds, after which the Cache data
     * expires.  In a production app this value should be much higher
     * (e.g., 10 minutes) - we keep it small here to help with
     * testing.
     */
    private int DEFAULT_CACHE_TIMEOUT = 10;

    /**
     * Define a class that will cache the WeatherData since it doesn't
     * change rapidly.  This class is passed to the
     * GenericSingleton.instance() method to retrieve the one and only
     * instance of the WeatherCache.
     */
    public static class WeatherCache 
           extends ExecutorServiceTimeoutCache<String, List<WeatherData>> {}

    /**
     * Hook method called when the Service is created.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        // TODO -- you fill in here.

        // Increment the reference count for the WeatherCache
        // singleton, which is shared by both Services.
        GenericSingleton.instance(WeatherCache.class).incrementRefCount();
    }

    /**
     * Hook method called when the last client unbinds from the
     * Service.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        // TODO -- you fill in here.

        // Decrement the reference count for the AcronymCache
        // singleton, which shuts it down when the count drops to 0.
        // When this happens, the GenericSingleton needs to remove the
        // AcronymCache.class entry in its map.
        if (GenericSingleton.instance(WeatherCache.class).decrementRefCount() == 0)
            GenericSingleton.remove(WeatherCache.class);
    }

    /**
     * Conditionally queries the Weather Service web service to obtain
     * a List of WeatherData corresponding to the @a location if it's
     * been more than 10 seconds since the last query to the Weather
     * Service.  Otherwise, simply return the cached results.
     */
    protected List<WeatherData> getWeatherResults(String location) {
        Log.d(TAG,
                "Looking up results in the cache for " + location);

        // TODO -- you fill in here.
        /**
         * Try to get the results from the WeatherCache.
         * This method is called from different threads so WeatherCache uses a ConcurrentHashMap to
         * ensure THREAD SAFE EXECUTION.
         *
         * Since Weather Data is rapidly changing, results are not persisted beyond the current session.
         */
        List<WeatherData> results =
                GenericSingleton.instance(WeatherCache.class).get(location);

        if (results != null) {
            Log.d(TAG, "Getting results from the cache for " + location);

            // Return the results from the cache.
            return results;
        } else {
            Log.d(TAG,
                    "Getting results from the Weather Service for " + location);

            // The results weren't already in the cache or were
            // "stale", so obtain them from the Weather Service.
            results = getResultsFromWeatherService(location);

            if (results != null)
                // Store the results into the cache for up to
                // DEFAULT_CACHE_TIMEOUT seconds based on the location
                // and return the results.
                GenericSingleton.instance(WeatherCache.class).put
                        (location,
                                results,
                                DEFAULT_CACHE_TIMEOUT);
            return results;
        }
    }

    /**
     * Actually query the Weather Service web service to get the
     * current WeatherData.  Usually only returns a single element in
     * the List, but can return multiple elements if they are sent
     * back from the Weather Service.
     */
    private List<WeatherData> getResultsFromWeatherService(String location) {
        // Create a List that will return the WeatherData obtained
        // from the Weather Service web service.
        List<WeatherData> returnList = null;
            
        try {
            // Create a URL that points to desired location the
            // Weather Service.
            URL url = new URL(mWeatherServiceURL + location);
            final URI uri = new URI(url.getProtocol(),
                                    url.getUserInfo(),
                                    url.getHost(),
                                    url.getPort(),
                                    url.getPath(),
                                    url.getQuery(),
                                    url.getRef());
            url = uri.toURL();

            // Opens a connection to the Weather Service.
            HttpURLConnection urlConnection =
                (HttpURLConnection) url.openConnection();

            // Sends the GET request and returns a stream containing
            // the Json results.
            try (InputStream in =
                 new BufferedInputStream(urlConnection.getInputStream())) {
                    // Create the parser.
                 final WeatherDataJsonParser parser =
                     new WeatherDataJsonParser();
            
                // Parse the Json results and create List of
                // WeatherData objects.
                returnList = parser.parseJsonStream(in);
            } finally {
                urlConnection.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("WeatherServiceBase", e.toString() + ": " + e.getMessage());
            // create and return an object with a useful error message.
            return createWeatherDatas("I/O Problem: " + e.toString());
        }

        /**
         *   Return the List containing the JSON parsed WeatherData object.
         *   The list could be null or empty or contain:
         *   A WeatherData object which is either:
         *   1. Initiated with the necessary data and a NULL (error) message OR
         *   2. Initiated with NULL FIELDS EXCEPT the parsed (ERROR) MESSAGE.
         *
         *   It must be handled appropriately.
         */
            return returnList;

    }
    public static ArrayList<WeatherData> createWeatherDatas(String msg) {
        ArrayList<WeatherData> weatherDatas = new ArrayList<WeatherData>(1);
        ArrayList<WeatherData.Weather> weathers = new ArrayList<>(1);
        weathers.add(new WeatherData.Weather());
        WeatherData weatherData = new WeatherData(msg, null, 0L, 0L ,new WeatherData.Sys(),
                new WeatherData.Main(), new WeatherData.Wind(), weathers);
        weatherDatas.add(weatherData);
        return weatherDatas;
    }
}
