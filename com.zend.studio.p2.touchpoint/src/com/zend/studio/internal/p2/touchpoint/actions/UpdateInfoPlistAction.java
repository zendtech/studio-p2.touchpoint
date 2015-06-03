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
package com.zend.studio.internal.p2.touchpoint.actions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.Version;

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

		try {
			preserveOldLaunchersLibraries(parameters);
			updateInfoPlistFile(parameters);
		} catch (Exception e) {
			return new Status(Status.ERROR, PLUGIN_ID, e.getMessage(), e);
		}

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

	private void preserveOldLaunchersLibraries(Map<String, Object> parameters)
			throws IOException {
		String startupJarLocation = getStartupJarLocation();
		if (startupJarLocation != null) {
			File startupJar = new File(startupJarLocation);
			if (startupJar.exists()) {
				// protect from deletion
				removeWritePermissions(startupJar);
			} else {
				// already deleted - create symbolic link to the new library
				Path newStartupJarPath = Paths.get(startupJar.getParentFile()
						.getAbsolutePath(), getNewStartupJarName(parameters));
				Files.createSymbolicLink(Paths.get(startupJarLocation),
						newStartupJarPath);
			}
		}

		String launcherLibraryLocation = getLauncherLibraryLocation();
		if (launcherLibraryLocation != null) {
			File launcherLibrary = new File(launcherLibraryLocation);
			File bundleFolder = launcherLibrary.getParentFile();
			if (bundleFolder.exists()) {
				// protect from deletion
				removeWritePermissionsRecursively(bundleFolder);
			} else {
				// already deleted - create symbolic link to the new library
				bundleFolder.mkdir();
				Path newLauncherLibraryPath = Paths.get(
						bundleFolder.getAbsolutePath(),
						getNewLauncherLibraryName(parameters));
				Files.createSymbolicLink(Paths.get(launcherLibraryLocation),
						newLauncherLibraryPath);
			}
		}
	}

	private String getStartupJarLocation() throws IOException {
		return getCommandParameterValue("-startup");
	}

	private String getLauncherLibraryLocation() throws IOException {
		return getCommandParameterValue("--launcher.library");
	}

	private String getCommandParameterValue(String param) {
		String regex = String.format("(%s)\\n(.+)", param);
		Pattern pattern = Pattern.compile(regex);

		String commands = System.getProperty("eclipse.commands");
		Matcher matcher = pattern.matcher(commands);

		return (matcher.find()) ? matcher.group(2) : null;
	}

	private void removeWritePermissionsRecursively(File file)
			throws IOException {
		removeWritePermissions(file);
		if (file.isDirectory()) {
			for (File child : file.listFiles()) {
				removeWritePermissionsRecursively(child);
			}
		}
	}

	private void removeWritePermissions(File file) throws IOException {
		Path path = Paths.get(file.getAbsolutePath());
		Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(
				path, LinkOption.NOFOLLOW_LINKS);
		permissions.remove(PosixFilePermission.OWNER_WRITE);
		permissions.remove(PosixFilePermission.GROUP_WRITE);
		permissions.remove(PosixFilePermission.OTHERS_WRITE);
		Files.setPosixFilePermissions(path, permissions);
	}

	private String getNewStartupJarName(Map<String, Object> parameters) {
		String startupJarVersion = getBundleVersion(parameters,
				"org.eclipse.equinox.launcher");
		return String.format("org.eclipse.equinox.launcher_%s.jar",
				startupJarVersion);
	}

	private String getNewLauncherLibraryName(Map<String, Object> parameters) {
		String launcherLibraryVersion = getBundleVersion(parameters,
				"org.eclipse.equinox.launcher.cocoa.macosx.x86_64");

		if (launcherLibraryVersion.startsWith("1.1.200.")) {
			return "eclipse_1607.so";
		} else if (launcherLibraryVersion.startsWith("1.1.300.")) {
			return "eclipse_1612.so";
		}

		throw new IllegalStateException(
				"Cannot determine launcher library name for bundle with version "
						+ launcherLibraryVersion);
	}

	private IStatus updateInfoPlistFile(Map<String, Object> parameters)
			throws IOException {
		File infoPlistFile = getInfoPlistFile();
		if (infoPlistFile == null) {
			return new Status(Status.ERROR, PLUGIN_ID,
					"Info.plist file not found.");
		} else {
			Path path = Paths.get(infoPlistFile.getAbsolutePath());
			Charset charset = StandardCharsets.UTF_8;

			String content = new String(Files.readAllBytes(path), charset);

			String productNewVersion = getProductNewVersion(parameters);
			if (productNewVersion != null) {
				content = content
						.replaceAll(
								"(?<=<key>CFBundleGetInfoString<\\/key>\\W{1,100}<string>\\D{1,100})(\\d{1,2}\\.\\d{1,2}\\.\\d{1,2})(?=\\D{1,100}<\\/string>)",
								productNewVersion);
				content = content
						.replaceAll(
								"(?<=<key>CFBundleShortVersionString<\\/key>\\W{1,100}<string>)(.*)(?=<\\/string>)",
								productNewVersion);
				content = content
						.replaceAll(
								"(?<=<key>CFBundleVersion<\\/key>\\W{1,100}<string>)(.*)(?=<\\/string>)",
								productNewVersion);
			}

			String startupJarVersion = getBundleVersion(parameters,
					"org.eclipse.equinox.launcher");
			if (startupJarVersion != null) {
				content = content
						.replaceAll(
								"(?<=<string>-startup<\\/string>\\W{1,100}.{1,100}\\/org\\.eclipse\\.equinox\\.launcher_)(.+?)(?=\\.jar<\\/string>)",
								startupJarVersion);
			}

			String launcherLibraryVersion = getBundleVersion(parameters,
					"org.eclipse.equinox.launcher.cocoa.macosx.x86_64");
			if (launcherLibraryVersion != null) {
				content = content
						.replaceAll(
								"(?<=<string>--launcher.library<\\/string>\\W{1,100}.{1,100}\\/org\\.eclipse\\.equinox\\.launcher\\.cocoa\\.macosx\\.x86_64_)(.+?)(?=<\\/string>)",
								launcherLibraryVersion);
			}

			Files.write(path, content.getBytes(charset));
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

	private String getBundleVersion(Map<String, Object> parameters,
			String symbolicName) {
		IProfile profile = (IProfile) parameters.get("profile");
		IQuery<IInstallableUnit> query = QueryUtil.createMatchQuery(
				"this.id == $0", symbolicName);
		IQueryResult<IInstallableUnit> result = profile.available(query, null);

		if (!result.isEmpty()) {
			return result.iterator().next().getVersion().toString();
		}

		return null;
	}

	private String getProductNewVersion(Map<String, Object> parameters) {
		IInstallableUnit iu = (IInstallableUnit) parameters.get("iu");

		if (iu != null) {
			Version version = new Version(iu.getVersion().toString());
			return String.format("%s.%s.%s", version.getMajor(),
					version.getMinor(), version.getMicro());
		}

		return null;
	}

}