Zend Studio p2 Touchpoint Actions
=================================

This project provides p2 touchpoint actions to extend the capabilities of the update manager for Zend Studio.

updateInfoPlist
---------------

Updates the Info.plist file of the Mac application with the new version of Zend Studio and the correct paths of the newly installed Equinox launcher bundles.

If the currently used Equinox launcher bundles are still available then the write permissions are removed to avoid the update manager deleting them. If the bundles are already deleted then a symbolic link is created in their place pointing to the newly installed launcher bundles.

All the above is done to prevent Zend Studio to fail starting after upgrade, because it makes an attempt to start using old and already deleted launcher bundles.

### Usage

Add the below instructions in the p2.inf file of a plugin fragment that executes only on Mac OS X.

- Make sure the touchpoint action is installed before the product is installed/updated:
```
metaRequirements.0.namespace=com.zend.studio.p2.touchpoint
metaRequirements.0.name=updateInfoPlist
metaRequirements.0.range=[1.0.0, 2.0.0)
```

- Execute the touchpoint action during the Configure phase:
```
instructions.configure = com.zend.studio.p2.touchpoint.updateInfoPlist();
```
