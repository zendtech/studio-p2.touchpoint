Zend Studio p2 Touchpoint Actions
=================================

This project provides p2 touchpoint actions to extend the capabilities of the update manager for Zend Studio.

UpdateInfoPlist
---------------

Updates the Info.plist file of the Mac application with the correct paths of the newly installed Equinox launcher bundles.

### Usage

Add the below instructions in the p2.inf file of the RCP product.

Make sure the touchpoint action is installed before the product is installed/updated:
```
metaRequirements.8.namespace=org.eclipse.equinox.p2.iu
metaRequirements.8.name=com.zend.studio.p2.touchpoint.feature.feature.group
metaRequirements.8.range=[1.0.0, 2.0.0)
```

Execute the touchpoint action during the Configure phase:
```
instructions.configure = \
com.zend.studio.p2.touchpoint.UpdateInfoPlist();
```
