package com.example.jazz.control;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class Launch extends Activity {

    private DessinActivity mDrawView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        mDrawView = (DessinActivity)findViewById(R.id.activity_draw);
        // rend visible la vue
        mDrawView.setVisibility(View.VISIBLE);
    }
}
