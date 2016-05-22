//
// EcceImageComponent.java
//

package com.codespinner.ecce;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;
import java.net.*;
import com.codespinner.util.*;

public class EcceImageComponent extends Component
{
    private Image image;
    private boolean fullScreen;

    public EcceImageComponent (Image image, boolean fullScreen)
    {
	this.image = image;
	this.fullScreen = fullScreen;
    }

    public void removeNotify ()
    {
	if (image != null)
	    image.flush ();
	image = null;
    }

    public void paint (Graphics g)
    {
	Dimension screenSize = getSize ();
	if (image != null)
	{
	    int iwidth = image.getWidth (this);
	    int iheight = image.getHeight (this);
	    if (iwidth > 0 && iheight > 0)
	    {
		boolean fullScreen = this.fullScreen ||
				     iwidth > screenSize.width ||
				     iheight > screenSize.height;
		frameImage (g, image, fullScreen);
		return;
	    }
	}

	// If control gets this far, no image was drawn.  Clear the screen.
	g.setColor (getBackground ());
	g.fillRect (0, 0, screenSize.width, screenSize.height);
	g.setColor (Color.black);
	g.drawString ("Loading image...", 50, 50);
    }

    private void frameImage (Graphics g, Image image, boolean fullScreen)
    {
	int iwidth = image.getWidth (this);
	int iheight = image.getHeight (this);
	Dimension screenSize = getSize ();

	int vwidth = iwidth;
	int vheight = iheight;
	if (fullScreen)
	{
	    double horizScale = (double) screenSize.width / (double) iwidth;
	    double vertScale = (double) screenSize.height / (double) iheight;
	    double scale = Math.min (horizScale, vertScale);
	    vwidth = (int) (iwidth * scale);
	    vheight = (int) (iheight * scale);
	}
	
	int topMargin = (screenSize.height - vheight) / 2;
	int leftMargin = (screenSize.width - vwidth) / 2;

	Color bgColor = getBackground ();
	g.setColor (bgColor);
	g.fillRect (0, 0, screenSize.width, topMargin);
	g.fillRect (0, topMargin + vheight, screenSize.width, topMargin + 1);
	g.fillRect (0, topMargin, leftMargin, vheight);
	g.fillRect (leftMargin + vwidth, topMargin, leftMargin + 1,
		    vheight);

	g.drawImage (image, leftMargin, topMargin, vwidth, vheight,
		      bgColor, this);
    }

    public void update (Graphics g)
    {
	paint (g);
    }
}
