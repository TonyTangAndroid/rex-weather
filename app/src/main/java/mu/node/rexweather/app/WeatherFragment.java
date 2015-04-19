package mu.node.rexweather.app;

import android.app.Fragment;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.http.HttpException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import hugo.weaving.DebugLog;
import mu.node.rexweather.app.Helpers.TemperatureFormatter;
import mu.node.rexweather.app.Models.CurrentWeather;
import mu.node.rexweather.app.Models.WeatherForecast;
import mu.node.rexweather.app.Services.LocationService;
import mu.node.rexweather.app.Services.WeatherService;
import retrofit.RetrofitError;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Weather Fragment.
 * <p>
 * Displays the current weather as well as a 7 day forecast for our location. Data is loaded
 * from a web service.
 */
public class WeatherFragment extends Fragment {

    private static final String KEY_CURRENT_WEATHER = "key_current_weather";
    private static final String KEY_WEATHER_FORECASTS = "key_weather_forecasts";
    private static final long LOCATION_TIMEOUT_SECONDS = 20;
    private static final String TAG = WeatherFragment.class.getCanonicalName();

    private final CompositeSubscription mCompositeSubscription = new CompositeSubscription();
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private TextView mLocationNameTextView;
    private TextView mCurrentTemperatureTextView;
    private ListView mForecastListView;
    private TextView mAttributionTextView;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_weather, container, false);
        mLocationNameTextView = (TextView) rootView.findViewById(R.id.location_name);
        mCurrentTemperatureTextView = (TextView) rootView
                .findViewById(R.id.current_temperature);

        // Set up list view for weather forecasts.
        mForecastListView = (ListView) rootView.findViewById(R.id.weather_forecast_list);
        final WeatherForecastListAdapter adapter = new WeatherForecastListAdapter(
                new ArrayList<WeatherForecast>(), getActivity());
        mForecastListView.setAdapter(adapter);

        mAttributionTextView = (TextView) rootView.findViewById(R.id.attribution);
//        mAttributionTextView.setVisibility(View.INVISIBLE);

        // Set up swipe refresh layout.
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView
                .findViewById(R.id.swipe_refresh_container);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.brand_main,
                android.R.color.black,
                R.color.brand_main,
                android.R.color.black);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateWeather();
            }
        });

        updateWeather();

        return rootView;

    }


    @Override
    public void onDestroy() {
        mCompositeSubscription.unsubscribe();
        super.onDestroy();
    }


    private void updateWeather() {
        mSwipeRefreshLayout.setRefreshing(true);
        final LocationManager locationManager = (LocationManager) getActivity()
                .getSystemService(Context.LOCATION_SERVICE);
        final LocationService locationService = new LocationService(locationManager);
        final Observable<HashMap<String, List<WeatherForecast>>> fetchDataObservable = locationService.getLocation()
                .timeout(LOCATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .flatMap(this::sendRequest);
        mCompositeSubscription.add(fetchDataObservable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateUI, this::onError, this::onComplete));
    }

    private Observable<? extends HashMap<String, List<WeatherForecast>>> sendRequest(Location location) {
        final WeatherService weatherService = new WeatherService();
        final double longitude = location.getLongitude();
        final double latitude = location.getLatitude();
        return Observable.zip(weatherService.fetchCurrentWeather(longitude, latitude),
                weatherService.fetchWeatherForecasts(longitude, latitude),
                WeatherFragment.this::getWeatherData
        );
    }


    private HashMap<String, List<WeatherForecast>> getWeatherData(CurrentWeather currentWeather, List<WeatherForecast> weatherForecasts) {
        HashMap<String, List<WeatherForecast>> weatherData = new HashMap<>();
        List<WeatherForecast> currentWeatherList = new ArrayList<>();
        currentWeatherList.add(currentWeather);
        weatherData.put(KEY_CURRENT_WEATHER, currentWeatherList);
        weatherData.put(KEY_WEATHER_FORECASTS, weatherForecasts);
        return weatherData;
    }


    @DebugLog
    private void onComplete() {
        mSwipeRefreshLayout.setRefreshing(false);
        mAttributionTextView.setVisibility(View.VISIBLE);
    }

    private void onError(Throwable error) {
        mSwipeRefreshLayout.setRefreshing(false);
        if (error instanceof TimeoutException) {
            Crouton.makeText(getActivity(),
                    R.string.error_location_unavailable, Style.ALERT).show();
        } else if (error instanceof RetrofitError
                || error instanceof HttpException) {
            Crouton.makeText(getActivity(),
                    R.string.error_fetch_weather, Style.ALERT).show();
        } else {
            Log.e(TAG, error.getMessage());
            error.printStackTrace();
            throw new RuntimeException("See inner exception");
        }
    }

    private void updateUI(HashMap<String, List<WeatherForecast>> stringListHashMap) {
        final CurrentWeather currentWeather = (CurrentWeather) stringListHashMap
                .get(KEY_CURRENT_WEATHER).get(0);
        mLocationNameTextView.setText(currentWeather.getLocationName());
        mCurrentTemperatureTextView.setText(
                TemperatureFormatter.format(currentWeather.getTemperature()));
        final List<WeatherForecast> weatherForecasts = stringListHashMap.get(KEY_WEATHER_FORECASTS);
        final WeatherForecastListAdapter adapter = (WeatherForecastListAdapter)
                mForecastListView.getAdapter();
        adapter.clear();
        adapter.addAll(weatherForecasts);

    }
}