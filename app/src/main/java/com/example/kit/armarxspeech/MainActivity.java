package com.example.kit.armarxspeech;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import armarx.AudioEncoding;
import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;
import edu.cmu.pocketsphinx.SpeechRecognizer;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, RecognitionListener
{
    public static final String TAG = "ArmarXSpeech";

    /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "wakeup";

    // used to activate armar speech recognition
    private static final String KEYPHRASE = "OKAY ARMAR";

    // Used to handle permission request
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    public static int N_TASKS = 0;
    private static boolean isListening = false;
    private static boolean isMuted = false;

    private SpeechRecognizer recognizer;
    private long lastTime = 0;
    private WaveRecorder waveRecorder;
    private HashMap<String, Integer> captions;
    private MediaPlayer m_notify;
    private static String mFileName = null;
    private TextView s_notify;
    private FloatingActionButton fab_micro, fab_send;
    private LinearLayout warning_bar;
    private TextView warning_action;
    private Menu options_menu;
    private EditText cmd;


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
                long millis = System.currentTimeMillis();
                if((millis - lastTime) > 1000)
                {
                    if(isListening)
                    {
                        stopListenX(true, true);
                    }
                    lastTime = millis;
                }
                else
                {
                    Log.e(TAG, "NOPE: "+ Long.toString(millis-lastTime));
                }
            }
        });

        fab_micro = (FloatingActionButton) findViewById(R.id.fab_micro);
        fab_micro.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                long millis = System.currentTimeMillis();
                if((millis - lastTime) > 1000)
                {
                    Log.e(TAG, "YES: "+ Long.toString(millis-lastTime));
                    if(isListening)
                    {
                        stopListenX(true, true);
                    }
                    else
                    {
                        startListenX();
                    }
                    lastTime = millis;
                }
                else
                {
                    Log.e(TAG, "NOPE: "+ Long.toString(millis-lastTime));
                }
            }
        });

        fab_send = (FloatingActionButton) findViewById(R.id.fab_send);
        fab_send.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //send msg to server
                EditText cmd = ((EditText) findViewById(R.id.cmd));
                Client.send(cmd.getText().toString());

                //reset text
                ((EditText) findViewById(R.id.cmd)).setText("");
            }
        });

        cmd = (EditText) findViewById(R.id.cmd);
        cmd.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                if(s.toString().length() <= 0)
                {
                    toggleFAB(fab_micro);
                }
                else if (s.toString().length() > 0)
                {
                    toggleFAB(fab_send);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

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

                Log.d(TAG, "warning_action.onClick calls switchSearch");
                runRecognizerSetup();
            }
        });

        // Prepare the data for UI
        captions = new HashMap<String, Integer>();
        captions.put(KWS_SEARCH, R.string.kws_status);

        // Prepare recorder
        waveRecorder = new WaveRecorder();
    }

    private void toggleFAB(FloatingActionButton fab)
    {
        if(fab.equals(fab_micro))
        {
            fab_send.setVisibility(View.GONE);
            fab_micro.setVisibility(View.VISIBLE);
        }
        else
        {
            fab_micro.setVisibility(View.GONE);
            fab_send.setVisibility(View.VISIBLE);
        }
    }

    private void runRecognizerSetup()
    {
        // Check if user has given permission to record audio
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }

        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        new AsyncTask<Void, Void, Exception>()
        {
            @Override
            protected Exception doInBackground(Void... params)
            {
                N_TASKS++;
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
            protected void onPostExecute(final Exception result)
            {
                if (result != null)
                {
                    ((TextView) findViewById(R.id.status))
                            .setText("Failed to init recognizer " + result);
                    Log.e(TAG, "Error onPostExecute");
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
        stopListenX(false, false);

        if (recognizer != null) {
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
                .setDictionary(new File(assetsDir, "3979.dic"))

                //.setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                .setKeywordThreshold(1e-30f) // Threshold to tune for keyphrase to balance between false alarms and misses old: 1e-45f
                .setBoolean("-allphone_ci", true)  // Use context-independent phonetic search, context-dependent is too slow for mobile


                .getRecognizer();
        recognizer.addListener(this);

        // Create keyword-activation search
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
    }

    private void switchSearch(String searchName)
    {
        recognizer.stop();

        if (!isMuted) {
            // If we are spotting
            if (searchName.equals(KWS_SEARCH))
            {
                recognizer.startListening(searchName);
            }

            String caption = getResources().getString(captions.get(KWS_SEARCH));
            ((TextView) findViewById(R.id.status)).setText(caption);
        }
    }

    private void startListenX()
    {
        //stop recognizer
        isListening = true;

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }

        // change status
        ((TextView) findViewById(R.id.status)).setText(getResources().getString(R.string.listening));

        // change color
        ColorStateList colorStateList = ContextCompat.getColorStateList(getApplicationContext(), R.color.orange);
        fab_micro.setBackgroundTintList(colorStateList);

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
            m_notify.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
            {
                @Override
                public void onCompletion(MediaPlayer mp)
                {
                    // start recording
                    waveRecorder.startRecording();
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

    private void stopListenX(boolean streamFile, boolean startRecognizer)
    {
        // stop recording
        waveRecorder.stopRecording();


        //send chunks
        if (streamFile) {

            new AsyncTask<Void, Void, Exception>()
            {
                @Override
                protected Exception doInBackground(Void... params)
                {
                    File file = new File(waveRecorder.getFilename());
                    int size = (int) file.length();
                    byte[] bytes = new byte[size];
                    try {
                        BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                        buf.read(bytes, 0, bytes.length);
                        buf.close();

                        //Client.sendFile(bytes, AudioEncoding.PCM, System.currentTimeMillis());
                        Client.streamFile(getApplicationContext(), waveRecorder.getFilename(), AudioEncoding.PCM, System.currentTimeMillis(), waveRecorder.getMinBufferSize());
                    } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    return null;
                }
            }.execute();
        }

        //change color
        ColorStateList colorStateList = ContextCompat.getColorStateList(getApplicationContext(), R.color.colorAccent);
        if(fab_micro != null)
        {
            fab_micro.setBackgroundTintList(colorStateList);
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

        String caption = getResources().getString(captions.get(KWS_SEARCH));
        ((TextView) findViewById(R.id.status)).setText(caption);

        if(startRecognizer)
        {
            if (recognizer != null) {
                recognizer.cancel();
                recognizer.shutdown();
            }

            runRecognizerSetup();
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
            //display in short period of time
            Toast.makeText(getApplicationContext(), "No settings available.", Toast.LENGTH_SHORT).show();
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

                Log.d(TAG, "onOptionsItemSelected() calls switchSearch");
                runRecognizerSetup();
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
                    recognizer.shutdown();
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
    public void onEndOfSpeech() {
       /* if (recognizer != null && !recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);*/
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
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE))
            startListenX();
        else
            ((TextView) findViewById(R.id.status)).setText(text);
    }

    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(Hypothesis hypothesis) {
    }

    @Override
    public void onError(Exception error)
    {
        Log.e(TAG, "Error MainActivity Callback");
        ((TextView) findViewById(R.id.status)).setText(error.getMessage());
        error.printStackTrace();

        Log.e(TAG, "NUMBER OF TASKS: "+ Integer.toString(N_TASKS));
    }

    @Override
    public void onTimeout()
    {
        //switchSearch(KWS_SEARCH);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        runRecognizerSetup();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        stopListenX(false, false);
        if (recognizer != null)
        {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

}
