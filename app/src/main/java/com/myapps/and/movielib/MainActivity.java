package com.myapps.and.movielib;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.drm.DrmStore;
import android.graphics.Movie;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.menu.MenuAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RatingBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static android.R.attr.button;
import static android.R.attr.filter;

public class MainActivity extends AppCompatActivity {

    ImageButton addMovieIB;
    ListView moviesLV;
    TextView emptyListTV;    // New list. No movies indication
    static String filterStr = AppConstants.EMPTY_FILTER; // Reset filter
    ImageView filterIV;      // Filter indication

    String settingOp=null;   // Holds enable/disable indication for messages

    ArrayList<MyMovie>  allMovies;           // Array list to hold all movies from db
    Cursor              moviesCursor;        // Query cursor for movies table

    MovieCursorAdapter    moviesCursorAdapter;

    // Save package variables from context menu for further use
    View currentView;
    int  selectedMoviePosition=-1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load application settings
        AppConstants.loadSettings(this);


        emptyListTV=(TextView) findViewById(R.id.emptyListTV);
        filterIV=(ImageView) findViewById(R.id.filterIV);
        filterIV.setVisibility(View.INVISIBLE); // Start with empty filter

        // Register add button for context menu
        addMovieIB = (ImageButton) findViewById(R.id.addMovieIB);
        setAddMovieOnClickButton();

        // Initialization
        moviesLV = (ListView) findViewById(R.id.moviesLV);
        allMovies = new ArrayList<MyMovie>();
        moviesCursorAdapter = new MovieCursorAdapter(this,moviesCursor);
        moviesLV.setAdapter(moviesCursorAdapter);

        // Enable shortClick for show plot for movie
         moviesLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                displayPlot(position);
            }
        });

        //Enable LongClick for specific movie options
        registerForContextMenu(moviesLV);

        // refresh screen at statup
        refreshScreen(filterStr);

        // Enable search filter field on filterET
        // Not using FilterQueryProvider
        EditText searchFilterET = (EditText) findViewById(R.id.filterET);
        searchFilterET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStr=s.toString();
                refreshScreen(filterStr);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshScreen(filterStr);

    }

    //**********************************************************************/
    //*                                                                    */
    //**********************************************************************/




    // Adds on click listener for addMovieIB
    private void setAddMovieOnClickButton () {

        addMovieIB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final Animation myAnim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.bounce);
                addMovieIB.startAnimation(myAnim);

                PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Intent addMovie;
                        switch (item.getItemId()) {
                            case R.id.pAddManual:
                                addMovie = new Intent(MainActivity.this, AddEditMovie.class);
                                addMovie.putExtra(AppConstants.KEY_ID, AppConstants.EMPTY_ID);
                                startActivityForResult(addMovie, AppConstants.ACTION_NEW_MOVIE_MANUAL);
                                break;
                            case R.id.pAddWeb:
                                addMovie = new Intent(MainActivity.this, SearchWeb.class);
                                addMovie.putExtra(AppConstants.KEY_ID, AppConstants.EMPTY_ID);
                                startActivityForResult(addMovie, AppConstants.ACTION_NEW_MOVIE_WEB);
                                break;
                        }
                        return true;
                    }
                });
                popupMenu.inflate(R.menu.popup_menu);
                popupMenu.show();
            }

        });
    }

    // Enable options menu for activity
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflate = getMenuInflater();
        inflate.inflate(R.menu.options_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        String msgStr=null;
        switch (item.getItemId()) {

            // Exit app
            case R.id.oExit:
                // TODO full exit
                // this.finishAffinity();
                finish();
                break;

            // Delete all option
            case R.id.oDeleteAll:
                // Display confirmation dialog
                msgStr=getString(R.string.msg_delete_all_alert);
                AlertDialog deleteAlert = new AlertDialog.Builder(this)
                        .setTitle(msgStr)
                        .setMessage(getString(R.string.msg_continue_alert))
                        .setPositiveButton(getString(R.string.msg_action_yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Deletes row from database and refresh screen
                                int movieCount=MovieDBHandler.deleteAllMovies(MainActivity.this);
                                refreshScreen(filterStr);
                                Toast.makeText(MainActivity.this,movieCount+" "+getString(R.string.msg_n_movies_deleted),Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(getString(R.string.msg_action_no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(MainActivity.this,getString(R.string.msg_delete_cancelled),Toast.LENGTH_SHORT).show();
                            }
                        })
                        .create();
                deleteAlert.show();
                break;

            // Setting menu
            case R.id.setSaveImage:
                // Modify settings for saving image
                String action=null;
                if ( AppConstants.saveImageLocaly ){
                    action=getString(R.string.msg_alert_action_disable);
                    settingOp=getString(R.string.msg_disabled);
                }else
                {
                    action=getString(R.string.msg_alert_action_enable);
                    settingOp=getString(R.string.msg_enabled);
                }
                msgStr = getString(R.string.msg_alert_settings)+" "+action+" "+getString(R.string.msg_alert_save_image);
                final AlertDialog settingDialog = new AlertDialog.Builder(this)
                        //.setTitle(msgStr)
                        .setMessage(msgStr)
                        .setPositiveButton(action, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Modify value
                                AppConstants.updateSaveImageLocaly(! AppConstants.saveImageLocaly);
                                Toast.makeText(MainActivity.this,getString(R.string.msg_alert_save_image)+" "+settingOp,Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();

                            }
                        })
                        .create();
                settingDialog.show();
                break;

            case R.id.setAutoDownload:
                // Modify settings for auto download image
                if ( AppConstants.autoDownloadImage ){
                    action=getString(R.string.msg_alert_action_disable);
                    settingOp=getString(R.string.msg_disabled);
                }else
                {
                    action=getString(R.string.msg_alert_action_enable);
                    settingOp=getString(R.string.msg_enabled);
                }
                msgStr = getString(R.string.msg_alert_settings)+" "+action+" "+getString(R.string.msg_alert_autoload_image);
                final AlertDialog settingDialog2 = new AlertDialog.Builder(this)
                        .setMessage(msgStr)
                        .setPositiveButton(action, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Modify value
                                AppConstants.updateAutoDownloadImage(! AppConstants.autoDownloadImage);
                                Toast.makeText(MainActivity.this,getString(R.string.msg_alert_autoload_image)+" "+settingOp,Toast.LENGTH_SHORT).show();

                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();

                            }
                        })
                        .create();
                settingDialog2.show();
                break;

            // Sort menu options
            case R.id.sortSubject:
                MovieDBHandler.setOrderBy(DBConstants.SUBJECT_C+" ASC");
                refreshScreen(filterStr);
                break;

            case R.id.sortRating:
                MovieDBHandler.setOrderBy(DBConstants.RATING_C+" DESC");
                refreshScreen(filterStr);
                break;

            case R.id.sortSeen:
                MovieDBHandler.setOrderBy(DBConstants.WATCHED_C+" ASC");
                refreshScreen(filterStr);
                break;
        }
        return true;
    }

    // Enable context menu for edit/delete movie
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

        currentView=v;


        if ( v.getId() == R.id.moviesLV ) {
            // Save current item position
            selectedMoviePosition = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
            menu.setHeaderTitle(allMovies.get(selectedMoviePosition).subject);
        }

         // Display set watched/unwatched according to movie selected
        getMenuInflater().inflate(R.menu.movie_action_menu,menu);
        if (allMovies.get(selectedMoviePosition).Watched == DBConstants.NOT_WATCHED)
        {
            menu.getItem(3).setVisible(false);
        } else
        {
            menu.getItem(2).setVisible(false);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {


        switch (item.getItemId()) {
            case R.id.aEditMovie:
                // Edit movie
                Intent addMovie = new Intent(MainActivity.this,AddEditMovie.class);
                addMovie.putExtra(AppConstants.KEY_ID,allMovies.get(selectedMoviePosition).getId());
                Toast.makeText(MainActivity.this,getString(R.string.msg_editing_movie)+" "+allMovies.get(selectedMoviePosition).getSubject(),Toast.LENGTH_SHORT).show();
                startActivityForResult(addMovie,AppConstants.ACTION_EDIT_MOVIE);
                break;

            case R.id.aDelMovie:
                // Delete movie upon user confirmation
                String msgStr=getString(R.string.msg_about_to_delete)+" "+ allMovies.get(selectedMoviePosition).getSubject() ;
                AlertDialog deleteAlert = new AlertDialog.Builder(MainActivity.this)
                        .setTitle(msgStr)
                        .setMessage(getString(R.string.msg_continue_alert))
                        .setPositiveButton(getString(R.string.msg_action_yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Delete row from database and refresh screen
                                MovieDBHandler.deleteMovieById(MainActivity.this,allMovies.get(selectedMoviePosition).getId());
                                refreshScreen(filterStr);
                                Toast.makeText(MainActivity.this,getString(R.string.msg_n_movie_deleted),Toast.LENGTH_SHORT).show();

                            }
                        })
                        .setNegativeButton(getString(R.string.msg_action_no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(MainActivity.this,getString(R.string.msg_delete_cancelled),Toast.LENGTH_SHORT).show();
                            }
                        })
                        .create();

                deleteAlert.show();
                break;

            case R.id.aSetWatched:
                // Setup watched indicator for movie
                MovieDBHandler.setWatched(this,allMovies.get(selectedMoviePosition).getId(),true);
                refreshScreen(filterStr);
                break;

            case R.id.aSetUnwatched:
                // Setup unwatched indicator for movie
                MovieDBHandler.setWatched(this,allMovies.get(selectedMoviePosition).getId(),false);
                refreshScreen(filterStr);
                break;

            case R.id.arateMovie:
                // Display popup rating menu
                rateDialog();
                break;

        }
        return true;
    }

    // Setup on activity result actions - currently no actions
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ( resultCode == RESULT_OK ) {
            switch (requestCode) {
                case AppConstants.ACTION_NEW_MOVIE_MANUAL:
                    // TODO upon new movie manual - TBD
                    break;
                case AppConstants.ACTION_EDIT_MOVIE:
                    // TODO upon edit movie - TBD
                    break;
                case AppConstants.ACTION_NEW_MOVIE_WEB:
                    // TODO upo new movie from internet - TBD
                    break;

            }
            refreshScreen(filterStr);
        }
    }

    // Popup rating dialog for selected movie
    private void rateDialog()
    {
        final AlertDialog.Builder rateDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.rateme,null);

        rateDialog.setTitle(allMovies.get(selectedMoviePosition).getSubject());
        rateDialog.setView(dialogView);
        final RatingBar rating = (RatingBar) dialogView.findViewById(R.id.ratingMovie);
        rating.setRating(allMovies.get(selectedMoviePosition).getRating());

        // Button OK
        rateDialog.setPositiveButton(getString(R.string.rate_movie),
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                MovieDBHandler.setRating(MainActivity.this,allMovies.get(selectedMoviePosition).getId(),(int) rating.getRating());
                refreshScreen(filterStr);
            }
        })
        // Button Cancel
                .setNegativeButton(getString(R.string.cancel),
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        rateDialog.create();
        rateDialog.show();

    }



    // Retrieve movie plot from the internet in case of editing from web search result
    private void displayPlot(int itemPosition)
    {

        final AlertDialog.Builder plotDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.activity_display_body,null);


        plotDialog.setTitle(allMovies.get(itemPosition).getSubject());
        plotDialog.setView(dialogView);

        // Button back
        plotDialog.setNegativeButton(getString(R.string.back),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                           }
                        });

        plotDialog.create();
        plotDialog.show();

        TextView body = (TextView) dialogView.findViewById(R.id.iBodyTV);
        body.setText(allMovies.get(itemPosition).getBody());

    }


    // Refresh screen after changes
    public void refreshScreen (String filter) {

        // Reset movies array
        allMovies.clear();

        // Get data from database with/without title filtering
        if ( filter.equals(AppConstants.EMPTY_FILTER) ) {
            moviesCursor = MovieDBHandler.fetchAllMovies(this);
            filterIV.setVisibility(View.INVISIBLE);
        }
        else
        {
            moviesCursor = MovieDBHandler.fetchByTitle(this,filterStr);
            filterIV.setVisibility(View.VISIBLE);
        }

        // Load cursor data to movies array
        while ( moviesCursor.moveToNext() )
        {
            allMovies.add(new MyMovie( moviesCursor.getInt(moviesCursor.getColumnIndex(DBConstants.ID_C)),
                    moviesCursor.getString( moviesCursor.getColumnIndex(DBConstants.SUBJECT_C)),
                    moviesCursor.getString( moviesCursor.getColumnIndex(DBConstants.BODY_C)),
                    moviesCursor.getString( moviesCursor.getColumnIndex(DBConstants.IMAGE_URL_C)),
                    moviesCursor.getInt( moviesCursor.getColumnIndex(DBConstants.RATING_C)),
                    moviesCursor.getInt( moviesCursor.getColumnIndex(DBConstants.WATCHED_C))));
        }

        // Notify adapter with data change
        moviesCursorAdapter.swapCursor(moviesCursor);

        if ( allMovies.size() == 0 & filter.equals(AppConstants.EMPTY_FILTER) ) {
            // Empty list
            moviesLV.setVisibility(View.GONE);
            emptyListTV.setVisibility(View.VISIBLE);
            addMovieIB.requestFocus();

        }
        else
        {
            moviesLV.setVisibility(View.VISIBLE);
            emptyListTV.setVisibility(View.GONE);
        }
    }




}
