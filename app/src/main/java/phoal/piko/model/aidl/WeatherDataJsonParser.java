package phoal.piko.model.aidl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import phoal.piko.model.aidl.WeatherData.Main;
import phoal.piko.model.aidl.WeatherData.Sys;
import phoal.piko.model.aidl.WeatherData.Weather;
import phoal.piko.model.aidl.WeatherData.Wind;
import android.util.JsonReader;
import android.util.JsonToken;

/**
 * Parses the Json weather data returned from the Weather Services API
 * and returns a List of WeatherData objects that contain this data.
 */
public class WeatherDataJsonParser {
    /**
     * Used for logging purposes.
     */
    private final String TAG =
        this.getClass().getCanonicalName();

    /**
     * Parse the @a inputStream and convert it into a List of JsonWeather
     * objects.
     */
    public List<WeatherData> parseJsonStream(InputStream inputStream)
        throws IOException {

        // TODO -- you fill in here.
        /** Create a JsonReader for the inputStream.
         *  JsonReader implements Closeable - so use a try-with-resources statement to ensure that
         *  reader.close() is GUARANTEED after method returns OR throws an exception.
         */

        try (JsonReader reader =
                     new JsonReader(new InputStreamReader(inputStream,
                             "UTF-8"))) {
            // Log.d(TAG, "Parsing the results returned as an array");

            // List may be empty OR contain a NON-INITIATED WeatherData object.
            return parseJsonWeatherDataArray(reader);
        }
        // finally {reader.close()} called automatically via try-with-resources statement.

    }

    /**
     * Parse a Json stream and convert it into a List of WeatherData
     * objects.
     */
    public List<WeatherData> parseJsonWeatherDataArray(JsonReader reader)
        throws IOException {
        // TODO -- you fill in here.
        ArrayList<WeatherData> weatherDatas = new ArrayList<WeatherData>();
        if (reader.peek() == JsonToken.BEGIN_OBJECT) {
            // It's just a single object - skip to next method.
            weatherDatas.add(parseJsonWeatherData(reader));
            return weatherDatas;
        }
        else { // this is a batched request - CURRENTLY NEVER USED by this App!
            reader.beginArray();
            try {
                // Check array not empty
                if (reader.peek() != JsonToken.END_ARRAY)
                    while (reader.hasNext()) {
                        weatherDatas.add(parseJsonWeatherData(reader));
                    }
                // Return ArrayList which could still be empty.
                return weatherDatas;
            } finally {
                reader.endArray();
            }
        }
    }

    /**
     * Parse a Json stream and return a WeatherData object.
     */
    public WeatherData parseJsonWeatherData(JsonReader reader) 
        throws IOException {

        // TODO -- you fill in here.
        WeatherData weatherData = new WeatherData();
        reader.beginObject();
        // No check here for JsonToken.END_OBJECT - it will just go on and return an empty list.
        try {
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case WeatherData.name_JSON:
                        weatherData.setName(reader.nextString());
                        break;
                    case WeatherData.dt_JSON:
                        weatherData.setDate(reader.nextLong());
                        break;
                    case WeatherData.cod_JSON:
                        weatherData.setCod(reader.nextLong());
                        break;
                    case WeatherData.message_JSON:
                        weatherData.setMessage(reader.nextString());
                        break;
                    case WeatherData.weather_JSON:
                        weatherData.setWeathers(parseWeathers(reader));
                        break;
                    case WeatherData.main_JSON:
                        weatherData.setMain(parseMain(reader));
                        break;
                    case WeatherData.wind_JSON:
                        weatherData.setWind(parseWind(reader));
                        break;
                    case WeatherData.sys_JSON:
                        weatherData.setSys(parseSys(reader));
                        break;
                    default:
                        reader.skipValue();
                        // Log.d(TAG, "ignoring " + name);
                        break;
                }
            }
            return weatherData;
        } finally {
            reader.endObject();
            /**
             * Could return a DEFAULT CONSTRUCTOR WeatherData object with all fields null
             */
        }

    }
    
    /**
     * Parse a Json stream and return a List of Weather objects.
     */
    public List<Weather> parseWeathers(JsonReader reader) throws IOException {
        // TODO -- you fill in here.
        ArrayList<Weather> weathers = new ArrayList<>();
        reader.beginArray();
        // No check here for JsonToken.END_ARRAY - it will just go on and return an empty list.
        try {
            while (reader.hasNext()) {
                weathers.add(parseWeather(reader));
            }
            return weathers;
        } finally {
            reader.endArray();
        }
    }

    /**
     * Parse a Json stream and return a Weather object.
     */
    public Weather parseWeather(JsonReader reader) throws IOException {
        // TODO -- you fill in here.
        Weather weather = new Weather();
        reader.beginObject();
        // No check here for JsonToken.END_OBJECT - it will just go on and return a default object.
        try {
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case Weather.id_JSON:
                        weather.setId(reader.nextLong());
                        break;
                    case Weather.main_JSON:
                        weather.setMain(reader.nextString());
                        break;
                    case Weather.description_JSON:
                        weather.setDescription(reader.nextString());
                        break;
                    case Weather.icon_JSON:
                        weather.setIcon(reader.nextString());
                        break;
                    // unexpected object - ignore.
                    default:
                        reader.skipValue();
                        // Log.d(TAG, "ignoring " + name);
                        break;
                }
            }
            return weather;
        } finally {
            reader.endObject();
        }
    }

    /**
     * Parse a Json stream and return a Main Object.
     */
    public Main parseMain(JsonReader reader) 
        throws IOException {
        // TODO -- you fill in here.
        Main main = new Main();
        reader.beginObject();
        // No check here for JsonToken.END_OBJECT - it will just go on and return a default object.
        try {
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case Main.temp_JSON:
                        main.setTemp(reader.nextDouble());
                        break;
                    case Main.humidity_JSON:
                        main.setHumidity(reader.nextLong());
                        break;
                    case Main.pressure_JSON:
                        main.setPressure(reader.nextDouble());
                        break;
                    // unexpected object - ignore.
                    default:
                        reader.skipValue();
                        // Log.d(TAG, "ignoring " + name);
                        break;
                }
            }
            return main;
        } finally {
            reader.endObject();
        }
    }

    /**
     * Parse a Json stream and return a Wind Object.
     */
    public Wind parseWind(JsonReader reader) throws IOException {
        // TODO -- you fill in here.
        Wind wind = new Wind();
        reader.beginObject();
        // No check here for JsonToken.END_OBJECT - it will just go on and return a default object.
        try {
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case Wind.speed_JSON:
                        wind.setSpeed(reader.nextDouble());
                        break;
                    case Wind.deg_JSON:
                        wind.setDeg(reader.nextDouble());
                        break;
                    // unexpected object - ignore.
                    default:
                        reader.skipValue();
                        // Log.d(TAG, "ignoring " + name);
                        break;
                }
            }
            return wind;
        } finally {
            reader.endObject();
        }
    }

    /**
     * Parse a Json stream and return a Sys Object.
     */
    public Sys parseSys(JsonReader reader)
        throws IOException {
        // TODO -- you fill in here.
        Sys sys = new Sys();
        reader.beginObject();
        // No check here for JsonToken.END_OBJECT - it will just go on and return a default object.
        try {
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case Sys.sunrise_JSON:
                        sys.setSunrise(reader.nextLong());
                        break;
                    case Sys.sunset_JSON:
                        sys.setSunset(reader.nextLong());
                        break;
                    case Sys.country_JSON:
                        sys.setCountry(reader.nextString());
                        break;
                    case Sys.message_JSON:
                        sys.setMessage(reader.nextDouble());
                        break;
                    // unexpected object - ignore.
                    default:
                        reader.skipValue();
                        // Log.d(TAG, "ignoring " + name);
                        break;
                }
            }
            return sys;
        } finally {
            reader.endObject();
        }
    }
}
