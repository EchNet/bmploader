//
// ColorName.java
//

package com.codespinner.ecce;

import java.awt.*;
import java.util.*;

public class ColorName
{
    /**
     * String to Color.
     */
    public static Color decode (String str)
	throws NumberFormatException, ColorNameException,
	       MissingResourceException
    {
	return decode (str, null);
    }

    /**
     * String to Color within Locale.
     */
    public static Color decode (String str, Locale locale)
	throws NumberFormatException, ColorNameException,
	       MissingResourceException
    {
	if (str == null)
	    throw new ColorNameException ("null");

	if (str.startsWith ("#"))
	    return decodeHex (str, 1);
	if (str.startsWith ("0x"))
	    return decodeHex (str, 2);

	if (locale == null)
	    locale = Locale.getDefault ();
	String bundleName = ColorName.class.getName () + "s";
	ResourceBundle bundle = ResourceBundle.getBundle (bundleName, locale);

	Color c = (Color) bundle.getObject (str);
	if (c == null)
	    throw new ColorNameException (str);
	return c;
    }

    //
    // Decode a 6-digit hex number.
    //
    private static Color decodeHex (String str, int start)
	throws NumberFormatException
    {
	str = str.substring (start);

	int len = str.length ();
	if (len != 6)
	    throw new NumberFormatException ();

	int total = 0;
	for (int i = 0; i < len; ++i)
	{
	    int digit = Character.digit (str.charAt (i), 16);
	    if (digit < 0 || digit >= 16) 
		throw new NumberFormatException ();

	    total <<= 4;
	    total += digit;
	}

	return new Color (total);
    }
}
