package org.ecosia.ntp.cards;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import androidx.core.content.res.ResourcesCompat;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.ecosia.ntp.News;
import org.ecosia.utils.StorageHelpers;
import org.ecosia.utils.TimestampHelpers;

public class NewsCard extends Card {

    private static final String IMAGE_NAME = "image_";

    private final TextView mTitleView;
    private final TextView mDateView;
    private final ImageView mThumbnailView;

    public NewsCard(RelativeLayout layout, CardPosition position) {
        super(layout, position);

        mTitleView = layout.findViewById(org.chromium.chrome.R.id.text_news);
        mDateView =  layout.findViewById(org.chromium.chrome.R.id.date_news);
        mThumbnailView = layout.findViewById(org.chromium.chrome.R.id.thumbnail_news);
    }

    public void initialize(News article, int position, View.OnClickListener clickListener, Resources resources) {
        super.initialize(clickListener);

        mTitleView.setText(Html.fromHtml(article.getText()));
        mDateView.setText(TimestampHelpers.getCompactFormattedDate(article.getTimestamp()));

        Bitmap bitmap = StorageHelpers.loadImageFromStorage(IMAGE_NAME + position);
        Drawable bitmapDrawable;
        if (bitmap != null) {
            bitmapDrawable = Drawable.createFromPath(StorageHelpers.getPathForImage(IMAGE_NAME + position));
        } else {
            bitmapDrawable = ResourcesCompat.getDrawable(resources, org.chromium.chrome.R.drawable.ic_ecosia, null);
        }
        mThumbnailView.setImageDrawable(bitmapDrawable);
    }
}
