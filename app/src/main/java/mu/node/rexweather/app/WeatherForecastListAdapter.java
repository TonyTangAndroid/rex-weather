package mu.node.rexweather.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import mu.node.rexweather.app.Helpers.DayFormatter;
import mu.node.rexweather.app.Helpers.TemperatureFormatter;
import mu.node.rexweather.app.Models.WeatherForecast;

/**
 * Provides items for our list view.
 */
public class WeatherForecastListAdapter extends BaseAdapter {

    private List<WeatherForecast> weatherForecasts;
    private Context context;

    public WeatherForecastListAdapter(final List<WeatherForecast> weatherForecasts,
                                      final Context context) {
        super();
        this.weatherForecasts = weatherForecasts;
        this.context = context;
    }

    @Override
    public boolean isEnabled(final int position) {
        return false;
    }

    @Override
    public int getCount() {
        return weatherForecasts != null ? weatherForecasts.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return weatherForecasts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            final LayoutInflater layoutInflater = LayoutInflater.from(context);
            convertView = layoutInflater.inflate(R.layout.weather_forecast_list_item, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.dayTextView = (TextView) convertView.findViewById(R.id.day);
            viewHolder.descriptionTextView = (TextView) convertView
                    .findViewById(R.id.description);
            viewHolder.maximumTemperatureTextView = (TextView) convertView
                    .findViewById(R.id.maximum_temperature);
            viewHolder.minimumTemperatureTextView = (TextView) convertView
                    .findViewById(R.id.minimum_temperature);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final WeatherForecast weatherForecast = (WeatherForecast) getItem(position);

        final DayFormatter dayFormatter = new DayFormatter(context);
        final String day = dayFormatter.format(weatherForecast.getTimestamp());
        viewHolder.dayTextView.setText(day);
        viewHolder.descriptionTextView.setText(weatherForecast.getDescription());
        viewHolder.maximumTemperatureTextView.setText(
                TemperatureFormatter.format(weatherForecast.getMaximumTemperature()));
        viewHolder.minimumTemperatureTextView.setText(
                TemperatureFormatter.format(weatherForecast.getMinimumTemperature()));

        return convertView;
    }

    public void clear() {
        if (weatherForecasts != null) {
            weatherForecasts = null;
        }
    }

    public void addAll(List<WeatherForecast> forecasts) {
        if (weatherForecasts == null) {
            weatherForecasts = new ArrayList<>();
            weatherForecasts.addAll(forecasts);
        } else {
            weatherForecasts.clear();
            weatherForecasts.addAll(forecasts);
        }
    }


    /**
     * Cache to avoid doing expensive findViewById() calls for each getView().
     */
    private class ViewHolder {
        private TextView dayTextView;
        private TextView descriptionTextView;
        private TextView maximumTemperatureTextView;
        private TextView minimumTemperatureTextView;
    }
}