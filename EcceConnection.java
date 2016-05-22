//
// EcceConnection.java
//

package com.codespinner.ecce;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

/**
 * Loading this class causes ** to be established as the URL stream
 * factory.  This may be done at most once per application.
 */
public class EcceConnection extends URLConnection
{
    // Maintain a private, global list of open zip files.  A zip file
    // is a RandomAccessFile open for read only.
    //
    private static Map zipMap = Collections.synchronizedMap (new HashMap ());

    /**
     *
     */
    public static void registerZipFile (String zipName, ZipFile zf)
    {
	zipMap.put (zipName, zf);
    }

    private ZipFile zf;
    private String ext;
    private InputStream input;

    /**
     * Constructor.
     */
    public EcceConnection (URL url) throws IOException
    {
	super (url);
	this.ext = url.getFile ();
    }

    /**
     * Constructor.
     */
    public EcceConnection (URL url, int separatorIndex) throws IOException
    {
	super (url);

	String fileString = url.getFile ();
	String zipName = fileString.substring (0, separatorIndex);
	String ext = fileString.substring (separatorIndex + 1);;

	ZipFile zf = (ZipFile) zipMap.get (zipName);
	if (zf == null)
	{
	    zf = new ZipFile (zipName);
	    zipMap.put (zipName, zf);
	}

	this.zf = zf;
	this.ext = ext;
    }

    /**
     * Opens a communications link to the resource referenced by this 
     * URL, if such a connection has not already been established. 
     * @exception  IOException  if an I/O error occurs while opening the
     *               connection.
     */
    public void connect() throws IOException
    {
	if (!connected)
	{
	    if (zf != null)
	    {
		ZipEntry ze = zf.getEntry (ext);
		if (ze == null)
		    ze = zf.getEntry (ext.replace ('/', '\\'));
		if (ze == null)
		    throw new IOException (url + ": no such entry in zip file.");
		input = zf.getInputStream (ze);
	    }
	    else
	    {
		input = new FileInputStream (ext);
	    }
	    connected = true;
	}
    }

    /**
     * Return an input stream that reads from this connection.
     * @return     an input stream that reads from this open connection.
     * @exception  IOException              if an I/O error occurs while
     *               creating the input stream.
     */
    public InputStream getInputStream() throws IOException
    {
	// Yes, calling connect here is the right thing to do.  Necessary,
	// in fact.  The javadoc was no help in figuring this out.
	connect ();
	return input;
    }

    /**
     * Get the content type of this file.
     */
    public String getContentType ()
    {
	// This appears to work only in some versions of Java...
	//return getFileNameMap ().getContentTypeFor (ext);
	// HACK For now...
	String bah = ext.toLowerCase ();
	if (bah.endsWith (".jpg"))
	    return "image/jpeg";
	if (bah.endsWith (".gif"))
	    return "image/gif";
	if (bah.endsWith (".bmp"))
	    return "image/bmp";
	return "text/plain";
    }

    /**
     * Get the length of this file.
     */
    public int getContentLength ()
    {
	if (zf != null)
	{
	    ZipEntry ze = zf.getEntry (ext);
	    if (ze == null)
		ze = zf.getEntry (ext.replace ('/', '\\'));
	    if (ze == null)
		return 0;
	    return (int) ze.getSize ();
	}
	else
	{
	    return (int) new File (ext).length ();
	}
    }
}
