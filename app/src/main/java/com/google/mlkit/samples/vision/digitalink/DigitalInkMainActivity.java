package com.google.mlkit.samples.vision.digitalink;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.mlkit.samples.vision.digitalink.StrokeManager.DownloadedModelsChangedListener;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Main activity which creates a StrokeManager and connects it to the DrawingView. */
@SuppressWarnings("ALL")
public class DigitalInkMainActivity extends AppCompatActivity implements
        DownloadedModelsChangedListener, View.OnClickListener {
//    PaintView mPaintView;
//    int colorBackground,colorBrush;

    private ImageButton currentStrokePaint, drawBtn,baru,erase,save;
    private DrawingView drawingView;
    



    private static final String TAG = "MLKDI.Activity";
    private static final ImmutableMap<String, String> NON_TEXT_MODELS =
            ImmutableMap.of(
                    "zxx-Zsym-x-autodraw",
                    "Autodraw",
                    "zxx-Zsye-x-emoji",
                    "Emoji",
                    "zxx-Zsym-x-shapes",
                    "Shapes");
    @VisibleForTesting
    final StrokeManager strokeManager = new StrokeManager();
    private ArrayAdapter<ModelLanguageContainer> languageAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_digital_ink_main);
        drawingView = (DrawingView)findViewById(R.id.drawing_view);
        drawBtn = (ImageButton)findViewById(R.id.draw_btn);
        baru = (ImageButton)findViewById(R.id.new_btn);
        erase = (ImageButton)findViewById(R.id.erase_btn);
        save = (ImageButton)findViewById(R.id.save_btn);
        LinearLayout paintLayout = findViewById(R.id.paint_colors);
        currentStrokePaint = (ImageButton)paintLayout.getChildAt(0);
        currentStrokePaint.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));
        drawBtn.setOnClickListener(this);
        baru.setOnClickListener(this);
        erase.setOnClickListener(this);
        save.setOnClickListener(this);

        Spinner languageSpinner = findViewById(R.id.languages_spinner);

        drawingView = (DrawingView)findViewById(R.id.drawing_view);
        StatusTextView statusTextView = findViewById(R.id.status_text_view);
        drawingView.setStrokeManager(strokeManager);
        statusTextView.setStrokeManager(strokeManager);

        strokeManager.setStatusChangedListener(statusTextView);
        strokeManager.setContentChangedListener(drawingView);
        strokeManager.setDownloadedModelsChangedListener(this);
        strokeManager.setClearCurrentInkAfterRecognition(true);
        strokeManager.setTriggerRecognitionAfterInput(true);

        languageAdapter = populateLanguageAdapter();
        languageAdapter.setDropDownViewResource
                (android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(languageAdapter);
        strokeManager.refreshDownloadedModelsStatus();

        languageSpinner.setOnItemSelectedListener(
                new OnItemSelectedListener() {
                    @Override
                    public void onItemSelected
                            (AdapterView<?> parent, View view, int position, long id) {
                        String languageCode =
                                ((ModelLanguageContainer) parent.getAdapter().getItem(position)).getLanguageTag();
                        if (languageCode == null) {
                            return;
                        }
                        Log.i(TAG, "Selected language: " + languageCode);
                        strokeManager.setActiveModel(languageCode);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        Log.i(TAG, "No language selected");
                    }
                });

        strokeManager.reset();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public void paintClicked(View view){
        if(view!=currentStrokePaint){
            ImageButton imgView = (ImageButton)view;
            String color = view.getTag().toString();
            drawingView.setColor(color);
            imgView.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));
            currentStrokePaint.setImageDrawable(getResources().getDrawable(R.drawable.paint));
            currentStrokePaint=(ImageButton)view;

        }
    }

    public void downloadClick(View v) {
        strokeManager.download();
    }

    public void clearClick(View v) {
        strokeManager.reset();
        DrawingView drawingView = findViewById(R.id.drawing_view);
        drawingView.clear();
    }

    public void deleteClick(View v) {
        strokeManager.deleteActiveModel();
    }


    private static class ModelLanguageContainer implements Comparable<ModelLanguageContainer> {
        private final String label;
        @Nullable
        private final String languageTag;
        private boolean downloaded;

        private ModelLanguageContainer(String label, @Nullable String languageTag) {
            this.label = label;
            this.languageTag = languageTag;
        }

        /**
         * Populates and returns a real model identifier, with label, language tag and downloaded
         * status.
         */
        public static ModelLanguageContainer createModelContainer(String label, String languageTag) {
            // Offset the actual language labels for better readability
            return new ModelLanguageContainer(label, languageTag);
        }

        /** Populates and returns a label only, without a language tag. */
        public static ModelLanguageContainer createLabelOnly(String label) {
            return new ModelLanguageContainer(label, null);
        }

        public String getLanguageTag() {
            return languageTag;
        }

        public void setDownloaded(boolean downloaded) {
            this.downloaded = downloaded;
        }

        @NonNull
        @Override
        public String toString() {
            if (languageTag == null) {
                return label;
            } else if (downloaded) {
                return "   [D] " + label;
            } else {
                return "   " + label;
            }
        }

        @Override
        public int compareTo(ModelLanguageContainer o) {
            return label.compareTo(o.label);
        }
    }

    @Override
    public void onDownloadedModelsChanged(Set<String> downloadedLanguageTags) {
        for (int i = 0; i < languageAdapter.getCount(); i++) {
            ModelLanguageContainer container = languageAdapter.getItem(i);
            container.setDownloaded(downloadedLanguageTags.contains(container.languageTag));
        }
    }

    private ArrayAdapter<ModelLanguageContainer> populateLanguageAdapter() {
        ArrayAdapter<ModelLanguageContainer> languageAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        languageAdapter.add(ModelLanguageContainer.createLabelOnly("Select language"));
        languageAdapter.add(ModelLanguageContainer.createLabelOnly("Non-text Models"));

        // Manually add non-text models first
        for (String languageTag : NON_TEXT_MODELS.keySet()) {
            languageAdapter.add(
                    ModelLanguageContainer.createModelContainer(
                            NON_TEXT_MODELS.get(languageTag), languageTag));
        }
        languageAdapter.add(ModelLanguageContainer.createLabelOnly("Text Models"));

        ImmutableSortedSet.Builder<ModelLanguageContainer> textModels =
                ImmutableSortedSet.naturalOrder();
        for (DigitalInkRecognitionModelIdentifier modelIdentifier :
                DigitalInkRecognitionModelIdentifier.allModelIdentifiers()) {
            if (NON_TEXT_MODELS.containsKey(modelIdentifier.getLanguageTag())) {
                continue;
            }

            StringBuilder label = new StringBuilder();
            label.append(new Locale(modelIdentifier.getLanguageSubtag()).getDisplayName());
            if (modelIdentifier.getRegionSubtag() != null) {
                label.append(" (").append(modelIdentifier.getRegionSubtag()).append(")");
            }

            if (modelIdentifier.getScriptSubtag() != null) {
                label.append(", ").append(modelIdentifier.getScriptSubtag()).append(" Script");
            }
            textModels.add(
                    ModelLanguageContainer.createModelContainer(
                            label.toString(), modelIdentifier.getLanguageTag()));
        }
        languageAdapter.addAll(textModels.build());
        return languageAdapter;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.draw_btn){
            drawingView.setupDrawing();
        }
        if(v.getId()==R.id.erase_btn){
            drawingView.setErase(true);
            drawingView.setBrushSize(drawingView.getLastBrushSize());
        }
        if(v.getId()==R.id.new_btn){
            AlertDialog.Builder newDialog = new AlertDialog.Builder(this);
            newDialog.setTitle("New drawing");
            newDialog.setMessage("Start new drawing (you will lose the current drawing)?");
            newDialog.setPositiveButton("Yes", (dialog, which) -> {
                drawingView.clear();
                dialog.dismiss();
            });
            newDialog.setNegativeButton("No", (dialog, which) -> dialog.cancel());
            newDialog.show();
        }
        if(v.getId()==R.id.save_btn){
            AlertDialog.Builder saveDialog = new AlertDialog.Builder(this);
            saveDialog.setTitle("Save drawing");
            saveDialog.setMessage("Save drawing to device Gallery?");
            saveDialog.setPositiveButton("Yes", (dialog, which) -> {
                drawingView.setDrawingCacheEnabled(true);
                String imgSaved = MediaStore.Images.Media.insertImage(
                        getContentResolver(), drawingView.getDrawingCache(),
                        UUID.randomUUID().toString()+".png", "drawing");
                if(imgSaved!=null){
                    Toast savedToast = Toast.makeText(getApplicationContext(),
                            "Drawing saved to Gallery!", Toast.LENGTH_SHORT);
                    savedToast.show();
                }
                else{
                    Toast unsavedToast = Toast.makeText(getApplicationContext(),
                            "Oops! Image could not be saved.", Toast.LENGTH_SHORT);
                    unsavedToast.show();
                }
                drawingView.destroyDrawingCache();

            });
            saveDialog.setNegativeButton("No", (dialog, which) -> dialog.cancel());
            saveDialog.show();
        }
    }

}