package org.ecosia.utils;

import org.chromium.base.task.AsyncTask;

public class RetrieveDataTask extends AsyncTask<Object> {

    private final RetrieveDelegate mRetrievable;

    public RetrieveDataTask(RetrieveDelegate retrievable) {
        mRetrievable = retrievable;
    }

    @Override
    protected Object doInBackground() {
        return mRetrievable.doInBackground();
    }

    @Override
    protected void onPostExecute(Object result) {
        mRetrievable.onPostExecute(result);
    }
}
