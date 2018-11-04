package com.example.android.emojify;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import timber.log.Timber;

public class Emojifier {

    private static final String LOG_TAG = Emojifier.class.getName();
    private static final float SMILING_PROBABILITY_THRESHOLD = 0.654f; //has to be determined upon several sample tests
    private static final float EYE_OPEN_PROBABILITY_THRESHOLD = 0.454f; //has to be determined upon several sample tests
    private static final float EMOJI_SCALE_FACTOR = 1.2f;


    public static Bitmap detectFacesAndOverlayEmoji(@NonNull Context context, @NonNull Bitmap picture) {

        //create the Face Detector instance
        FaceDetector faceDetector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setTrackingEnabled(false)
                .build();

        //create the picture frame
        Frame frame = new Frame.Builder().setBitmap(picture).build();

        //detect the number of faces in the picture
        SparseArray<Face> faceArray = faceDetector.detect(frame);

        Timber.d(LOG_TAG, "Number of Detected Faces: " + faceArray.size());

        if (faceArray.size() < 1)
            Toast.makeText(context, "No face detected", Toast.LENGTH_SHORT).show();

        //Get Classification details
        Bitmap emojiBitmap;
        Bitmap resultBitmap = picture;
        for (int faceIndex = 0; faceIndex < faceArray.size(); faceIndex++) {
            Timber.d("Face: ", faceIndex);
            emojiBitmap = whichEmoji(context, faceArray.valueAt(faceIndex));

            if (emojiBitmap != null)
                resultBitmap = addEmojiToFace(resultBitmap, emojiBitmap, faceArray.valueAt(faceIndex));
        }

        //release the face detector
        faceDetector.release();

        return resultBitmap;
    }

    private static Bitmap whichEmoji(Context context, Face face) {
        Timber.d("Probability of Smiling: ", face.getIsSmilingProbability());
        Timber.d("Probability of Left Eye Open: ", face.getIsLeftEyeOpenProbability());
        Timber.d("Probability of Right Eye Open: ", face.getIsRightEyeOpenProbability() + "\n\n");

        boolean isSmiling = isSmiling(face);
        boolean isLeftEyeOpened = isLeftEyeOpen(face);
        boolean isRightEyeOpened = isRightEyeOpen(face);

        if (isSmiling) {
            if (isLeftEyeOpened && isRightEyeOpened)
                return BitmapFactory.decodeResource(context.getResources(), Emoji.SMILE.getValue());
            else if (isLeftEyeOpened && !isRightEyeOpened)
                return BitmapFactory.decodeResource(context.getResources(), Emoji.RIGHT_WINK.getValue());
            else if (!isLeftEyeOpened && isRightEyeOpened)
                return BitmapFactory.decodeResource(context.getResources(), Emoji.LEFT_WINK.getValue());
            else if (!isLeftEyeOpened && !isRightEyeOpened)
                return BitmapFactory.decodeResource(context.getResources(), Emoji.CLOSED_EYE_SMILE.getValue());
        } else {
            if (isLeftEyeOpened && isRightEyeOpened)
                return BitmapFactory.decodeResource(context.getResources(), Emoji.FROWN.getValue());
            else if (isLeftEyeOpened && !isRightEyeOpened)
                return BitmapFactory.decodeResource(context.getResources(), Emoji.LEFT_WINK_FROWN.getValue());
            else if (!isLeftEyeOpened && isRightEyeOpened)
                return BitmapFactory.decodeResource(context.getResources(), Emoji.RIGHT_WINK_FROWN.getValue());
            else if (!isLeftEyeOpened && !isRightEyeOpened)
                return BitmapFactory.decodeResource(context.getResources(), Emoji.CLOSED_EYE_FROWN.getValue());
        }
        return null;
    }

    private static Bitmap addEmojiToFace(@NonNull Bitmap backgroundBitmap, @NonNull Bitmap emojiBitmap, Face face) {
        //Mutate the background image
        Bitmap resultBitmap = Bitmap.createBitmap(backgroundBitmap.getWidth(), backgroundBitmap.getHeight(),
                backgroundBitmap.getConfig());

        // Scale the emoji so it looks better (smaller) on the face
        float scaleFactor = EMOJI_SCALE_FACTOR;

        // Determine the size of the emoji to match the width of the face and preserve aspect ratio
        int aspectRatio = emojiBitmap.getHeight() / emojiBitmap.getWidth();
        int newEmojiWidth = (int) (face.getWidth() * scaleFactor);
        int newEmojiHeight = (int) ((emojiBitmap.getHeight() * aspectRatio) * scaleFactor) + 1;


        // Scale the emoji
        emojiBitmap = Bitmap.createScaledBitmap(emojiBitmap, newEmojiWidth, newEmojiHeight, false);

        // Determine the emoji position so it best lines up with the face
        float emojiPositionX =
                (face.getPosition().x + face.getWidth() / 2) - emojiBitmap.getWidth() / 2;
        float emojiPositionY =
                (face.getPosition().y + face.getHeight() / 2) - emojiBitmap.getHeight() / 3;

        // Create the canvas and draw the bitmaps to it
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        canvas.drawBitmap(emojiBitmap, emojiPositionX, emojiPositionY, null);

        return resultBitmap;
    }

    private static boolean isLeftEyeOpen(Face face) {
        return face.getIsLeftEyeOpenProbability() >= EYE_OPEN_PROBABILITY_THRESHOLD;
    }

    private static boolean isRightEyeOpen(Face face) {
        return face.getIsRightEyeOpenProbability() >= EYE_OPEN_PROBABILITY_THRESHOLD;
    }

    private static boolean isSmiling(Face face) {
        return face.getIsSmilingProbability() >= SMILING_PROBABILITY_THRESHOLD;
    }

    public enum Emoji {

        SMILE(R.drawable.smile),
        FROWN(R.drawable.frown),
        LEFT_WINK(R.drawable.leftwink),
        RIGHT_WINK(R.drawable.rightwink),
        LEFT_WINK_FROWN(R.drawable.leftwinkfrown),
        RIGHT_WINK_FROWN(R.drawable.rightwinkfrown),
        CLOSED_EYE_SMILE(R.drawable.closed_smile),
        CLOSED_EYE_FROWN(R.drawable.closed_frown);

        private int value;

        Emoji(@DrawableRes int value) {
            this.value = value;
        }

        int getValue() {
            return value;
        }
    }
}
