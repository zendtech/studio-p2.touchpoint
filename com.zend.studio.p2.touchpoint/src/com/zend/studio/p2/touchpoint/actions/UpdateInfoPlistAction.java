/*******************************************************************************
 * Copyright (c) 2015 Zend Technologies Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Kaloyan Raev - initial API and implementation
 *******************************************************************************/
package com.zend.studio.p2.touchpoint.actions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.osgi.service.datalocation.Location;

/**
 * Updates the Info.plist file of the Mac application with the correct paths of
 * the newly installed Equinox launcher bundles.
 */
public class UpdateInfoPlistAction extends ProvisioningAction {

	private static final String PLUGIN_ID = "com.zend.studio.p2.touchpoint";

	@Override
	public IStatus execute(Map<String, Object> parameters) {
		// Execute only on Mac OS X
		if (!isMac())
			return Status.OK_STATUS;

		updateSystemProperties();
		updateInfoPlistFile();

		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo(Map<String, Object> parameters) {
		// Undo not supported
		return Status.OK_STATUS;
	}

	private boolean isMac() {
		return Platform.OS_MACOSX.equals(Platform.getOS());
	}

	private void updateSystemProperties() {
		String eclipseCommands = System.getProperty("eclipse.commands");
		if (eclipseCommands != null) {
			eclipseCommands = eclipseCommands.replaceAll(
					"\\/org\\.eclipse\\.equinox\\.launcher_.+?(?=\n)",
					"/org.eclipse.equinox.launcher_1.3.0.v20140415-2008.jar");
			eclipseCommands = eclipseCommands
					.replaceAll(
							"\\/org\\.eclipse\\.equinox\\.launcher\\.cocoa\\.macosx\\.x86_64_.+?(?=\n)",
							"/org.eclipse.equinox.launcher.cocoa.macosx.x86_64_1.1.200.v20150204-1316/eclipse_1607.so");
			System.setProperty("eclipse.commands", eclipseCommands);
		}
	}

	private IStatus updateInfoPlistFile() {
		try {
			File infoPlistFile = getInfoPlistFile();
			if (infoPlistFile == null) {
				return new Status(Status.ERROR, PLUGIN_ID,
						"Info.plist file not found.");
			} else {
				Path path = Paths.get(infoPlistFile.getAbsolutePath());
				Charset charset = StandardCharsets.UTF_8;

				String content = new String(Files.readAllBytes(path), charset);
				content = content
						.replaceAll(
								"\\/org\\.eclipse\\.equinox\\.launcher_.+?(?=<)",
								"/org.eclipse.equinox.launcher_1.3.0.v20140415-2008.jar");
				content = content
						.replaceAll(
								"\\/org\\.eclipse\\.equinox\\.launcher\\.cocoa\\.macosx\\.x86_64_.+?(?=<)",
								"/org.eclipse.equinox.launcher.cocoa.macosx.x86_64_1.1.200.v20150204-1316");
				Files.write(path, content.getBytes(charset));
			}
		} catch (IOException e) {
			return new Status(Status.ERROR, PLUGIN_ID, e.getMessage(), e);
		}

		return Status.OK_STATUS;
	}

	private File getInfoPlistFile() {
		File contents = getContentsFolder();
		return (contents == null) ? null : new File(contents, "Info.plist");
	}

	private File getContentsFolder() {
		Location installLocation = Platform.getInstallLocation();
		if (installLocation != null) {
			File installLocationFolder = new File(installLocation.getURL()
					.getFile());
			File contents = installLocationFolder;
			while (contents != null && !"Contents".equals(contents.getName())) {
				contents = contents.getParentFile();
			}
			return contents;
		}

		return null;
	}

}
