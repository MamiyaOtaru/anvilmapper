Voxelmap output image processor
============

Usage Instructions:
* run VoxelMap with this line in your voxelmap.properties file: "Output Images:true" (without quotes)
* join the server or load the singleplayer world.
* ensure you have your chosen resourcec pack loaded and it is the time of day you want, then pan around the world map.  All areas you view will be output
* images will be created in /VoxelMods/voxelMap/cache/WORLDNAME(/optionalSubworldName)/dimensionName/images/z1
* z1 is for zoom level 1, where one block is one pixel.
* edit run.bat so the program argument is directory of the world you want to process (the /VoxelMods/voxelMap/cache/WORLDNAME part)
* Run 'run.bat' 
* each dimensions images folder will have added z2, z4, z8 folders for zooming in, and z0.5, z0.25, z0.125 and z0.0625 folders for zooming out
* edit the java source to mot certain zoom levels (each zoom in quadruples the number of files).  If you have 1,000 region files, you will end up with 64,000 .pngs in z8
* Open 'index.html' in a web browser to view the map.

Compilation Instructions:
* Compile using javac:
    javac src\anvilmapper\AnvilMapper.java -sourcepath src 

