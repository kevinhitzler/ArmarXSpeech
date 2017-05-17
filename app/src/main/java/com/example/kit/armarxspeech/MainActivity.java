package com.example.kit.armarxspeech;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import armarx.AudioEncoding;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
{
    public static final String TAG = "ArmarXSpeech";

    /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "wakeup";

    // used to activate armar speech recognition
    private static final String KEYPHRASE = "OKAY ARMAR";

    // App permissions
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    public static int N_TASKS = 0;
    private static boolean isListening = false;
    private static boolean isMuted = false;

    private long lastTime = 0;
    private WaveRecorder waveRecorder;
    private HashMap<String, Integer> captions;
    private static String mFileName = null;
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

        fab_micro = (FloatingActionButton) findViewById(R.id.fab_micro);
        fab_micro.setOnTouchListener(new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            if(event.getAction() == MotionEvent.ACTION_DOWN)
            {
                startListenX();
                Vibrator vb = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vb.vibrate(70);
            }
            if(event.getAction() == MotionEvent.ACTION_UP)
            {
                stopListenX(true);
            }
            return true;
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


    private boolean allPermissionsGranted()
    {
        boolean WRITE_EXTERNAL_STORAGE_GRANTED = false;
        boolean RECORD_AUDIO_GRANTED = false;

        // check for permissions
        // WRITE EXTERNAL STORAGE PERMISSION
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {
            // External Storage permission has not been granted.
            requestWriteExternalStoragePermission();
        }
        else
        {
            // Write External Storage permissions is already available, show the camera preview.
            Log.i(TAG, "WRITE_EXTERNAL_STORAGE permission has already been granted.");
            WRITE_EXTERNAL_STORAGE_GRANTED = true;
        }

        // RECORD AUDIO PERMISSION
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {
            // Record Audio permission has not been granted.
            requestRecordAudioPermission();
        }
        else
        {
            // Record Audio permission is already available.
            Log.i(TAG, "RECORD AUDIO permission has already been granted.");
            RECORD_AUDIO_GRANTED = true;
        }

        if(WRITE_EXTERNAL_STORAGE_GRANTED &&
           RECORD_AUDIO_GRANTED)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    private void requestWriteExternalStoragePermission()
    {
        Log.i(TAG, "Write External Storage permission has NOT been granted. Requesting permission.");

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE))
        {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example if the user has previously denied the permission.
            Log.i(TAG, "Displaying write external storage permission rationale to provide additional context.");

            Snackbar.make(warning_bar, R.string.permission_write_external_storage_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                        }
                    })
                    .show();
        }
        else
        {
            // Write External Storage permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
    }

    private void requestRecordAudioPermission()
    {
        Log.i(TAG, "Record audio permission has NOT been granted. Requesting permission.");

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.RECORD_AUDIO))
        {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example if the user has previously denied the permission.
            Log.i(TAG, "Displaying record audio storage permission rationale to provide additional context.");

            Snackbar.make(warning_bar, R.string.permission_record_audio_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.RECORD_AUDIO},
                                    MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
                        }
                    })
                    .show();
        }
        else
        {
            // Record audio permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
        }
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        stopListenX(false);
    }

    private void startListenX()
    {
        if(allPermissionsGranted() == true)
        {
            //stop recognizer
            isListening = true;

            // change status
            ((TextView) findViewById(R.id.status)).setText(getResources().getString(R.string.listening));

            // change color
            ColorStateList colorStateList = ContextCompat.getColorStateList(getApplicationContext(), R.color.orange);
            fab_micro.setBackgroundTintList(colorStateList);

            //start listening
            waveRecorder.startRecording();
        }
    }

    private void stopListenX(boolean streamFile)
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
                    File file = new File(waveRecorder.getTempFile());

                    int size = (int) file.length();
                    Log.d("MainActivity", "Get File: "+file.getAbsolutePath()+", Size: "+size);
                    byte[] bytes = new byte[size];
                    try {
                        BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                        buf.read(bytes, 0, bytes.length);
                        buf.close();

                        //Client.sendFile(bytes, AudioEncoding.PCM, System.currentTimeMillis());
                        Client.streamFile(getApplicationContext(), waveRecorder.getTempFile(), AudioEncoding.PCM, System.currentTimeMillis(), waveRecorder.getMinBufferSize());
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

        String caption = getResources().getString(captions.get(KWS_SEARCH));
        ((TextView) findViewById(R.id.status)).setText(caption);

        isListening = false;
    }

    public void showMessageToUser(String text)
    {
        ((TextView) findViewById(R.id.status)).setText(text);
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
            //display ip and port settings
            // custom dialog
            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.settings_dialog);

            final EditText editTextIP = (EditText) dialog.findViewById(R.id.editTextIP);
            editTextIP.setText(Client.IP_ADDRESS_SERVER);

            final EditText editTextPort = (EditText) dialog.findViewById(R.id.editTextPort);
            editTextPort.setText(Client.PORT_SERVER);

            // ok button
            Button okBtn = (Button) dialog.findViewById(R.id.btnSettingsOK);
            // if button is clicked, close the custom dialog
            okBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Client.IP_ADDRESS_SERVER = editTextIP.getText().toString();
                    Client.PORT_SERVER = editTextPort.getText().toString();
                    dialog.dismiss();
                }
            });

            // cancel button
            Button cancelBtn = (Button) dialog.findViewById(R.id.btnSettingsCancel);
            // if button is clicked, close the custom dialog
            cancelBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            dialog.show();


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
            }
            else
            {
                // start muting
                isMuted = true;
                warning_bar.setVisibility(View.VISIBLE);
                item.setChecked(true);
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

}
