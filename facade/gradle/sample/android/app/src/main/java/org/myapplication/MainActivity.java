package org.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onResume() {
        super.onResume();
        try {
            check(System.currentTimeMillis() > 1 ? null : 1);
            Toast.makeText(this, "No exception", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this,
                           String.format("Got a %s: %s", e.getClass().getName(), e.getMessage()),
                           Toast.LENGTH_LONG).show();
        }
    }

    private static void check(@NN Integer s) {
        System.out.println(s);
    }
}
