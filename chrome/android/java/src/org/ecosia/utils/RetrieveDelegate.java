package org.ecosia.utils;

public interface RetrieveDelegate<T> {

    T doInBackground();

    void onPostExecute(T result);
}
