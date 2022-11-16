package org.ecosia.ntp;

import org.ecosia.utils.TimestampHelpers;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

public class News implements Serializable {
    private String mText;
    private String mTrackingName;
    private long mTimestamp;
    private URL mTargetUrl;
    private URL mImageUrl;
    private String mLanguage;
    private int mLevel;

    public News(JSONObject jsonNews) throws JSONException, MalformedURLException {
        mText = jsonNews.getString("text");
        mTrackingName = jsonNews.getString("tracking_name");
        mTimestamp = TimestampHelpers.parseTimestamp(jsonNews.getString("publish_date"));
        mTargetUrl = new URL(jsonNews.getString("target_url"));
        mImageUrl = new URL(jsonNews.getString("image_url"));
        mLanguage = jsonNews.getString("language");
        mLevel = jsonNews.getInt("level");
    }

    public String getText() {
        return mText;
    }

    protected String getTrackingName() {
        return mTrackingName;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public URL getTargetUrl() {
        return mTargetUrl;
    }

    public URL getImageUrl() {
        return mImageUrl;
    }

    public String getLanguage() {
        return mLanguage;
    }

    public int getLevel() {
        return mLevel;
    }
}
