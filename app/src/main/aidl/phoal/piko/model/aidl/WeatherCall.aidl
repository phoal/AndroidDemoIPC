package phoal.piko.model.aidl;

import java.util.List;
import phoal.piko.model.aidl.WeatherData;

/**
 * Interface defining the method implemented within WeatherServiceSync
 * that provides synchronous access to the Weather Service web
 * service.
 */
interface WeatherCall {
   /**
    * A two-way (blocking) call that retrieves information about the
    * current weather from the Weather Service web service and returns
    * a List of WeatherData objects containing the results from the
    * Weather Service web service back to the WeatherActivity.  If the
    * @a location isn't found then return a List with size 0.
    */
    List<WeatherData> getCurrentWeather(in String location);
}
