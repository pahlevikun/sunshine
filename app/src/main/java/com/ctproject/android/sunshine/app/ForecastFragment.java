package com.ctproject.android.sunshine.app;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> Adapter;
    private ProgressDialog dialog;

    private final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";

    private final String QUERY_PARAM = "q";
    private final String FORMAT_PARAM = "mode";
    private final String UNITS_PARAM = "units";
    private final String DAYS_PARAM = "cnt";
    private final String APPID_PARAM = "APPID";

    private final String DATA_LIST = "list";
    private final String DATA_WEATHER = "weather";
    private final String DATA_TEMPERATURE = "temp";
    private final String DATA_MAX = "max";
    private final String DATA_MIN = "min";
    private final String DATA_DESCRIPTION = "main";

    private final String MYAPPID = "7e772cfe74762c4add9e4ac5afdf882d";

    private String[] values = {
            "Jakarta, Mon Apr 04 - Rain - 32/27 °C",
            "Jakarta, Tue Apr 05 - Rain - 30/26 °C",
            "Jakarta, Wed Apr 06 - Rain - 30/26 °C",
            "Jakarta, Thu Apr 07 - Rain - 29/26 °C",
            "Jakarta, Fri Apr 08 - Rain - 30/27 °C",
            "Jakarta, Sat Apr 09 - Rain - 31/26 °C",
            "Jakarta, Sun Apr 10 - Rain - 29/26 °C"
    };

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecast_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            WheaterAsync weatherAsync = new WheaterAsync();
            weatherAsync.execute("Jakarta");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        List<String> dataForecast = new ArrayList<String>(Arrays.asList(values));

        Adapter = new ArrayAdapter<String>(getActivity(),
                R.layout.list_item_main_forecast, R.id.list_item_main_forecast_textview, dataForecast);

        View rootView = inflater.inflate(R.layout.fragment_main_forecast, container, false);

        dialog = new ProgressDialog(getActivity());
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(Adapter);

        return rootView;
    }

    public class WheaterAsync extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = WheaterAsync.class.getSimpleName();

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Sedang Mengambil Data.\nTunggu Sebentar...");
            dialog.show();
        }

        private String getReadableDateString(long time){
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        private String formatHighLows(double high, double low) {
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);
            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        private String[] ambilDataJSON(String jsonForecastData, int numDays)
                throws JSONException {

            JSONObject forecastJson = new JSONObject(jsonForecastData);
            JSONArray weatherArray = forecastJson.getJSONArray(DATA_LIST);

            Time dayTime = new Time();
            dayTime.setToNow();
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);
            dayTime = new Time();

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
                String day;
                String description;
                String highAndLow;

                JSONObject forecastdata = weatherArray.getJSONObject(i);
                long dateTime;
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);
                JSONObject weatherObject = forecastdata.getJSONArray(DATA_WEATHER).getJSONObject(0);
                description = weatherObject.getString(DATA_DESCRIPTION);
                JSONObject temperatureObject = forecastdata.getJSONObject(DATA_TEMPERATURE);
                double high = temperatureObject.getDouble(DATA_MAX);
                double low = temperatureObject.getDouble(DATA_MIN);
                highAndLow = formatHighLows(high, low);

                resultStrs[i] ="Jakarta, " + day + " - " + description + " - " + highAndLow + "° C";
            }

            for (String s : resultStrs) {
                Log.v(LOG_TAG, "Data forecast: " + s);
            }
            return resultStrs;

        }
        @Override
        protected String[] doInBackground(String... params) {

            if (params.length == 0) {
                return null;
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String jsonForecastData = null;

            String format = "json";
            String units = "metric";
            int numDays = 7;

            try {

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .appendQueryParameter(APPID_PARAM, MYAPPID)
                        .build();

                URL url = new URL(builtUri.toString());
                Log.v(LOG_TAG, "URL JSON : " + builtUri.toString());
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }
                jsonForecastData = buffer.toString();

                Log.v(LOG_TAG, "Data forecast : " + jsonForecastData);

                Thread.sleep(5000);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Data forecast gagal di load ", e);
                return null;
            } catch (InterruptedException e) {
                e.printStackTrace();
                Toast.makeText(getActivity().getApplicationContext(),"Gagal Load Data",Toast.LENGTH_SHORT).show();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Gagal", e);
                    }
                }
            }

            try {
                return ambilDataJSON(jsonForecastData, numDays);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                Adapter.clear();
                for(String dayForecastStr : result) {
                    Adapter.add(dayForecastStr);
                }
                dialog.dismiss();
                Toast.makeText(getActivity().getApplicationContext()," Berhasil mengambil Data ",Toast.LENGTH_SHORT).show();
            }
        }
    }
}
