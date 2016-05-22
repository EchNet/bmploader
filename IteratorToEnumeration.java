//
// IteratorToEnumeration.java
//

package com.codespinner.ecce;

import java.util.*;

public class IteratorToEnumeration implements Enumeration
{
    private Iterator iter;

    /**
     * Constructor.
     */
    public IteratorToEnumeration (Iterator iter)
    {
	this.iter = iter;
    }

    public boolean hasMoreElements ()
    {
	return iter.hasNext ();
    }

    public Object nextElement ()
    {
	return iter.next ();
    }
}
