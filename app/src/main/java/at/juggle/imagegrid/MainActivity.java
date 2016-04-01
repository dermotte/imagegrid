package at.juggle.imagegrid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.jabistudio.androidjhlabs.filter.DoGFilter;
import com.jabistudio.androidjhlabs.filter.EdgeFilter;
import com.jabistudio.androidjhlabs.filter.GrayscaleFilter;
import com.jabistudio.androidjhlabs.filter.InvertFilter;
import com.jabistudio.androidjhlabs.filter.PosterizeFilter;
import com.jabistudio.androidjhlabs.filter.QuantizeFilter;
import com.jabistudio.androidjhlabs.filter.util.AndroidUtils;
import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;
import com.joanzapata.iconify.fonts.FontAwesomeModule;
import com.ortiz.touchview.TouchImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import at.juggle.artistgrid.R;

public class MainActivity extends AppCompatActivity {
    private static int RESULT_LOAD_IMAGE = 1;
    private static int RESULT_SETTINGS_ACTIVITY = 2;
    public final static String PREFS_NAME = "PrefsFileArtistGrid";
    public final static String IMG_CACHED = "image.png";
    int rows, cols, lineWidth, alpha;
    private int lineColor;
    boolean colorpicker, squareGrid;
    Bitmap buffer = null, original = null;
    private float maxImageSide = 1200;

    private ArrayList<int[]> gridSize = new ArrayList<int[]>();
    private int currentGridSizeIndex = 0;
    int[] lineColors = new int[] {Color.BLACK, Color.WHITE, Color.RED, Color.GREEN, Color.BLUE, Color.CYAN, Color.MAGENTA, Color.YELLOW};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Iconify.with(new FontAwesomeModule());
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);

        gridSize.add(new int[]{3, 4});
        gridSize.add(new int[]{4, 5});
        gridSize.add(new int[]{5, 6});
        gridSize.add(new int[]{6, 8});
        gridSize.add(new int[]{0, 0});

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setImageDrawable(new IconDrawable(this, FontAwesomeIcons.fa_folder_open).colorRes(R.color.colorPrimaryDark).actionBarSize());
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

//                int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(),
//                        Manifest.permission.READ_EXTERNAL_STORAGE);
//
//                if (permissionCheck == PackageManager.PERMISSION_GRANTED)
                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });

        final FloatingActionButton fabfull = (FloatingActionButton) findViewById(R.id.leaveFullscreenButton);
        fabfull.setImageDrawable(new IconDrawable(this, FontAwesomeIcons.fa_chevron_down).colorRes(R.color.colorPrimaryDark).actionBarSize());
        fabfull.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fabfull.setVisibility(View.INVISIBLE);
                getSupportActionBar().show();
            }
        });

        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        rows = settings.getInt("rows", 4);
        cols = settings.getInt("cols", 3);
        lineWidth = settings.getInt("lineWidth", 2);
        alpha = settings.getInt("lineAlpha", 128);
        lineColor = settings.getInt("lineColor", 0);
        colorpicker = settings.getBoolean("colorpicker", false);
        squareGrid = settings.getBoolean("squareGrid", false);
        TouchImageView view = (TouchImageView) findViewById(R.id.mainImageView);
        view.setMaxZoom(5f);
        view.setOnTouchListener(new ColorPickerOnTouchListener(view, this));

        // Get the intent that started this activity
        Intent intent = getIntent();
        // Figure out what to do based on the intent type
        if (intent != null && intent.getType()!=null && intent.getType().indexOf("image/") != -1) {
            Uri data = intent.getData();
            if (data == null) data = intent.getClipData().getItemAt(0).getUri();
            if (data !=null ) openImage(data);
            else {
                Toast.makeText(getApplicationContext(), "Error: Could not open image!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.action_share).setIcon(new IconDrawable(this, FontAwesomeIcons.fa_share).colorRes(R.color.colorWhite).actionBarSize());
        menu.findItem(R.id.action_save).setIcon(new IconDrawable(this, FontAwesomeIcons.fa_save).colorRes(R.color.colorWhite).actionBarSize());
        menu.findItem(R.id.action_fullscreen).setIcon(new IconDrawable(this, FontAwesomeIcons.fa_arrows_alt).colorRes(R.color.colorWhite).actionBarSize());
        menu.findItem(R.id.action_grid).setIcon(new IconDrawable(this, FontAwesomeIcons.fa_th).colorRes(R.color.colorWhite).actionBarSize());
        menu.findItem(R.id.action_menu_filter).setIcon(new IconDrawable(this, FontAwesomeIcons.fa_filter).colorRes(R.color.colorWhite).actionBarSize());
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
            Intent intent = new Intent(this, DisplaySettingsActivity.class);
            startActivityForResult(intent, RESULT_SETTINGS_ACTIVITY);
            return true;
        } else if (id == R.id.action_grid) {
            // change grid size ...
            currentGridSizeIndex++;
            currentGridSizeIndex = currentGridSizeIndex % gridSize.size();
            rows = gridSize.get(currentGridSizeIndex)[0];
            cols = gridSize.get(currentGridSizeIndex)[1];

            SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt("rows", rows);
            editor.putInt("cols", cols);
            // Commit the edits!
            editor.commit();

            // paint ...
            if (original!=null) paintLines(original);
        } else if (id == R.id.action_filter_comic) {
            if (original!=null) applyFilters(0);
        } else if (id == R.id.action_filter_edges) {
            if (original!=null) applyFilters(1);
        } else if (id == R.id.action_filter_gray) {
            if (original!=null) applyFilters(2);
        } else if (id == R.id.action_filter_reset) {
            if (original!=null) applyFilters(3);
        } else if (id == R.id.action_fullscreen) {
            // hide status bar
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
            android.support.v7.app.ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) actionBar.hide();
            findViewById(R.id.leaveFullscreenButton).setVisibility(View.VISIBLE);
        } else if (id == R.id.action_share) {
            try {
//                File outputDir = getApplicationContext().getCacheDir(); // context being the Activity pointer
//                File outputFile = File.createTempFile("image", "jpeg", outputDir);
                if (buffer!=null) {
                    File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "artistgrid");
                    if (!directory.exists()) directory.mkdirs();
                    File outputFile = new File(directory, "grid_" + (android.text.format.DateFormat.format("yyyy_MM_dd-hh_mm_ss", new java.util.Date())) + ".jpg");
                    FileOutputStream outStream = new FileOutputStream(outputFile);
                    buffer.compress(Bitmap.CompressFormat.JPEG, 75, outStream);
                    outStream.flush();
                    outStream.close();
                    String uri = MediaStore.Images.Media.insertImage(getContentResolver(), outputFile.getAbsolutePath(), outputFile.getName(), outputFile.getName());
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.setType("image/jpeg");
                    sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(outputFile));
                    startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (id == R.id.action_save) {
            if (buffer == null) return true;
            // save edges file
            File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "artistgrid");
            if (!directory.exists()) directory.mkdirs();
            File toSave = new File(directory, "grid_" + (android.text.format.DateFormat.format("yyyy_MM_dd-hh_mm_ss", new java.util.Date())) + ".png");
            try {
                FileOutputStream outStream = new FileOutputStream(toSave);
                // todo ...
                buffer.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                outStream.flush();
                outStream.close();
                Toast.makeText(getApplicationContext(), "Saved to " + directory + "!", Toast.LENGTH_LONG).show();
                MediaStore.Images.Media.insertImage(getContentResolver(), toSave.getAbsolutePath(), toSave.getName(), toSave.getName());
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Save failed!", Toast.LENGTH_LONG).show();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            openImage(selectedImage);
        }
    }

    private void openImage(Uri selectedImage) {
        try {
            original = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
            File file = new File(getApplicationContext().getFilesDir(), IMG_CACHED);
            if (file.exists()) file.delete();
            OutputStream out = new FileOutputStream(file);
            int w = original.getWidth();
            int h = original.getHeight();
            if (Math.max(w, h) > maxImageSide) {
                float scalefactor = 1f;
                if (h > w) {
                    scalefactor = maxImageSide / h;
                } else scalefactor = maxImageSide / w;
                original = Bitmap.createScaledBitmap(original, (int) (scalefactor * w), (int) (scalefactor * h), true);
            }
            original.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            paintLines(original);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Cannot open " + selectedImage.getPath() + "!", Toast.LENGTH_LONG).show();
        }
    }

    private void applyFilters(int which) {
        if (which==0) { // comic
            PosterizeFilter quantizeFilter = new PosterizeFilter();
            EdgeFilter edgeFilter = new EdgeFilter();
            InvertFilter invertFilter = new InvertFilter();
            GrayscaleFilter grayscaleFilter = new GrayscaleFilter();
            int[] pixels = AndroidUtils.bitmapToIntArray(original);
            int[] pixels2;
            pixels2 = quantizeFilter.filter(AndroidUtils.bitmapToIntArray(original), original.getWidth(), original.getHeight());
            pixels = grayscaleFilter.filter(pixels, original.getWidth(), original.getHeight());
            pixels = edgeFilter.filter(pixels, original.getWidth(), original.getHeight());
            pixels = invertFilter.filter(pixels, original.getWidth(), original.getHeight());
            Bitmap tmp1 = Bitmap.createBitmap(pixels, 0, original.getWidth(), original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
            Bitmap tmp2 = Bitmap.createBitmap(pixels2, 0, original.getWidth(), original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
            original = combineWithOverlay(tmp1, tmp2);
        } else if (which==1) { // edges only
            InvertFilter invertFilter = new InvertFilter();
            GrayscaleFilter grayscaleFilter = new GrayscaleFilter();
            EdgeFilter edgeFilter = new EdgeFilter();
            int[] pixels = AndroidUtils.bitmapToIntArray(original);
            pixels = grayscaleFilter.filter(pixels, original.getWidth(), original.getHeight());
            pixels = edgeFilter.filter(pixels, original.getWidth(), original.getHeight());
            pixels = invertFilter.filter(pixels, original.getWidth(), original.getHeight());
            original = Bitmap.createBitmap(pixels, 0, original.getWidth(), original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
        } else if (which==2) { // black & white
            GrayscaleFilter grayscaleFilter = new GrayscaleFilter();
            int[] pixels = AndroidUtils.bitmapToIntArray(original);
            pixels = grayscaleFilter.filter(pixels, original.getWidth(), original.getHeight());
            original = Bitmap.createBitmap(pixels, 0, original.getWidth(), original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
        } else if (which==3) { // reset ..
            File file = new File(getApplicationContext().getFilesDir(), IMG_CACHED);
            if (file.exists()) {
                try {
                    original = BitmapFactory.decodeStream(new FileInputStream(file));
                    paintLines(original);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        paintLines(original);
    }

    protected Bitmap combineWithOverlay(Bitmap edges, Bitmap image) {
        Bitmap result = image.copy(Bitmap.Config.ARGB_8888, true);


        Paint p = new Paint();
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DARKEN));
        BitmapShader gradientShader = new BitmapShader(edges, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        p.setShader(gradientShader);


        Canvas c = new Canvas();
        c.setBitmap(result);
        c.drawBitmap(image, 0, 0, null);
        c.drawRect(0, 0, image.getWidth(), image.getHeight(), p);

        return result;
    }

    private void paintLines(Bitmap bitmap) {
        if (bitmap==null) return;
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Paint linePaint = new Paint();
        linePaint.setColor(lineColors[this.lineColor]);
        linePaint.setAlpha(alpha);


        buffer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        Canvas c = new Canvas(buffer);
//        Paint colorFilter = new Paint(); // todo: check filtering ...
//        colorFilter.setColorFilter(new PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.LIGHTEN));
//        colorFilter.setFilterBitmap(true);
        c.drawBitmap(bitmap, 0, 0, null);
        int rowOffset = h / (rows + 1);
        if (!squareGrid) {
            for (int i = 0; i < cols; i++) {
                for (int k = 0; k < lineWidth; k++) {
                    c.drawLine((i + 1) * w / (cols + 1) + k, 0, (i + 1) * w / (cols + 1) + k, h, linePaint);
                }
            }
        } else if (rows>0) {
            int numColLines = w / rowOffset;
            int offsetCols = (w%rowOffset)/2;
            for (int i=0; i<= numColLines; i++) {
                for (int k = 0; k < lineWidth; k++) {
                    c.drawLine((i) * rowOffset + k + offsetCols, 0, (i) * rowOffset + k + offsetCols, h, linePaint);
                }
            }
        }

        for (int i = 0; i < rows; i++) {
            for (int k = 0; k < lineWidth; k++) {
                c.drawLine(0, (i + 1) * rowOffset + k, w, (i + 1) * rowOffset + k, linePaint );
            }
        }


        TouchImageView view = (TouchImageView) findViewById(R.id.mainImageView);
        view.setImageBitmap(buffer);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("rows", rows);
        editor.putInt("cols", cols);
        editor.putInt("lineWidth", lineWidth);
        editor.putInt("lineAlpha", alpha);
        editor.putBoolean("colorpicker", colorpicker);
        editor.putBoolean("squareGrid", squareGrid);
        // Commit the edits!
        editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        rows = settings.getInt("rows", 4);
        cols = settings.getInt("cols", 3);
        lineWidth = settings.getInt("lineWidth", 3);
        colorpicker = settings.getBoolean("colorpicker", false);
        String tmp = settings.getString("currentImage", null);
        try {
            File file = new File(getApplicationContext().getFilesDir(), IMG_CACHED);
            if (file.exists()) {
                original = BitmapFactory.decodeStream(new FileInputStream(file));
                paintLines(original);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ColorPickerOnTouchListener implements View.OnTouchListener {
        private final TouchImageView view;
        private MainActivity mainActivity;

        public ColorPickerOnTouchListener(TouchImageView view, MainActivity original) {
            this.view = view;
            this.mainActivity = original;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Log.i(getClass().getName(), "touch registered.");
            if (mainActivity.colorpicker) {
                float x = event.getX();
                float y = event.getY();
                PointF pointF = view.transformCoordTouchToBitmap(x, y, true);
                if (mainActivity.original != null && pointF.x < mainActivity.original.getWidth() && pointF.y < mainActivity.original.getHeight()) {
                    int color = mainActivity.original.getPixel(((int) pointF.x), ((int) pointF.y));
                    String hexColor = "";
                    hexColor += Color.red(color) < 16 ? "0" + Integer.toHexString(Color.red(color)) : Integer.toHexString(Color.red(color));
                    hexColor += Color.green(color) < 16 ? "0" + Integer.toHexString(Color.green(color)) : Integer.toHexString(Color.green(color));
                    hexColor += Color.blue(color) < 16 ? "0" + Integer.toHexString(Color.blue(color)) : Integer.toHexString(Color.blue(color));

                    String rgb = "(";
                    rgb += Color.red(color) + ", ";
                    rgb += Color.green(color) + ", ";
                    rgb += Color.blue(color) + ")";
                    Snackbar.make(view, mainActivity.getString(R.string.color) + " " + rgb + " / #" + hexColor, Snackbar.LENGTH_INDEFINITE).show();
                }
            }
            return true;
        }
    }
}
