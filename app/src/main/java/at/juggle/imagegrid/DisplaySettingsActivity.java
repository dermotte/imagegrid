package at.juggle.imagegrid;

import android.content.SharedPreferences;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import at.juggle.artistgrid.R;

public class DisplaySettingsActivity extends AppCompatActivity {
    int cols, rows, lineWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_settings);

        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        rows = settings.getInt("rows", 4);
        cols = settings.getInt("cols", 3);
        lineWidth = settings.getInt("lineWidth", 3);
        boolean colorpicker = settings.getBoolean("colorpicker", false);

        ((CheckBox) findViewById(R.id.editColorpicker)).setChecked(colorpicker);
        ((EditText) findViewById(R.id.editRows)).setText(rows + "");
        ((EditText) findViewById(R.id.editCols)).setText(cols+"");
        ((EditText) findViewById(R.id.editLineWidth)).setText(lineWidth + "");
        ((EditText) findViewById(R.id.editLineAlpha)).setText(settings.getInt("lineAlpha", 128) + "");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.save_settings);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
//                SharedPreferences.Editor editor = settings.edit();
//
//                editor.putInt("rows", Integer.parseInt(((EditText) findViewById(R.id.editRows)).getText().toString()));
//                editor.putInt("cols", Integer.parseInt(((EditText) findViewById(R.id.editCols)).getText().toString()));
//                editor.putInt("lineWidth", Integer.parseInt(((EditText) findViewById(R.id.editLineWidth)).getText().toString()));
//
//                // Commit the edits!
//                editor.commit();

                back();
            }
        });

    }

    @Override
    protected void onPause() {
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("rows", clamp(Integer.parseInt(((EditText) findViewById(R.id.editRows)).getText().toString()), 0, 50));
        editor.putInt("cols", clamp(Integer.parseInt(((EditText) findViewById(R.id.editCols)).getText().toString()), 0, 50));
        editor.putInt("lineWidth", clamp(Integer.parseInt(((EditText) findViewById(R.id.editLineWidth)).getText().toString()), 1, 16));
        int alpha = Integer.parseInt(((EditText) findViewById(R.id.editLineAlpha)).getText().toString());
        editor.putInt("lineAlpha", clamp(alpha, 0, 255));
        editor.putBoolean("colorpicker", ((CheckBox) findViewById(R.id.editColorpicker)).isChecked());
        editor.commit();
//        Log.i(this.getClass().getName(), "written to prefs. .... " + Integer.parseInt(((EditText) findViewById(R.id.editRows)).getText().toString()));
        super.onPause();
    }

    private int clamp(int value, int min, int max) {
        value = Math.max(min, value);
        value = Math.min(max, value);
        return value;
    }

    private void back() {
        NavUtils.navigateUpFromSameTask(this);
    }
}
