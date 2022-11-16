package org.ecosia.ntp;

import android.content.Context;
import android.content.Intent;

import org.chromium.base.test.BaseRobolectricTestRunner;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.ecosia.ntp.EcosiaStatisticsManager.ACTION_ECOSIA_STATS_FETCH;
import static org.ecosia.ntp.EcosiaStatisticsManager.EXTRA_ECOSIA_STATS_FETCH_SUCCESS;
import static org.ecosia.ntp.EcosiaStatisticsManager.EXTRA_TREE_COUNTER_RATE;
import static org.ecosia.ntp.EcosiaStatisticsManager.EXTRA_TREE_COUNTER_VALUE;

import androidx.test.core.app.ApplicationProvider;

@RunWith(BaseRobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public final class EcosiaStatisticsTest {

    private static final String RAW_RESPONSE =
            "{\"results\": [" +
                    "{\"name\": \"Active Users\", \"value\": \"15000000.00\", \"last_updated\": \"2019-12-09T10:49:00.000000Z\"}, " +
                    "{\"name\": \"Cost per tree\", \"value\": \"0.20\", \"last_updated\": \"2018-03-01T11:00:00.604110Z\"}, " +
                    "{\"name\": \"EUR=>USD\", \"value\": \"1.08\", \"last_updated\": \"2017-01-27T16:32:08.675314Z\"}, " +
                    "{\"name\": \"Facebook Likes\", \"value\": \"216093.00\", \"last_updated\": null}, " +
                    "{\"name\": \"Planting sites\", \"value\": \"9000.00\", \"last_updated\": null}, " +
                    "{\"name\": \"Time per tree (seconds)\", \"value\": \"0.8\", \"last_updated\": \"2019-08-14T15:00:00.0Z\"}, " +
                    "{\"name\": \"Total Searches\", \"value\": \"321788830916.00\", \"last_updated\": null}, " +
                    "{\"name\": \"Total Trees Planted\", \"value\": \"64270918.00\", \"last_updated\": \"2019-08-14T15:00:00.0Z\"}, " +
                    "{\"name\": \"Tree planting donations\", \"value\": \"12858770.00\", \"last_updated\": \"2019-12-09T11:35:00.000000Z\"}, " +
                    "{\"name\": \"WWF Donation\", \"value\": \"1271557.00\", \"last_updated\": null}]}";

    private Context mContext;

    @Before
    public void setUp() throws InterruptedException {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void fillIntentFromOptionsResponseTest() {
        EcosiaStatisticsManager statisticsManager = new EcosiaStatisticsManager(mContext);

        Intent intent = new Intent(ACTION_ECOSIA_STATS_FETCH);
        intent.putExtra(EXTRA_ECOSIA_STATS_FETCH_SUCCESS, false);

        Assert.assertEquals(0, intent.getIntExtra(EXTRA_TREE_COUNTER_VALUE, 0));
        Assert.assertEquals(0, intent.getIntExtra(EXTRA_TREE_COUNTER_RATE, 0));

        try {
            intent = statisticsManager.fillIntentFromOptionsResponse(RAW_RESPONSE, intent);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(64270918, intent.getIntExtra(EXTRA_TREE_COUNTER_VALUE, 0));
        Assert.assertEquals(0.8, intent.getFloatExtra(EXTRA_TREE_COUNTER_RATE, 0), 0.0001);
    }
}
