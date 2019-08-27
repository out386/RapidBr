package com.out386.rapidbr.services.overlay;

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

import android.content.SharedPreferences;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import static com.out386.rapidbr.services.overlay.BrightnessOverlayService.DEF_OVERLAY_BUTTON_COLOUR;
import static com.out386.rapidbr.services.overlay.BrightnessOverlayService.KEY_BR_ICON_COLOUR;
import static com.out386.rapidbr.services.overlay.BrightnessOverlayService.KEY_SCREEN_DIM_AMOUNT;
import static com.out386.rapidbr.services.overlay.BrightnessOverlayService.MSG_TOGGLE_OVERLAY;

public class ServiceLauncher {
    public static boolean toggleBrightnessService(Messenger serviceMessenger,
                                                  SharedPreferences prefs) {
        int overlayButtonColour = prefs.getInt(KEY_BR_ICON_COLOUR, DEF_OVERLAY_BUTTON_COLOUR);
        int screenDimAmount = prefs.getInt(KEY_SCREEN_DIM_AMOUNT, 0);
        return sendMessageToBrightnessService(
                serviceMessenger, MSG_TOGGLE_OVERLAY, overlayButtonColour, screenDimAmount, false);

    }

    /**
     * Sends a {@link android.os.Message} to the provided {@link android.os.Messenger}. If it
     * returns false, it might be an indication that the provided {@code serviceMessenger} is no
     * longer valid.
     *
     * @param serviceMessenger The {@link Messenger} to send the {@link Message} to
     * @param what             The {@code what} parameter of the {@link Message}
     * @param arg1             The {@code arg1} parameter of the {@link Message}
     * @param arg2             The {@code arg2} parameter of the {@link Message}
     * @return True if the {@link Message} could be sent, false otherwise
     */
    public static boolean sendMessageToBrightnessService(Messenger serviceMessenger,
                                                         int what, int arg1, int arg2) {
        return sendMessageToBrightnessService(serviceMessenger, what, arg1, arg2, true);
    }

    private static boolean sendMessageToBrightnessService(
            Messenger serviceMessenger, int what, int arg1, int arg2, boolean directCall) {
        if (directCall && what == MSG_TOGGLE_OVERLAY)
            throw new IllegalArgumentException(
                    "Use toggleBrightnessService to toggle the service instead of sending the Message directly");

        if (serviceMessenger != null) {
            try {
                serviceMessenger.send(Message.obtain(
                        null, what, arg1, arg2));
                return true;
            } catch (RemoteException e) {
                return false;
            }
        }
        return false;
    }
}
