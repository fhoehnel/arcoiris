package de.arcoiris;

import android.content.Context;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.FileNotFoundException;
import java.io.IOException;
import android.util.Log;


public class OfflineListEntry extends LinearLayout {

    private static final int LIST_THUMBNAIL_SIZE_SMALL = 240;
    private static final int LIST_THUMBNAIL_SIZE_LARGE = 400;

    private OfflineQueueEntryInfo queuedFile = null;

    private String queueFileName = null;

    public OfflineListEntry(Context context, OfflineQueueEntryInfo queuedFile, final MainActivity activity, boolean firstEntryOfDay) {
        super(context);

        queueFileName = queuedFile.getQueueImgFile().getName();

        float densityFactor = getResources().getDisplayMetrics().density;

        int screenHeight = (int) (activity.getWindow().getDecorView().getHeight() / densityFactor);
        int screenNativePixWidth = activity.getWindow().getDecorView().getWidth();

        int thumbnailSize = LIST_THUMBNAIL_SIZE_SMALL;

        if (screenHeight > MainActivity.MIN_SCREEN_HEIGHT_FOR_LARGE_THUMBS) {
            thumbnailSize = LIST_THUMBNAIL_SIZE_LARGE;
        }

        int blogTextWidth = screenNativePixWidth - thumbnailSize - 100;

        setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        setPadding(10, 10, 10, 10);

        ImageView queuePicView = new ImageView(context);

        Bitmap bitmap;
        try {
            Uri pictureUri = Uri.parse("file:///" + queuedFile.getQueueImgFile().getAbsolutePath());

            bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), pictureUri);

            Bitmap scaledBitmap = PictureUtils.getResizedBitmap(bitmap, thumbnailSize);

            if (scaledBitmap != bitmap) {
                // if scaledBitmap is the same size as original bitmap, a new instance is NOT created, so we must not recylcle the orig image
                bitmap.recycle();
            }

            queuePicView.setImageBitmap(scaledBitmap);

            queuePicView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.showBlogForm(MainActivity.BLOG_FORM_MODE_MODIFY);
                    activity.presetBlogFormValues(((OfflineListEntry) v.getParent()).getQueueFileName());
                }
            });

            addView(queuePicView);

            LinearLayout buttonCont = new LinearLayout(context);
            buttonCont.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
            buttonCont.setOrientation(LinearLayout.VERTICAL);
            buttonCont.setBackgroundColor(0xffe0e0e0);
            buttonCont.setGravity(Gravity.RIGHT);

            ImageButton editButton = new ImageButton(context);
            editButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            editButton.setBackgroundColor(0xffe0e0e0);
            editButton.setPadding(8, 0, 8, 8);
            editButton.setImageResource(R.drawable.edit);

            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.showBlogForm(MainActivity.BLOG_FORM_MODE_MODIFY);
                    activity.presetBlogFormValues(((OfflineListEntry) v.getParent().getParent()).getQueueFileName());
                }
            });

            buttonCont.addView(editButton);

            ImageButton deleteButton = new ImageButton(context);
            deleteButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            deleteButton.setBackgroundColor(0xffe0e0e0);
            deleteButton.setPadding(8, 0, 8, 8);
            deleteButton.setImageResource(R.drawable.delete);

            buttonCont.addView(deleteButton);

            if (!firstEntryOfDay) {
                ImageButton moveUpButton = new ImageButton(context);
                moveUpButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                // moveUpButton.setBackgroundColor(0xffe0e0e0);
                moveUpButton.setBackgroundColor(0xffe0e0e0);
                moveUpButton.setPadding(8, 0, 8, 0);
                moveUpButton.setImageResource(R.drawable.moveup);

                buttonCont.addView(moveUpButton);

                moveUpButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.moveOfflineEntryUp(((OfflineListEntry) v.getParent().getParent()).getQueueFileName());
                    }
                });
            }

            addView(buttonCont);

            String shortText = queuedFile.getMetaData().getBlogText();
            if (shortText.length() > MainActivity.MAX_LIST_TEXT_LENGTH) {
                shortText = shortText.substring(0, MainActivity.MAX_LIST_TEXT_LENGTH - 4) + " ...";
            }

            TextView blogTextView = new TextView(context);
            blogTextView.setPadding(10, 0, 0, 0);
            blogTextView.setWidth(blogTextWidth);
            blogTextView.setTextColor(Color.BLACK);
            blogTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            blogTextView.setSingleLine(false);
            blogTextView.setEnabled(false);
            blogTextView.setText(shortText);
            addView(blogTextView);

            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.deleteOfflineEntry(((OfflineListEntry) v.getParent().getParent()).getQueueFileName());
                }
            });
        } catch (FileNotFoundException e) {
            Log.e("arcoiris", "failed to read image data of queued picture", e);
        } catch (IOException e) {
            Log.e("arcoiris", "failed to read image data of queued picture", e);
        }
    }

    public String getQueueFileName() {
        return queueFileName;
    }
}
