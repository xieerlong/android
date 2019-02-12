/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tools.idea.tests.gui.cuj

import com.android.tools.idea.gradle.util.BuildMode
import com.android.tools.idea.tests.gui.framework.GuiTestRule
import com.android.tools.idea.tests.gui.framework.GuiTests
import com.android.tools.idea.tests.gui.framework.RunIn
import com.android.tools.idea.tests.gui.framework.TestGroup
import com.android.tools.idea.tests.gui.framework.fixture.ChooseResourceDialogFixture
import com.android.tools.idea.tests.gui.framework.fixture.CreateResourceFileDialogFixture
import com.android.tools.idea.tests.gui.framework.fixture.ResourceExplorerFixture
import com.android.tools.idea.tests.gui.framework.fixture.assetstudio.AssetStudioWizardFixture
import com.android.tools.idea.tests.gui.framework.fixture.newpsd.openPsd
import com.android.tools.idea.tests.gui.framework.fixture.newpsd.selectDependenciesConfigurable
import com.intellij.testGuiFramework.framework.GuiTestRemoteRunner
import org.fest.swing.core.MouseButton
import java.io.IOException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.awt.event.KeyEvent

/**
 * UI test for "Adding new QR scanning feature to existing app" CUJ
 */
@RunWith(GuiTestRemoteRunner::class)
class QrScanningCujTest {

  @Rule
  @JvmField
  val guiTest = GuiTestRule()

  @Test
  @RunIn(TestGroup.UNRELIABLE)
  @Throws(IOException::class)
  fun qrScanningCuj() {
    // Import VotingApp project
    val ide = guiTest.importProjectAndWaitForProjectSyncToFinish("VotingApp")
      .waitAndInvokeMenuPath("Build", "Make Project")
      .waitForBuildToFinish(BuildMode.ASSEMBLE)

    // Add constraint layout dependency from the PSD
    ide.openPsd()
      .run {
        selectDependenciesConfigurable().run {
          findModuleSelector().selectModule("app")
          findDependenciesPanel().clickAddLibraryDependency().run {
            findSearchQueryTextBox().setText("constraint-layout")
            findSearchButton().click()
            findVersionsView(true).run {
              cell("1.1.3").click()
            }
            clickOk()
          }
        }
        clickOk()
      }
      .waitForGradleProjectSyncToFinish()

    // Create new layout file
    ide.run {
      projectView
        .selectAndroidPane()
        .clickPath(MouseButton.RIGHT_BUTTON, "app", "res", "layout")
        .openFromMenu({ CreateResourceFileDialogFixture.find(it) }, arrayOf("New", "Layout resource file"))
        .setFilename("activity_main_qr_scan")
        .setRootElement("android.support.constraint.ConstraintLayout").clickOk()
      invokeMenuPath("Build", "Make Project")
    }
      .closeBuildPanel()
      .closeProjectPanel()

    // Build layout from UI in the layout editor
    ide.editor
      .getLayoutEditor(false)
      .showOnlyDesignView().run {
        dragComponentToSurface("Common", "TextView")
          .findView("TextView", 0)
          .createConstraintFromTopToTopOfLayout()
          .createConstraintFromLeftToLeftOfLayout()
          .createConstraintFromRightToRightOfLayout()
        dragComponentToSurface("Common", "Button")
          .findView("Button", 0)
          .createConstraintFromBottomToBottomOfLayout()
          .createConstraintFromLeftToLeftOfLayout()
          .createConstraintFromRightToRightOfLayout()
      }

    // Import SVG resource using the Resource Explorer
    ide.openResourceManager()
      .run {
        ResourceExplorerFixture.find(robot())
          .clickAddButton()
        openFromMenu({ AssetStudioWizardFixture.find(it) }, arrayOf("Vector Asset"))
          .useLocalFile(GuiTests.getTestDataDir()!!.toString() + "/VotingApp/ic_qr_code.svg")
          .setName("ic_qr_code")
          .clickNext()
          .clickFinish()
      }
      .closeResourceManager()

    // Add an image view to the layout
    ide.editor
      .getLayoutEditor(false).run {
        dragComponentToSurface("Common", "ImageView")
        ChooseResourceDialogFixture.find(robot()).run {
          searchField.setText("ic_qr")
            .pressAndReleaseKeys(KeyEvent.VK_DOWN)
          clickOK()
        }
        findView("ImageView", 0)
          .createConstraintFromLeftToLeftOfLayout()
          .createConstraintFromRightToRightOfLayout()
        configToolbar.chooseDevice("Pixel 3 XL")
          .chooseLayoutVariant("Landscape")
          .chooseDevice("Pixel 2")
          .chooseLayoutVariant("Portrait")
      }

    // Edit MainActivity.java
    ide.editor
      .open("app/src/main/java/com/src/adux/votingapp/MainActivity.java")
      .select("(activity_main_edit_text)")
      .typeText("activity_main_qr")
      .autoCompleteWindow
      .item("activity_main_qr_scan")
      .doubleClick()

    // Check that the project compiles
    ide.waitAndInvokeMenuPath("Build", "Rebuild Project")
      .waitForBuildToFinish(BuildMode.REBUILD)

    // Add dependency by typing it in build.gradle file
    ide.editor
      .open("app/build.gradle")
      .moveBetween("dependencies {\n", "")
      .typeText("    implementation 'com.google.android.gms:play-services-vision:+'\n")
      .ideFrame
      .requestProjectSync()
      .waitForGradleProjectSyncToFinish()
      .closeBuildPanel()

    // Create a new layout file from the File > New > Android Resource File menu
    ide.openFromMenu({ CreateResourceFileDialogFixture.find(it) }, arrayOf("File", "New", "Android Resource File"))
      .setType("layout")
      .setRootElement("LinearLayout")
      .setFilename("actions_main")
      .clickOk()

    // Create a new vector drawable from an icon available in the Vector Asset Wizard
    ide.openFromMenu({ AssetStudioWizardFixture.find(it) }, arrayOf("File", "New", "Vector Asset")).run {
      switchToClipArt()
      chooseIcon()
        .filterByNameAndSelect("flash on")
        .clickOk()
      setName("ic_flash_on_white_24dp")
      setColor("FFFFFF")
      clickNext()
      clickFinish()
    }

    // Add an ImageView by dragging it from the palette in the preview, and select ic_flash_on_white_24dp as the source
    ide.editor
      .getLayoutPreview(false).run {
        dragComponentToSurface("Common", "ImageView")
        ChooseResourceDialogFixture.find(robot()).run {
          searchField.setText("ic_flash_on_white")
            .pressAndReleaseKeys(KeyEvent.VK_DOWN)
          clickOK()
        }
      }
  }
}
