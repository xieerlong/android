/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.idea.gradle.project.sync.errors;

import com.android.tools.idea.gradle.project.sync.hyperlink.InstallNdkHyperlink;
import com.android.tools.idea.gradle.project.sync.hyperlink.SetNdkDirHyperlink;
import com.android.tools.idea.gradle.util.LocalProperties;
import com.android.tools.idea.project.hyperlink.NotificationHyperlink;
import com.android.tools.idea.sdk.IdeSdks;
import com.intellij.openapi.project.Project;
import java.io.File;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static com.intellij.openapi.util.text.StringUtil.isNotEmpty;

public class MissingNdkErrorHandler extends BaseSyncErrorHandler {
  @Nullable
  @Override
  protected String findErrorMessage(@NotNull Throwable rootCause, @NotNull Project project) {
    String message = rootCause.getMessage();
    if (isNotEmpty(message) && matchesNdkNotConfigured(getFirstLineMessage(message))) {
      updateUsageTracker(project);
      return "NDK not configured.";
    }
    else if (isNotEmpty(message) && matchesTriedInstall(message)) {
      updateUsageTracker(project);
      return message;
    }
    return null;
  }

  /**
   * @param errorMessage first line of the error message
   * @return whether or not this error message indicates that the NDK was found not to be configured
   */
  private static boolean matchesNdkNotConfigured(@NotNull String errorMessage) {
    return errorMessage.startsWith("NDK not configured.") || errorMessage.startsWith("NDK location not found.");
  }

  /**
   * @param errorMessage the error message
   * @return whether the given error message was generated by the Android Gradle Plugin failing to download the ndk-bundle package.
   */
  private static boolean matchesTriedInstall(@NotNull String errorMessage) {
    return (errorMessage.startsWith("Failed to install the following Android SDK packages as some licences have not been accepted.") ||
            errorMessage.startsWith("Failed to install the following SDK components:")) && errorMessage.contains("ndk-bundle NDK");
  }

  @Override
  @NotNull
  protected List<NotificationHyperlink> getQuickFixHyperlinks(@NotNull Project project, @NotNull String text) {
    File ndkPath = IdeSdks.getInstance().getAndroidNdkPath();
    if (ndkPath != null) {
      // There is an existing NDK on disk. Probably an older gradle plugin was unable to locate it
      // because it is in the NDK side-by-side folder. Let's offer to set ndk.dir to that value.
      // go/ndk-sxs
      File localSettingsNdkDir;
      try {
        localSettingsNdkDir = getLocalPropertiesNdkDir(project);
      }
      catch (IOException e) {
        // Couldn't access local.properties. No hyperlink because we likely wouldn't be able to
        // write to local.properties anyway.
        return Collections.emptyList();
      }
      if (localSettingsNdkDir == null) {
        return Collections.singletonList(new SetNdkDirHyperlink(
          ndkPath,
          String.format("Set ndk.dir in local.properties to %s", ndkPath)));
      } else {
        // If the paths are identical then replacing won't help.
        // This only checks for exactly the same path (not collapsing .., etc)
        if (ndkPath.getPath() == localSettingsNdkDir.getPath()) {
          return Collections.emptyList();
        } else {
          return Collections.singletonList(new SetNdkDirHyperlink(
            ndkPath,
            String.format("Replace ndk.dir in local.properties with %s", ndkPath)));
        }
      }
    }
    return Collections.singletonList(new InstallNdkHyperlink());
  }


  @Nullable
  protected File getLocalPropertiesNdkDir(Project project) throws IOException {
    return new LocalProperties(project).getAndroidNdkPath();
  }
}
