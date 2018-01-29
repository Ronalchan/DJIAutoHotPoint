package com.dji.test;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.view.Window;

public class DefaultLayoutActivity extends AppCompatActivity implements View.OnClickListener{

    private Button mMediaManagerBtn;
    private Button mHotPointBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_default_layout);

        mMediaManagerBtn = (Button)findViewById(R.id.btn_mediaManager);
        mMediaManagerBtn.setOnClickListener(this);

        mHotPointBtn = (Button)findViewById(R.id.btn_hotPointMission);
        mHotPointBtn.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_mediaManager: {
                Intent intent = new Intent(this, MediaActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.btn_hotPointMission:{
                Intent intent = new Intent(this,MainActivity.class);
                startActivity(intent);
                this.finish();
                break;
            }
            default:
                break;
        }
    }
}