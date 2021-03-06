/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.plugins.ws;

import com.google.common.base.Optional;
import java.util.Objects;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.plugins.PluginDownloader;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.server.user.UserSession;
import org.sonar.updatecenter.common.PluginUpdate;
import org.sonar.updatecenter.common.UpdateCenter;

import static java.lang.String.format;
import static org.sonar.server.plugins.edition.EditionBundledPlugins.isEditionBundled;

/**
 * Implementation of the {@code install} action for the Plugins WebService.
 */
public class InstallAction implements PluginsWsAction {

  private static final String PARAM_KEY = "key";

  private final UpdateCenterMatrixFactory updateCenterFactory;
  private final PluginDownloader pluginDownloader;
  private final UserSession userSession;

  public InstallAction(UpdateCenterMatrixFactory updateCenterFactory,
    PluginDownloader pluginDownloader, UserSession userSession) {
    this.updateCenterFactory = updateCenterFactory;
    this.pluginDownloader = pluginDownloader;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("install")
      .setPost(true)
      .setSince("5.2")
      .setDescription("Installs the latest version of a plugin specified by its key." +
        "<br/>" +
        "Plugin information is retrieved from Update Center." +
        "<br/>" +
        "Requires user to be authenticated with Administer System permissions")
      .setHandler(this);

    action.createParam(PARAM_KEY).setRequired(true)
      .setDescription("The key identifying the plugin to install");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();

    String key = request.mandatoryParam(PARAM_KEY);
    PluginUpdate pluginUpdate = findAvailablePluginByKey(key);
    pluginDownloader.download(key, pluginUpdate.getRelease().getVersion());
    response.noContent();
  }

  private PluginUpdate findAvailablePluginByKey(String key) {
    PluginUpdate pluginUpdate = null;

    Optional<UpdateCenter> updateCenter = updateCenterFactory.getUpdateCenter(false);
    if (updateCenter.isPresent()) {
      pluginUpdate = updateCenter.get().findAvailablePlugins()
        .stream()
        .filter(Objects::nonNull)
        .filter(u -> key.equals(u.getPlugin().getKey()))
        .findFirst()
        .orElse(null);
    }

    if (pluginUpdate == null) {
      throw new IllegalArgumentException(
        format("No plugin with key '%s' or plugin '%s' is already installed in latest version", key, key));
    }
    if (isEditionBundled(pluginUpdate.getPlugin())) {
      throw new IllegalArgumentException(format(
        "SonarSource commercial plugin with key '%s' can only be installed as part of a SonarSource edition",
        pluginUpdate.getPlugin().getKey()));
    }

    return pluginUpdate;
  }
}
