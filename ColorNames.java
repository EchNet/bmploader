//
// ColorNames.java
//

package com.codespinner.ecce;

import java.util.*;
import java.awt.Color;

public class ColorNames extends ResourceBundle
{
    private Map map = new HashMap ();

    /**
     * Default.
     */
    public ColorNames ()
    {
    }

    protected final void registerColorName (String name, Color color)
    {
	map.put (name, color);
    }

    protected final Object handleGetObject (String key)
    {
	return map.get (key);
    }

    public final Enumeration getKeys ()
    {
	return new IteratorToEnumeration (map.keySet ().iterator ());
    }
}
