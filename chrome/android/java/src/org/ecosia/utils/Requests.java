package org.ecosia.utils;

import android.util.Pair;

import org.chromium.net.ChromiumNetworkAdapter;
import org.chromium.net.NetworkTrafficAnnotationTag;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Requests {
    private static final int READ_TIMEOUT_MS = 10000;
    private static final int CONNECT_TIMEOUT_MS = 15000;

    public String downloadContent(String requestUrl) throws IOException {
        return downloadContentAndHeaders(requestUrl, new HashMap<>()).first;
    }

    public Pair<String, Map<String, List<String>>> downloadContentAndHeaders(String requestUrl, HashMap<String, String> requestHeaders) throws IOException {
        InputStream inputStream = null;

        try {
            URL url = new URL(requestUrl);
            HttpURLConnection conn = (HttpURLConnection) ChromiumNetworkAdapter.openConnection(url, NetworkTrafficAnnotationTag.NO_TRAFFIC_ANNOTATION_YET) ;
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            for(Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
            conn.connect();
            inputStream = conn.getInputStream();

            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line).append('\n');
            }
            return new Pair<>(total.toString(), conn.getHeaderFields());

        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }
}
