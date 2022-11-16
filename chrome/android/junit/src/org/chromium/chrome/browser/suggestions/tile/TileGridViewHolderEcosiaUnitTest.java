package org.chromium.chrome.browser.suggestions.tile;

import android.util.SparseArray;
import android.view.ViewGroup;

import org.chromium.base.test.BaseRobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

@RunWith(BaseRobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class TileGridViewHolderEcosiaUnitTest {

    private static final int NUMBER_OF_TILES = 10;

    private List<Tile> mTiles;

    @Mock
    private TileGridLayout mViewGroup;

    @Mock
    private TileGridViewHolder mTileGridViewHolder;

    @Mock
    private TileGroup mTileGroup;

    @Mock
    private TileRenderer mTileRenderer;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mTiles = new ArrayList<>(NUMBER_OF_TILES);
        for (int i = 0; i < NUMBER_OF_TILES; i++) {
            final Tile tile = mock(Tile.class);
            mTiles.add(tile);
        }

        final SparseArray<List<Tile>> tilesSparseArray = mock(SparseArray.class);
        when(tilesSparseArray.size()).thenReturn(1);
        when(tilesSparseArray.get(anyInt())).thenReturn(mTiles);
        when(mTileGroup.getTileSections()).thenReturn(tilesSparseArray);

        mTileGridViewHolder = new TileGridViewHolder(mViewGroup, 4, 2);
        mTileGridViewHolder.bindDataSource(mTileGroup, mTileRenderer);
    }

    @Test
    public void testPreventRenderingOfMoreThanEightTiles() {
        mTileGridViewHolder.refreshData();

        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(mTileRenderer).renderTileSection(captor.capture(), any(ViewGroup.class), isNull());
        assertEquals(8, captor.getValue().size());
    }

}
