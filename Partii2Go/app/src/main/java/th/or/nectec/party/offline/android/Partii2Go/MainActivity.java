package th.or.nectec.party.offline.android.Partii2Go;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.TextView;

import java.io.File;

import th.or.nectec.partii.embed.android.EmbeddedUtils.Constant;
import th.or.nectec.partii.embed.android.EmbeddedUtils.ModelUtil;

import th.or.nectec.party.offline.android.Partii2Go.button.ConvertingButton;
import th.or.nectec.party.offline.android.Partii2Go.button.MainButton;
import th.or.nectec.party.offline.android.Partii2Go.button.RecordingButton;
import th.or.nectec.party.offline.android.Partii2Go.helper.LangModelHelper;
import th.or.nectec.party.offline.android.Partii2Go.util.DilaogUtil;

import th.or.nectec.partii.embed.android.RecognitionListener;
import th.or.nectec.partii.embed.android.SpeechRecognizer;

import android.view.Menu;
import android.view.MenuItem;
import android.content.Context;
import android.util.Log;

import android.os.Handler;
import android.os.Message;

import io.netpie.microgear.Microgear;
import io.netpie.microgear.MicrogearEventListener;


/**
 * Created by hin on 5/7/16.
 * Editted by Suwan Tongpu (Code Runner Co., Ltd.) on 19/11/16
 */
public class MainActivity extends AppCompatActivity implements RecognitionListener, ModelUtil.OnReceiveStatusListener {

    private TextView messageText;
    private TextView resultText;
    private TextView resultHeader;
    private CardView resultPanel;

    MainButton defaultButton;
    RecordingButton dictatingButton;
    ConvertingButton processingButton;
    private SpeechRecognizer recognizer;

    private String decodedStr = "";
    private boolean isSetupRecognizer = false;
    private ModelUtil mUtil = new ModelUtil();
    String imei = "";

    private Microgear microgear = null;
    private String appid = ""; //APP_ID
    private String key = ""; //KEY
    private String secret = ""; //SECRET
    private String alias = "DigitalOUTPUT_HTML_web";
    private String thing = "NodeMCU1";

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            String string = bundle.getString("myKey");
            System.out.println(string);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        microgear.bindServiceResume();
        if (!isSetupRecognizer) {

        }

        String langStr = PreferenceManager.getDefaultSharedPreferences(this).getString("language_preference", null);
        if (langStr != null) {

            if (langStr.equals("TH")) {
                Constant.setThai();
            } else {
                Constant.setEnglish();
            }
        }

        messageText.setText(Constant.CONVERSION_START_MSG);
        resultHeader.setText(Constant.CONVERSION_HEADER);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        microgear = new Microgear(this);
        MicrogearCallBack callback = new MicrogearCallBack();
        microgear.connect(appid, key, secret, alias);
        microgear.setCallback(callback);

        //getSupportActionBar().setIcon(R.drawable.app_top);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.drawable.bar_img);
        getSupportActionBar().setDisplayUseLogoEnabled(true);

        resultPanel = (CardView) findViewById(R.id.resultPanel);
        messageText = (TextView) findViewById(R.id.textView1);
        resultText = (TextView) findViewById(R.id.resultText);
        resultHeader = (TextView) findViewById(R.id.resultHeader);

        defaultButton = (MainButton) findViewById(R.id.main);
        defaultButton.setImageResource(R.drawable.main);

        processingButton = (ConvertingButton) findViewById(R.id.convert_start);
        processingButton.setImageResource(R.drawable.convert_start);

        dictatingButton = (RecordingButton) findViewById(R.id.sound_record);
        dictatingButton.setImageResource(R.drawable.sound_record2);

        resultHeader.setText(Constant.CONVERSION_HEADER);
        resultText.setText("");
        messageText.setText(Constant.CONVERSION_START_MSG);

        resultPanel.setVisibility(View.GONE);
        messageText.setVisibility(View.VISIBLE);

        defaultButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (isSetupRecognizer) {
                    defaultButton.setVisibility(View.GONE);
                    processingButton.setVisibility(View.GONE);
                    dictatingButton.setVisibility(View.VISIBLE);
                    recognizer.startListening();
                    displayMessage(Constant.CONVERSION_SPEAKING_MSG);
                } else {
                    if (mUtil.isPermissionGranted(getApplicationContext())) {
                        File f = new File(getExternalFilesDir("") + "/sync" + "/assets.lst");
                        if (f.exists()) {
                            setUpRecognizer();

                            defaultButton.setVisibility(View.GONE);
                            processingButton.setVisibility(View.GONE);
                            dictatingButton.setVisibility(View.VISIBLE);
                            recognizer.startListening();
                            displayMessage(Constant.CONVERSION_SPEAKING_MSG);
                        } else {
                            final AlertDialog.Builder infoDialog = new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Warning")
                                    .setMessage(Constant.LANG_MODEL_NOT_DOWNLOAD)
                                    .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            // do nothing
                                        }
                                    });
                            infoDialog.show();
                        }
                    } else {
                        mUtil.requestPermission(getApplicationContext());
                    }
                }
            }
        });

        dictatingButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dictatingButton.setVisibility(View.GONE);
                defaultButton.setVisibility(View.GONE);
                processingButton.setVisibility(View.VISIBLE);

                displayMessage(Constant.CONVERSION_IN_PROGRESS);
                recognizer.stop();
            }
        });

        processingButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dictatingButton.setVisibility(View.GONE);
                processingButton.setVisibility(View.GONE);
                defaultButton.setVisibility(View.VISIBLE);
                recognizer.stop();
                displayMessage(Constant.CONVERSION_START_MSG);
            }
        });

        if (mUtil.isPermissionGranted(getApplicationContext())) {
            File f = new File(getExternalFilesDir("") + "/sync");

            if (f.isDirectory() && !isSetupRecognizer) {
                setUpRecognizer();
            }
        } else {
            mUtil.requestPermission(getApplicationContext());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.download:

                final Context context = this;
                new LangModelHelper().buildLanguageModel(context,
                        MainActivity.this, getExternalFilesDir(""));

                return (true);

            case R.id.about:
                new DilaogUtil().showAboutDialog(this);
                return (true);

            case R.id.setting:
                Intent i = new Intent(this, SettingPreferencesActivity.class);
                startActivity(i);
                return (true);
        }
        return (super.onOptionsItemSelected(item));
    }

    public void setUpRecognizer() {
        Log.d("Recognizer", "Setting recognizer");

        //Use auto enpoint
        String autoendpointStr = PreferenceManager.getDefaultSharedPreferences(this).getString("autoendpoint_preference", null);
        if (autoendpointStr == null) {
            autoendpointStr = "Off";
        }

            /*
            System.out.println("Auto-endpoint:"+autoendpointStr);
            if(autoendpointStr.equals("On")){
                config.setBoolean("use_aep",true);
            }*/

        recognizer = mUtil.getRecognizer(this);
        if (recognizer.getDecoder() == null) {
            finish();
        }
        recognizer.addListener(this);
        isSetupRecognizer = true;

    }

    private View.OnClickListener onRecordClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //recognizer.startListening(SEARCH_IDX, TIMEOUT);
            //recognizer.startListening(SEARCH_IDX);
        }
    };

    private void hideMessage() {
        messageText.animate()
                .translationY(1000)
                .alpha(0.0f)
                .setListener(new AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        messageText.setVisibility(View.GONE);
                        messageText.setTranslationY(0);
                    }
                });
    }

    private void hideResultPanel() {

        resultPanel.animate()
                .translationY(1000)
                .alpha(0.0f)
                .setListener(new AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        resultPanel.setVisibility(View.GONE);
                        resultPanel.setTranslationY(0);
                    }
                });
    }

    private void displayMessage(final String message) {
        hideResultPanel();

        messageText.animate()
                .translationY(1000)
                .alpha(0.0f)
                .setListener(new AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        messageText.setVisibility(View.GONE);
                        messageText.setTranslationY(0);
                        messageText.setText(message);
                        messageText.animate()
                                .alpha(1.0f)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        messageText.setVisibility(View.VISIBLE);
                                    }
                                });
                    }
                });
    }

    private void displayResultPanel(final String message) {
        System.out.println("displayResultPanel:" + message);
        hideMessage();
        resultText.setText(message);
        resultPanel.animate()
                .translationY(1000)
                .alpha(0.0f)
                .setListener(new AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        resultPanel.setVisibility(View.GONE);
                        resultPanel.setTranslationY(0);

                        resultPanel.animate()
                                .alpha(1.0f)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        resultPanel.setVisibility(View.VISIBLE);
                                    }
                                });
                    }
                });
    }


    private View.OnClickListener onStopClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            recognizer.stop();
        }
    };

    private View.OnClickListener onClearClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //decodedStr = "";
            //txtresult.setText("");
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        recognizer.cancel();
        recognizer.shutdown();
        microgear.disconnect();
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onProgress(int i) {
    }

    @Override
    public void onEndOfSpeech() {
    }

    @Override
    public void onPartialResult(String s) {

        if (s != null) {
            if (!s.equals(SpeechRecognizer.NO_HYP) &&
                    !s.equals(SpeechRecognizer.REQUEST_NEXT)) {
                decodedStr = s + " ";
                System.out.println("onPartialResult:" + decodedStr);
                //txtresult.setText(decodedStr);

                /*
                if (config.getBoolean("use_aep")){
                    sendCMD(s);
                    displayResultPanel(decodedStr);
                }
                */
            }
        }
    }

    @Override
    public void onResult(String s) {
        if (s != null) {
            if (!s.equals(SpeechRecognizer.NO_HYP) &&
                    !s.equals(SpeechRecognizer.REQUEST_NEXT)) {

                sendCMD(s);
                decodedStr = s + " ";

                displayResultPanel(decodedStr);

                System.out.println("onResult:" + decodedStr);
                dictatingButton.setVisibility(View.GONE);
                processingButton.setVisibility(View.GONE);
                defaultButton.setVisibility(View.VISIBLE);

            }
        }
    }

    public void sendCMD(String s) {
        if (s.trim().equals("เดินหน้า") == true) {
            microgear.chat(thing, "Go");
        } else if (s.trim().equals("เลี้ยวซ้าย") == true) {
            microgear.chat(thing, "TurnLeft");
        } else if (s.trim().equals("เลี้ยวขวา") == true) {
            microgear.chat(thing, "TurnRight");
        } else if (s.trim().equals("ถอยหลัง") == true) {
            microgear.chat(thing, "Upturn");
        } else if (s.trim().equals("หยิบของ") == true) {
            microgear.chat(thing, "Catch");
        } else if (s.trim().equals("ยกของ") == true) {
            microgear.chat(thing, "Hold");
        } else if (s.trim().equals("วางของ") == true) {
            microgear.chat(thing, "Put");
        } else if (s.trim().equals("หยุดเดิน") == true) {
            microgear.chat(thing, "Stop");
        } else if (s.trim().equals("เพิ่มความเร็ว") == true) {
            microgear.chat(thing, "IncreaseSpeed");
        } else if (s.trim().equals("ลดความเร็ว") == true) {
            microgear.chat(thing, "DecreaseSpeed");
        }


    }

    @Override
    public void onError(Exception e) {
    }

    @Override
    public void onTimeout() {
    }

    @Override
    public void onReceiveDownloadCompleteStatus() {
        recognizer.stop();
        recognizer.shutdown();
        setUpRecognizer();
    }

    @Override
    public void onReceiveDownloadFailedStatus() {

    }

    class MicrogearCallBack implements MicrogearEventListener {
        @Override
        public void onConnect() {
            Message msg = handler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putString("myKey", "Now I'm connected with netpie");
            msg.setData(bundle);
            handler.sendMessage(msg);
            Log.i("Connected", "Now I'm connected with netpie");
        }

        @Override
        public void onMessage(String topic, String message) {
            Message msg = handler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putString("myKey", topic + " : " + message);
            msg.setData(bundle);
            handler.sendMessage(msg);
            Log.i("Message", topic + " : " + message);
        }

        @Override
        public void onPresent(String token) {
            Message msg = handler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putString("myKey", "New friend Connect :" + token);
            msg.setData(bundle);
            handler.sendMessage(msg);
            Log.i("present", "New friend Connect :" + token);
        }

        @Override
        public void onAbsent(String token) {
            Message msg = handler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putString("myKey", "Friend lost :" + token);
            msg.setData(bundle);
            handler.sendMessage(msg);
            Log.i("absent", "Friend lost :" + token);
        }

        @Override
        public void onDisconnect() {
            Message msg = handler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putString("myKey", "Disconnected");
            msg.setData(bundle);
            handler.sendMessage(msg);
            Log.i("disconnect", "Disconnected");
        }

        @Override
        public void onError(String error) {
            Message msg = handler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putString("myKey", "Exception : " + error);
            msg.setData(bundle);
            handler.sendMessage(msg);
            Log.i("exception", "Exception : " + error);
        }

        @Override
        public void onInfo(String info) {
            Message msg = handler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putString("myKey", "Exception : " + info);
            msg.setData(bundle);
            handler.sendMessage(msg);
            Log.i("info", "Info : " + info);
        }
    }
}
