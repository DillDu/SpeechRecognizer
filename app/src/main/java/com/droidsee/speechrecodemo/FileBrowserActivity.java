package com.droidsee.speechrecodemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Toast;

import com.droidsee.speechrecodemo.Utils.Utils;

import java.io.File;
import java.util.ArrayList;

public class FileBrowserActivity extends AppCompatActivity {

    private AbsListView filesView;
    private FileAdapter filesAdapter;
    private File currentFolder;
    private File defaultFolder;
    private static final String TAG = "MainActivity";

    private Animation fadeOut;
    private AnimationSet animationIn;

    private ActionMode actionMode;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_browser);
        setSupportActionBar(findViewById(R.id.toolbar));

        filesView = findViewById(R.id.files_view);



        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            }
        }

        setDefaultPath();
        currentFolder = new File(defaultFolder.getAbsolutePath());
        filesAdapter = new FileAdapter(currentFolder, currentFolder.getAbsolutePath()
                .equals(defaultFolder.getAbsolutePath()), this);

        setActionMode();

        setAnimations();
    }

    @Override
    protected void onResume(){
        super.onResume();
        setDefaultPath();
        refreshFiles();
    }

    @Override
    protected void onPause(){
        super.onPause();
        unsetActionMode();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if(grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED)
            Toast.makeText(this, "The storage won't be accesible..", Toast.LENGTH_SHORT).show();
    }



    // Overflow menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
//        getMenuInflater().inflate(R.menu.overflow_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
//        switch (item.getItemId()){
//            case R.id.action_settings:
//                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
//                Log.i(TAG,"Directing to Settings activity..");
//                startActivity(intent);
//                return true;
//            case R.id.action_refresh:
//                refreshFiles();
//                Toast.makeText(this, "Files refreshed..", Toast.LENGTH_SHORT).show();
//                return true;
//        }
        return super.onOptionsItemSelected(item);
    }



    // Activity initialization

    /**
     * Inits default path from shared preferences
     */
    private void setDefaultPath(){
        String internalStoragePath = getObbDir().getParentFile().getParentFile().getParentFile()
                .getAbsolutePath();

        defaultFolder = new File(PreferenceManager.getDefaultSharedPreferences(this).getString(
                getResources().getString(R.string.default_folder_key),internalStoragePath));
    }

    /**
     * Inits animations of files ListView/GridView during refreshing
     */
    private void setAnimations(){
        fadeOut = new AlphaAnimation(1,0);
        fadeOut.setDuration(50);
        fadeOut.setFillAfter(true);

        Animation scaleIn = new ScaleAnimation(0.95f, 1f, 0.95f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleIn.setStartOffset(50);
        scaleIn.setDuration(150);
        Animation fadeIn = new AlphaAnimation(0,1);
        fadeIn.setStartOffset(50);
        fadeIn.setDuration(100);
        fadeIn.setFillAfter(true);

        animationIn = new AnimationSet(true);
        animationIn.addAnimation(scaleIn);
        animationIn.addAnimation(fadeIn);
    }

    /**
     * Inits on item clicks from files ListView/GridView, inits Action mode
     */
    private void setActionMode(){
        filesView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(actionMode != null) {

                }
//                    listItemSelect(position);
                else{
                    File item = (File)filesAdapter.getItem(position);
                    if(item == null)
                        return;
                    if(item.isDirectory())
                        setCurrentFolder(new File(currentFolder.getAbsolutePath() + File.separator +
                                item.getName()));
                    else{
                        Uri uri = Uri.fromFile(item);
                        uri = Uri.parse(Utils.deCodeUrl(uri.toString()));
//                        Log.e("item_uri", uri.toString());
                        Intent intent = new Intent();
                        intent.setData(uri);
                        setResult(RESULT_OK,intent);
                        finish();
                    }
                }
            }
        });
//        filesView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//            @Override
//            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//                listItemSelect(position);
//                return true;
//            }
//        });
    }



    // Files list changing

    /**
     * Refreshes files ListView/GridView
     */
    void refreshFiles(){
        unsetActionMode();
        new Runnable() {
            @Override
            public void run() {
                filesView.startAnimation(fadeOut);

                filesAdapter = new FileAdapter(currentFolder, currentFolder.getAbsolutePath()
                        .equals(defaultFolder.getAbsolutePath()), FileBrowserActivity.this);
                filesView.setAdapter(filesAdapter);

                filesView.startAnimation(animationIn);
            }
        }.run();
        Log.i(TAG,"Files refreshed");
    }

    /**
     * Sets current folder to entered
     * @param folder folder to be current
     */
    void setCurrentFolder(File folder){
        this.currentFolder = folder;
        Log.i(TAG,"Current folder: " + folder.getAbsolutePath());
        refreshFiles();
    }

    /**
     * Sets deafult folder as current
     */
    void setDefaultFolder(){
        this.currentFolder = new File(defaultFolder.getAbsolutePath());
        Log.i(TAG,"Current folder: " + defaultFolder.getAbsolutePath());
        refreshFiles();
    }



    // Methods for Action mode (CAB)

//    /**
//     * Called when Action mode is on or is to be set on
//     * @param position clicked file position in list
//     */
//    private void listItemSelect(int position){
//        filesAdapter.itemSelect(position);
//        boolean hasCheckedItems = filesAdapter.getSelectedCount() > 0;
//        if(hasCheckedItems && actionMode == null) {
//        }
//
//        else if(!hasCheckedItems && actionMode != null)
//            actionMode.finish();
//
//        if(actionMode != null)
//            actionMode.setTitle(filesAdapter.getSelectedCount() + " selected items");
//    }

    /**
     * Disbales Action mode
     */
    void unsetActionMode(){
        filesAdapter.removeSelection();
        if(actionMode != null)
            actionMode = null;
    }

    /**
     * Returns Action mode object
     * @return action mode
     */
    ActionMode getActionMode(){
        return actionMode;
    }
}
