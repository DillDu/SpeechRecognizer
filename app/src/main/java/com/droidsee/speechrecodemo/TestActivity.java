package com.droidsee.speechrecodemo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import com.droidsee.speechrecodemo.R;
import com.droidsee.speechrecodemo.Utils.Utils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
//        String data = Utils.readFileToString("/Users/ddd/Downloads/s10e.wav");
//        File file = new File("/Users/ddd/Downloads/s10etest.wav");
//        try {
//            Utils.string2File(data, file);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        System.out.println("end");
    }
}
