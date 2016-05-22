//
// Ecce.java
//

package com.codespinner.ecce;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;
import java.net.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import com.codespinner.util.*;

public class Ecce extends Window
{
    public static void main (String[] args)
    {
	try
	{
	    ArgProc argProc = new ArgProc ();
	    argProc.setMinimumNumberOfArguments (0);
	    argProc.allowSwitch ("-full", null);
	    argProc.allowOption ("-bg", null);

	    Argv argv = argProc.process (args);

	    Ecce ecce = new Ecce ();
	    ecce.setFullScreen (argv.isSwitchSet ("-full"));
	    String bg = argv.getOption ("-bg");
	    if (bg != null)
	    {
		ecce.setBackground (ColorName.decode (bg));
	    }

	    if (argv.countArguments () == 0)
	    {
		ecce.addTarget (".");
	    }
	    else
	    {
		while (argv.hasNextArgument ())
		{
		    ecce.addTarget (argv.nextArgument ());
		}
	    }

	    ecce.show ();
	    ecce.requestFocus ();
	}
	catch (Exception e)
	{
	    System.err.println (e.getMessage ());
	    System.exit (1);
	}
    }

    private List targetList = new ArrayList ();
    private int cursor = 0;
    private boolean fullScreen = false;

    public Ecce ()
    {
	super (new Frame ("Ecce"));

	// Load and register ecce protocol.
	EcceStreamHandler.init ();

	// Size this window to full screen.
	Toolkit tk = getToolkit ();
	Dimension screen = tk.getScreenSize ();
	setBounds (0, 0, screen.width, screen.height);

	// One and only one component.
	setLayout (new GridLayout (1, 1));

	// Register my keyPressed handler.
	addKeyListener (new KeyAdapter ()
	{
	    public void keyPressed (KeyEvent e)
	    {
		Ecce.this.keyPressed (e);
	    }
	});
    }

    public void setFullScreen (boolean fullScreen)
    {
	this.fullScreen = fullScreen;
    }

    public void addTarget (String targetName)
	throws Exception
    {
	File f = new File (targetName);
	if (!f.exists ())
	    throw new Exception (targetName + ": not found.");

	boolean wasEmpty = targetList.size () == 0;
	ZipFile zf;

	if (f.isDirectory ())
	{
	    String[] dirList = f.list ();

	    if (dirList != null)
	    {
		Arrays.sort (dirList);

		for (int i = 0; i < dirList.length; ++i)
		{
		    addFileTarget (targetName, dirList[i]);
		}
	    }
	}
	else if (addFileTarget (null, targetName))
	{
	}
	else if ((zf = openZipFile (targetName)) != null)
	{
	    EcceConnection.registerZipFile (targetName, zf);

	    for (Enumeration e = zf.entries (); e.hasMoreElements (); )
	    {
		ZipEntry entry = (ZipEntry) e.nextElement ();
		addFileTarget (targetName, entry.getName ());
	    }
	}
	else
	{
	    throw new Exception (targetName + ": invalid target.");
	}

	if (wasEmpty)
	{
	    cursor = 0;
	    newContent ();
	}
    }

    private boolean addFileTarget (String base, String ext)
    {
	if (isImageName (ext) || isTextName (ext) || isHtmlName (ext))
	{
	    targetList.add (makeEcceUrl (base, ext));
	    return true;
	}
	return false;
    }

    private static String makeEcceUrl (String base, String ext)
    {
	StringBuffer buf = new StringBuffer ();

	buf.append ("ecce:");
	if (base != null)
	{
	    buf.append (base.replace ('\\', '/'));
	    buf.append ("/");
	}
	buf.append (ext.replace ('\\', '/'));

	return buf.toString ();
    }

    private static boolean isImageName (String name)
    {
	name = name.toLowerCase ();
	return name.endsWith (".jpg") || name.endsWith (".gif") || 
	       name.endsWith (".bmp");
    }

    private static boolean isTextName (String name)
    {
	name = name.toLowerCase ();
	return name.endsWith (".txt") || name.endsWith (".cfg") ||
		name.endsWith (".ini") || name.equals ("readme");
    }

    private static boolean isHtmlName (String name)
    {
	name = name.toLowerCase ();
	return name.endsWith (".html") || name.endsWith (".htm");
    }

    private static ZipFile openZipFile (String name)
	throws IOException
    {
	try
	{
	    return new ZipFile (name);
	}
	catch (IOException e)
	{
	    name = name.toLowerCase ();
	    if (name.endsWith (".jar") || name.endsWith (".zip"))
		throw e;
	}

	return null;
    }

    private void keyPressed (KeyEvent e)
    {
	switch (e.getKeyCode ())
	{
	case KeyEvent.VK_ESCAPE:
	    System.exit (0);
	    break;
	case KeyEvent.VK_PAGE_UP:
	case KeyEvent.VK_LEFT:
	case KeyEvent.VK_UP:
	    if (cursor > 0)
	    {
		--cursor;
		newContent ();
	    }
	    break;
	case KeyEvent.VK_SPACE:
	case KeyEvent.VK_RIGHT:
	case KeyEvent.VK_DOWN:
	case KeyEvent.VK_PAGE_DOWN:
	    if (cursor < targetList.size () - 1)
	    {
		++cursor;
		newContent ();
	    }
	    break;
	case KeyEvent.VK_HOME:
	    cursor = 0;
	    newContent ();
	    break;
	case KeyEvent.VK_END:
	    cursor = targetList.size () - 1;
	    newContent ();
	    break;
	default:
	    getToolkit ().beep ();
	    break;
	}
    }

    private void newContent ()
    {
	if (cursor < 0 || cursor >= targetList.size ())
	    return;

	removeAll ();

	try
	{
	    Component comp = null;
	    String urlStr = (String) targetList.get (cursor);
	    URL url = new URL (urlStr);
	    if (isHtmlName (urlStr))
	    {
		HTMLDocument doc = (HTMLDocument) new HTMLEditorKit ().createDefaultDocument ();
		String str = loadText ((InputStream) url.getContent ());
		doc.setInnerHTML (doc.getDefaultRootElement (), str);
		comp = new JTextArea (doc);
	    }
	    else
	    {
		Image image = getToolkit ().getImage (url);
		if (image != null)
		{
		    comp = new EcceImageComponent (image, fullScreen);
		}
	    }

	    if (comp != null)
	    {
		add (comp);
	    }
	}
	catch (Exception e)
	{
	    e.printStackTrace ();
	}

	validate ();
	repaint ();
	System.gc ();
    }

    private static String loadText (InputStream in)
	throws IOException
    {
	BufferedReader reader = new BufferedReader (new InputStreamReader (in));
	String line;
	StringBuffer buf = new StringBuffer ();
	while ((line = reader.readLine ()) != null)
	{
	    buf.append (line);
	}
	in.close ();
	return buf.toString ();
    }
}
