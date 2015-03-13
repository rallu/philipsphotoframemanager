# Description #

This package adds simple methods to upload images and files to Philips 8FF3WMI/00 digital photoframe. Includes also ability to create, rename and delete directories and files.

This library is build using JDK 1.6.

# Usage #

To use this class add .jar file to project and import clause:

```
import fi.uta.cs.tauchi.philipsphotoframemanager.PhilipsPhotoFrameManager;
```

Then in code create new instance and pass photo frames IP address in constructor. For example if photoframe ip is 192.168.0.5.

```
PhilipsPhotoFrameManager manager = new PhilipsPhotoFrameManager("192.168.0.5");
```

Then you can use manager to send and retrieve information to photoframe.

```
//deletes mypicture.jpg from interal memory folder ALBUM
manager.deleteFile("/internal/ALBUM/mypicture.jpg");

//deletes recursively ALBUM folder and all content
manager.deleteFolder("/internal/ALBUM/");

//Creates newalbum folder to ALBUM folder
manager.makedir("/internal/ALBUM/", "newalbum");

//returns XML File with disk usage information from interal memory and attached devices
manager.readFileSystemInformation();

//renames folder to another
manager.rename("/interal/ALBUM/newalbum/", "/internal/ALBUM/myalbum/");

//retrieves File class of mypicture.jpg
manager.requestFile("/internal/ALBUM/mypicture.jpg");

//gets folder content in XML File
manager.requestFolderList("/interal/ALBUM/");

//retrieves Image class of mypicture.jpg
manager.requestImage("/internal/ALBUM/mypicture.jpg");

//uploads filetoupload to ALBUM folder with name newpicture.jpg
manager.sendFile("/internal/ALBUM/", "newpicture.jpg", filetoupload, "image/jpeg");

//change IP address of photoframe
manager.setHost("192.168.0.3");
```