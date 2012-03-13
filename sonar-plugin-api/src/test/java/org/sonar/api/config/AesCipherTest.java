/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.config;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.sonar.api.CoreProperties;

import java.io.File;
import java.net.URL;
import java.security.Key;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AesCipherTest {

  @Test
  public void generateRandomSecretKey() {
    AesCipher cipher = new AesCipher(new Settings());

    String key = cipher.generateRandomSecretKey();

    assertThat(StringUtils.isNotBlank(key), is(true));
    assertThat(Base64.isArrayByteBase64(key.getBytes()), is(true));
  }

  @Test
  public void encrypt() throws Exception {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.ENCRYPTION_PATH_TO_SECRET_KEY, pathToSecretKey());
    AesCipher cipher = new AesCipher(settings);

    String encryptedText = cipher.encrypt("sonar");
    System.out.println(encryptedText);
    assertThat(StringUtils.isNotBlank(encryptedText), is(true));
    assertThat(Base64.isArrayByteBase64(encryptedText.getBytes()), is(true));
  }

  @Test
  public void decrypt() throws Exception {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.ENCRYPTION_PATH_TO_SECRET_KEY, pathToSecretKey());
    AesCipher cipher = new AesCipher(settings);

    // the following value has been encrypted with the key /org/sonar/api/config/AesCipherTest/aes_secret_key.txt
    String clearText = cipher.decrypt("9mx5Zq4JVyjeChTcVjEide4kWCwusFl7P2dSVXtg9IY=");

    assertThat(clearText, is("this is a secret"));
  }

  @Test
  public void encryptThenDecrypt() throws Exception {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.ENCRYPTION_PATH_TO_SECRET_KEY, pathToSecretKey());
    AesCipher cipher = new AesCipher(settings);

    assertThat(cipher.decrypt(cipher.encrypt("foo")), is("foo"));
  }

  @Test
  public void loadSecretKeyFromFile() throws Exception {
    AesCipher cipher = new AesCipher(new Settings());
    Key secretKey = cipher.loadSecretFileFromFile(pathToSecretKey());
    assertThat(secretKey.getAlgorithm(), is("AES"));
    assertThat(secretKey.getEncoded().length, greaterThan(10));
  }

  private String pathToSecretKey() throws Exception {
    URL resource = getClass().getResource("/org/sonar/api/config/AesCipherTest/aes_secret_key.txt");
    return new File(resource.toURI()).getCanonicalPath();
  }
}
