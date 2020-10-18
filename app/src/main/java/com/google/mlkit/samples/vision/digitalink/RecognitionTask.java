package com.google.mlkit.samples.vision.digitalink;

import android.graphics.Paint;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer;
import com.google.mlkit.vision.digitalink.Ink;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.mlkit.samples.vision.digitalink.RecognitionTask.RecognizedInk.curr_text;

/** Task to run asynchronously to obtain recognition results. */
public class RecognitionTask {

    private static final String TAG = "MLKD.RecognitionTask";
    private final DigitalInkRecognizer recognizer;
    private final Ink ink;
    @Nullable
    private RecognizedInk currentResult;
    private final AtomicBoolean cancelled;
    private final AtomicBoolean done;

    public RecognitionTask(DigitalInkRecognizer recognizer, Ink ink) {
        this.recognizer = recognizer;
        this.ink = ink;
        this.currentResult = null;
        cancelled = new AtomicBoolean(false);
        done = new AtomicBoolean(false);
    }

    public void cancel() {
        cancelled.set(true);
    }

    public boolean done() {
        return done.get();
    }

    @Nullable
    public RecognizedInk result() {
        return this.currentResult;
    }

    /** Helper class that stores an ink along with the corresponding recognized text. */
    public static class RecognizedInk {
        public static Paint currentResult;
        public static String curr_text;
        public final Ink ink;
        public final String text;

        RecognizedInk(Ink ink, String text) {
            this.ink = ink;
            this.text = text;
        }
    }

    public Task<String> run() {
        Log.i(TAG, "RecoTask.run");
        return recognizer
                .recognize(this.ink)
                .onSuccessTask(
                        result -> {
                            if (cancelled.get() || result.getCandidates().isEmpty()) {
                                return Tasks.forResult(null);
                            }
                            currentResult = new RecognizedInk(ink, result.getCandidates().get(0).getText());
                            curr_text = currentResult.text;
                            System.out.println(curr_text);
                            Log.i(TAG, "result: " + curr_text);
                            done.set(true);
                            return Tasks.forResult(curr_text);
                        });
    }
}