/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package at.juggle.imagegrid;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.*;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Android app for putting a grid on an image and displaying it.
 *
 * @author Mathias Lux, mathias@juggle.at (c) 2014
 */
public class ImageGridActivity extends Activity {
    private static int RESULT_LOAD_IMAGE = 1;
    private Bitmap bitmap = null;
    private Bitmap edges = null;
    private Bitmap buffer = null;
    private Canvas c;
    private ArrayList<int[]> gridSize = new ArrayList<int[]>();
    private int currentGridSize = 0;
    private boolean customGrid = false;
    private boolean showEdges = false;
    private boolean isProcessing = true;
    GridDialog d;
    private int customX = 3, customY = 4;
    private Point displaySize;

    static {
        if (!OpenCVLoader.initDebug()) {
            System.err.println("Init error!");
        }
    }

    private ImageButton buttonShowEdges;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        d = new GridDialog(this);
        displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        gridSize.add(new int[]{3, 4});
        gridSize.add(new int[]{4, 5});
        gridSize.add(new int[]{5, 6});
        gridSize.add(new int[]{6, 8});
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        ImageButton buttonLoadImage = (ImageButton) findViewById(R.id.button_open_image);
        buttonLoadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });
        ImageButton buttonGrid = (ImageButton) findViewById(R.id.button_grid);
        buttonGrid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (bitmap != null) {
                    customGrid = false;
                    currentGridSize = (currentGridSize + 1) % gridSize.size();
                    reDrawImage();
                }
            }
        });
        ImageButton buttonAddGrid = (ImageButton) findViewById(R.id.button_add_grid);
        buttonAddGrid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (bitmap != null) {
                    customGrid = true;
                    d.show(getFragmentManager(), "tag");
                }
            }
        });
        buttonShowEdges = (ImageButton) findViewById(R.id.button_show_edges);
        buttonShowEdges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (bitmap != null && !isProcessing) {
                    showEdges = !showEdges;
                    reDrawImage();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
//            buttonShowEdges.setEnabled(false);
            showEdges = false;
            isProcessing = true;
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);

            int columnIndex = cursor.getColumnIndexOrThrow(filePathColumn[0]);
            cursor.moveToFirst();
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            Log.i("grid", "picturePath = " + picturePath);
            Log.i("grid", "selectedImage.getPath() = " + selectedImage.getPath());
            if (picturePath != null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPurgeable = true;
                options.inMutable = true;
                bitmap = BitmapFactory.decodeFile(picturePath, options);
                float scale = (float) Math.max(displaySize.x, displaySize.y) / Math.max(bitmap.getWidth(), bitmap.getHeight());
                if (scale < 1f) bitmap = Bitmap.createScaledBitmap(bitmap, (int) (scale * bitmap.getWidth()), (int) (scale * bitmap.getHeight()), true);

                if (bitmap.getWidth() > bitmap.getHeight()) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90f);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                }
                this.edges = bitmap.copy(Bitmap.Config.RGB_565, true);

                CannyTask ct = new CannyTask();
                ct.execute();

                buffer = bitmap.copy(Bitmap.Config.RGB_565, true);
                c = new Canvas(buffer);
                reDrawImage();
            } else {
                bitmap = null;
                Toast.makeText(getApplicationContext(), "Cannot open " + selectedImage.getPath() + "! Please use local images.", Toast.LENGTH_LONG).show();
            }

        }


    }

    private class CannyTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... voids) {
            Mat mat = new Mat();
            Mat matEdges = new Mat();
            Utils.bitmapToMat(bitmap, mat);
//                orig = mat.clone();
            // convert to gray
            Imgproc.cvtColor(mat, matEdges, Imgproc.COLOR_BGR2GRAY);
            // prepare for histogram and determine median for canny ...
            LinkedList<Mat> matList = new LinkedList<Mat>();
            matList.add(mat);
            MatOfInt channels = new MatOfInt(0);
            Mat hist = new Mat();
            MatOfInt histSize = new MatOfInt(256);
            MatOfFloat ranges = new MatOfFloat(0f, 256f);
            Imgproc.calcHist(matList, channels, new Mat(), hist, histSize, ranges);
            double medianVal = ((double) (mat.cols() * mat.rows())) / 2d;
            double sum = 0;
            double median = 0;
            for (int i = 0; i < 256; i++) {
                sum += hist.get(i, 0)[0];
                if (sum < medianVal) median = (double) i;
            }
            channels.release();
            hist.release();
            ranges.release();
            histSize.release();
            // canny edge ...
            Imgproc.Canny(matEdges, matEdges, median*0.66, median*1.33);
            Core.bitwise_not(matEdges, matEdges);
            Imgproc.cvtColor(matEdges, matEdges, Imgproc.COLOR_GRAY2RGB);
            Mat tmp = new Mat();
            Imgproc.cvtColor(mat, tmp, Imgproc.COLOR_RGBA2RGB);
            Imgproc.bilateralFilter(tmp, mat, 12, 280, 280);
//                Core.addWeighted(mat, 0.5, edges, 0.5, 0.0, mat);
            Core.min(mat, matEdges, mat);
            Utils.matToBitmap(mat, edges);
            mat.release();
            tmp.release();
            matEdges.release();
            isProcessing = false;
            return null;
        }

        protected void onProgressUpdate() {
//            setProgressPercent(progress[0]);
        }

        protected void onPostExecute() {
            // showDialog("Downloaded " + result + " bytes");
//            buttonShowEdges.setEnabled(true);
        }

    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public void setGrid() {
        String x = ((EditText) (d.getDialog().findViewById(R.id.numberCellsX))).getText().toString();
        String y = ((EditText) (d.getDialog().findViewById(R.id.numberCellsY))).getText().toString();
        customX = Integer.parseInt(x);
        customY = Integer.parseInt(y);
        customGrid = true;
        reDrawImage();
    }

    public void reDrawImage() {
        if (showEdges) {
            drawImage(edges);
        } else {
            drawImage(bitmap);
        }
    }

    private void drawImage(Bitmap bitmap) {
        ImageView imageView = (ImageView) findViewById(R.id.imageView);
//        Bitmap toDraw = bitmap.copy(Bitmap.Config.RGB_565, true);
        c.drawBitmap(bitmap, 0, 0, new Paint());
        drawLines();
        imageView.setImageBitmap(buffer);
    }

    private void drawLines() {
        Paint paint = new Paint();
        paint.setARGB(128, 255, 255, 255);
        paint.setStrokeWidth(5f);
        Paint paintBlack = new Paint();
        paintBlack.setARGB(64, 0, 0, 64);
        paintBlack.setStrokeWidth(3f);
        float y = c.getHeight();
        float x = c.getWidth();
        float stepsX = gridSize.get(currentGridSize)[0];
        float stepsY = gridSize.get(currentGridSize)[1];
        if (customGrid) {
            stepsX = customX;
            stepsY = customY;
        }

        for (int i = 0; i <= stepsX; i++) {
            c.drawLine(i * x / stepsX, 0, i * x / stepsX, y, paint);
            c.drawLine(i * x / stepsX, 0, i * x / stepsX, y, paintBlack);
        }
        for (int i = 0; i <= stepsY; i++) {
            c.drawLine(0, i * y / stepsY, x, i * y / stepsY, paint);
            c.drawLine(0, i * y / stepsY, x, i * y / stepsY, paintBlack);
        }
//        c.drawLine(0, 0, 0, y, paint);
//        c.drawLine(x/4, 0, x/4, y, paint);
//
//        c.drawLine(3*x/4, 0, 3*x/4, y, paint);
//        c.drawLine(x, 0, x, y, paint);
    }
}
