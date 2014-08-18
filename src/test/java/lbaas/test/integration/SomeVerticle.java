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
package lbaas.test.integration;

import org.vertx.java.platform.Verticle;
import org.vertx.testtools.VertxAssert;

public class SomeVerticle extends Verticle {

  @Override
public void start() {
    VertxAssert.initialize(vertx);

    // You can also assert from other verticles!!
    VertxAssert.assertEquals("foo", "foo");

    // And complete tests from other verticles
    VertxAssert.testComplete();
  }
}
