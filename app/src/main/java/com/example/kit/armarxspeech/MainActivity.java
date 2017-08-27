package com.example.kit.armarxspeech;

import android.Manifest;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import armarx.AudioEncoding;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
{
    public static final String TAG = "ArmarXSpeech";

    // App permissions
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final int MIN_BUTTON_PRESS_TIME = 200;
    private long fab_microPressedStartTime;

    public static int N_TASKS = 0;
    private static boolean isListening = false;
    public boolean isMuted = false;

    private long lastTime = 0;
    private WaveRecorder waveRecorder;
    private HashMap<String, Integer> captions;
    private static String mFileName = null;
    private FloatingActionButton fab_micro, fab_send;
    private LinearLayout info_bar;
    private TextView info_text;
    private TextView info_action;
    private Menu options_menu;
    private EditText cmd;
    private Client _client;
    private ScrollView scroll_view_chat;
    private TextToSpeech text_to_speech;
    private ChatAdapter listAdapter;
    private ListView mainListView;
    private CoordinatorLayout coordinatorLayout;


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

        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        fab_micro = (FloatingActionButton) findViewById(R.id.fab_micro);
        fab_micro.setOnTouchListener(new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            if(event.getAction() == MotionEvent.ACTION_DOWN)
            {
                // change color
                ColorStateList colorStateList = ContextCompat.getColorStateList(getApplicationContext(), R.color.orange);
                fab_micro.setBackgroundTintList(colorStateList);

                // activate haptic feedback
                Vibrator vb = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vb.vibrate(70);

                // set starttime for button press
                fab_microPressedStartTime = System.currentTimeMillis();

                // check if thread is still running
                if(!isListening)
                {
                    startListenX();
                }

            }
            if(event.getAction() == MotionEvent.ACTION_UP)
            {
                //change color
                ColorStateList colorStateList = ContextCompat.getColorStateList(getApplicationContext(), R.color.colorAccent);
                if(fab_micro != null)
                {
                    fab_micro.setBackgroundTintList(colorStateList);
                }

                // check how long button was pressed
                long fab_microPressedTime = System.currentTimeMillis() - fab_microPressedStartTime;
                if(fab_microPressedTime < MIN_BUTTON_PRESS_TIME)
                {
                    Toast.makeText(getApplicationContext(),getResources().
                            getText(R.string.prompt_press_button), Toast.LENGTH_SHORT).show();
                }

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
                _client.sendTextMessage(cmd.getText().toString());

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

        info_bar = (LinearLayout) findViewById(R.id.info_bar);
        info_text = (TextView) findViewById(R.id.info_text);
        info_action = (TextView) findViewById(R.id.info_action);
        info_action.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                info_bar.setVisibility(View.GONE);
            }
        });

        scroll_view_chat = (ScrollView) findViewById(R.id.scroller_chat);
        text_to_speech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener()
        {
            @Override
            public void onInit(int status)
            {
                if(status != TextToSpeech.ERROR) {
                    text_to_speech.setLanguage(Locale.ENGLISH);
                }
            }
        });

        // initialize chat
        mainListView = (ListView) findViewById(R.id.mainListView);
        List<ChatMessage> chatMessages = new ArrayList<ChatMessage>();
        listAdapter = new ChatAdapter(this, chatMessages, mainListView);
        mainListView.setAdapter( listAdapter);


        // Prepare recorder
        waveRecorder = new WaveRecorder();

        // start client
        startClient();
    }

    private void startClient()
    {
        _client = new Client(this);
        _client.connect();
    }

    public TextToSpeech getTextToSpeach()
    {
        return text_to_speech;
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
            // Write External Storage permissions is already available
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

            Snackbar.make(coordinatorLayout, R.string.permission_write_external_storage_rationale,
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

            Snackbar.make(coordinatorLayout, R.string.permission_record_audio_rationale,
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
            Log.d("MainActivity", "Starting to listen...");
            isListening = true;

                //start listening
            waveRecorder.startRecording();
        }
    }

    private void stopListenX(boolean streamFile)
    {
        // check if it was listened
        if(!isListening)
        {
            Log.d("MainActivity", "Cannot stop listening, because i did not listen before...");
            return;
        }

        Log.d("MainActivity", "Stopping to listen...");
        waveRecorder.stopRecording();

        //send chunks
        if (streamFile)
        {
            new AsyncTask<Void, Void, Exception>() {
                @Override
                protected Exception doInBackground(Void... params) {
                    File file = new File(waveRecorder.getTempFile());

                    int size = (int) file.length();
                    Log.d("MainActivity", "Get File: " + file.getAbsolutePath() + ", Size: " + size);
                    byte[] bytes = new byte[size];
                    try {
                        BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                        buf.read(bytes, 0, bytes.length);
                        buf.close();

                        _client.streamAudioFile(waveRecorder.getTempFile(), AudioEncoding.PCM, waveRecorder.getMinBufferSize());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return null;
                }
            }.execute();
        }

        isListening = false;
        Log.d("MainActivity", "Stopped listening.");
    }

    public void printToInfoBar(String text, int backgroundColor)
    {
        info_bar.setBackgroundColor(backgroundColor);
        info_text.setText(text);
        info_bar.setVisibility(View.VISIBLE);
    }

    public void printToChat(long timestamp, String name, String message)
    {
        //ChatMessage(int messageType, String messageID, String message, String partnerID, String ownDeviceID)
        boolean isMine = (name.equals("ArmarX")) ? false : true;
        ChatMessage friendsMessage = new ChatMessage(ChatMessage.MESSAGE_TYPE_NORMAL_CHATMESSAGE, "0", message, "1", "12345", ArmarXUtils.convertTime(timestamp), isMine);
        listAdapter.add(friendsMessage);

        if(!isMine)
        {
            mainListView.smoothScrollToPosition(listAdapter.getCount()-1);
        }
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
            editTextIP.setText(Client.SERVER_IP);

            final EditText editTextPort = (EditText) dialog.findViewById(R.id.editTextPort);
            editTextPort.setText(Client.SERVER_PORT);

            // ok button
            Button okBtn = (Button) dialog.findViewById(R.id.btnSettingsOK);
            // if button is clicked, close the custom dialog
            okBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Here it is 2
                    Client.SERVER_IP = editTextIP.getText().toString();
                    Client.SERVER_PORT = editTextPort.getText().toString();
                    startClient();
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
                item.setChecked(false);

                // stop muting
                isMuted = false;
                printToInfoBar("Voice activated.", ContextCompat.getColor(this, R.color.green));
            }
            else
            {
                item.setChecked(true);

                // start muting
                isMuted = true;
                printToInfoBar("Voice muted.", ContextCompat.getColor(this, R.color.orange));

                TextToSpeech tts = getTextToSpeach();
                if(tts.isSpeaking())
                {
                    tts.stop();
                }
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static Bitmap getScreenShot(View view) {
        View screenView = view.getRootView();
        screenView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(screenView.getDrawingCache());
        screenView.setDrawingCacheEnabled(false);
        return bitmap;
    }

    public static void store(Bitmap bm, String fileName){
        final String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Screenshots";
        File dir = new File(dirPath);
        if(!dir.exists())
            dir.mkdirs();
        File file = new File(dirPath, fileName);
        try {
            FileOutputStream fOut = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.PNG, 85, fOut);
            fOut.flush();
            fOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void shareImage(File file){
        Uri uri = Uri.fromFile(file);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("image/*");

        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, "");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        try {
            startActivity(Intent.createChooser(intent, "Share Screenshot"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this.getApplicationContext(), "No App Available", Toast.LENGTH_SHORT).show();
        }
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_aboutus)
        {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://h2t.anthropomatik.kit.edu/"));
            startActivity(browserIntent);
        }
        else if (id == R.id.nav_aboutArmarX)
        {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://armarx.humanoids.kit.edu/"));
            startActivity(browserIntent);
        }
        else if (id == R.id.nav_share)
        {
            // close menu
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawers();

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
                    //View rootView = findViewById(R.id.scroller_chat);
                    Bitmap screenshot = getScreenShot(rootView);
                    Calendar c = Calendar.getInstance();
                    int seconds = c.get(Calendar.SECOND);
                    store(screenshot, "ArmarXSpeech_"+seconds+".jpg");
                    final String screenShotPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Screenshots";
                    File screenshotFile = new File(screenShotPath, "ArmarXSpeech_"+seconds+".jpg");
                    shareImage(screenshotFile);
                }
            }, 500);
        }
        else if (id == R.id.nav_send)
        {
            try
            {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, "ArmarXSpeech");
                String sAux = "Hey, I found this App at the KIT.\n";
                sAux = sAux + "https://armarx.humanoids.kit.edu/ \n";
                i.putExtra(Intent.EXTRA_TEXT, sAux);
                startActivity(Intent.createChooser(i, "choose one"));
            }
            catch(Exception e)
            {
                //e.toString();
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}
