package com.out386.rapidbr.utils;

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

public class BoolUtils {
    public static int packBool(boolean bool1, boolean bool2) {
        int value = bool1 ? 0b1 : 0b0;
        value <<= 1;
        if (bool2)
            value |= 0b1;
        return value;
    }

    public static boolean[] unpackBool(int integer) {
        boolean[] value = new boolean[2];
        int mask = 0b1;
        value[1] = (integer & mask) == 1;
        mask <<= 1;
        int t = integer & mask;
        t >>= 1;
        value[0] = t == 1;
        return value;
    }
}
