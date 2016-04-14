package synchive;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import fileManagement.SynchiveDirectory;
import fileManagement.SynchiveFile;
import support.Utilities;
import synchive.EventCenter.Events;
import fileManagement.FileProcessor;


/**
 * @Category: Processing Class: Main grunt of the NuttySync
 * @Process: Reads in uid for source and if desCRC does not exist generate file as well as reading uid for des.
 *           After reading in every file, scan through source and for each file in src, mark if found in des, otherwise
 *           copy into des. Afterwards, if file in des has not been marked, "delete" aka move to a separate location.
 */
/*
@Process: Items are stored initially stored as FILE_NOT_EXIST. Once
*           doesFileExist() is called, if the file is found, then the FileFlag
*           is modified to FILE_EXIST. */
public class SynchiveDiff
{
    private String LEFTOVER_FOLDER = FileProcessor.LEFTOVER_FOLDER;
    // srcLoc = source directory, desLoc = destination directory
    private File srcLoc, desLoc;
    // crcFile = source crc file
    private File crcFile;
    private Hashtable<String, SynchiveDirectory> destinationList; // Mapping of each file in directory
    private ArrayList<SynchiveFile> sourceCRCFiles; // list of all files in source location

    public SynchiveDiff(File curDir, File backupDir) throws Error, IOException
    {
        this.srcLoc = curDir;
        this.desLoc = backupDir;

        crcFile = new File(backupDir.getPath() + "\\" + Utilities.CRC_FILE_NAME);
        FileProcessor desReader = new FileProcessor(backupDir, Utilities.DESTINATION);
        destinationList = desReader.getDirectoryList();   
    }

    public void syncLocations()
    {
        FileProcessor rd;
        try
        {
            rd = new FileProcessor(srcLoc, Utilities.SOURCE);
            sourceCRCFiles = rd.getCRCFileList();
            
            postEvent(Events.Status, "Comparing Differences...");
            for(int i = 0; i < sourceCRCFiles.size(); i++)
            {
                // get file to search and search in hashTable of directories
                SynchiveFile temp = sourceCRCFiles.get(i); // file to parse through
                if(temp.copyAllowed())
                {
                    SynchiveDirectory dir =
                        destinationList.get(Utilities.convertToDirectoryLvl(
                            temp.getParentFile().getPath(), temp.getLevel(), srcLoc.getPath()));
                    boolean isRoot = temp.getParent().equals(srcLoc.getPath()) ? true : false; // if file is in root dir

                    if(dir != null && dir.getFiles().size() > 0)
                    {
                        // if directory exist find file in directory
                        boolean flag = dir.doesFileExist(temp.getUniqueID());
                        if(!flag) // file not exist
                        {
                            dir.addFile(temp.getUniqueID(), SynchiveDirectory.FileFlag.FILE_EXIST); // add to hashTable
                            copyFile(temp, StandardCopyOption.REPLACE_EXISTING); // Copy file over
                            postEvent(Events.ProcessingFile, isRoot ? "Added \"" + temp.getName() + "\" to \"root\"" : 
                                "Added \"" + temp.getName() + "\" to \"" + dir.getRealFolderName() + "\"");
                        }
                    }
                    else
                    {
                        // make new directory
                        String relativeDir = isRoot ? "\\" : temp.getParentFile().getName();
                        String relativeDirFromRoot = temp.getParent().substring(srcLoc.getPath().length());
                        String destinationDir = desLoc.getPath() + relativeDirFromRoot;
                        File fd = new File(destinationDir);
                        
                        if(!isRoot)
                            createDirectory(fd);
                        
                        SynchiveDirectory newDir =
                            isRoot ? new SynchiveDirectory(Utilities.convertToDirectoryLvl(desLoc.getPath(), 0, desLoc.getPath()))
                                : new SynchiveDirectory(Utilities.convertToDirectoryLvl(fd.getPath(), temp.getLevel(), desLoc.getPath()));

                        newDir.setRealFolderName(relativeDir);
                        newDir.addFile(temp.getUniqueID(), SynchiveDirectory.FileFlag.FILE_EXIST); // add file to new folder
                        destinationList.put(newDir.getUniqueID(), newDir); // add newDir to folderHashTable

                        copyFile(temp, StandardCopyOption.REPLACE_EXISTING); // copy file over
                        
                        postEvent(Events.ProcessingFile, isRoot ? 
                            "Added \"" + temp.getName() + "\" to \"root\"" :
                            "Added \"" + temp.getName() + "\" to \"" + newDir.getRealFolderName() + "\"");
                    }
                }
                else
                {
                    postEvent(Events.ErrorOccurred, "Did not copy \"" + temp.getName() + "\" due to CRC mismatch.");
                }
                
            }

            // clean up stuff
            insertToFile(); // write newly added files to crcFile
            postEvent(Events.Status, "Operation Completed");
        }
        catch (IOException | Error e)
        {
        }
    }

    private void createDirectory(File location) throws IOException
    {
        Path parentDir = location.getParentFile().toPath();

        if(!Files.exists(parentDir))
        {
            createDirectory(location.getParentFile());
        }
        postEvent(Events.ProcessingFile, "Directory \"" + location.getName() + "\" Created");
        location.mkdir();
    }

    // copies file from source to des with same name and folder
    private void copyFile(SynchiveFile file, StandardCopyOption op) throws IOException
    {
        String relativePath = file.getParent().substring(srcLoc.getPath().length());
        String destinationPath = desLoc.getPath() + relativePath + "\\" + file.getName();
        try
        {
            Files.copy(Paths.get(file.getPath()), Paths.get(destinationPath), op);
        }
        catch (IOException e)
        {
            postEvent(Events.ErrorOccurred, "Unable to copy file " + file.getName());
        }
        catch (UnsupportedOperationException e)
        {
            postEvent(Events.ErrorOccurred, "Unable to copy file " + file.getName());
        }
        catch (SecurityException e)
        {
            postEvent(Events.ErrorOccurred, "Unable to copy file " + file.getName());
        }
        // CRC32 Check
        // if failed... delete file and try again?
        if(file.getCRC().compareToIgnoreCase(Utilities.calculateCRC32(new File(destinationPath))) != 0)
        {
            postEvent(Events.ErrorOccurred, "CRC MISMATCH for file: " + file.getName());
        }
    }

    // copies file from des to leftovers and deletes original file
    private void cutFile(File file, StandardCopyOption op) throws IOException
    {
        String relativePath = file.getParent().substring(desLoc.getPath().length());
        String destinationPath = desLoc.getPath() + "\\" + LEFTOVER_FOLDER + relativePath + "\\" + file.getName();

        try
        {
            Files.copy(Paths.get(file.getPath()), Paths.get(destinationPath), op);
            // CRC32 Check
            // unoptimized, should grab src crcVal from file if failed... delete file and try again?
            if(!Utilities.calculateCRC32(file).equals(Utilities.calculateCRC32(new File(destinationPath))))
            {
                postEvent(Events.ErrorOccurred, "CRC MISMATCH for file: " + file.getName());
            }
            file.delete();
            postEvent(Events.ProcessingFile, 
                "File \"" + file.getName() + "\" in \"" + relativePath + 
                "\" not found in source. Moved to \"" + LEFTOVER_FOLDER + "\"");
        }
        catch (IOException e)
        {
            postEvent(Events.ErrorOccurred, "Unable to copy file: " + file.getName());
        }
    }

    private void removeEmptyDirectories(File file) throws IOException
    {
        if(file.isDirectory() && file.list().length == 0)
        {
            File parent = file.getParentFile(); // get parent directory
            file.delete(); // delete current directory
            postEvent(Events.ProcessingFile, "Deleted empty directory \"" + file.getName());

            if(parent.getPath().equals(desLoc.getPath())) // safety check
                return;
            removeEmptyDirectories(parent);
        }
    }

    // rewrites crcFile in des to include new files
    private void insertToFile()
    {
        System.out.println("Rewritting CRC file...");
        try
        {
            crcFile.delete();
            BufferedWriter output = new BufferedWriter(new FileWriter(crcFile));
            Enumeration<String> enu = destinationList.keys();
            while(enu.hasMoreElements()) // go through folders
            {
                String folderName = enu.nextElement();
                output.write(folderName);
                output.newLine();
                // go through files in folder
                SynchiveDirectory dir = destinationList.get(folderName);
                Enumeration<String> enuFiles = dir.getFiles().keys();
                while(enuFiles.hasMoreElements())
                {
                    String fileCRC = enuFiles.nextElement();
                    SynchiveDirectory.FileFlag val = dir.getValueForKey(fileCRC);
                    if(val != null)
                    {
                        if(val == SynchiveDirectory.FileFlag.FILE_EXIST) // only write matching files
                        {
                            output.write(fileCRC);
                            output.newLine();
                        }
                        else if(val == SynchiveDirectory.FileFlag.FILE_NOT_EXIST)
                        {
                            // make leftover folder for not found ones
                            File leftoversFolder = new File(desLoc.getPath() + "\\" + LEFTOVER_FOLDER);
                            if(!Files.exists(leftoversFolder.toPath()))
                                createDirectory(leftoversFolder);

                            // Get file that needs to be removed
                            String filePath = getFilePathFromFileCRC(desLoc, dir, fileCRC);
                            File toRemove = new File(filePath);
                            String toRemoveLeftoverPath = getLeftOverPath(toRemove);
                            File toRemoveLeftover = new File(toRemoveLeftoverPath);
                            if(!Files.exists(toRemoveLeftover.getParentFile().toPath()))
                                createDirectory(toRemoveLeftover.getParentFile());

                            cutFile(toRemove, StandardCopyOption.REPLACE_EXISTING);
                            removeEmptyDirectories(toRemove.getParentFile());
                        }
                    }
                }
            }
            output.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private String getLeftOverPath(File file)
    {
        String retVal = desLoc.getPath() + "\\" + LEFTOVER_FOLDER + file.getPath().substring(desLoc.getPath().length());
        return retVal;
    }

    private String getFilePathFromFileCRC(File desLoc, SynchiveDirectory fileDir, String fileName)
    {
        String[] splitDir = fileDir.getUniqueID().split(": ");
        String[] splitFile = fileName.split("\"");
        String retVal = desLoc.getPath() + (splitDir.length == 2 ? splitDir[1] : "") + "\\" + splitFile[1];
        return retVal;
    }
    
    private void postEvent(Events e, String str)
    {
        EventCenter.getInstance().postEvent(e, str);
    }
}
