package com.example.kit.armarxspeech;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static android.widget.Toast.makeText;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, RecognitionListener
{
    public static final String TAG = "ArmarXSpeech";

    /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "wakeup";

    // used to activate armar speech recognition
    private static final String KEYPHRASE = "okay robot";

    // Used to handle permission request
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private boolean isListening = false;
    private boolean isMuted = false;

    private SpeechRecognizer recognizer;
    private HashMap<String, Integer> captions;
    private MediaPlayer m_notify;
    private TextView s_notify;
    private FloatingActionButton fab;
    private LinearLayout warning_bar;
    private TextView warning_action;
    private Menu options_menu;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        RelativeLayout content_layout = (RelativeLayout) findViewById(R.id.content_layout);
        content_layout.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(isListening)
                {
                    stopListenX();
                }
            }
        });

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(isListening)
                {
                    stopListenX();
                }
                else
                {
                    startListenX();
                }
            }
        });

        warning_bar = (LinearLayout) findViewById(R.id.warning_bar);
        warning_action = (TextView) findViewById(R.id.warning_action);
        warning_action.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // stop muting
                isMuted = false;
                warning_bar.setVisibility(View.GONE);
                MenuItem action_mute = (MenuItem) options_menu.findItem(R.id.action_mute);
                action_mute.setChecked(false);

                if(recognizer != null)
                {
                    switchSearch(KWS_SEARCH);
                }
            }
        });

        // Prepare the data for UI
        captions = new HashMap<String, Integer>();
        captions.put(KWS_SEARCH, R.string.kws_status);
    }

    private void runRecognizerSetup()
    {
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        new AsyncTask<Void, Void, Exception>()
        {
            @Override
            protected Exception doInBackground(Void... params)
            {
                try
                {
                    Assets assets = new Assets(MainActivity.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                }
                catch (IOException e)
                {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result)
            {
                if (result != null)
                {
                    ((TextView) findViewById(R.id.status))
                            .setText("Failed to init recognizer " + result);
                }
                else
                {
                    switchSearch(KWS_SEARCH);
                }
            }
        }.execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                runRecognizerSetup();
            }
            else
            {
                finish();
            }
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (recognizer != null)
        {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    private void setupRecognizer(File assetsDir) throws IOException
    {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))

                .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                .setKeywordThreshold(1e-35f) // Threshold to tune for keyphrase to balance between false alarms and misses old: 1e-45f
                .setBoolean("-allphone_ci", true)  // Use context-independent phonetic search, context-dependent is too slow for mobile


                .getRecognizer();
        recognizer.addListener(this);

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
    }

    private void switchSearch(String searchName)
    {
        recognizer.stop();

        if(!isMuted)
        {
            // If we are spotting
            if (searchName.equals(KWS_SEARCH))
            {
                recognizer.startListening(searchName);
            }
        }
    }

    private void startListenX()
    {
        isListening = true;

        // change status
        ((TextView) findViewById(R.id.status)).setText("Armar is listening...");

        // change color
        ColorStateList colorStateList = ContextCompat.getColorStateList(getApplicationContext(), R.color.orange);
        fab.setBackgroundTintList(colorStateList);

        // play sound
        if(m_notify == null)
        {
            m_notify = MediaPlayer.create(getApplicationContext(), R.raw.beep_sound);
            m_notify.setLooping(false);
            m_notify.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
        }
        else
        {
            m_notify.start();
        }

        // show snackbar
        if(s_notify == null)
        {
            s_notify = (TextView) findViewById(R.id.prompt);
        }
        s_notify.setVisibility(View.VISIBLE);
    }

    private void stopListenX()
    {
        //change color
        ColorStateList colorStateList = ContextCompat.getColorStateList(getApplicationContext(), R.color.colorAccent);
        if(fab != null)
        {
            fab.setBackgroundTintList(colorStateList);
        }

        // pause and hide
        if(m_notify != null)
        {
            m_notify.pause();
        }
        if(s_notify != null)
        {
            s_notify.setVisibility(View.GONE);
        }

        // change status
        String caption = getResources().getString(captions.get(KWS_SEARCH));
        ((TextView) findViewById(R.id.status)).setText(caption);
        if(recognizer != null)
        {
            switchSearch(KWS_SEARCH);
        }

        isListening = false;
    }

    @Override
    public void onBackPressed()
    {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START))
        {
            drawer.closeDrawer(GravityCompat.START);
        }
        else
        {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.options_menu = menu;
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }
        else if(id == R.id.action_mute)
        {
            if(item.isChecked())
            {
                // stop muting
                isMuted = false;
                warning_bar.setVisibility(View.GONE);
                item.setChecked(false);

                if(recognizer != null)
                {
                    switchSearch(KWS_SEARCH);
                }
            }
            else
            {
                // start muting
                isMuted = true;
                warning_bar.setVisibility(View.VISIBLE);
                item.setChecked(true);
                if(recognizer != null)
                {
                    recognizer.stop();
                    recognizer.cancel();
                }
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    /**
     * We stop recognizer here to get a final result
     */
    @Override
    public void onEndOfSpeech()
    {
        switchSearch(KWS_SEARCH);
    }

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis)
    {
        if (hypothesis == null)
        {
            return;
        }

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE))
        {
            recognizer.stop();
        }
    }

    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(Hypothesis hypothesis)
    {
        if (hypothesis != null)
        {
            String text = hypothesis.getHypstr();
            Log.d(TAG, "Recognized Keyphrase: "+ KEYPHRASE);
            startListenX();
        }
    }

    @Override
    public void onError(Exception error)
    {
        ((TextView) findViewById(R.id.status)).setText(error.getMessage());
    }

    @Override
    public void onTimeout()
    {
        switchSearch(KWS_SEARCH);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        startRecognitionService();
    }

    private void startRecognitionService()
    {
        // set up listener
        // Check if user has given permission to record audio
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck == PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
        runRecognizerSetup();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        stopListenX();
        if(recognizer != null)
        {
            if (recognizer != null)
            {
                recognizer.cancel();
                recognizer.shutdown();
            }
        }
    }

}
