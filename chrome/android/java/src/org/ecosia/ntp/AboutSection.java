package org.ecosia.ntp;

import android.graphics.drawable.Drawable;

import org.ecosia.utils.TimestampHelpers;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

public class AboutSection  {
    private String mTitle;
    private Drawable mImage;
    private String mText;
    private boolean mExpand;
    private String mTargetUrl;

    public AboutSection(String title, Drawable img, String text, String url) {
        mTitle = title;
        mImage = img;
        mText = text;
        mTargetUrl = url;
        mExpand = false; // Default will be false
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public Drawable getImage() {
        return mImage;
    }

    public void setImage(Drawable image) {
        this.mImage = image;
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        this.mText = text;
    }

    public boolean isExpand() {
        return mExpand;
    }

    public void setExpand(boolean expand) {
        this.mExpand = expand;
    }

    public String getTargetUrl() {
        return mTargetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.mTargetUrl = targetUrl;
    }

}
