/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.newventuresoftware.waveformdemo;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import java.lang.reflect.*;
import java.lang.annotation.Annotation;

import com.newventuresoftware.waveform.WaveformView;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity implements PlaybackThread.MyCallback, RecordingThread.RecCallback {

    DrawView drawView;


    private WaveformView mRealtimeWaveformView;
    private RecordingThread mRecordingThread;
    private PlaybackThread mPlaybackThread;
    private static final int REQUEST_RECORD_AUDIO = 13;

    private int mytime;
    private int mytime2;

   // PlaybackThread one = new PlaybackThread();
   // long time= one.getTotalWritten();
   // long realtime = time/22050/2;

  //  RecordingThread two = new RecordingThread();
  //  long time2= two.getShortsRead();
  //  long realtime2 = time2/22050/2;

    Class clazz = WaveformView.class;
    Package p = clazz.getPackage();
    int modifiers = clazz.getModifiers();
    //int field = clazz.getField("mAudioLength");
    Field[] fields = clazz.getDeclaredFields();

    //Field dtField = clazz.getDeclaredField("mAudioLength");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Log.v("p", String.valueOf(p) + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

        for (Field field : fields) {
           Log.v("Lala","\t"+field.getName() + ";!!!!!!!!!!!!!!!!!!!!!!!!");
        }


        mRealtimeWaveformView = (WaveformView) findViewById(R.id.waveformView);


        mRecordingThread = new RecordingThread(new AudioDataReceivedListener() {
            @Override
            public void onAudioDataReceived(short[] data) {
                mRealtimeWaveformView.setSamples(data);
            }
        }, MainActivity.this);

        final WaveformView mPlaybackView = (WaveformView) findViewById(R.id.playbackWaveformView);

        FloatingActionButton important = (FloatingActionButton) findViewById(R.id.important);
        important.setOnClickListener(new View.OnClickListener() {
                                         public void onClick(View v) {
                                            // drawView = new DrawView(this);
                                            // drawView.setBackgroundColor(Color.WHITE);
                                            // setContentView(drawView);

                                             SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd");
                                             Date now = new Date();
                                             String fileName = formatter.format(now) + ".txt";//like 2016_01_12.txt
                                             String sBody = "Time: "+mytime+ "  Time2: "+mytime2  ;

                                             try
                                             {
                                                // File root = new File(Environment.getExternalStorageDirectory()+File.separator+"Music_Folder", "Report Files");
                                                 File root = new File(Environment.getExternalStorageDirectory(), "Notes");
                                                 if (!root.exists())
                                                 {
                                                     root.mkdirs();
                                                 }
                                                 File gpxfile = new File(root, fileName);


                                                 FileWriter writer = new FileWriter(gpxfile,true);
                                                 writer.append(sBody+"\n\n");
                                                 writer.flush();
                                                 writer.close();
                                                // Toast.makeText(this, "Data has been written to Report File", Toast.LENGTH_SHORT).show();
                                             }
                                             catch(IOException e)
                                             {
                                                 e.printStackTrace();

                                             }
                                         }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mRecordingThread.recording()) {
                    startAudioRecordingSafe();
                } else {
                    mRecordingThread.stopRecording();
                }
            }
        });

        short[] samples = null;
        try {
            samples = getAudioSample();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (samples != null) {
            final FloatingActionButton playFab = (FloatingActionButton) findViewById(R.id.playFab);

            mPlaybackThread = new PlaybackThread(samples, new PlaybackListener() {
                @Override
                public void onProgress(int progress) {
                    mPlaybackView.setMarkerPosition(progress);
                }
                @Override
                public void onCompletion() {
                    mPlaybackView.setMarkerPosition(mPlaybackView.getAudioLength());
                    playFab.setImageResource(android.R.drawable.ic_media_play);
                }
            },MainActivity.this);
            mPlaybackView.setChannels(1);
            mPlaybackView.setSampleRate(PlaybackThread.SAMPLE_RATE);
            mPlaybackView.setSamples(samples);

            playFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mPlaybackThread.playing()) {
                        mPlaybackThread.startPlayback();
                        playFab.setImageResource(android.R.drawable.ic_media_pause);
                    } else {
                        mPlaybackThread.stopPlayback();
                        playFab.setImageResource(android.R.drawable.ic_media_play);
                    }
                }
            });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        mRecordingThread.stopRecording();
        mPlaybackThread.stopPlayback();
    }

    private short[] getAudioSample() throws IOException{
        InputStream is = getResources().openRawResource(R.raw.jinglebells);
        byte[] data;
        try {
            data = IOUtils.toByteArray(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }

        ShortBuffer sb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        short[] samples = new short[sb.limit()];
        sb.get(samples);
        return samples;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startAudioRecordingSafe() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            mRecordingThread.startRecording();
        } else {
            requestMicrophonePermission();
        }
    }

    private void requestMicrophonePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.RECORD_AUDIO)) {
            // Show dialog explaining why we need record audio
            Snackbar.make(mRealtimeWaveformView, "Microphone access is required in order to record audio",
                    Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                            android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
                }
            }).show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mRecordingThread.stopRecording();
        }
        int supertime = mRealtimeWaveformView.getAudioLength();
        Log.v("suprtime", supertime+"!!!!!!!!!!!!!!!!!!!!!!!!!");
     //   Log.v("realtime", +realtime+"!!!!!!!!!!!!!!!!!!!!!!!!!");
      //  Log.v("realtime", +realtime2+"!!!!!!!!!!!!!!!!!!!!!!!!!");
    }

    @Override
    public void callbackCall(int x) {
        mytime = x;
        Log.i("TIME", x +"    Very important info" );
    }

    @Override
    public void reccallbackCall(int y) {
        mytime2 = y;
        Log.i("TIME", y +"    Very important info 2!!!!" );
    }
}
