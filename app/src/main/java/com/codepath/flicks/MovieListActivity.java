package com.codepath.flicks;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import com.codepath.flicks.models.Config;
import com.codepath.flicks.models.Movie;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

import static com.loopj.android.http.AsyncHttpClient.log;

public class MovieListActivity extends AppCompatActivity {

    //constants
    //base URL for the API
    public final static String API_BASE_URL = "https://api.themoviedb.org/3";
    //param name for the API key
    public final static String API_KEY_PARAM = "api_key";
    /*
    //the API key -- TODO move to a secure location
    public final static String API_KEY = "a7b3baebc805db68306433dc6b0dc7e9";
    */
    //tag for logging from this activity
    public final static String TAG = "MovieListActivity";

    AsyncHttpClient client;
    //list of currently playing movies
    ArrayList<Movie> movies;
    //the recycler view
    RecyclerView rvMovies;
    //the adapter wired to the recycler view
    MovieAdapter adapter;
    //image config
    Config config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_list);
        //initialize the client
        client = new AsyncHttpClient();
        movies = new ArrayList<>();
        //intialize the adapter - movies array cannot be reinitialized after now
        adapter = new MovieAdapter(movies);

        //resolve the recycler view and connect a layout manager and the adapter
        rvMovies = (RecyclerView) findViewById(R.id.rvMovies);
        rvMovies.setLayoutManager(new LinearLayoutManager(this));
        rvMovies.setAdapter(adapter);

        //get the configuration on app creation
        getConfiguration();
    }

    //get the list of currently playing movies from the API
    private void getNowPlaying(){
        //create the url
        String url = API_BASE_URL + "/movie/now_playing";
        //set request parameters
        RequestParams params = new RequestParams();
        params.put(API_KEY_PARAM, getString(R.string.api_key)); //API key, always required
        //execute a GET request expecting a JSON object response
        client.get(url, params, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    //load the results into movie lists
                    JSONArray results = response.getJSONArray("results");

                    for(int i = 0; i<results.length();i++){
                        Movie movie = new Movie(results.getJSONObject(i));
                        movies.add(movie);
                        //notify adapter that a row was added
                        adapter.notifyItemInserted(movies.size()-1);
                    }
                    Log.i(TAG, String.format("Loaded %s movies", results.length()));

                }catch(JSONException e){
                    logError("Failed to parse now playing movies", e, true);
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                logError("Failed to get data from now_playing endpoint", throwable, true);
            }
        });
    }


    private void getConfiguration(){
        //create the url
        String url = API_BASE_URL + "/configuration";
        //set request parameters
        RequestParams params = new RequestParams();
        params.put(API_KEY_PARAM, getString(R.string.api_key)); //API key, always required
        //execute a GET request expecting a JSON object response
        client.get(url, params, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {

                try {
                    config = new Config(response);
                    Log.i(TAG,
                            String.format("Loaded configuration with imageBaseUrl %s and PosterSize %s",
                                    config.getImageBaseUrl(),
                                    config.getPosterSize()));

                    //pass config to adapter
                    adapter.setConfig(config);
                    //get the now playing movie list
                    getNowPlaying();
                }catch(JSONException e){
                    logError("Failed parsing configuration", e, true);
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                logError("Failed getting configuration", throwable, true);
            }

        });
    }

    //handle errors, log and alert the user
    private void logError(String message, Throwable error, boolean alertUser){
        //always logs the error
        log.e(TAG, message, error);
        //avoids silent errors by alerting user
        if(alertUser){
            //show a long toast with the error message
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }
    }
}
