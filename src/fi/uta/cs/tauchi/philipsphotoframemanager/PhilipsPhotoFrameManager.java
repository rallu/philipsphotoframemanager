/*
* ###################################################################
*
* PhilipsPhotoFrameManager
*
* PhilipsPhotoFrameManager (fi/uta/cs/tauchi/philipsphotoframemanager/)
*
* Copyright (c) 1998-2007 University of Tampere
* Speech-based and Pervasive Interaction Group
* Tampere Unit for Human-Computer Interaction (TAUCHI)
* Department of Computer Sciences
*
* http://code.google.com/p/philipsphotoframemanager/
* juha-pekka.rajaniemi@cs.uta.fi
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
* USA
*
* ###################################################################
*/

package fi.uta.cs.tauchi.philipsphotoframemanager;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Manager class used to control Philips 8ff3wmi/00 model photoframe.
 *
 * @author Juha-Pekka Rajaniemi, University of Tampere <juha dot rajaniemi at gmail dot com>
 *
 */
public class PhilipsPhotoFrameManager {

    private URL url;
    private String host;
    private final int port = 80;
    private URLConnection urlconnection;

    public PhilipsPhotoFrameManager(String host) {
        setHost(host);
    }

    /**
     * To change host address of
     *
     * @param host
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Requests folders content in XML format
     *
     * @param folder Folder to poll
     * @return Folder content in XML
     * @throws java.io.IOException
     */
    public String requestFolderList(String folder) throws IOException {
        return readResponse(doGet("/list?path=" + rawUrlEncode(folder)));
    }

    /**
     * Gets requested image file
     *
     * @param filename image to request
     * @return
     * @throws java.io.IOException
     */
    public Image requestImage(String filename) throws IOException {
        Image image = null;
        InputStream is = doGet("/get?path=" + rawUrlEncode(filename));
        image = ImageIO.read(is);
        return image;
    }

    /**
     * Gets requested file
     *
     * @param filename
     * @return
     * @throws java.io.IOException
     */
    public File requestFile(String filename) throws IOException {
        File file = null;
        InputStream is = doGet("/get?path=" + rawUrlEncode(filename));

        FileOutputStream fos = new FileOutputStream(file);

        byte[] buf = new byte[500000];
        int nread;
        synchronized (is) {
            while ((nread = is.read(buf, 0, buf.length)) >= 0) {
                fos.write(buf, 0, nread);
            }
        }
        fos.flush();
        fos.close();
        is.close();

        return file;
    }

    /**
     * Delete specified file from device
     *
     * @param filename
     * @return
     * @throws java.io.IOException
     */
    public String deleteFile(String filename) throws IOException {
        return readResponse(doGet("/delete?path=" + rawUrlEncode(filename)));
    }

    /**
     * Recursively deletes folder and all files in it
     *
     * @param folder
     * @return
     * @throws java.io.IOException
     */
    public void deleteFolder(String folder) throws IOException {
        try {
            String list = requestFolderList(folder);
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(list)));

            doc.getDocumentElement().normalize();
            System.out.println("Root element " + doc.getDocumentElement().getNodeName());
            NodeList nodeLst = doc.getDocumentElement().getChildNodes();
            String path = doc.getDocumentElement().getAttribute("path");
            for (int s = 0; s < nodeLst.getLength(); s++) {
                if (nodeLst.item(s).getNodeName().equals("dir")) {
                    deleteFolder(path + nodeLst.item(s).getChildNodes().item(0).getNodeValue() + "/");
                } else if (nodeLst.item(s).getNodeName().equals("file")) {
                    deleteFile(path + nodeLst.item(s).getChildNodes().item(0).getNodeValue());
                }
            }
            deleteFile(folder);
        } catch (SAXException ex) {
            Logger.getLogger(PhilipsPhotoFrameManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(PhilipsPhotoFrameManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Sends file to photoframe.
     *
     * @param folder
     * @param filename
     * @param file
     * @param mime mimetype of file. jpeg images are image/jpeg
     * @return
     * @throws java.io.IOException
     */
    public String sendFile(String folder, String filename, File file, String mime) throws IOException {
        if (folderExists(folder)) {
            return this.successXml(readResponse(doPost(rawUrlEncode(folder), rawUrlEncode(filename), file, mime)));
        }

        return errorXml(folder + " not found.");
    }

    /**
     * Reads device filestorages and reports their disk usages
     *
     * @return
     * @throws java.io.IOException
     */
    public String readFileSystemInformation() throws IOException {
        return readResponse(doGet("/filesystems"));
    }

    /**
     * Creates directory to some directory
     *
     * @param toFolder
     * @param newFolder
     * @return
     * @throws java.io.IOException
     */
    public String makeDir(String toFolder, String newFolder) throws IOException {
        if (!folderExists(toFolder)) {
            throw new FileNotFoundException(toFolder + " was not found from device");
        }

        return readResponse(doGet("/mkdir?path=" + rawUrlEncode(toFolder) + rawUrlEncode(newFolder)));
    }

    /**
     * Renames directory to another
     *
     * @param toRename
     * @param targetName
     * @return
     * @throws java.io.IOException
     */
    public String rename(String toRename, String targetName) throws IOException {
        if (!folderExists(toRename)) {
            throw new FileNotFoundException(toRename + " was not found from device");
        }

        return readResponse(doGet("/rename?src=" + rawUrlEncode(toRename) + "&dst=" + rawUrlEncode(targetName)));
    }

    /**
     * Checks if directory exists on device
     *
     * @param folder
     * @return
     */
    private boolean folderExists(String folder) {
        try {
            requestFolderList(folder);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Makes the actual GET request
     *
     * @param requestString
     * @return inputstream of response
     * @throws java.io.IOException
     */
    private InputStream doGet(String requestString) throws IOException {
        url = new URL("http", host, port, requestString);
        urlconnection = url.openConnection();
        urlconnection.setDoInput(true);
        return urlconnection.getInputStream();
    }

    /**
     *
     *
     * @param folder folder where to upload
     * @param filename name of file in photoframe
     * @param file file to send to photoframe
     * @param mime can be null
     * @return
     * @throws java.io.IOException
     */
    private InputStream doPost(String folder, String filename, File file, String mime) throws IOException {
        InputStream is = (InputStream) new FileInputStream(file);
        return doPost(folder, filename, is, file.length(), mime);
    }

    /**
     * Makes the actual POST request
     *
     * @param folder
     * @param filename
     * @param istream inputstream to data to send to server
     * @param streamlength length of inputstream data in bytes
     * @param mime
     * @return
     * @throws java.io.IOException
     */
    private InputStream doPost(String folder, String filename, InputStream istream, long streamlength, String mime) throws IOException {
        if (mime == null)
            mime = "content/unknown";

        String lineDelimiter = "\r\n";
        String s1 = "--Part-Boundary" + lineDelimiter;
        s1 += "Content-Disposition: form-data; name=\"file\"" + lineDelimiter;
        s1 += "Content-Type: " + mime + lineDelimiter;
        s1 += "Content-Transfer-Encoding: binary" + lineDelimiter + lineDelimiter;

        String s2 = lineDelimiter + "--Part-Boundary--" + lineDelimiter;

        url = new URL("http", host, 80, "/post?path=" + folder + "%2F" + filename);

        urlconnection = url.openConnection();
        urlconnection.setDoInput(true);
        urlconnection.setDoOutput(true);
        urlconnection.setUseCaches(false);
        urlconnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=\"Part-Boundary\"");
        urlconnection.setRequestProperty("Content-Length", (s1.length() + s2.length() + streamlength) + "");
        urlconnection.setRequestProperty("Host", host);

        OutputStream output = urlconnection.getOutputStream();

        output.write(s1.getBytes());

        byte[] buf = new byte[500000];
        int nread;

        synchronized (istream) {
            while ((nread = istream.read(buf, 0, buf.length)) >= 0) {
                output.write(buf, 0, nread);
            }
        }
        output.flush();
        buf = null;

        output.write(s2.getBytes());
        output.flush();
        output.close();

        istream.close();

        return urlconnection.getInputStream();
    }

    /**
     * Encodes string properly. Meaning spaces are %20 not +
     *
     * @param s
     * @return
     */
    private String rawUrlEncode(String s) {
        String tmp = "";
        try {
            tmp = URLEncoder.encode(s, "UTF-8");
            tmp.replaceAll("\\+", "%20");
            return tmp;
        } catch (UnsupportedEncodingException ex) {
        }

        return s;
    }

    /**
     * Reads response string from inputstream
     *
     * @param is
     * @return reads string based response from inputstream
     * @throws java.io.IOException
     */
    private String readResponse(InputStream is) throws IOException {
        String r = "";
        String str = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        while (null != ((str = reader.readLine()))) {
            r += str + "\n";
        }
        reader.close();
        return r;
    }

    /**
     * Change error message to XML
     *
     * @param message to modify
     * @return xml message
     */
    private String errorXml(String message) {
        String error = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
        error += "<error><message>" + message + "</message></error>";
        return error;
    }

    /**
     * Change success message to XML
     *
     * @param message to modify
     * @return xml message
     */
    private String successXml(String message) {
        String error = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
        error += "<success><message>" + message + "</message></success>";
        return error;
    }
}
