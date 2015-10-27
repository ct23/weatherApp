package com.colin.simpleweather;

import android.graphics.Typeface;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Fragment containing the main Weather activity.
 */
public class WeatherActivityFragment extends Fragment {

    // Icon typeface to be used
    Typeface weatherFont;

    // TextViews from XML layout
    TextView cityField;
    TextView updatedField;
    TextView detailsField;
    TextView currentTemperatureField;
    TextView weatherIcon;

    // Use a Handler object to update user inferface
    // (cannot update UI from background thread)
    Handler handler;

    // Initialize Handler object in constructor
    public WeatherActivityFragment() {
        handler = new Handler();
    }

    // Create and return the View
    // Initialize the TextViews from XML layout
    // Set icon to use weatherFont typeface
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_weather, container, false);
        cityField = (TextView) rootView.findViewById(R.id.city_field);
        updatedField = (TextView) rootView.findViewById(R.id.updated_field);
        detailsField = (TextView) rootView.findViewById(R.id.details_field);
        currentTemperatureField = (TextView) rootView.findViewById(R.id.current_temperature_field);
        weatherIcon = (TextView) rootView.findViewById(R.id.weather_icon);

        weatherIcon.setTypeface(weatherFont);
        return rootView;
    }

    // Initialize weatherFont object to use Typeface
    // Invoke updateWeatherData method
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        weatherFont = Typeface.createFromAsset(getActivity().getAssets(), "fonts/weather.ttf");
        updateWeatherData(new CityPreference(getActivity()).getCity());
    }

    /**
     * Since only the main Thread is allowed to update the UI
     * of an Android app, calling Toast or renderWeather directly
     * from the background thread would lead to a runtime error.
     * That is why we call these methods using the Handler's
     * post method.
     */
    // Updates weather data for given city
    // Displays a Toast error if city not found
    private void updateWeatherData(final String city) {
        new Thread() {
            public void run() {
                final JSONObject json = RemoteFetch.getJSON(getActivity(), city);
                if (json == null) {
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(getActivity(), getActivity().getString(R.string.place_not_found),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    handler.post(new Runnable() {
                        public void run() {
                            renderWeather(json);
                        }
                    });
                }
            }
        }.start();
    }

    // Updates the TextView objects
    private void renderWeather(JSONObject json) {
        try {
            cityField.setText(json.getString("name").toUpperCase(Locale.US)
                    + ", " +
                    json.getJSONObject("sys").getString("country"));

            JSONObject details = json.getJSONArray("weather").getJSONObject(0);
            JSONObject main = json.getJSONObject("main");
            detailsField.setText(
                    details.getString("description").toUpperCase(Locale.US) +
                    "\n" + "Humidity: " + main.getString("humidity") + "%"
                    + "\n" + "Pressure: " + main.getString("pressure") + " hPa");

            currentTemperatureField.setText(
                    String.format("%.2f", main.getDouble("temp")) + " ℉");

            DateFormat df = DateFormat.getDateTimeInstance();
            String updatedOn = df.format(new Date(json.getLong("dt")*1000));
            updatedField.setText("Last update: " + updatedOn);

            setWeatherIcon(details.getInt("id"),
                    json.getJSONObject("sys").getLong("sunrise") * 1000,
                    json.getJSONObject("sys").getLong("sunset") * 1000);
        } catch (Exception e) {
            Log.e("SimpleWeather", "One or more fields not found in the JSON data");
        }
    }

    // Method to map a weather id to an icon
    // Weather codes in 200 range related to T-storms
    // Weather codes in 300 range related to drizzles
    // Weather codes in 500 range signify rain
    // etc...
    private void setWeatherIcon(int actualId, long sunrise, long sunset) {
        int id = actualId/100;
        String icon = "";
        if (actualId == 800) {
            long currentTime = new Date().getTime();
            if (currentTime >= sunrise && currentTime < sunset) {
                // is daytime
                icon = getActivity().getString(R.string.weather_sunny);
            } else {
                // is nighttime
                icon = getActivity().getString(R.string.weather_clear_night);
            }
        } else {
            switch(id) {
                case 2 : icon = getActivity().getString(R.string.weather_thunder);
                case 3 : icon = getActivity().getString(R.string.weather_drizzle);
                case 7 : icon = getActivity().getString(R.string.weather_foggy);
                case 8 : icon = getActivity().getString(R.string.weather_cloudy);
                case 6 : icon = getActivity().getString(R.string.weather_snowy);
                case 5: icon = getActivity().getString(R.string.weather_rainy);
                    break;

            }
        }
        weatherIcon.setText(icon);
    }

    // Method to let user update their current city
    public void changeCity(String city) {
        updateWeatherData(city);
    }
}
