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
package com.zend.studio.internal.p2.touchpoint;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	public static final String ID = "com.zend.studio.p2.touchpoint"; //$NON-NLS-1$

	private static BundleContext context = null;

	@Override
	public void start(BundleContext ctx) throws Exception {
		Activator.context = ctx;
	}

	@Override
	public void stop(BundleContext ctx) throws Exception {
		Activator.context = null;
	}

	public static BundleContext getContext() {
		return Activator.context;
	}

}
