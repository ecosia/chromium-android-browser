/*
 * Copyright (C) 2023 Ecosia Android App source
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.ecosia.firstrun;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

final class EcosiaFirstRunAdapter extends FragmentPagerAdapter {

    private final List<Fragment> mPages = new ArrayList<>();

    EcosiaFirstRunAdapter(final FragmentManager fragmentManager) {
        super(fragmentManager);
        createPages();
    }

    private void createPages() {
        if (mPages.isEmpty()) {
            final EcosiaFirstRunPageFragment pageFragmentOne = new EcosiaFirstRunPageFragment(EcosiaFirstRunPageFragment.Page.PROOF_1);
            final EcosiaFirstRunPageFragment pageFragmentTwo = new EcosiaFirstRunPageFragment(EcosiaFirstRunPageFragment.Page.PROOF_2);
            final EcosiaFirstRunPageFragment pageFragmentThree = new EcosiaFirstRunPageFragment(EcosiaFirstRunPageFragment.Page.PROOF_3);
            final EcosiaFirstRunPageFragment pageFragmentFour = new EcosiaFirstRunPageFragment(EcosiaFirstRunPageFragment.Page.PROOF_4);
            mPages.add(0, pageFragmentOne);
            mPages.add(1, pageFragmentTwo);
            mPages.add(2, pageFragmentThree);
            mPages.add(3, pageFragmentFour);
        }
    }

    @NonNull
    @Override
    public Fragment getItem(final int pos) {
        return mPages.get(pos);
    }

    @Override
    public int getCount() {
        return mPages.size();
    }
}
