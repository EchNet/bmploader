//
// EcceStreamHandler.java
//

package com.codespinner.ecce;

import java.io.*;
import java.net.*;

/**
 * 
 * 
 */
public class EcceStreamHandler extends URLStreamHandler
{
    static
    {
	// Hello, my name is EcceURLStreamHandlerFactory, and I'll be your
	// URL stream handler factory today.
	//
	URL.setURLStreamHandlerFactory (new EcceURLStreamHandlerFactory ());
    }

    private static class EcceURLStreamHandlerFactory
	implements URLStreamHandlerFactory
    {
	/**
	 * Creates a new <code>URLStreamHandler</code> instance with the
	 * specified protocol.
	 *
	 * @param   protocol   the protocol ("<code>ftp</code>",
	 *                     "<code>http</code>", "<code>nntp</code>", etc.).
	 * @return  a <code>URLStreamHandler</code> for the specific protocol.
	 * @see     java.io.URLStreamHandler
	 */
	public URLStreamHandler createURLStreamHandler (String protocol)
	{
	    if (protocol.equalsIgnoreCase ("ecce"))
		return new EcceStreamHandler ();

	    // If this method returns null, then the default Java protocol
	    // handling takes over.
	    return null;
	}
    }

    /**
     * Call this to get started.
     */
    public static void init ()
    {
    }

    /**
     * Parses the string representation of a <code>URL</code> into a
     * <code>URL</code> object.
     * <p>
     * If there is any inherited context, then it has already been
     * copied into the <code>URL</code> argument.
     * <p>
     * The <code>parseURL</code> method of <code>URLStreamHandler</code>
     * parses the string representation as if it were an
     * <code>http</code> specification. Most URL protocol families have a
     * similar parsing. A stream protocol handler for a protocol that has
     * a different syntax must override this routine.
     *
     * @param   u       the <code>URL</code> to receive the result of parsing
     *                  the spec.
     * @param   spec    the <code>String</code> representing the URL that
     *                  must be parsed.
     * @param   start   the character index at which to begin parsing. This is
     *                  just past the '<code>:</code>' (if there is one) that
     *                  specifies the determination of the protocol name.
     * @param   limit   the character position to stop parsing at. This is the
     *                  end of the string or the position of the
     *                  "<code>#</code>" character, if present. All information
     *                  after the sharp sign indicates an anchor.
     */
    protected void parseURL(URL url, String spec, int start, int limit)
    {
	// If URL is relative, file is already set to the base.
	String file = url.getFile();
	String ref = url.getRef();

	// The whole spec string is a path name.  There is no host, no port.
	if (start < limit)
	{
	    if (spec.charAt(start) == '/')
	    {
		file = spec.substring(start, limit);
	    }
	    else if (file != null && file.length() > 0)
	    {
		/* relative to the context file - use either
		 * Unix separators || platform separators
		 */
		int ind = Math.max(file.lastIndexOf('/'),
				   file.lastIndexOf(File.separatorChar));
                // if there is no file separator there is no relative dir
                if (ind < 0)
                    ind = 0;
		file = file.substring(0, ind) + "/" + spec.substring(start, limit);
	    }
	    else
	    {
		file = spec.substring(start, limit);
	    }
	}
	if ((file == null) || (file.length() == 0)) {
	    file = "/";
	}

	// Resolve . components.
	//
	int i;
	while ((i = file.indexOf("/./")) >= 0) {
	    file = file.substring(0, i) + file.substring(i + 2);
	}

	// Resolve .. components.
	//
	while ((i = file.indexOf("/../")) >= 0) {
	    if ((limit = file.lastIndexOf('/', i - 1)) >= 0) {
		file = file.substring(0, limit) + file.substring(i + 3);
	    } else {
		file = file.substring(i + 3);
	    }
	}

	setURL (url, url.getProtocol (), null, 0, file, ref);
    }

    /**
     * Open a connection to the object referenced by the <code>URL</code>
     * argument.
     * @param      u   the URL that this connects to.
     * @return     a <code>URLConnection</code> object for the <code>URL</code>.
     * @exception  IOException  if an I/O error occurs while opening the
     *               connection.
     */
    protected URLConnection openConnection (URL url) throws IOException
    {
	String path = url.getFile ();
	String subpath = path;
	int end = -1;
	for (;;)
	{
	    File f = new File (subpath);
	    if (f.exists ())
	    {
		if (!f.canRead ())
		    throw new IOException (f + ": cannot read");
		if (end == -1)
		{
		    return new EcceConnection (url);
		}
		else
		{
		    return new EcceConnection (url, end);
		}
	    }
	    end = end < 0 ? path.lastIndexOf ('/')
			  : path.lastIndexOf ('/', end - 1);
	    if (end <= 0)
		break;
	    subpath = path.substring (0, end);
	}

	throw new IOException (path + ": not found");
    }
}
