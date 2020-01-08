/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.tests.gui.framework.heapassertions.bleak

class BleakOptions private constructor(var iterations: Int, var checks: List<BleakCheck<*,*>>) {
  constructor() : this(DEFAULT_ITERATION_COUNT, listOf())

  fun iterations(i: Int): BleakOptions {
    iterations = i
    return this
  }

  fun withCheck(check: BleakCheck<*,*>): BleakOptions {
    checks += check
    return this
  }

  companion object {
    val DEFAULT_ITERATION_COUNT = 3
  }

}