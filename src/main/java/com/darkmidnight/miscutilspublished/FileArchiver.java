package com.darkmidnight.miscutilspublished;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;

/**
 * Example Usage:
 * <code>
 * FileArchiver fa = new FileArchiver("/path/to/archive/"); // This is the directory that'll form the basis of your archive.
 * fa.moveDirectory("/path/to/files", true); // This points to the files you want to reorganise and move into the archive.
 * </code>
 * @author Dark Midnight Studios
 */
public class FileArchiver {

    final String baseAbs;
    final String baseRel;
    final int maxFilesPerDir;

    /**
     * Create a FileArchive at the given path.
     *
     * @param archivePath
     */
    public FileArchiver(String archivePath) {
        baseAbs = archivePath;
        baseRel = baseAbs.substring(baseAbs.substring(0, baseAbs.length() - 1).lastIndexOf("/")).replaceAll("/", "");
        maxFilesPerDir = 2500;
    }

    /**
     * As per FileArchiver(String archivePath), but with override for maximum
     * files per directory
     *
     * @param archivePath
     * @param maxFilesPerDir
     */
    public FileArchiver(String archivePath, int maxFilesPerDir) {
        baseAbs = archivePath;
        baseRel = baseAbs.substring(baseAbs.substring(0, baseAbs.length() - 1).lastIndexOf("/")).replaceAll("/", "");
        this.maxFilesPerDir = maxFilesPerDir;
    }

    /**
     * As retrieveFile but jump straight to loading the file as a byte array
     *
     * @param hash
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public byte[] retrieveBytes(String hash) throws FileNotFoundException, IOException {
        return Files.readAllBytes(retrieveFile(hash).toPath());
    }

    /**
     * Retrieve the file with the given hash from the archive directory
     *
     * @param hash
     * @return
     * @throws FileNotFoundException
     */
    public File retrieveFile(String hash) throws FileNotFoundException {
        File dir = new File(baseAbs);
        return retrieve_recurse(hash, dir);
    }

    /**
     * If the archive is sufficiently large to be nested several directories
     * deep, recurse until the file is found
     *
     * @param hash
     * @param dir
     * @return
     * @throws FileNotFoundException if the file is not found
     */
    private File retrieve_recurse(String hash, File dir) throws FileNotFoundException {
        File lookingFor = new File(dir.getAbsolutePath() + "/" + hash);
        if (!lookingFor.exists()) {
            for (File f : dir.listFiles()) {
                if (f.isDirectory() && hash.startsWith(f.getName())) {
                    return retrieve_recurse(hash, f);
                }
            }
        } else {
            return lookingFor;
        }
        throw new FileNotFoundException("No file with hash " + hash + " found in archive");
    }

    /**
     * Cycles through each file in the provided directory and attempts to
     * process it
     *
     * @param dirToProcess
     * @param recurse
     */
    public void moveDirectory(String dirToProcess, boolean recurse) {
        if (!dirToProcess.endsWith("/")) {
            dirToProcess = dirToProcess + "/";
        }
        for (File f : new File(dirToProcess).listFiles()) {
            if (f.isFile()) {
                try {
                    moveFile(f);
                } catch (IOException | NoSuchAlgorithmException ex) {
                    System.out.println("ERROR when moving " + f.getAbsolutePath());
                }
            }
            if (f.isDirectory() && recurse) {
                moveDirectory(f.getAbsolutePath(), recurse);
            }
        }
    }

    /**
     * Hash the file, rename it to the hash, and move it into the archiving
     * directory, under the appropriate subdirectory (if any)
     *
     * @param f
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public void moveFile(File f) throws IOException, NoSuchAlgorithmException {
        byte[] b = Files.readAllBytes(f.toPath());
        String s = Hash.createSHA256(b);

        int depth = 1;
        StringBuilder path = new StringBuilder(baseAbs);
        for (int i = 1; i <= s.length(); i++) {
            depth = i;
            if (new File(path.toString() + "/" + s.substring(0, depth)).exists()) {
                path.append(s.substring(0, depth)).append("/");
            } else {
                break;
            }
        }
        depth--; // Will always be one higher, so decrement
        System.out.println("Moving " + f.getAbsolutePath() + " to " + path.toString());    // Save it here, then check the size of the directory/
        File dest = new File(path.toString() + s);
        if (dest.exists()) {
            System.out.println("Warning - the target destination already exists, indiciating that this is a duplicate. It will still move, but the existing one will be overwritten");
        }
        f.renameTo(dest); // Save loc
        checkDirSize(path.toString());
    }

    /**
     * Check that no directory contains more than the max allowed number of
     * files. If it does, create subdirectories based off the next character in
     * the hash file name
     *
     * @param path
     */
    private void checkDirSize(String path) {
        File d = new File(path);
        if (d.isDirectory()) {
            if (d.listFiles().length >= maxFilesPerDir) { // Arbitrary number, need to strike a balance between number of subdirs and size of them
                System.out.println("Directory " + d.getAbsolutePath() + " now larger than " + maxFilesPerDir); // Potential limit when subdir gets to certain depth??

                String[] bits = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};
                for (String bit : bits) {
                    File newDir1 = new File(d.getAbsolutePath() + "/" + (d.getName().equals(baseRel) ? bit : d.getName() + bit));
                    if (!newDir1.exists()) {
                        newDir1.mkdirs();
                    }
                    System.out.println("Created " + newDir1.getAbsolutePath());
                }

                for (File f : d.listFiles()) {
                    if (!f.isDirectory()) {
                        for (String bit : bits) {
                            String subDir = (d.getName().equals(baseRel) ? bit : d.getName() + bit);
                            if (f.getName().startsWith(subDir)) {
                                f.renameTo(new File(d.getAbsolutePath() + "/" + (d.getName().equals(baseRel) ? bit : d.getName() + bit) + "/" + f.getName()));
                            }
                        }
                    }
                }

                for (File f : d.listFiles()) {
                    if (f.isDirectory()) {
                        checkDirSize(f.getAbsolutePath());
                    }
                }
            }
        } else {
            throw new RuntimeException("This should always be a directory...");
        }
    }

}
