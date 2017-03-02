package com.myapps.and.movielib;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;

public class SearchWeb extends AppCompatActivity {

    ArrayList<MyMovie> allWebMovies;
    ArrayAdapter<MyMovie> moviesAdapter;
    ListView      moviesLV;
    int selectedMovie = -1;
    int nextPage=1;    // next page number to fetch. Incremented/Decremented each time getPrev/NexttBatch called
    int movieCount=0;  // Maximum number of movies for query

    static boolean keepRunning;  // TODO abort async-task ?

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_web);

        final ImageButton nextIB = (ImageButton) findViewById(R.id.sNextIB);
        nextIB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getNextBatch();
            }
        });

        final ImageButton prevIB = (ImageButton) findViewById(R.id.sPrevIB);
        prevIB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPrevBatch();
            }
        });

        // Search for string key in OMDB
        ((ImageButton) findViewById(R.id.sGoBtn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextPage=1;
                getNextBatch(); // Get first page
                prevIB.setVisibility(View.INVISIBLE);
            }
        });


        allWebMovies = new ArrayList<MyMovie>();
        moviesLV = (ListView) findViewById(R.id.sMoviesLV);
        moviesAdapter = new ArrayAdapter<MyMovie>(this,R.layout.single_web_movie,R.id.wMovieTV,allWebMovies);
        moviesLV.setAdapter(moviesAdapter);


        // Enable adding selected movie to db
        moviesLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent addFromWeb = new Intent(SearchWeb.this,AddEditMovie.class);
                MyMovie webMovie=allWebMovies.get(position);
                addFromWeb.putExtra(AppConstants.KEY_ID,AppConstants.WEB_ID);
                addFromWeb.putExtra(AppConstants.KEY_SUBJECT,webMovie.getSubject());
                addFromWeb.putExtra(AppConstants.KEY_MOVIE_IMDB,webMovie.getImdb());
                addFromWeb.putExtra(AppConstants.KEY_IMAGE_URL,webMovie.getImageUrl());
                Toast.makeText(SearchWeb.this,getString(R.string.msg_editing_movie)+" "+webMovie.getSubject(),Toast.LENGTH_SHORT).show();
                selectedMovie=position;
                startActivityForResult(addFromWeb,AppConstants.ACTION_NEW_MOVIE_WEB);


            }
        });

        // TODO Enable AutoComplete Search - need to deal with site response time
        // should only be used if searchAutocomplete is true
        TextWatcher titleWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (abs(start-before) > AppConstants.MIN_CHAR_CHANGED)
                {
                    getList(1);
                    nextPage++;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        EditText titleET= (EditText) findViewById(R.id.sSubjectET);
        if ( AppConstants.searchSuggestions) {
            titleET.addTextChangedListener(titleWatcher);
        }

        // Enable cancel button
        ((ImageButton) findViewById(R.id.sCancelIB)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ( resultCode == RESULT_OK )
        {
            // Movie added. remove it from list
            // allWebMovies.remove(selectedMovie);
            // moviesAdapter.notifyDataSetChanged();
        }
    }

    // Retrieve next set of movies for search
    private void getNextBatch(){
        getList(nextPage);
        nextPage++;

    }

    // Retrieve previous set of movies for search
    private void getPrevBatch() {
        if ( nextPage > 1 ) {
            nextPage--;
            getList(nextPage);
        }
    }

    // Get movie list
    private void getList (int pageNum)
    {

        if ( isNetworkAvailable() ) {
            String key = ((EditText) findViewById(R.id.sSubjectET)).getText().toString().trim();
            if (!key.equals("")) {
                // process url to avoid problematic characters
                key = key.replace(" ", "+");
                key = key.replace("\\", "\\\\");
                key = key.replace("\"", "");
                String urlStr = "http://www.omdbapi.com/?s=\"" + key + "\"&plot=full&type=movie&r=json&page="+pageNum;
                // Look for string in background
                Log.d("webSearch", urlStr);
                downLoadMovieList myList = new downLoadMovieList();
                myList.execute(urlStr);

            } else {

                Animation myAnim = AnimationUtils.loadAnimation(SearchWeb.this, R.anim.shake);
                ImageButton goBtn = (ImageButton) findViewById(R.id.sGoBtn);
                goBtn.startAnimation(myAnim);
                Toast.makeText(SearchWeb.this, getString(R.string.msg_enter_string_for_search), Toast.LENGTH_SHORT).show();
            }
        } else
        {
            Toast.makeText(SearchWeb.this,R.string.msg_no_internet_access,Toast.LENGTH_SHORT).show();
        }
    }
    // Get list of movies from the internet matching requested title
    private class downLoadMovieList extends AsyncTask<String,Integer,String> {

        ProgressDialog downloading = new ProgressDialog(SearchWeb.this);
        HttpURLConnection connection = null;

        RotateAnimation myAnim = (RotateAnimation) AnimationUtils.loadAnimation(SearchWeb.this,R.anim.rotate);
        int currentPage=nextPage;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //downloading.setTitle(getString(R.string.msg_downloading));
            //downloading.setMessage(getString(R.string.msg_wait));
            //downloading.setCancelable(true);
            //downloading.show();
            movieCount=0;
            ImageButton searchBtn = (ImageButton) findViewById(R.id.sGoBtn);
            searchBtn.startAnimation(myAnim);

        }

        @Override
        protected String doInBackground(String... params) {
            StringBuilder response=new StringBuilder();

            try {
                URL website=new URL(params[0]);
                connection = (HttpURLConnection) website.openConnection();
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(
                                connection.getInputStream()));
                 String inputLine;
                while ((inputLine = in.readLine()) != null )
                    response.append(inputLine);
                in.close();

            } catch (MalformedURLException e) {
                e.printStackTrace();
                response.append("");
            } catch (IOException e) {
                response.append("");
                e.printStackTrace();
            }

            finally {
                if ( connection != null )
                {
                    connection.disconnect();
                }
            }


            return response.toString();
        }

        @Override
        protected void onPostExecute(String movies) {
            // Intialize listview
            allWebMovies.clear();

            if ( ! movies.equals("") ) {
                try {
                    JSONObject mainObject = new JSONObject(movies);
                    if (mainObject.getBoolean("Response")) {
                        movieCount=mainObject.getInt("totalResults");
                        JSONArray resultMoviesArray = new JSONArray();
                        resultMoviesArray = mainObject.getJSONArray("Search");
                        JSONObject aMovie=new JSONObject();
                        for (int i = 0; i < resultMoviesArray.length(); i++) {

                            aMovie = resultMoviesArray.getJSONObject(i);
                            MyMovie webMovie = new MyMovie(AppConstants.WEB_ID,
                                    aMovie.getString("Title"),
                                    "",
                                    aMovie.getString("Poster"));
                            webMovie.setImdb(aMovie.getString("imdbID"));
                            allWebMovies.add(webMovie);

                        }
                        //Toast.makeText(SearchWeb.this,getString(R.string.msg_click_to_save),Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SearchWeb.this, getString(R.string.msg_movie_not_found), Toast.LENGTH_SHORT).show();
                        Log.d("searchWeb", mainObject.getString("Error"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else
            {
                Toast.makeText(SearchWeb.this,getString(R.string.msg_error_loading_list),Toast.LENGTH_SHORT).show();
            }



            moviesAdapter.notifyDataSetChanged();

            ImageButton nextIB=(ImageButton) findViewById(R.id.sNextIB);
            if ( (movieCount - (currentPage*AppConstants.OMDB_PAGE_BATCH_COUNT)) > 0 ) {
                 nextIB.setVisibility(View.VISIBLE);
            } else {
                 nextIB.setVisibility(View.INVISIBLE);
            }
            ImageButton prevIB=(ImageButton) findViewById(R.id.sPrevIB);
            if ( (currentPage) > 1 ) {
                prevIB.setVisibility(View.VISIBLE);
            } else {
                prevIB.setVisibility(View.INVISIBLE);
            }

            //downloading.dismiss();
            myAnim.cancel();
            myAnim.reset();

        }
    }

    // Verify ability to connect to the internet
    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }

        return false;
    }
}
