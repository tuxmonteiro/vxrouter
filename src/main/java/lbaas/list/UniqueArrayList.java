/*
 * Copyright (c) 2014 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
 * KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
 * PARTICULAR PURPOSE.
 */
package lbaas.list;

import java.util.ArrayList;

public class UniqueArrayList<T> extends ArrayList<T> {

    private static final long serialVersionUID = 250697580015525312L;

    public UniqueArrayList() {
        super();
    }

    public UniqueArrayList(ArrayList<T> aList) {
        this();
        if (aList!=null) {
            for (T object: aList) {
                this.add(object);
            }
        }
    }

    @Override
    public boolean add(T object) {
        if (object!=null && !contains(object)) {
            return super.add(object);
        }
        return false;
    }
}
