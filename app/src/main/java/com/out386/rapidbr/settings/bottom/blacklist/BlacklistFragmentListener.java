package com.out386.rapidbr.settings.bottom.blacklist;

/*
 * Copyright (C) 2019 Ritayan Chakraborty <ritayanout@gmail.com>
 *
 * This file is part of RapidBr
 *
 * RapidBr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RapidBr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RapidBr.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

import com.out386.rapidbr.settings.bottom.blacklist.picker.BlacklistPickerItem;

public interface BlacklistFragmentListener {
    /**
     * Called when an app was picked in {@link com.out386.rapidbr.settings.bottom.blacklist.picker.BlacklistPickerFragment}
     *
     * @param item The item representing the app that was just picked.
     */
    void onAppPicked(BlacklistPickerItem item);

    /**
     * Called when the internal list has to be saved because the fragment is being removed.
     */
    void onSaveNeeded();
}
