package com.droidsee.speechrecodemo;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.droidsee.loadingviews.AVLoadingIndicatorView;
import com.droidsee.speechrecodemo.Utils.ConstraintUtil;
import com.droidsee.speechrecodemo.Utils.FFmpegUtil;
import com.microsoft.cognitiveservices.speech.CancellationReason;
import com.microsoft.cognitiveservices.speech.PropertyId;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.translation.SpeechTranslationConfig;
import com.microsoft.cognitiveservices.speech.translation.TranslationRecognizer;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    //
    // Configuration for speech recognition
    //

    // Replace below with your own subscription key
    private static final String SpeechSubscriptionKey = "YourSpeechSubscriptionKey";
    // Replace below with your own service region (e.g., "westus").
    private static final String SpeechRegion = "westus";

    //
    // Configuration for intent recognition
    //

    // Replace below with your own Language Understanding subscription key
    // The intent recognition service calls the required key 'endpoint key'.
    private static final String LanguageUnderstandingSubscriptionKey = "YourLanguageUnderstandingSubscriptionKey";
    // Replace below with the deployment region of your Language Understanding application
    private static final String LanguageUnderstandingServiceRegion = "YourLanguageUnderstandingServiceRegion";
    // Replace below with the application ID of your Language Understanding application
    private static final String LanguageUnderstandingAppId = "YourLanguageUnderstandingAppId";

    private static String accessToken;
    private static long lastTokenTime = 0;

    private TextView tvRecognizedText;
    private TextView tvTranslatedText;
    private ImageButton ibtnOpenFile;
    private ImageButton ibtnTranslate;
    private ImageButton ibtnSwap;
    private Button btnTapToSpeak;
    private Spinner spinnerLanguage;
    private Spinner spinnerTranslate;
    private ScrollView svRecognized;
    private LinearLayout recognizedLayout;
    private AVLoadingIndicatorView avRecognizing;
    private ScrollView svTranslated;
    private LinearLayout translatedLayout;
    private AVLoadingIndicatorView avTranslating;
    private ConstraintLayout clRoot;

    private MicrophoneStream microphoneStream;
    private SpeechConfig speechConfig;
    private HashMap<String, String> languageMap = new HashMap<>();
    private HashMap<String, String> translateLanguageMap = new HashMap<>();
    private boolean enableTranslate = false;
//    private FFmpegHandler ffmpegHandler;

    private MicrophoneStream createMicrophoneStream() {
        if (microphoneStream != null) {
            microphoneStream.close();
            microphoneStream = null;
        }

        microphoneStream = new MicrophoneStream();
        return microphoneStream;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("statement", "onCreate");
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // create config
//        final SpeechConfig speechConfig;
        try {
            speechConfig = SpeechConfig.fromSubscription(SpeechSubscriptionKey, SpeechRegion);
//            Log.e("profanity", speechConfig.getProperty(String.valueOf(PropertyId.SpeechServiceResponse_RequestProfanityFilterTrueFalse)));
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            displayException(ex);
            return;
        }
//        ffmpegHandler = new FFmpegHandler(this);


        clRoot = findViewById(R.id.clRoot);
        tvRecognizedText = findViewById(R.id.tvRecognizedText);
        tvTranslatedText = findViewById(R.id.tvTranslatedText);
        ibtnOpenFile = findViewById(R.id.ibtnOpenFile);
        ibtnTranslate = findViewById(R.id.ibtnTranslate);
        ibtnSwap = findViewById(R.id.ibtnSwap);
        btnTapToSpeak = findViewById(R.id.btnTapToSpeak);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        spinnerTranslate = findViewById(R.id.spinnerTranslate);
        svRecognized = findViewById(R.id.svRecognized);
        avRecognizing = findViewById(R.id.avRecognizing);
        recognizedLayout = findViewById(R.id.recognizedLayout);
        svTranslated = findViewById(R.id.svTranslated);
        avTranslating = findViewById(R.id.avTranslating);
        translatedLayout = findViewById(R.id.translatedLayout);

        // Initialize SpeechSDK and request required permissions.
        try {
            // a unique number within the application to allow
            // correlating permission request responses with the request.
            int permissionRequestId = 5;

            // Request permissions needed for speech recognition
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, INTERNET, WRITE_EXTERNAL_STORAGE}, permissionRequestId);
        }catch(Exception ex) {
            Log.e("SpeechSDK", "could not init sdk, " + ex.toString());
            tvRecognizedText.setText("Could not initialize: " + ex.toString());
        }

        avRecognizing.hide();
        avTranslating.hide();
        setTranslatorEnabled(false);

        recognizedLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                svRecognized.post(() -> {
                    svRecognized.smoothScrollTo(0, bottom);
                });
            }
        });
        translatedLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                svTranslated.post(() -> {
                    svTranslated.smoothScrollTo(0, bottom);
                });
            }
        });

        String[] languages = getResources().getStringArray(R.array.recognize_languages);
        ArrayList<String> languageNameArray = new ArrayList<>();
        for(String s : languages)
        {
            String[] tLanguage = s.split("\\|");
            languageMap.put(tLanguage[0], tLanguage[1]);
            languageNameArray.add(tLanguage[0]);
        }

        String[] translateLanguages = getResources().getStringArray(R.array.translate_languages);
        for(String s : translateLanguages)
        {
            String[] tLanguage = s.split("\\|");
            translateLanguageMap.put(tLanguage[0], tLanguage[1]);
        }

        ArrayAdapter<String> arrayAdapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, languageNameArray);
        spinnerLanguage.setAdapter(arrayAdapter);
        spinnerTranslate.setAdapter(arrayAdapter);





        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                speechConfig.setSpeechRecognitionLanguage(languageMap.get(spinnerLanguage.getSelectedItem().toString()));
//                Log.e("spinner", spinnerLanguage.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //Continuous Recognition Async
        View.OnClickListener recognizeListener = new View.OnClickListener() {
            private static final String logTag = "reco";
            private boolean continuousListeningStarted = false;
            private SpeechRecognizer reco = null;
            private AudioConfig audioInput = null;
            private String buttonText = "";
            private ArrayList<String> content = new ArrayList<>();

            @Override
            public void onClick(View view) {
                final Button clickedButton = (Button) view;
//                disableButtons();
                setButtonEnabled(false);
                if (continuousListeningStarted) {
                    if (reco != null) {
                        final Future<Void> task = reco.stopContinuousRecognitionAsync();
                        setOnTaskCompletedListener(task, result -> {
                            Log.i(logTag, "Continuous recognition stopped by click.");
                            MainActivity.this.runOnUiThread(() -> {
                                clickedButton.setText(buttonText);
                            });
//                            enableButtons();
                            setButtonEnabled(true);
                            continuousListeningStarted = false;
                        });
                    } else {
                        continuousListeningStarted = false;
                    }

                    return;
                }

                clearTextBox();

                try {
                    content.clear();
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        audioInput = AudioConfig.fromDefaultMicrophoneInput();
                    }
                    else {
                        audioInput = AudioConfig.fromStreamInput(createMicrophoneStream());
                    }
                    reco = new SpeechRecognizer(speechConfig, audioInput);

                    reco.recognizing.addEventListener((o, speechRecognitionResultEventArgs) -> {
                        runOnUiThread(()-> {
                            if (avRecognizing != null && !avRecognizing.isShown()) {
                                avRecognizing.show();
                            }
                        });
                        final String s = speechRecognitionResultEventArgs.getResult().getText();
                        Log.i(logTag, "Intermediate result received: " + s);
                        content.add(s);
                        setRecognizedText(TextUtils.join(" ", content));
                        content.remove(content.size() - 1);
                    });

                    reco.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
                        runOnUiThread(() -> {
                            if (avRecognizing != null && avRecognizing.isShown()) {
                                avRecognizing.hide();
                            }
                        });
                        final String s = speechRecognitionResultEventArgs.getResult().getText();
                        Log.i(logTag, "Final result received: " + s);
                        content.add(s);
                        setRecognizedText(TextUtils.join(" ", content));
                    });

                    reco.sessionStarted.addEventListener((s, e) -> {
                        Log.i(logTag, "Session started event.");
                    });

                    reco.sessionStopped.addEventListener((s, e) -> {
                        runOnUiThread(() -> {
                            if (avRecognizing != null && avRecognizing.isShown()) {
                                avRecognizing.hide();
                            }
                        });
                        Log.i(logTag, "Session stopped event.");
                    });

                    final Future<Void> task = reco.startContinuousRecognitionAsync();
                    setOnTaskCompletedListener(task, result -> {
                        continuousListeningStarted = true;
                        MainActivity.this.runOnUiThread(() -> {
                            buttonText = clickedButton.getText().toString();
                            clickedButton.setText("Stop");
                            clickedButton.setEnabled(true);
                        });
                    });
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                    displayException(ex);
                }

            }
        };
        View.OnClickListener translateListener = new View.OnClickListener() {
            private static final String logTag = "reco";
            private boolean continuousListeningStarted = false;
            private TranslationRecognizer reco = null;
            private AudioConfig audioInput = null;
            private String buttonText = "";
            private ArrayList<String> content = new ArrayList<>();
            private ArrayList<String> translateContent = new ArrayList<>();

            @Override
            public void onClick(View view) {
                SpeechTranslationConfig speechTranslationConfig = SpeechTranslationConfig.fromSubscription(SpeechSubscriptionKey, SpeechRegion);
                speechTranslationConfig.setSpeechRecognitionLanguage(languageMap.get(spinnerLanguage.getSelectedItem().toString()));
                speechTranslationConfig.addTargetLanguage(translateLanguageMap.get(spinnerTranslate.getSelectedItem().toString()));
                final Button clickedButton = (Button) view;
                setButtonEnabled(false);
                if (continuousListeningStarted) {
                    if (reco != null) {
                        final Future<Void> task = reco.stopContinuousRecognitionAsync();
                        setOnTaskCompletedListener(task, result -> {
                            Log.i(logTag, "Continuous recognition stopped.");
                            MainActivity.this.runOnUiThread(() -> {
                                clickedButton.setText(buttonText);
                            });
                            setButtonEnabled(true);
                            continuousListeningStarted = false;
                        });
                    } else {
                        continuousListeningStarted = false;
                    }

                    return;
                }

                clearTextBox();

                try {
                    content.clear();
                    translateContent.clear();
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        audioInput = AudioConfig.fromDefaultMicrophoneInput();
                    }
                    else {
                        audioInput = AudioConfig.fromStreamInput(createMicrophoneStream());
                    }
                    reco = new TranslationRecognizer(speechTranslationConfig, audioInput);

                    reco.recognizing.addEventListener((o, translationRecognitionEventArgs) -> {
                        runOnUiThread(()-> {
                            if (avRecognizing != null && !avRecognizing.isShown()) {
                                avRecognizing.show();
                            }
                            if (avTranslating != null && !avTranslating.isShown()) {
                                avTranslating.show();
                            }
                        });
                        //recognize
                        final String sReco = translationRecognitionEventArgs.getResult().getText();
                        Log.i(logTag, "Intermediate result received: " + sReco);
                        content.add(sReco);
//                        setRecognizedText(TextUtils.join(" ", content));
                        //translate
                        Map<String, String> map = translationRecognitionEventArgs.getResult().getTranslations();
                        for(String element : map.keySet()) {
                            Log.i(logTag, "    TRANSLATING into '" + element + "': " + map.get(element));
                        }
                        final String sTrans = map.get(translateLanguageMap.get(spinnerTranslate.getSelectedItem().toString()));
                        translateContent.add(sTrans);


                        setRecoAndTransText(TextUtils.join(" ", content), TextUtils.join(" ", translateContent));
                        content.remove(content.size() - 1);
                        translateContent.remove(translateContent.size() - 1);
                    });

                    reco.recognized.addEventListener((o, speechTranslationResultEventArgs) -> {
                        if (speechTranslationResultEventArgs.getResult().getReason() == ResultReason.TranslatedSpeech) {
                            runOnUiThread(() -> {
                                if (avRecognizing != null && avRecognizing.isShown()) {
                                    avRecognizing.hide();
                                }
                                if (avTranslating != null && avTranslating.isShown()) {
                                    avTranslating.hide();
                                }
                            });

//                            System.out.println("RECOGNIZED in '" + fromLanguage + "': Text=" + e.getResult().getText());

                            Map<String, String> map = speechTranslationResultEventArgs.getResult().getTranslations();
//                            for(String element : map.keySet()) {
//                                System.out.println("    TRANSLATED into '" + element + "': " + map.get(element));
//                            }

                            final String sReco = speechTranslationResultEventArgs.getResult().getText();
                            Log.i(logTag, "Final result received: " + sReco);
                            content.add(sReco);
//                            setRecognizedText(TextUtils.join(" ", content));

                            final String sTrans = map.get(translateLanguageMap.get(spinnerTranslate.getSelectedItem().toString()));
                            Log.i(logTag, "Final translation received: " + sTrans);
                            translateContent.add(sTrans);
                            setRecoAndTransText(TextUtils.join(" ", content), TextUtils.join(" ", translateContent));
                        }
                        if (speechTranslationResultEventArgs.getResult().getReason() == ResultReason.RecognizedSpeech) {
                            runOnUiThread(() -> {
                                if (avRecognizing != null && avRecognizing.isShown()) {
                                    avRecognizing.hide();
                                }
                            });
//                            System.out.println("RECOGNIZED: Text=" + e.getResult().getText());
//                            System.out.println("    Speech not translated.");

                            final String s = speechTranslationResultEventArgs.getResult().getText();
                            Log.i(logTag, "Final recognition received: " + s);
                            content.add(s);
                            setRecognizedText(TextUtils.join(" ", content));
                        }
                        else if (speechTranslationResultEventArgs.getResult().getReason() == ResultReason.NoMatch) {
                            Log.i(logTag,"NOMATCH: Speech could not be recognized.");
                        }

                    });

                    reco.canceled.addEventListener((s, e) -> {
                        Log.i(logTag,"CANCELED: Reason=" + e.getReason());

                        if (e.getReason() == CancellationReason.Error) {
                            System.out.println("CANCELED: ErrorCode=" + e.getErrorCode());
                            System.out.println("CANCELED: ErrorDetails=" + e.getErrorDetails());
                            System.out.println("CANCELED: Did you update the subscription info?");
                        }
                    });

                    reco.sessionStarted.addEventListener((s, e) -> {
                        System.out.println("\nSession started event.");
                    });

                    reco.sessionStopped.addEventListener((s, e) -> {
                        runOnUiThread(() -> {
                            if (avRecognizing != null && avRecognizing.isShown()) {
                                avRecognizing.hide();
                            }
                            if (avTranslating != null && avTranslating.isShown()) {
                                avTranslating.hide();
                            }
                        });
                        System.out.println("\nSession stopped event.");
                    });

                    final Future<Void> task = reco.startContinuousRecognitionAsync();
                    setOnTaskCompletedListener(task, result -> {
                        continuousListeningStarted = true;
                        MainActivity.this.runOnUiThread(() -> {
                            buttonText = clickedButton.getText().toString();
                            clickedButton.setText("Stop");
                            clickedButton.setEnabled(true);
                        });
                    });
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                    displayException(ex);
                }

            }
        };

        btnTapToSpeak.setOnClickListener(recognizeListener);

        ibtnSwap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pos = spinnerLanguage.getSelectedItemPosition();
                spinnerLanguage.setSelection(spinnerTranslate.getSelectedItemPosition());
                spinnerTranslate.setSelection(pos);
            }
        });
        ibtnOpenFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFileBrowser();
            }
        });
        ibtnTranslate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(enableTranslate) {
                    btnTapToSpeak.setOnClickListener(recognizeListener);
                    ibtnTranslate.getBackground().setTint(getResources().getColor(R.color.gray_button));
                    setTranslatorEnabled(false);
                }
                else {
                    btnTapToSpeak.setOnClickListener(translateListener);
                    ibtnTranslate.getBackground().setTint(getResources().getColor(R.color.blue_button_activate));
                    setTranslatorEnabled(true);
                }
                enableTranslate = !enableTranslate;
            }
        });

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.e("statement", "onSaveInstanceState");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e("statement", "onStart");
    }

    @Override
    protected void onPause(){
        super.onPause();
        Log.e("statement", "onPause");
    }
    @Override
    protected void onResume(){
        super.onResume();
        Log.e("statement", "onResume");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e("statement", "onStop");


    }

    long exitTime = 0;
    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        if(System.currentTimeMillis() - exitTime < 2000) {
            finish();
            System.exit(0);
        }
        else {
            exitTime = System.currentTimeMillis();
            Toast.makeText(this, "Press again to exit.", Toast.LENGTH_SHORT).show();
        }
    }

    private final int REQUEST_OPENFILE = 0;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_OPENFILE) {
            if(resultCode == RESULT_OK) {
                try {
                    String path = data.getData().toString().replaceFirst("file://", "");
                    File outPath = new File(Environment.getExternalStorageDirectory().getPath() + File.separator +
                            "droidsee");
                    if(!outPath.exists()) {
                        outPath.mkdir();
                    }
                    String tempPath = outPath.getPath() + File.separator + "temp.wav";
//                    Log.e("path", tempPath);
                    FFmpegUtil.transformToWav(this, path, tempPath, ffmpegHandler);
//                    recognizeByFile(path);
//                    recognizeByApi(path);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @SuppressLint("HandlerLeak")
    Handler ffmpegHandler = new Handler() {
        String tag = "ffmpeg";
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case FFmpegUtil.FFMPEG_SUCCESS:
                    recognizeByFile(msg.obj.toString());
                    Log.e(tag, "success" + msg.obj.toString());
                    break;
                case FFmpegUtil.FFMPEG_FAILURE:
                    Log.e(tag, msg.obj == null ? "init failed" : "failed  : " + msg.obj.toString());
                    break;
                case FFmpegUtil.FFMPEG_START:
                    Log.e(tag, "start");
                    break;
                case FFmpegUtil.FFMPEG_FINISH:
                    Log.e(tag, "finish");
                    break;
                case FFmpegUtil.FFMPEG_PROGRESS:
                    Log.e(tag, "progress : " + msg.obj.toString());
                    break;
            }
        }
    };

    private void displayException(Exception ex) {
        tvRecognizedText.setText(ex.getMessage() + System.lineSeparator() + TextUtils.join(System.lineSeparator(), ex.getStackTrace()));
    }

    private void clearTextBox() {
        AppendTextLine("", "", true);
    }

    private void setRecognizedText(final String s) {
        AppendTextLine(s, "", true);
    }

    private void setRecoAndTransText(final String strReco, final String strTrans) {
        AppendTextLine(strReco, strTrans, true);
    }

    private void AppendTextLine(final String strReco, final String strTrans, final Boolean erase) {
        MainActivity.this.runOnUiThread(() -> {
            if (erase) {
                tvRecognizedText.setText(strReco);
                tvTranslatedText.setText(strTrans);
            } else {
                tvRecognizedText.setText(tvRecognizedText.getText() + System.lineSeparator() + strReco);
                tvTranslatedText.setText(tvTranslatedText.getText() + System.lineSeparator() + strTrans);
            }
        });
    }

    private <T> void setOnTaskCompletedListener(Future<T> task, OnTaskCompletedListener<T> listener) {
        s_executorService.submit(() -> {
            T result = task.get();
            listener.onCompleted(result);
            return null;
        });
    }

    private interface OnTaskCompletedListener<T> {
        void onCompleted(T taskResult);
    }

    private static ExecutorService s_executorService;
    static {
        s_executorService = Executors.newCachedThreadPool();
    }

    private void setButtonEnabled(boolean enabled) {
        MainActivity.this.runOnUiThread(() -> {
            btnTapToSpeak.setEnabled(enabled);
            ibtnOpenFile.setEnabled(enabled);
            ibtnTranslate.setEnabled(enabled);
            ibtnSwap.setEnabled(enabled);
            spinnerLanguage.setEnabled(enabled);
            spinnerTranslate.setEnabled(enabled);
        });
    }

    private void setTranslatorEnabled(boolean enabled) {
        MainActivity.this.runOnUiThread(() -> {
            ConstraintUtil constraintUtil = new ConstraintUtil(clRoot);
            ConstraintUtil.ConstraintBegin begin = constraintUtil.beginWithAnim();
            if(enabled) {
                begin.Top_toBottomOf(R.id.rlLanguageButtonLayout, R.id.include);
                ibtnSwap.setVisibility(View.VISIBLE);
                spinnerTranslate.setVisibility(View.VISIBLE);
                begin.Top_toBottomOf(R.id.svTranslated, R.id.rlLanguageButtonLayout);
            }
            else {
                begin.clear(R.id.rlLanguageButtonLayout, ConstraintSet.TOP);
                ibtnSwap.setVisibility(View.GONE);
                spinnerTranslate.setVisibility(View.GONE);
                begin.clear(R.id.svTranslated, ConstraintSet.TOP);
            }
            begin.commit();
        });
    }

    private void showFileBrowser() {
        Intent intent = new Intent(MainActivity.this, FileBrowserActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivityForResult(intent, REQUEST_OPENFILE);
    }

    private void recognizeByFile(String filePath)
    {
        String logtag = "recofile";
//        AtomicLong endTime = new AtomicLong(0);
//        AtomicReference<AsyncTask> taskEnd = new AtomicReference<>();
        Dialog loadingDialog = new Dialog(this);
        loadingDialog.setCanceledOnTouchOutside(false);
        loadingDialog.setContentView(R.layout.dialog_loading);
//        loadingDialog.setTitle(R.string.recognizing);
//        loadingDialog.setCancelable(true);

        AtomicBoolean continuousListeningStarted = new AtomicBoolean(false);
        if(!enableTranslate) {
            AudioConfig audioInput = AudioConfig.fromWavFileInput(filePath);
            SpeechRecognizer reco = new SpeechRecognizer(speechConfig, audioInput);
            ArrayList<String> content = new ArrayList<>();
            setButtonEnabled(false);

            loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    if (continuousListeningStarted.get()) {
                        if (reco != null) {
                            final Future<Void> task = reco.stopContinuousRecognitionAsync();
                            setOnTaskCompletedListener(task, result -> {
//                                if(taskEnd.get() != null && !taskEnd.get().isCancelled()) {
//                                    taskEnd.get().cancel(true);
//                                }
                                Log.i(logtag, "Continuous recognition stopped by cancel.");
                                setButtonEnabled(true);
                                continuousListeningStarted.set(false);
                            });
                        } else {
                            continuousListeningStarted.set(false);
                        }
                        return;
                    }
                }
            });
            loadingDialog.show();
            clearTextBox();

            try {
                content.clear();
//                audioInput = AudioConfig.fromWavFileInput(filePath);
//                reco = new SpeechRecognizer(speechConfig, audioInput);

                reco.recognizing.addEventListener((o, speechRecognitionResultEventArgs) -> {
                    runOnUiThread(() -> {
                        if (avRecognizing != null && !avRecognizing.isShown()) {
                            avRecognizing.show();
                        }
                    });
                    final String s = speechRecognitionResultEventArgs.getResult().getText();
                    Log.i(logtag, "Intermediate result received: " + s);
                    content.add(s);
                    setRecognizedText(TextUtils.join(" ", content));
                    content.remove(content.size() - 1);
//                    endTime.set(System.currentTimeMillis());
                });

                reco.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
                    runOnUiThread(() -> {
                        if (avRecognizing != null && avRecognizing.isShown()) {
                            avRecognizing.hide();
                        }
                    });
                    final String s = speechRecognitionResultEventArgs.getResult().getText();
                    Log.i(logtag, "Final result received: " + s);
                    content.add(s);
                    setRecognizedText(TextUtils.join(" ", content));
//                    endTime.set(System.currentTimeMillis());
                });

                reco.sessionStarted.addEventListener((s, e) -> {
                    Log.i(logtag, "Session sessionStarted event.");
//                    taskEnd.set(new AsyncTask() {
//                        @Override
//                        protected Object doInBackground(Object[] objects) {
//                            while(true) {
//                                if(endTime.get() != 0 && System.currentTimeMillis() - endTime.get() >= 5000) {
//                                    loadingDialog.cancel();
//                                    break;
//                                }
//                            }
//                            return null;
//                        }
//                    });
//                    taskEnd.get().execute();
                });
                reco.sessionStopped.addEventListener((s, e) -> {
                    runOnUiThread(() -> {
                        if (avRecognizing != null && avRecognizing.isShown()) {
                            avRecognizing.hide();
                        }
                    });
                    if(loadingDialog.isShowing()) {
                        loadingDialog.cancel();
                    }
                    delTempFile(filePath);
                    Log.i(logtag, "Session stopped event.");
                });
                reco.canceled.addEventListener((s, e) -> {
                    runOnUiThread(() -> {
                                if (avRecognizing != null && avRecognizing.isShown()) {
                                    avRecognizing.hide();
                                }
                            });
                    setButtonEnabled(true);
                    delTempFile(filePath);
                    Log.i(logtag, "Session canceled event.");
                });
                Future<Void> taskStart = reco.startContinuousRecognitionAsync();
                setOnTaskCompletedListener(taskStart, result -> {
                    continuousListeningStarted.set(true);
                });
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                displayException(ex);
            }
        }
        //translate-------------------------------------------
        else {

            AudioConfig audioInput = AudioConfig.fromWavFileInput(filePath);
            ArrayList<String> content = new ArrayList<>();
            ArrayList<String> translateContent = new ArrayList<>();
            setButtonEnabled(false);
            SpeechTranslationConfig speechTranslationConfig = SpeechTranslationConfig.fromSubscription(SpeechSubscriptionKey, SpeechRegion);
            speechTranslationConfig.setSpeechRecognitionLanguage(languageMap.get(spinnerLanguage.getSelectedItem().toString()));
            speechTranslationConfig.addTargetLanguage(translateLanguageMap.get(spinnerTranslate.getSelectedItem().toString()));
            TranslationRecognizer reco = new TranslationRecognizer(speechTranslationConfig, audioInput);

            loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    if (continuousListeningStarted.get()) {
                        if (reco != null) {
                            final Future<Void> task = reco.stopContinuousRecognitionAsync();
                            setOnTaskCompletedListener(task, result -> {
                                Log.i(logtag, "Continuous transition stopped by cancel.");
                                setButtonEnabled(true);
                                continuousListeningStarted.set(false);
                            });
                        } else {
                            continuousListeningStarted.set(false);
                        }

                        return;
                    }
                }
            });
            loadingDialog.show();
            clearTextBox();

            try {
                content.clear();
                translateContent.clear();
//                audioInput = AudioConfig.fromWavFileInput(filePath);
//
//                reco = new TranslationRecognizer(speechTranslationConfig, audioInput);

                reco.recognizing.addEventListener((o, translationRecognitionEventArgs) -> {
                    runOnUiThread(()-> {
                        if (avRecognizing != null && !avRecognizing.isShown()) {
                            avRecognizing.show();
                        }
                        if (avTranslating != null && !avTranslating.isShown()) {
                            avTranslating.show();
                        }
                    });
                    //recognize
                    final String sReco = translationRecognitionEventArgs.getResult().getText();
                    content.add(sReco);
//                        setRecognizedText(TextUtils.join(" ", content));
                    //translate
                    Map<String, String> map = translationRecognitionEventArgs.getResult().getTranslations();
                    final String sTrans = map.get(translateLanguageMap.get(spinnerTranslate.getSelectedItem().toString()));
                    translateContent.add(sTrans);


                    setRecoAndTransText(TextUtils.join(" ", content), TextUtils.join(" ", translateContent));
                    content.remove(content.size() - 1);
                    translateContent.remove(translateContent.size() - 1);
//                    endTime.set(System.currentTimeMillis());
                });

                reco.recognized.addEventListener((o, speechTranslationResultEventArgs) -> {
                    if (speechTranslationResultEventArgs.getResult().getReason() == ResultReason.TranslatedSpeech) {
                        runOnUiThread(() -> {
                            if (avRecognizing != null && avRecognizing.isShown()) {
                                avRecognizing.hide();
                            }
                            if (avTranslating != null && avTranslating.isShown()) {
                                avTranslating.hide();
                            }
                        });


//                            System.out.println("RECOGNIZED in '" + fromLanguage + "': Text=" + e.getResult().getText());

                        Map<String, String> map = speechTranslationResultEventArgs.getResult().getTranslations();
//                            for(String element : map.keySet()) {
//                                System.out.println("    TRANSLATED into '" + element + "': " + map.get(element));
//                            }

                        final String sReco = speechTranslationResultEventArgs.getResult().getText();
                        content.add(sReco);
//                            setRecognizedText(TextUtils.join(" ", content));

                        final String sTrans = map.get(translateLanguageMap.get(spinnerTranslate.getSelectedItem().toString()));
                        translateContent.add(sTrans);
                        setRecoAndTransText(TextUtils.join(" ", content), TextUtils.join(" ", translateContent));
//                        endTime.set(System.currentTimeMillis());
                        Log.i(logtag, "Translating...");
                    }
                    if (speechTranslationResultEventArgs.getResult().getReason() == ResultReason.RecognizedSpeech) {
                        runOnUiThread(() -> {
                            if (avRecognizing != null && avRecognizing.isShown()) {
                                avRecognizing.hide();
                            }
                        });
//                            System.out.println("RECOGNIZED: Text=" + e.getResult().getText());
//                            System.out.println("    Speech not translated.");

                        final String s = speechTranslationResultEventArgs.getResult().getText();
                        content.add(s);
                        setRecognizedText(TextUtils.join(" ", content));
                    }
                    else if (speechTranslationResultEventArgs.getResult().getReason() == ResultReason.NoMatch) {
                    }
                });

                reco.canceled.addEventListener((s, e) -> {
                    setButtonEnabled(true);
                    Log.i(logtag, "CANCELED: Reason=" + e.getReason());
                    if (e.getReason() == CancellationReason.Error) {
                        System.out.println("CANCELED: ErrorCode=" + e.getErrorCode());
                        System.out.println("CANCELED: ErrorDetails=" + e.getErrorDetails());
                        System.out.println("CANCELED: Did you update the subscription info?");
                    }
                    delTempFile(filePath);
                });

                reco.sessionStarted.addEventListener((s, e) -> {
                    Log.i(logtag, "Session started event.");
                });

                reco.sessionStopped.addEventListener((s, e) -> {
                    runOnUiThread(() -> {
                        if (avRecognizing != null && avRecognizing.isShown()) {
                            avRecognizing.hide();
                        }
                        if (avTranslating != null && avTranslating.isShown()) {
                            avTranslating.hide();
                        }
                    });
                    setButtonEnabled(true);
                    if(loadingDialog.isShowing()) {
                        loadingDialog.cancel();
                    }
                    delTempFile(filePath);
                    Log.i(logtag, "Session stopped event.");
                });
                Future<Void> taskStart = reco.startContinuousRecognitionAsync();
                setOnTaskCompletedListener(taskStart, result -> {
                    continuousListeningStarted.set(true);
                });
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                displayException(ex);
            }
        }
    }

    private void delTempFile(String filePath)
    {
        File file = new File(filePath);
        if(file.exists()) {
            file.delete();
        }
    }
//        static String GET_TOKEN_URL = "https://[region].api.cognitive.microsoft.com/sts/v1.0/issuetoken";
//    static String POST_URL = "https://[region].stt.speech.microsoft.com/speech/recognition/conversation/cognitiveservices/v1?language=[language]&profanity=raw";
////  https://westus.stt.speech.microsoft.com/speech/recognition/conversation/cognitiveservices/v1?language=en-US&profanity=raw
//    private void recognizeByApi(String filePath) {
//        AsyncTask task = new AsyncTask() {
//            @Override
//            protected Object doInBackground(Object[] objects) {
//                //get token
//                if(accessToken == null || (System.currentTimeMillis() - lastTokenTime) >= 540000) {
//                    String getTokenUrl = GET_TOKEN_URL.replace("[region]", SpeechRegion);
//                    HashMap<String, String> getTokenParam = new HashMap<>();
//                    getTokenParam.put("Ocp-Apim-Subscription-Key", SpeechSubscriptionKey);
//                    String getTokenResponse = HttpPost.getInstance().postData(getTokenUrl, "", getTokenParam);
//                    accessToken = getTokenResponse;
//                    lastTokenTime = System.currentTimeMillis();
//                    Log.e("get_token", getTokenResponse);
//                }
//
//                //request recognize
//                String url = POST_URL.replace("[region]", SpeechRegion).replace("[language]", speechConfig.getSpeechRecognitionLanguage());
//                HashMap<String, String> param = new HashMap<>();
//                param.put("Ocp-Apim-Subscription-Key", SpeechSubscriptionKey);
//                param.put("Authorization", "Bearer " + accessToken);
//                param.put("Content-type", "audio/wav; codecs=audio/pcm; samplerate=16000");
//                param.put("Transfer-Encoding", "chunked");
//                param.put("Expect","100-continue");
//                param.put("Accept", "application/json");
//                String response = null;
//                response = HttpPost.getInstance().postData(url, new File(filePath), param);
//                Log.e("api_recognize", response);
//                String finalResponse = response;
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        tvRecognizedText.setText(finalResponse);
//                    }
//                });
//                return null;
//            }
//        };
//        task.execute();
//    }

//    static class FFmpegHandler extends Handler {
//
//        private WeakReference<MainActivity> mOuter;
//
//        public FFmpegHandler (MainActivity mainActivity) {
//            mOuter = new WeakReference<>(mainActivity);
//        }
//
//        String tag = "ffmpeg";
//        @Override
//        public void handleMessage(Message msg) {
//            super.handleMessage(msg);
//            MainActivity outer = mOuter.get();
//            if(outer != null) {
//                switch (msg.what) {
//                    case FFmpegUtil.FFMPEG_SUCCESS:
//                        Log.e(tag, "success" + msg.obj.toString());
//                        break;
//                    case FFmpegUtil.FFMPEG_FAILURE:
//                        Log.e(tag, "init fail" + msg.obj.toString());
//                        break;
//                    case FFmpegUtil.FFMPEG_START:
//                        Log.e(tag, "start");
//                        break;
//                    case FFmpegUtil.FFMPEG_FINISH:
//                        Log.e(tag, "finish");
//                        break;
//                    case FFmpegUtil.FFMPEG_PROGRESS:
//                        Log.e(tag, "progress : " + msg.obj.toString());
//                        break;
//                }
//            }
//        }
//    }
}
