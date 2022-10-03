package com.github.richardflee.astroimagej._main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Contains static utility methods
 */
public class AstapUtils {
	
	// JFileChooser dialog title
	private static final String DIALOG_TITLE = "Open FITS Folder";
		
	
	/**
	 * Configures file chooser to select folders only
	 * 
	 * @param spath open file chooser in this folder
	 * @return dir selecting file chooser
	 */
	public static JFileChooser configFileChooser(String spath) {		
		File file = new File(spath);
		JFileChooser jfc = new JFileChooser(file);
		jfc.setDialogTitle(DIALOG_TITLE);
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		// var filter = new FileNameExtensionFilter("TEXT FILES", "txt", "text");
		jfc.setFileFilter(new FileNameExtensionFilter("Fits Files", "fit", "fits", "fts"));
		return jfc;
	}
	
	/**
	 * Validates fits file extensions
	 * 
	 * @param file input file
	 * @return true if file extension is .fit, .fits or .fts, false otherwise
	 */
	public static boolean isFitsFile(Path file) {
		String[] extn = { ".fit", ".fits", ".fts" };
		String s = file.toString().toLowerCase();
		return Arrays.stream(extn).anyMatch(entry -> s.endsWith(entry));
	}
	
	/**
	 * Deletes specified folder, including all files and sub-folders.
	 * <p>Deletes directory recursively:
	 *  https://www.baeldung.com/java-delete-directory</p>
	 * 
	 * @param path path to folder to be deleted
	 */
	public static boolean deleteDirectory(Path path) {
		File directoryToBeDeleted = path.toFile();
	    File[] allContents = directoryToBeDeleted.listFiles();
	    if (allContents != null) {
	        for (File file : allContents) {
	            deleteDirectory(file.toPath());
	        }
	    }
	    return directoryToBeDeleted.delete();
	}
	
	/**
	 * Deletes files with fits filenames but non-fits extensions.
	 * <p>Example: ../image.fits, extn = .ini, deletes ../image.ini file</p>
	 * 
	 * @param fits  path to fits file
	 * @param extn typical extensions are .ini or .wcs 
	 */
	public static void deleteFile(Path fits, String extn) {
		Path path = changeExtension(fits, extn);
		try {
			Files.delete(path);
		} catch (IOException e) {
			// missing file, do nothing
		}
	}
	
	/**
	 * Deletes empty folder
	 */
	public static void deleteFolderIfEmpty(String spath) {
		var file = new File(spath);
		if (file.length() == 0) {
			file.delete();
		}
	}
	
	/*
	 * Replaces file extension e.g. .fits with extn e.g. .ini 
	 */
	private static Path changeExtension(Path path, String extn) {
		var spath = path.toString();
		int idx = spath.lastIndexOf('.');
		spath = spath.substring(0, idx) + extn;
		return Path.of(spath);
	}

}
