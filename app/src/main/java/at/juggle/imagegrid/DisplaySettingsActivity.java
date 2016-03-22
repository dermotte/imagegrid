package at.juggle.imagegrid;

import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;


import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;
import com.joanzapata.iconify.fonts.FontAwesomeModule;

import at.juggle.artistgrid.R;

public class DisplaySettingsActivity extends AppCompatActivity {
    int cols, rows, lineWidth, lineColor;
    boolean squareGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Iconify.with(new FontAwesomeModule());
        setContentView(R.layout.activity_display_settings);

        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        rows = settings.getInt("rows", 4);
        cols = settings.getInt("cols", 3);
        lineWidth = settings.getInt("lineWidth", 3);
        lineColor = settings.getInt("lineColor", 0);
        squareGrid = settings.getBoolean("squareGrid", false);
        boolean colorpicker = settings.getBoolean("colorpicker", false);
        boolean savefileonexit = settings.getBoolean("savefileonexit", true);

        ((CheckBox) findViewById(R.id.editColorpicker)).setChecked(colorpicker);
        // ((CheckBox) findViewById(R.id.editSavefileonexit)).setChecked(savefileonexit);
        ((CheckBox) findViewById(R.id.checkSquareGrid)).setChecked(squareGrid);
        ((EditText) findViewById(R.id.editRows)).setText(rows + "");
        ((EditText) findViewById(R.id.editCols)).setText(cols + "");
        if (squareGrid) ((EditText) findViewById(R.id.editCols)).setEnabled(false);
        ((EditText) findViewById(R.id.editLineWidth)).setText(lineWidth + "");
        ((SeekBar) findViewById(R.id.editLineAlpha)).setProgress(settings.getInt("lineAlpha", 128));

        Spinner spinner = (Spinner) findViewById(R.id.color_spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.colors_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setSelection(lineColor);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.save_settings);
        fab.setImageDrawable(new IconDrawable(this, FontAwesomeIcons.fa_save).colorRes(R.color.colorPrimaryDark).actionBarSize());

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

        CheckBox squareGrid = (CheckBox) findViewById(R.id.checkSquareGrid);
        squareGrid.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                EditText cols = (EditText) findViewById(R.id.editCols);
                if (isChecked) {
                    cols.setEnabled(false);
                } else cols.setEnabled(true);
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
        int alpha = ((SeekBar) findViewById(R.id.editLineAlpha)).getProgress();
        editor.putInt("lineAlpha", clamp(alpha, 0, 255));
        editor.putInt("lineColor", ((Spinner) findViewById(R.id.color_spinner)).getSelectedItemPosition());
        editor.putBoolean("colorpicker", ((CheckBox) findViewById(R.id.editColorpicker)).isChecked());
        // editor.putBoolean("savefileonexit", ((CheckBox) findViewById(R.id.editSavefileonexit)).isChecked());
        editor.putBoolean("squareGrid", ((CheckBox) findViewById(R.id.checkSquareGrid)).isChecked());
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
