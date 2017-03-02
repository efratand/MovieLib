package com.myapps.and.movielib;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import static android.R.attr.onClick;

public class AddEditMovie extends AppCompatActivity {

    int movieId;          // Id passed by intent to identify data
    EditText subjectET;   // Movie title
    EditText bodyET;      // Plot
    EditText urlET;       // Image URL
    String originURL = "";
    boolean urlChanged;   // Indication for urlchange

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_movie);

        subjectET=(EditText) findViewById(R.id.eSubjectET);
        bodyET=(EditText) findViewById(R.id.eBodyET);
        urlET=(EditText) findViewById(R.id.eUrlET);
        // Clear Image view from default image
        ImageView imageIV = (ImageView) findViewById(R.id.movieIV);
        imageIV.setImageBitmap(null);


        Intent addEditMovie=getIntent();

        // Get movieId from intent
        movieId = addEditMovie.getIntExtra(AppConstants.KEY_ID,AppConstants.EMPTY_ID);

        switch ( movieId ) {
            case AppConstants.EMPTY_ID:
                // This is a new movie. No previous data
                break;

            case AppConstants.WEB_ID:
                // This is a new movie from web. Copy content for editing.
                subjectET.setText(addEditMovie.getStringExtra(AppConstants.KEY_SUBJECT));
                // Save initial url for comparing on update
                originURL=addEditMovie.getStringExtra(AppConstants.KEY_IMAGE_URL);
                urlET.setText(originURL);
                getPlot plot = new getPlot(bodyET);
                plot.execute(addEditMovie.getStringExtra(AppConstants.KEY_MOVIE_IMDB));
                if ( AppConstants.autoDownloadImage & ! urlET.getText().equals(""))
                {
                    downLoadImage();
                }
                break;

            default:
                // This is an existing movie. Copy content for editing.
                MyMovie movie = MovieDBHandler.fetchMovieFromDB(AddEditMovie.this, movieId);
                subjectET.setText(movie.getSubject());
                bodyET.setText(movie.getBody());
                // Save initial url for comparing on update
                originURL=movie.getImageUrl();
                urlET.setText(originURL);
                // If url exists for movie try and display image
                if ( ! movie.getImageUrl().equals("") ) {
                    // Check if local image exists
                    if (! movie.getImageString64().equals("")) {
                        // Encode image string to bitmap
                        Bitmap image = encodeBitmapToStringBase64(movie.getImageString64());
                        ImageView imageView = (ImageView) findViewById(R.id.movieIV);
                        imageView.setImageBitmap(image);
                        imageView.setVisibility(View.VISIBLE);
                        // Set local movie indication
                        ImageView movieAttachedIV = (ImageView) findViewById(R.id.eAttachedIV);
                        movieAttachedIV.setVisibility(View.VISIBLE);
                    } else {
                        // Check if auto download enabled
                        if (AppConstants.autoDownloadImage) {
                            downLoadImage();
                        }
                    }
                }
        }

        subjectET.requestFocus();
        subjectET.setSelection(0);

        // Watch URL change to match image
        // Search fileter is disabled due to performance issues of OMDB site
        urlET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                urlChanged = true;
            }

        });

        // Enable share movie info
        final ImageButton shareIB = (ImageButton) findViewById(R.id.eShareIB);
        shareIB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Animation myAnim = AnimationUtils.loadAnimation(AddEditMovie.this, R.anim.bounce);
                shareIB.startAnimation(myAnim);
            shareMovieInfo();
            }
        });

        // Disable notification for saved image
        final ImageView pinIv = (ImageView) findViewById(R.id.eAttachedIV);
        pinIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ( pinIv.getVisibility() == View.VISIBLE )
                {
                    Toast.makeText(AddEditMovie.this,getString(R.string.msg_n_db_img),Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Enable download image button according to image url
        final ImageButton showBtn = (ImageButton) findViewById(R.id.eShowImageIB);
        showBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                downLoadImage();
            }
        });

        // Enable cancel button
        final ImageButton cancelIB = (ImageButton) findViewById(R.id.eCancelIB);
        cancelIB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Animation myAnim = AnimationUtils.loadAnimation(AddEditMovie.this, R.anim.bounce);
                cancelIB.startAnimation(myAnim);
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        // Enable save button
        final ImageButton saveIB = (ImageButton) findViewById(R.id.eSaveIB);
        saveIB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Animation myAnim = AnimationUtils.loadAnimation(AddEditMovie.this, R.anim.bounce);
                saveIB.startAnimation(myAnim);
            saveMovieInfo();
            }
        });


    }

    // -----------------
    // Share movie info
    //------------------
    private void shareMovieInfo() {
        if ( ! subjectET.getText().toString().equals("") ) {
            Intent shareInfo = new Intent(Intent.ACTION_SEND);
            shareInfo.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject)+" "+subjectET.getText().toString());
            String msgBody=getString(R.string.share_msg)+subjectET.getText().toString() +" "+ getString(R.string.share_msg1);
            if ( ! bodyET.getText().toString().equals(""))
            {
                msgBody=msgBody+"\n"+getString(R.string.share_msg2)+" "+bodyET.getText().toString();
            }
            shareInfo.putExtra(Intent.EXTRA_TEXT, msgBody);
            shareInfo.setType("text/plain");
            startActivityForResult(shareInfo, AppConstants.ACTION_SHARE);

        }
        else
        {
            Animation myAnim = AnimationUtils.loadAnimation(AddEditMovie.this, R.anim.shake);
            ImageButton shareBtn = (ImageButton) findViewById(R.id.eShareIB);
            shareBtn.startAnimation(myAnim);
            Toast.makeText(AddEditMovie.this,getString(R.string.msg_mandatory_subject),Toast.LENGTH_SHORT).show();
        }
    }
    //---------------------
    // Saving movie info
    //---------------------
    private void saveMovieInfo() {

        ImageView imageView = (ImageView) findViewById(R.id.movieIV);
        // Title is mandatory - verify it has value
        if ( ! subjectET.getText().toString().trim().equals("") ) {

            MyMovie modifiedMovie = new MyMovie(movieId, subjectET.getText().toString(), bodyET.getText().toString(), urlET.getText().toString());

            // Check if image needs to be saved
            if ( AppConstants.saveImageLocaly )
            {
                // If url changed and autodownload enabled - try and download image
                if ( urlChanged & ! originURL.equals(urlET.getText().toString()))
                {
                    if ( AppConstants.autoDownloadImage ) {
                        downLoadImage();
                    }
                    else {

                        imageView.setImageBitmap(null);
                        Toast.makeText(this,R.string.msg_error_url_image_mismatch,Toast.LENGTH_SHORT).show();
                    }
                }
                // Convert image to string64 to save it in database
                Bitmap bimage= ((BitmapDrawable)imageView.getDrawable()).getBitmap();
                String imageString64 = "";
                if ( bimage != null )
                {
                    imageString64 = decodeBitmapToStringBase64(bimage);
                }
                modifiedMovie.setImageString64(imageString64);
            }

            // Alert in case of a new movie - if title exists
            if ( (movieId == AppConstants.WEB_ID || movieId == AppConstants.EMPTY_ID ) & MovieDBHandler.doesTitleExists (this,subjectET.getText().toString())) {
                // Confirm adding duplicate title
                String msgStr=getString(R.string.msg_n_title_exists);
                final MyMovie M = modifiedMovie;
                AlertDialog duplicateAlert = new AlertDialog.Builder(this)
                        .setTitle(msgStr)
                        .setMessage(getString(R.string.msg_continue_alert))
                        .setPositiveButton(getString(R.string.msg_action_yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Add anyway
                                MovieDBHandler.updateMovie(AddEditMovie.this, M);
                                Toast.makeText(AddEditMovie.this, getString(R.string.msg_n_movie_added), Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            }
                        })
                        .setNegativeButton(getString(R.string.msg_action_no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Do nothing

                            }
                        })
                        .create();
                duplicateAlert.show();

            } else {
                MovieDBHandler.updateMovie(AddEditMovie.this, modifiedMovie);
                Toast.makeText(AddEditMovie.this, getString(R.string.msg_n_movie_added), Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        }
        else
        {
            subjectET.requestFocus();
            Animation myAnim = AnimationUtils.loadAnimation(AddEditMovie.this, R.anim.shake);
            ImageButton saveBtn = (ImageButton) findViewById(R.id.eSaveIB);
            saveBtn.startAnimation(myAnim);
            Toast.makeText(AddEditMovie.this,getString(R.string.msg_mandatory_subject),Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------------------------------
    // Decode bitmap image into StringBase64
    // ------------------------------------------
    private String decodeBitmapToStringBase64 ( Bitmap bimage) {
        // Convert image to string64

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bimage.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] byteArrayImage = baos.toByteArray();
        String stringImage = Base64.encodeToString(byteArrayImage, Base64.DEFAULT);
        return stringImage;
    }

    // --------------------------------------
    // Encode StringBase64 into bitmap image
    // --------------------------------------
    private Bitmap encodeBitmapToStringBase64 ( String stringBase64) {
        // Convert image to string64

        byte[] decodedString = Base64.decode(stringBase64, Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        return decodedByte;
    }

    // -------------------------------------------------------
    // Downloads image from internet according to image url
    // -------------------------------------------------------
    // TODO check download with no internet and url changed
    private void downLoadImage() {

        if (checkURL(urlET.getText())) {

            if (isNetworkAvailable()) {
                if (!urlET.getText().toString().trim().equals("")) {
                    new DownloadImageFromInternet((ImageView) findViewById(R.id.movieIV))
                            .execute(urlET.getText().toString());
                } else {
                    // No url to display
                    Toast.makeText(AddEditMovie.this, getString(R.string.msg_no_image_url), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(AddEditMovie.this, R.string.msg_no_internet_access, Toast.LENGTH_SHORT).show();
            }
        } else {
            // Url string not valid
            Toast.makeText(AddEditMovie.this, R.string.msg_invalid_image_url, Toast.LENGTH_SHORT).show();
        }

    }

    // ---------------------------------------------
    // Async task for downloading image from the web
    // ---------------------------------------------
    private class DownloadImageFromInternet extends AsyncTask<String, Void, Bitmap> {

        ImageView imageView;
        // ProgressDialog downloading = new ProgressDialog(AddEditMovie.this);


        RotateAnimation myAnim = (RotateAnimation) AnimationUtils.loadAnimation(AddEditMovie.this,R.anim.rotate);


        public DownloadImageFromInternet(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        protected void onPreExecute() {

            super.onPreExecute();
            //downloading.setTitle(getString(R.string.msg_downloading));
            //downloading.setMessage(getString(R.string.msg_wait));
            //downloading.setCancelable(true);
            //downloading.show();
            // Replaced progress bar with animation
            ImageButton showBtn = (ImageButton) findViewById(R.id.eShowImageIB);
            showBtn.startAnimation(myAnim);

        }

        protected Bitmap doInBackground(String... urls) {
            String imageURL = urls[0];
            // Saving original URL
            originURL = imageURL;
            Bitmap bimage = null;

            try {
                InputStream in = new java.net.URL(imageURL).openStream();
                bimage = BitmapFactory.decodeStream(in);

            } catch (Exception e) {
                Log.e("Error downloading image", e.getMessage());
                e.printStackTrace();
            }
            return bimage;
        }

        protected void onPostExecute(Bitmap result) {

            if (!(result == null)) {
                urlChanged = false; // download successful. Image matches url
                imageView.setImageBitmap(result);
                imageView.setVisibility(View.VISIBLE);
            }
            else
            {
                originURL=""; // No image loaded
                imageView.setImageBitmap(null);
                imageView.setVisibility(View.INVISIBLE);
                Toast.makeText(AddEditMovie.this,getString(R.string.msg_error_loading_image),Toast.LENGTH_SHORT).show();
            }
            //downloading.dismiss();
            // Replaced progress bar with animation
            myAnim.cancel();
            myAnim.reset();

        }
    }

    // --------------------------
    //  Download plot from OMDB
    // --------------------------
    private class getPlot extends AsyncTask<String,Integer,String> {

        ProgressDialog downloading = new ProgressDialog(AddEditMovie.this);
        EditText body;
        boolean networkAvailable=true;

        public getPlot ( EditText body ) {
            this.body=body;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            downloading.setTitle(getString(R.string.msg_downloading_plot));
            downloading.setMessage(getString(R.string.msg_wait));
            downloading.setCancelable(true);
            downloading.show();

        }

        @Override
        protected String doInBackground(String... params) {
            StringBuilder response=new StringBuilder();
            response.append("");
            HttpURLConnection connection = null;

            if ( isNetworkAvailable() ) {
                try {
                    String searchImdbStr = "http://www.omdbapi.com/?i=" + params[0];
                    URL website = new URL(searchImdbStr);
                    connection = (HttpURLConnection) website.openConnection();
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream()));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null)
                        response.append(inputLine);
                    in.close();

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    response.append("");
                } catch (IOException e) {
                    response.append("");
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
            else
            {
                networkAvailable=false;
            }
            Log.d("getPlot", response.toString());
            return response.toString();
        }

        @Override
        protected void onPostExecute(String result) {

            String plot="";

            if ( ! result.equals("") ) {
                try {
                    JSONObject mainObject = new JSONObject(result);
                    if (mainObject.getBoolean("Response")) {
                        plot=mainObject.getString("Plot");
                    } else {
                        // No plot available
                        plot=getString(R.string.msg_no_plot);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(AddEditMovie.this,getString(R.string.msg_err_download_plot),Toast.LENGTH_SHORT).show();
                }
            } else if (! networkAvailable)
            {
                Toast.makeText(AddEditMovie.this,R.string.msg_no_internet_access,Toast.LENGTH_SHORT).show();
            }

            body.setText(plot);
            downloading.dismiss();

        }
    }

    // --------------------
    // Check URL validity
    // --------------------
    private boolean checkURL(CharSequence input) {
        if (TextUtils.isEmpty(input)) {
            return false;
        }
        Pattern URL_PATTERN = Patterns.WEB_URL;
        boolean isURL = URL_PATTERN.matcher(input).matches();
        if (!isURL) {
            String urlString = input + "";
            if (URLUtil.isNetworkUrl(urlString)) {
                try {
                    new URL(urlString);
                    isURL = true;
                } catch (Exception e) {
                    isURL = false;
                }
            }
        }
        return isURL;
    }

    // ------------------------------------------
    // Verify ability to connect to the internet
    // ------------------------------------------
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
