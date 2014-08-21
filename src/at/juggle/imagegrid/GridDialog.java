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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.SeekBar;

/**
 * Android app for putting a grid on an image and displaying it.
 * @author Mathias Lux, mathias@juggle.at (c) 2014
 */
public class GridDialog extends DialogFragment {
    private final ImageGridActivity parent;

    public GridDialog(ImageGridActivity imageGridActivity) {
        parent = imageGridActivity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.grid_dialog, null))
                // Add action buttons
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        parent.setGrid();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        GridDialog.this.getDialog().cancel();
                    }
                });
        AlertDialog alertDialog = builder.create();
//        ((EditText) alertDialog.findViewById(R.id.numberCellsX)).setText("" + parent.customX);
//        ((EditText) alertDialog.findViewById(R.id.numberCellsY)).setText("" + parent.customY);
//        ((SeekBar) (alertDialog.findViewById(R.id.edgeImportanceSeekbar))).setProgress((int) ((ImageGridActivity.p - 10d)/50d*100d));
        return alertDialog;
    }

}