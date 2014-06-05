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
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;

import java.util.ArrayList;

/**
 * Android app for putting a grid on an image and displaying it.
 * @author Mathias Lux, mathias@juggle.at (c) 2014
 *
 */
public class ImageGridActivity extends Activity {
    private static int RESULT_LOAD_IMAGE = 1;
    private Bitmap bitmap = null;
    private Canvas c;
    private ArrayList<int[]> gridSize = new ArrayList<int[]>();
    private int currentGridSize = 0;
    private boolean customGrid = false;
    GridDialog d;
    private int customX = 3, customY = 4;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        d = new GridDialog(this);
        gridSize.add(new int[] {3,4});
        gridSize.add(new int[] {4,6});
        gridSize.add(new int[] {6,8});
        gridSize.add(new int[] {9,12});
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
                    currentGridSize = (currentGridSize +1) % gridSize.size();
                    drawImage(bitmap);
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);

            int columnIndex = cursor.getColumnIndexOrThrow(filePathColumn[0]);
            cursor.moveToFirst();
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            System.out.println("picturePath = " + picturePath);
            System.out.println("selectedImage.getPath() = " + selectedImage.getPath());
            if (picturePath!=null) {
                bitmap = BitmapFactory.decodeFile(picturePath).copy(Bitmap.Config.RGB_565, true);
                float scale = 1200f / Math.max(bitmap.getWidth(), bitmap.getHeight());
                bitmap = Bitmap.createScaledBitmap(bitmap, (int) (scale * bitmap.getWidth()), (int) (scale * bitmap.getHeight()), true);
                if (bitmap.getWidth() > bitmap.getHeight()) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90f);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                }
                drawImage(bitmap);
            } else {
                bitmap = null;
                Toast.makeText(getApplicationContext(), "Cannot open " + selectedImage.getPath() + "! Please use local images.", Toast.LENGTH_LONG).show();
            }

        }


    }

    public void setGrid() {
        String x = ((EditText) (d.getDialog().findViewById(R.id.numberCellsX))).getText().toString();
        String y = ((EditText) (d.getDialog().findViewById(R.id.numberCellsY))).getText().toString();
        customX= Integer.parseInt(x);
        customY = Integer.parseInt(y);
        customGrid = true;
        reDrawImage();
    }

    public void reDrawImage() {
        drawImage(bitmap);
    }

    private void drawImage(Bitmap bitmap) {
        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        Bitmap toDraw = bitmap.copy(Bitmap.Config.RGB_565, true);
        c = new Canvas(toDraw);
        drawLines(c);
        imageView.setImageBitmap(toDraw);
    }

    private void drawLines(Canvas canvas) {
        Paint paint = new Paint();
        paint.setARGB(128, 255, 255, 255);
        paint.setStrokeWidth(3f);
        Paint paintBlack = new Paint();
        paintBlack.setARGB(64, 0, 0, 0);
        paintBlack.setStrokeWidth(1f);
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
