package anvilmapper;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

/* TODO:
 *  - Load waypoints and output in JSON format? (No built in library for JSON, maybe use XML or CSV instead?)
 *  I have a program somewhere for converting voxelmap waypoints to dynmap markers, should adapt that sometime
 *  it was the old school non-extensible kind, so would need some work but would be relatively easy
 */

public class AnvilMapper {
	
	private final static Logger log = Logger.getLogger("anvilmapper");
	
    private File worldDir;
	private File imageDir;
	
	private int minX = 0;
	private int maxX = 0;
	private int minZ = 0;
	private int maxZ = 0;
	
    public AnvilMapper(File worldDir) {
        this.worldDir = worldDir;
   		this.worldDir.mkdirs();
    }
    
    public void processDimension(File imageDir) {
    	if (imageDir.isDirectory()) {
    		this.imageDir = imageDir;
    		File defaultZoomDir = new File(imageDir, "z1");
    		if (defaultZoomDir.isDirectory()) {
    			File[] imageFileList = defaultZoomDir.listFiles(new FilenameFilter() {
    				@Override
    				public boolean accept(File f, String name) {
    					return f.exists();// && f.getName().endsWith(".png"); // TODO endsWith exludes everything, dunno why.  figure out, or 
    				}
    			});
    			if (imageFileList != null && imageFileList.length > 0) {
    				// zoom in on current images
    				for (File imageFile : imageFileList) {
    					if (imageFile.isFile()) {
    						// get the region x and z from the region file name
    						String[] baseNameSplit = imageFile.getName().replaceAll(".png", "").split("\\,");
    						if (baseNameSplit.length == 2) {
    							try {
    								int x = Integer.parseInt(baseNameSplit[0]);
    								int z = Integer.parseInt(baseNameSplit[1]);
    								if (x < minX)
    									minX = x;
    								if (x > maxX)
    									maxX = x;
    								if (z < minZ)
    									minZ = z;
    								if (z > maxZ)
    									maxZ = z;
    								for (int shiftZoom = 1; shiftZoom <= 3; shiftZoom++)
    									this.splitRegionImage(x, z, imageFile, shiftZoom);
    							} 
    							catch (NumberFormatException e) {
    								e.printStackTrace();
    							}
    						} 
    						else {
    							System.out.println("image file " + imageFile + " did not pass the file name check");
    						}
    					}
    				}
    				// zoom out on current images
    				for (int shiftZoom = -1; shiftZoom >= -4; shiftZoom--) {
    					float zoom = (float)Math.pow(2, shiftZoom);
    					float zoomOut = 1f/zoom;
    					int startX = (int)Math.floor(minX/zoomOut);
    					int endX = (int)Math.floor(maxX/zoomOut);
    					int startZ = (int)Math.floor(minZ/zoomOut);
    					int endZ = (int)Math.floor(maxZ/zoomOut);
    					for (int x = startX; x <= endX; x++) {
    						for (int z = startZ; z <= endZ; z++) {
    							this.combineRegionImages(x, z, shiftZoom);
    						}
    					}
    				}
    			} 
    			else {
    				System.out.println("no image files found in " + defaultZoomDir);
    			}
    		}
    		else {
    			System.out.println(defaultZoomDir + " is not a directory");				
			}
		} 
		else {
			System.out.println(imageDir + " is not a directory");
		}
	}

	// TODO do all dimensions for a world
	public void processWorld(File dir) {
		
		File[] subDirList = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File f, String name) {
				return f.isDirectory();
			}
		});
		
		if (subDirList != null) {
			for (File subDir : subDirList) {
				if (subDir.getName().equals("images")) {
					try {
						this.processDimension(subDir);
					} 
					catch (NumberFormatException e) {
					}
				}
				else {
					processWorld(subDir);
				}
			}
		}
	}
	
	public static void writeImage(BufferedImage img, File imageFile) {
		// write the given image to the image file
		File dir = imageFile.getParentFile();
		if (!dir.exists()) {
			dir.mkdirs();
		}
		
		try {
			ImageIO.write(img, "png", imageFile);
		} 
		catch (IOException e) {
			System.out.println("could not write image to " + imageFile);
		}
	}
	
	private void splitRegionImage(int x, int z, File imageFile, int shiftZoom) {
		BufferedImage image = null;
		try {
			image = ImageIO.read(imageFile);
			if (!(image.getType() == BufferedImage.TYPE_4BYTE_ABGR)) {
				BufferedImage temp = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
				Graphics2D g2 = temp.createGraphics();
				g2.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
				g2.dispose();
				image = temp;
			}
		} 
		catch (IOException e) {
			e.printStackTrace();
			return;
			// TODO throw error
		}
		int size = image.getWidth();
		int splitSize = size >> shiftZoom;
		int zoom = (int)Math.pow(2, shiftZoom);

		BufferedImage zoomedImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = zoomedImage.createGraphics();

		for (int srcZ = 0; srcZ < size; srcZ += splitSize) {
			for (int srcX = 0; srcX < size; srcX += splitSize) {

				g.setPaint(Color.BLACK);
				g.fillRect(0, 0, size, size);
				g.drawImage(image, 0, 0, size, size, srcX, srcZ, srcX + splitSize, srcZ + splitSize, null);

				writeImage(zoomedImage, new File(this.imageDir, "z" + zoom + "/" + (x*zoom + srcX/splitSize) + "," + (z*zoom + srcZ/splitSize) + ".png"));
			}
		}

		g.dispose();
	}
	
	private void combineRegionImages(int x, int z, int shiftZoom) {
		int existingComponentImages = 0;
		float zoom = (float)Math.pow(2, shiftZoom);
		int zoomOut = (int)(1f/zoom);
		int size = 256;
		int splitSize = size / zoomOut;
		BufferedImage combinedImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = combinedImage.createGraphics();
		g.setPaint (new Color (0, 0, 0)); // set image to black
		g.fillRect (0, 0, combinedImage.getWidth(), combinedImage.getHeight());
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		int leftX = x * zoomOut;
		int topZ = z * zoomOut;
		for (int componentX = 0; componentX <= zoomOut; componentX++) {
			for (int componentZ = 0; componentZ <= zoomOut; componentZ++) {
				File componentFile = new File(this.imageDir, "z1/" + (leftX + componentX) + "," + (topZ + componentZ) + ".png");
				if (componentFile.exists()) {
					BufferedImage componentImage = null;
					try {
						componentImage = ImageIO.read(componentFile);
						if (!(componentImage.getType() == BufferedImage.TYPE_4BYTE_ABGR)) {
							BufferedImage temp = new BufferedImage(componentImage.getWidth(), componentImage.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
							Graphics2D g2 = temp.createGraphics();
							g2.drawImage(componentImage, 0, 0, componentImage.getWidth(), componentImage.getHeight(), null);
							g2.dispose();
							componentImage = temp;
						}
						existingComponentImages++;
						g.drawImage(componentImage, componentX*splitSize, componentZ*splitSize, (componentX+1)*splitSize, (componentZ+1)*splitSize, 0, 0, size, size, null);
					} 
					catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		g.dispose();
		if (existingComponentImages > 0)
			writeImage(combinedImage, new File(imageDir, "z" + zoom + "/" + x + "," + z + ".png"));
	}
	
	public static void main(String [] args) {
		
		if (args.length == 1) {
			File worldDir = new File(args[0]);
			//File worldDir = new File("E:/Games/minecraft/anvilmapper-master/");
			if (worldDir.isDirectory()) {
				AnvilMapper anvilMapper = new AnvilMapper(worldDir);
				anvilMapper.processWorld(worldDir);
			} 
			else {
				System.out.println(worldDir + " is not a directory");
			}
		} 
		else {
			System.out.println("please supply a directory in the arguments");
		}
	}
}
