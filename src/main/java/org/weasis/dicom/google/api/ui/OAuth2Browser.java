// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.weasis.dicom.google.api.ui;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.IOException;

import java.net.URI;

import java.awt.Toolkit;
import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.awt.datatransfer.StringSelection;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.weasis.dicom.google.explorer.Messages;

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.util.Preconditions;

/**
 * OAuth2 authorization browser that copies the authorization URL to the clipboard and shows the
 * notification dialog if there is no default browser configured or an error occurred while opening
 * the default browser.
 * 
 * @author Mikhail Ukhlin
 * @see AuthorizationCodeInstalledApp
 */
public class OAuth2Browser implements AuthorizationCodeInstalledApp.Browser {
  
  private static final Logger LOGGER =
      Logger.getLogger(OAuth2Browser.class.getName());

  /**
   * Single instance of this browser.
   */
  public static final AuthorizationCodeInstalledApp.Browser INSTANCE = new OAuth2Browser();
  
  /**
   * Do not allow more than one instance.
   */
  private OAuth2Browser() {
    super();
  }
  
  /**
   * Opens a browser at the given URL using {@link Desktop} if available, or alternatively copies
   * authorization URL to clipboard and shows notification dialog.
   * 
   * @param url URL to browse.
   * @throws IOException if an IO error occurred.
   * @see AuthorizationCodeInstalledApp#browse(String)
   */
  @Override
  public void browse(String url) throws IOException {
    Preconditions.checkNotNull(url);
    // Ask user to open in their browser using copy-paste
    System.out.println("Please open the following address in your browser:");
    System.out.println("  " + url);
    // Attempt to open it in the browser
    try {
      if (Desktop.isDesktopSupported()) {
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Action.BROWSE)) {
          System.out.println("Attempting to open that address in the default browser now...");
          desktop.browse(URI.create(url));
        } else {
          showNotification(url);
        }
      } else {
        showNotification(url);
      }
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Unable to open browser", e);
      showNotification(url);
    } catch (InternalError e) {
      // A bug in a JRE can cause Desktop.isDesktopSupported() to throw an
      // InternalError rather than returning false. The error reads,
      // "Can't connect to X11 window server using ':0.0' as the value of the
      // DISPLAY variable." The exact error message may vary slightly.
      LOGGER.log(Level.WARNING, "Unable to open browser", e);
      showNotification(url);
    }
  }

  /**
   * Copies authorization URL to clipboard and shows notification dialog.
   * 
   * @param url URL to browse.
   */
  private static void showNotification(String url) {
    // Copy authorization URL to clipboard
    final StringSelection selection = new StringSelection(url);
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    // Show notification dialog
    SwingUtilities.invokeLater(() -> {
      JOptionPane.showMessageDialog(null,
          Messages.getString("GoogleAPIClient.open_browser_message"));
    });
  }
  
}
