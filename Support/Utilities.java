package support;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;


/** Static Methods Class */
public class Utilities
{
    public static final String CRC_FILE_NAME = "~listOfFilesInCRC.txt";
    public static final String AUDIT_FILE_NAME = "~auditTrail.txt";
    public static final int SOURCE = 5;
    public static final int DESTINATION = 10;

//    public static final String getEncodedFileString(String fileName, String CRC32)
//    {
//        return CRC32 + " \"" + fileName + "\"";
//    }

    public static final String convertToDirectoryLvl(String filePath, int level, String rootPath)
    {
        return "~" + level + ": " + filePath.substring(rootPath.length());
    }
    
    // returns appropriate naming including CRC value. 
    /**
     * @param filename Filename with extension
     * @param extension Extension of filename (including '.')
     * @param CRC CRC value to put into filename
     * @param encasement Delimiters to surround CRC in
     * @return
     */
    public static final String getFilenameWithCRC(String filename, String extension, String CRC, String[] encasement)
    {
        // passing in extension instead of calling Utilities.getExtensionType to save an additional call
        final String[] delimiters = {")", "}", "]", "-", "_", "+", "="}; // used to check if extra spacing is required or not.
        boolean addExtraSpacing = true;
        String additionalSpacing = ""; // default no spacing
        String fullName = filename.substring(0, filename.length() - extension.length()); //strip out extension from name
        
        // loop through each trailing delimiter type to skip adding spacing
        for(String delim : delimiters)
        {
            if(fullName.endsWith(delim))
            {
                addExtraSpacing = false;
            }
        }
        if(addExtraSpacing) // if no tags detected, determine if using spaces or underscores
        {
            String[] spacesSplit = fullName.split(" ");
            // if spaces found use space, else use underscore
            additionalSpacing = spacesSplit.length > 1 ? " " : "_";
        }
       
        fullName += additionalSpacing + encasement[0] + CRC.toUpperCase() + encasement[1] + extension;
        return fullName;
    }
    
    /**
     * Parse and return the extension type
     * @param filename Filename to parse
     * @return Extension of filename including '.'
     */
    public static final String getExtensionType(String filename)
    {
        String[] splitStr = filename.split("[.]");
        if(splitStr.length == 1) // no extension found
        {
            return "";
        }
        return "." + splitStr[splitStr.length-1];
    }
    
    /**
     * Returns a set of all possible extension types in a file or directory. Including sub-directories.
     * @param file File or directory to search through
     * @return A set of all extensions in the file or directory
     */
    public static final Set<String> getExtensionsForFiles(File[] file)
    {
        Set<String> types = new HashSet<String>();
        for(File f : file)
        {
            if(f.isDirectory())
            {
                types.addAll(getExtensionsForFiles(f.listFiles())); // recurse through sub-directories
            }
            else
            {
                types.add(Utilities.getExtensionType(f.getName()));
            }
        }
        return types;
    }
    
    public static final boolean stringEndsWith(String string, String[] pattern)
    {
        for(String str : pattern)
        {
            if((str.equals("") && str.isEmpty()) || string.endsWith(str))
            {
                return true;
            }
        }
        return false;
    }

    public static final String calculateCRC32(File file) throws IOException
    {
        String hex = "";
        try
        {
            CheckedInputStream cis = new CheckedInputStream(new FileInputStream(file), new CRC32());
            byte[] buf = new byte[10240]; // 10mb

            while(cis.read(buf) >= 0)
                ;

            hex = Long.toHexString(cis.getChecksum().getValue());
            cis.close();
        }
        catch (IOException e)
        {
            System.out.println("Unable to determine crc32 value for file: " + file.getName());
            return "";
        }
        return hex;
    }

}
