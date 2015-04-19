package mu.node.rexweather.app.Services;

import com.google.gson.annotations.SerializedName;

import org.apache.http.HttpException;

import java.util.ArrayList;
import java.util.List;

import mu.node.rexweather.app.Models.CurrentWeather;
import mu.node.rexweather.app.Models.WeatherForecast;
import retrofit.RestAdapter;
import retrofit.http.GET;
import retrofit.http.Query;
import rx.Observable;

public class WeatherService {
    // We are implementing against version 2.5 of the Open Weather Map web service.
    private static final String WEB_SERVICE_BASE_URL = "http://api.openweathermap.org/data/2.5";
    private final OpenWeatherMapWebService mWebService;

    public WeatherService() {

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(WEB_SERVICE_BASE_URL)
                .setRequestInterceptor(request -> request.addHeader("Accept", "application/json"))
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        mWebService = restAdapter.create(OpenWeatherMapWebService.class);
    }

    private interface OpenWeatherMapWebService {
        @GET("/weather?units=metric")
        Observable<CurrentWeatherDataEnvelope> fetchCurrentWeather(@Query("lon") double longitude,
                                                                   @Query("lat") double latitude);

        @GET("/forecast/daily?units=metric&cnt=7")
        Observable<WeatherForecastListDataEnvelope> fetchWeatherForecasts(
                @Query("lon") double longitude, @Query("lat") double latitude);
    }

    public Observable<CurrentWeather> fetchCurrentWeather(final double longitude,
                                                          final double latitude) {
        return mWebService.fetchCurrentWeather(longitude, latitude)
                .flatMap(this::filterWebServiceError).map(this::getCurrentWeather);
    }

    private Observable<CurrentWeatherDataEnvelope> filterWebServiceError(CurrentWeatherDataEnvelope currentWeatherDataEnvelope) {
        return currentWeatherDataEnvelope.filterWebServiceErrors();
    }

    private CurrentWeather getCurrentWeather(CurrentWeatherDataEnvelope data) {
        return new CurrentWeather(data.locationName, data.timestamp,
                data.weather.get(0).description, data.main.temp,
                data.main.temp_min, data.main.temp_max);
    }

    public Observable<List<WeatherForecast>> fetchWeatherForecasts(final double longitude,
                                                                   final double latitude) {
        return mWebService.fetchWeatherForecasts(longitude, latitude)
                .flatMap(this::filterErrors)
                .map(this::getWeatherForecast);

    }

    private ArrayList<WeatherForecast> getWeatherForecast(WeatherForecastListDataEnvelope listData) {
        final ArrayList<WeatherForecast> weatherForecasts =
                new ArrayList<>();

        for (WeatherForecastListDataEnvelope.ForecastDataEnvelope data : listData.list) {
            final WeatherForecast weatherForecast = new WeatherForecast(
                    listData.city.name, data.timestamp, data.weather.get(0).description,
                    data.temp.min, data.temp.max);
            weatherForecasts.add(weatherForecast);
        }

        return weatherForecasts;
    }

    private Observable<? extends WeatherForecastListDataEnvelope> filterErrors(WeatherForecastListDataEnvelope weatherForecastListDataEnvelope) {
        return weatherForecastListDataEnvelope.filterWebServiceErrors();
    }

    /**
     * Base class for results returned by the weather web service.
     */
    private class WeatherDataEnvelope {
        @SerializedName("cod")
        private int httpCode;

        class Weather {
            public String description;
        }

        /**
         * The web service always returns a HTTP header code of 200 and communicates errors
         * through a 'cod' field in the JSON payload of the response body.
         */
        public Observable filterWebServiceErrors() {
            if (httpCode == 200) {
                return Observable.just(this);
            } else {
                return Observable.error(
                        new HttpException("There was a problem fetching the weather data."));
            }
        }
    }

    /**
     * Data structure for current weather results returned by the web service.
     */
    private class CurrentWeatherDataEnvelope extends WeatherDataEnvelope {
        @SerializedName("name")
        public String locationName;
        @SerializedName("dt")
        public long timestamp;
        public ArrayList<Weather> weather;
        public Main main;

        class Main {
            public float temp;
            public float temp_min;
            public float temp_max;
        }
    }

    /**
     * Data structure for weather forecast results returned by the web service.
     */
    private class WeatherForecastListDataEnvelope extends WeatherDataEnvelope {
        public Location city;
        public ArrayList<ForecastDataEnvelope> list;

        class Location {
            public String name;
        }

        class ForecastDataEnvelope {
            @SerializedName("dt")
            public long timestamp;
            public Temperature temp;
            public ArrayList<Weather> weather;
        }

        class Temperature {
            public float min;
            public float max;
        }
    }
}
