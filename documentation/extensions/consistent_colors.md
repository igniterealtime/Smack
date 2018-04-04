Consistent Colors
=================

[Back](index.md)

Since XMPP can be used on multiple platforms at the same time,
it might be a good idea to render given Strings like nicknames in the same
color on all platforms to provide a consistent user experience.

The utility class `ConsistentColor` allows the generation of colors to a given
string following the specification of [XEP-0392](https://xmpp.org/extensions/xep-0392.html).

## Usage
To generate a consistent color for a given string, call
```
float[] rgb = ConsistentColor.RGBFrom(input);
```
The resulting float array contains values for RGB in the range of 0 to 1.

## Color Deficiency Corrections
Some users might suffer from color vision deficiencies. To compensate those deficiencies,
the API allows for color correction. The color correction mode is a static value, which can be changed at any time.

To correct colors for users with red-green color deficiency use the following code:
```
ConsistentColor.activateRedGreenBlindnessCorrection();
```

For color correction for users with blue-blindness, call
```
ConsistentColor.activateBlueBlindnessCorrection();
```

To deactivate color vision deficiency correction, call
```
ConsistentColor.deactivateDeficiencyCorrection();
```