// BmpImageLoader.java:

package com.codespinner.ecce;

import java.awt.*;
import java.awt.image.IndexColorModel;
import java.awt.image.MemoryImageSource;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

/**
 * Installable image loader for Microsoft BMP images.  This class is
 * not normally used directly, but rather via the ImageLoader class.<p>
 *
 * @author SourceCraft, Inc.
 * @version 1.0 (June 16, 1996)
 */
public class BmpImageLoader
{
  private Toolkit toolkit;
  int width;
  int height;
  int bitCount;
  int colors;
  int scanStep;
  byte[] red;
  byte[] green;
  byte[] blue;
  byte[] imageData;

  public BmpImageLoader(Toolkit toolkit)
  {
    scanStep = -1;
    this.toolkit = toolkit;
  }

  /**
   * Reads a single byte from the input stream and returns its value.
   *
   * @return The value of the next byte on the input stream.
   */
  static byte readInt8(InputStream in)
       throws IOException
  {
    byte[] buf = new byte[1];
    in.read(buf);
    return buf[0];
  }

  /**
   * Reads a two-byte entity from the input stream and constructs a
   * a value from the bytes (ordered in LSBFirst format).
   *
   * @return The value of the next two bytes on the input stream in
   * LSBFirst format.
   */
  static short readInt16(InputStream in)
       throws IOException
  {
    byte[] buf = new byte[2];
    in.read(buf);
    // the & 0xff masks off sign extension when converting a byte
    // to an int.
    return (short)((buf[1] << 8) | (buf[0] & 0xff));
  }

  /**
   * Reads a four-byte entity in from the input stream and constructs a
   * value from the bytes (ordered in LSBFirst format).
   *
   * @return The value of the next four bytes on the input stream in
   * LSBFirst format.
   */
  static int readInt32(InputStream in)
       throws IOException
  {
    byte[] buf = new byte[4];
    in.read(buf);
    // the & 0xff tricks java into ignoring the sign
    return (buf[3] << 24) | (buf[2] << 16) | (buf[1] << 8) | (buf[0] & 0xff);
  }

  void readFileHeader(InputStream in)
       throws IOException
  {
    byte[] header = new byte[14];
    in.read(header);
    if ((header[0] != 'B') || (header[1] != 'M'))
      throw new IOException("Not a bitmap file");
  }

  /**
   * Reads in the bitmap header and sets fields appropriately.
   */
  void readBitmapHeader(InputStream in)
       throws IOException
  {
    readInt32(in); // size
    width = readInt32(in);
    height = readInt32(in);
    readInt16(in); // planes
    bitCount = readInt16(in);
    int compression = readInt32(in); // compression
    if (compression != 0)
      throw new IOException("Unsupported compression type: " + compression);
    int image_size = readInt32(in);
    readInt32(in); // horizontal resolution
    readInt32(in); // vertical resolution
    colors = readInt32(in);
    readInt32(in); // important colors

    if (width < 0) {
      width = -width;
      scanStep = 1;
    }

    // figure out default values for colors and bit count values
    if (colors == 0) {
      if (bitCount == 0) // bogus file
	throw new IOException("Invalid icon image file");
      colors = (1 << bitCount);
    }
    else if (bitCount == 0) {
      if (colors <= 2)
	bitCount = 1;
      else if (colors <= 4)
	bitCount = 2;
      else if (colors <= 8)
	bitCount = 3;
      else if (colors <= 16)
	bitCount = 4;
      else if (colors <= 32)
	bitCount = 5;
      else if (colors <= 64)
	bitCount = 6;
      else if (colors <= 128)
	bitCount = 7;
      else
	bitCount = 8;
    }
  }

  /**
   * Reads in palette data.
   */
  void readPalette(InputStream in)
       throws IOException
  {
    red = new byte[colors];
    green = new byte[colors];
    blue = new byte[colors];
    byte[] rgbquad = new byte[4];
    for (int j = 0; j < colors; j++) {
      in.read(rgbquad);
      red[j] = rgbquad[2];
      green[j] = rgbquad[1];
      blue[j] = rgbquad[0];
    }
  }

  /**
   * Reads in bitmap data and expands it to one byte per pixel.
   */
  void readBitmapData(InputStream in)
       throws IOException
  {
    if (bitCount > 8) // don't support high-color models
      throw new IOException("Unsupported color model (" +
			    String.valueOf(bitCount) + " bits)");

    // determine scanline length in whole bytes.
    int scanline_length;
    switch (bitCount) {
    case 1:
      scanline_length = (width + 7) / 8;
      break;
    case 4:
      scanline_length = (width + 1) / 2;
      break;
    case 8:
      scanline_length = width;
      break;
    default:
      throw new IOException("Unsupported color model (" +
			    String.valueOf(bitCount) + " bits)");
    }

    // pad scanline to 32-bit boundary
    scanline_length = (scanline_length + 3) & ~3;

    //
    // allocate buffers
    //
    imageData = new byte[width * height];
    byte[] scanline = new byte[scanline_length];

    //
    // read each scanline
    //
    int bytes_read = 0;
    int start_scanline;
    int end_scanline;
    if (scanStep < 0) {
      start_scanline = height - 1;
      end_scanline = -1;
    }
    else {
      start_scanline = 0;
      end_scanline = height;
    }
    for (int y = start_scanline; y != end_scanline; y += scanStep) {
      in.read(scanline);
      bytes_read += scanline_length;
      int dp = (y * width);

      //
      // 8 pixels per byte
      //
      if (bitCount == 1) {
	// break up complete scanline bytes
	int count = width / 8;
	int value;
	for (int x = 0; x < count; x++) {
	  value = scanline[x];
	  imageData[dp++] = (byte)((value & 0x80) == 0 ? 0 : 1);
	  imageData[dp++] = (byte)((value & 0x40) == 0 ? 0 : 1);
	  imageData[dp++] = (byte)((value & 0x20) == 0 ? 0 : 1);
	  imageData[dp++] = (byte)((value & 0x10) == 0 ? 0 : 1);
	  imageData[dp++] = (byte)((value & 0x08) == 0 ? 0 : 1);
	  imageData[dp++] = (byte)((value & 0x04) == 0 ? 0 : 1);
	  imageData[dp++] = (byte)((value & 0x02) == 0 ? 0 : 1);
	  imageData[dp++] = (byte)((value & 0x01) == 0 ? 0 : 1);
	}

	// pick up remaining bits in last byte
	int remnant = (width % 8);
	if (remnant > 0) {
	  int mask = 0x80;
	  value = scanline[count];
	  while (remnant-- > 0) {
	    imageData[dp++] = (byte)((value & mask) == 0 ? 0 : 1);
	    mask >>= 1;
	    if (mask == 0) {
	      mask = 0x80;
	    }
	  }
	}
      }

      //
      // 2 pixels per byte
      //
      else if (bitCount == 4) {
	// break up complete scanline bytes
	int count = width / 2;
	for (int x = 0; x < count; x++) {
	  int value = scanline[x];
	  imageData[dp++] = (byte)((value & 0xf0) >>> 4);
	  imageData[dp++] = (byte)(value & 0x0f);
	}

	// bit up last pixel if necessary
	if ((width % 2) > 0)
	  imageData[dp++] = (byte)((scanline[count] & 0xf0) >>> 4);
      }

      //
      // 1 pixel per byte
      //
      else { // bitCount == 8
	for (int x = 0; x < width; x++) // for the love of memcpy()
	  imageData[dp++] = scanline[x];
      }
    }
  }

  public Image load(String fileName)
       throws IOException
  {
    return load (new BufferedInputStream (new FileInputStream (fileName)));
  }

  public Image load (URL url)
       throws IOException
  {
    return load (url.openStream ());
  }

  public Image load(InputStream in)
       throws IOException
  {
      readFileHeader(in);
      readBitmapHeader(in);
      readPalette(in);
      readBitmapData(in);

    if (imageData == null)
      return null;
    MemoryImageSource imageSource =
      new MemoryImageSource(width, height,
			    new IndexColorModel(8, colors, red, green, blue),
			    imageData,
			    0, // offset
			    width); // scanline length

    // don't need the palette or image data any longer
    red = null;
    green = null;
    blue = null;
    imageData = null;

    return toolkit.createImage(imageSource);
  }
}
