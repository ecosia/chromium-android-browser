/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.ecosia.ntp;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.app.ChromeActivity;
import org.chromium.chrome.browser.suggestions.SiteSuggestion;
import org.chromium.chrome.browser.suggestions.tile.Tile;
import org.chromium.chrome.browser.suggestions.tile.TileGroup;
import org.chromium.content_public.browser.LoadUrlParams;
import org.chromium.ui.mojom.WindowOpenDisposition;
import java.util.ArrayList;

public class EcosiaNewTabPageRecyclerViewAdapter extends RecyclerView.Adapter<EcosiaNewTabPageRecyclerViewAdapter.EcosiaSitesViewHolder> {

    private Context mContext;
    private ArrayList<Drawable> mImgList;
    private ArrayList<String> mTextList;
    private ArrayList<String> mURLList;
    private ArrayList<SiteSuggestion> mSuggestionList;
    private TileGroup mTileGroup;
    private TileGroup.Delegate mTileGroupDelegate;

    public EcosiaNewTabPageRecyclerViewAdapter(Context context, ArrayList<Drawable> img, ArrayList<String> text,
                                               ArrayList<String> uRL, ArrayList<SiteSuggestion> suggestion) {
        mContext = context;
        mImgList = img;
        mTextList = text;
        mURLList = uRL;
        mSuggestionList = suggestion;
    }

    @NonNull
    @Override
    public EcosiaSitesViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int pos) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.ecosia_fav_tiles, viewGroup, false);
        EcosiaSitesViewHolder viewHolder = new EcosiaSitesViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull EcosiaSitesViewHolder ecosiaSitesViewHolder, int pos) {
        ecosiaSitesViewHolder.mImage.setImageDrawable(mImgList.get(pos));
        ecosiaSitesViewHolder.mText.setText(mTextList.get(pos));

        // Added OnClick for both IconView and Text as its parent onclick action is not reflecting on
        // child elements, also position is needed for the click , hence implementing it here
        ecosiaSitesViewHolder.mIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openURL(mURLList.get(pos));
            }
        });
        ecosiaSitesViewHolder.mText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openURL(mURLList.get(pos));
            }
        });

        ecosiaSitesViewHolder.mIcon.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //Open Menu to choose an action on Long press

                PopupMenu popup = new PopupMenu(v.getContext(), v, Gravity.CENTER);
                popup.inflate(R.menu.popup_top_tiles);
                popup.show();

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();
                        Tile tile = findTile(mSuggestionList.get(pos));
                        if (tile == null)
                            return false;
                        int wndOpenDisposition = WindowOpenDisposition.UNKNOWN ;
                        if (id == R.id.remove_tile) {
                            mTileGroup.setPendingRemovalUrl(mSuggestionList.get(pos).url);
                            mTileGroupDelegate.removeMostVisitedItem(tile, url -> mTileGroup.setPendingInsertionUrl(url));
                            return true;
                        }
                        if (id == R.id.open_new_tab) {
                            wndOpenDisposition = WindowOpenDisposition.NEW_FOREGROUND_TAB;
                        } else if (id == R.id.open_incog_tab) {
                            wndOpenDisposition = WindowOpenDisposition.OFF_THE_RECORD;
                        } else { // To download link
                            wndOpenDisposition = WindowOpenDisposition.SAVE_TO_DISK;
                        }
                        mTileGroupDelegate.openMostVisitedItem(wndOpenDisposition, tile);
                        return true;
                    }
                });

                return true;
            }
        });

    }

     public void openURL(String url) {
        LoadUrlParams params = new LoadUrlParams(url);
        ((ChromeActivity) mContext).getActivityTab().loadUrl(params);
    }

    @Nullable
    private Tile findTile(SiteSuggestion suggestion) {
        if (mTileGroup.getTileSections().get(suggestion.sectionType) == null) return null;
        for (Tile tile : mTileGroup.getTileSections().get(suggestion.sectionType)) {
            if (tile.getData().equals(suggestion)) return tile;
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return mImgList.size();
    }

    public void setData(ArrayList<Drawable> img, ArrayList<String> text, ArrayList<String> url, ArrayList<SiteSuggestion> suggestions) {
        mImgList = img;
        mTextList = text;
        mURLList = url;
        mSuggestionList = suggestions;
    }

    public void setTileGroupData(TileGroup tileGroup, TileGroup.Delegate tileGroupDelegate){
        mTileGroup = tileGroup;
        mTileGroupDelegate = tileGroupDelegate;
    }

    public void setIcon(Drawable icon, String url) {
        int index = mURLList.indexOf(url);
        if(index > -1) {
            mImgList.set(index, icon);
        }
    }


    public class EcosiaSitesViewHolder extends RecyclerView.ViewHolder {
        ImageView mImage;
        TextView mText;
        View mIcon;

        public EcosiaSitesViewHolder(@NonNull View itemView) {
            super(itemView);
            mImage = itemView.findViewById(R.id.site_img);
            mText = itemView.findViewById(R.id.site_text);
            mIcon = itemView.findViewById(R.id.tile_view_border);
        }
    }

}
