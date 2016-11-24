package iec;
// 
//  GifSequenceWriter.java
//  
//  Created by Elliot Kroo on 2009-04-25.
//
// This work is licensed under the Creative Commons Attribution 3.0 Unported
// License. To view a copy of this license, visit
// http://creativecommons.org/licenses/by/3.0/ or send a letter to Creative
// Commons, 171 Second Street, Suite 300, San Francisco, California, 94105, USA.


import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.*;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

public class GifSequenceWriter {
  protected ImageWriter gifWriter;
  protected ImageWriteParam imageWriteParam;
  protected IIOMetadata imageMetaData;
  
  //Own variables: 
  public static int fileNumber;
  
  /**
   * Creates a new GifSequenceWriter
   * 
   * @param outputStream the ImageOutputStream to be written to
   * @param imageType one of the imageTypes specified in BufferedImage
   * @param timeBetweenFramesMS the time between frames in miliseconds
   * @param loopContinuously wether the gif should loop repeatedly
   * @throws IIOException if no gif ImageWriters are found
   *
   * @author Elliot Kroo (elliot[at]kroo[dot]net)
   */
  public GifSequenceWriter(
      ImageOutputStream outputStream,
      int imageType,
      int timeBetweenFramesMS,
      boolean loopContinuously) throws IIOException, IOException {
    // my method to create a writer
    gifWriter = getWriter(); 
    imageWriteParam = gifWriter.getDefaultWriteParam();
    ImageTypeSpecifier imageTypeSpecifier =
      ImageTypeSpecifier.createFromBufferedImageType(imageType);

    imageMetaData =
      gifWriter.getDefaultImageMetadata(imageTypeSpecifier,
      imageWriteParam);

    String metaFormatName = imageMetaData.getNativeMetadataFormatName();

    IIOMetadataNode root = (IIOMetadataNode)
      imageMetaData.getAsTree(metaFormatName);

    IIOMetadataNode graphicsControlExtensionNode = getNode(
      root,
      "GraphicControlExtension");

    graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
    graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
    graphicsControlExtensionNode.setAttribute(
      "transparentColorFlag",
      "FALSE");
    graphicsControlExtensionNode.setAttribute(
      "delayTime",
      Integer.toString(timeBetweenFramesMS / 10));
    graphicsControlExtensionNode.setAttribute(
      "transparentColorIndex",
      "0");

    IIOMetadataNode commentsNode = getNode(root, "CommentExtensions");
    commentsNode.setAttribute("CommentExtension", "Created by MAH");

    IIOMetadataNode appEntensionsNode = getNode(
      root,
      "ApplicationExtensions");

    IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");

    child.setAttribute("applicationID", "NETSCAPE");
    child.setAttribute("authenticationCode", "2.0");

    int loop = loopContinuously ? 0 : 1;

    child.setUserObject(new byte[]{ 0x1, (byte) (loop & 0xFF), (byte)
      ((loop >> 8) & 0xFF)});
    appEntensionsNode.appendChild(child);

    imageMetaData.setFromTree(metaFormatName, root);

    gifWriter.setOutput(outputStream);

    gifWriter.prepareWriteSequence(null);
  }
  
  public void writeToSequence(RenderedImage img) throws IOException {
    gifWriter.writeToSequence(
      new IIOImage(
        img,
        null,
        imageMetaData),
      imageWriteParam);
  }
  
  /**
   * Close this GifSequenceWriter object. This does not close the underlying
   * stream, just finishes off the GIF.
   */
  public void close() throws IOException {
    gifWriter.endWriteSequence();    
  }

  /**
   * Returns the first available GIF ImageWriter using 
   * ImageIO.getImageWritersBySuffix("gif").
   * 
   * @return a GIF ImageWriter object
   * @throws IIOException if no GIF image writers are returned
   */
  private static ImageWriter getWriter() throws IIOException {
    Iterator<ImageWriter> iter = ImageIO.getImageWritersBySuffix("gif");
    if(!iter.hasNext()) {
      throw new IIOException("No GIF Image Writers Exist");
    } else {
      return iter.next();
    }
  }

  /**
   * Returns an existing child node, or creates and returns a new child node (if 
   * the requested node does not exist).
   * 
   * @param rootNode the <tt>IIOMetadataNode</tt> to search for the child node.
   * @param nodeName the name of the child node.
   * 
   * @return the child node, if found or a new node created with the given name.
   */
  private static IIOMetadataNode getNode(
      IIOMetadataNode rootNode,
      String nodeName) {
    int nNodes = rootNode.getLength();
    for (int i = 0; i < nNodes; i++) {
      if (rootNode.item(i).getNodeName().compareToIgnoreCase(nodeName)
          == 0) {
        return((IIOMetadataNode) rootNode.item(i));
      }
    }
    IIOMetadataNode node = new IIOMetadataNode(nodeName);
    rootNode.appendChild(node);
    return(node);
  }
  
  /**
  public GifSequenceWriter(
       BufferedOutputStream outputStream,
       int imageType,
       int timeBetweenFramesMS,
       boolean loopContinuously) {
   
   */
  
  static final FilenameFilter IMAGE_FILTER = new FilenameFilter() {
      public boolean accept(final File dir, final String name) {
          if (name.endsWith(".png")) {
              return (true);
          }
      return (false);
      }
  };
  
  public static void createGIF(String outputFolder) throws Exception{
	// LOADING IMAGES: 
    final File dir = new File("./db/images/");
    ArrayList<BufferedImage> images = new ArrayList<BufferedImage>(); 
    File[] imgFiles = dir.listFiles(new FileFilter() {
      @Override
      public boolean accept(File file) {
        return !file.isHidden();
      }
    });

    Arrays.sort(imgFiles, new Comparator<File>() {
      @Override
      public int compare(File o1, File o2) {
        int n1 = extractNumber(o1.getName());
        int n2 = extractNumber(o2.getName());

        return n1 - n2;
      }

      private int extractNumber(String name) {
        int i;
        try {
          int seperator = name.lastIndexOf('/') + 1;
          int e = name.lastIndexOf('.');
          String number = name.substring(seperator, e);
          i = Integer.parseInt(number);
        } catch(Exception e) {
          i = 0; // if filename does not match the format
          // then default to 0
        }
        return i;
      }
    });
    //imgFiles[imgFiles.length-1].delete(); 
    //System.out.println("ImgFiles size: " + imgFiles.length);
    for (int i = 0; i<imgFiles.length-1; i++) {
        BufferedImage img = null;
        try {
            img = ImageIO.read(imgFiles[i]);
            //System.out.println("image: " + imgFiles[i].getName());
            images.add(img);
            imgFiles[i].delete();
        } catch (final IOException e) {
            System.out.println("Something failed while loading the images" + e);
        }
    }
    // CREATING THE GIF:
    if (images.size() > 1) {
    	
      // grab the output image type from the first image in the sequence
      BufferedImage firstImage = images.get(0);
      
      // create a new BufferedOutputStream with the last argument
      String outputLocation = outputFolder + Integer.toString(fileNumber) + ".gif";
      ImageOutputStream output = 
        new FileImageOutputStream(new File(outputLocation));
      fileNumber++; 
      
      // create a gif sequence with the type of the first image, 1 second
      // between frames, which loops continuously
      GifSequenceWriter writer = 
        new GifSequenceWriter(output, firstImage.getType(), 100, false);
      
      // write out the first image to our sequence...
      writer.writeToSequence(firstImage);
      
      for(int i=1; i<images.size()-1; i++) {
    	  BufferedImage nextImage = images.get(i);
    	  writer.writeToSequence(nextImage);
      }
      
      System.out.println(outputLocation + " sucessfully saved");
      writer.close();
      output.close();
      
      
    } else {
    	
    	System.out.println("Usage: java GifSequenceWriter [list of gif files] [output file]");
    	System.out.println("Images array size: " + images.size());
    }
  }

  /**
   * BACHELOR METHOD!! Used to create one long image with the path mario has moved. Rewritten from createGIF method.
   * @param outputFolder Where to save the level path image
   * @throws Exception If I/O fails
   */
  public static void createLevelImage(String outputFolder) throws Exception{
    final File dir = new File("./db/levelImages/");
    ArrayList<BufferedImage> images = new ArrayList<BufferedImage>();
    File[] imgFiles = dir.listFiles(new FileFilter() {
      @Override
      public boolean accept(File file) {
        return !file.isHidden();
      }
    });

    Arrays.sort(imgFiles, new Comparator<File>() {
      @Override
      public int compare(File o1, File o2) {
        int n1 = extractNumber(o1.getName());
        int n2 = extractNumber(o2.getName());

        return n1 - n2;
      }

      private int extractNumber(String name) {
        int i;
        try {
          int seperator = name.lastIndexOf('/') + 1;
          int e = name.lastIndexOf('.');
          String number = name.substring(seperator, e);
          i = Integer.parseInt(number);
        } catch(Exception e) {
          i = 0; // if filename does not match the format
          // then default to 0
        }
        return i;
      }
    });

    for (int i = 0; i < imgFiles.length; i++) {
      BufferedImage img = null;
      try {
        System.out.println(imgFiles[i].getAbsolutePath());
        img = ImageIO.read(imgFiles[i]);

        if(i < imgFiles.length - 1) {
          img = img.getSubimage(0, 0, img.getWidth()/2, img.getHeight());
        }

        images.add(img);
        imgFiles[i].delete();
      } catch (final IOException e) {
        System.out.println("Something failed while loading the images" + e);
      }
    }

    // CREATE INITIAL IMAGE TO START FROM
    BufferedImage img1 = images.get(0);
    BufferedImage img2 = images.get(1);
    BufferedImage joined = joinBufferedImage(img1, img2);

    //System.out.println("TEST3");

    // CREATING THE LEVEL IMAGE:
    for(int i = 2; i < images.size(); i++) {
      joined = joinBufferedImage(joined, images.get(i));
    }

    // WRITE LEVEL IMAGE TO FILE
    File outputfile = new File(outputFolder);
    ImageIO.write(joined, "png", outputfile);

    System.out.println("Done creating imageLevel!!");
  }

  /**
   * join two BufferedImage
   * you can add a orientation parameter to control direction
   * you can use a array to join more BufferedImage
   *
   * Method taken from: http://stackoverflow.com/questions/20826216/copy-two-buffered-image-into-one-image-side-by-side
   */
  public static BufferedImage joinBufferedImage(BufferedImage img1,BufferedImage img2) {
    //do some calculate first
    int width = img1.getWidth() + img2.getWidth();
    int height = Math.max(img1.getHeight(), img2.getHeight());

    //create a new buffer and draw two image into the new image
    BufferedImage newImage = new BufferedImage(width,height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = newImage.createGraphics();
    Color oldColor = g2.getColor();

    //fill background
    g2.setPaint(Color.WHITE);
    g2.fillRect(0, 0, width, height);

    //draw image
    g2.setColor(oldColor);
    g2.drawImage(img1, null, 0, 0);
    g2.drawImage(img2, null, img1.getWidth(), 0);
    g2.dispose();

    return newImage;
  }
}
