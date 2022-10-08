package com.github.richardflee.astroimagej._main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Contains static utility methods
 */
public class AstapUtils {
	
	// JFileChooser dialog title
	private static final String DIALOG_TITLE = "Open FITS Folder";

	/**
	 * Configures file chooser to select folders only
	 * @param spath
	 *     open file chooser in this folder
	 * @return dir selecting file chooser
	 */
	public static JFileChooser configFileChooser(String spath) {
		File file = new File(spath);
		JFileChooser jfc = new JFileChooser(file);
		jfc.setDialogTitle(DIALOG_TITLE);
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		jfc.setFileFilter(new FileNameExtensionFilter("Fits Files", "fit", "fits", "fts"));
		return jfc;
	}

	/**
	 * Validates fits file extensions
	 * @param file
	 *     input file
	 * @return true if file extension is .fit, .fits or .fts, false otherwise
	 */
	public static boolean isFitsFile(Path file) {
		String[] extn = { ".fit", ".fits", ".fts" };
		String s = file.toString().toLowerCase();
		return Arrays.stream(extn).anyMatch(entry -> s.endsWith(entry));
	}

	/**
	 * Deletes specified folder, including all files and sub-folders. <p>Deletes
	 * directory recursively: https://www.baeldung.com/java-delete-directory</p>
	 * @param path
	 *     path to folder to be deleted
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
	 * Deletes files with fits filenames but non-fits extensions. <p>Example:
	 * ../image.fits, extn = .ini, deletes ../image.ini file</p>
	 * @param fits
	 *     path to fits file
	 * @param extn
	 *     typical extensions are .ini or .wcs
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

	/**
	 * Convert ra sexagesimal format to numeric value (hours). <p> Negative ra is
	 * converted to 24 - |ra| </p>
	 * @param raHms
	 *     in sexagesimal format HH:MM:SS.SS
	 * @return numeric ra in units hr (hh.hhhh)
	 */
	public static Double raHmsToRaHr(String raHms) throws Exception {

		boolean isNegative = raHms.charAt(0) == '-';

		// split input at ':' delim and coerce elements into appropriate range
		String[] el = raHms.split(":");
		double hh = Math.abs(Double.valueOf(el[0]));
		double mm = Double.valueOf(el[1]) % 60;
		double ss = Double.valueOf(el[2]) % 60;
		double raHr = (hh + mm / 60 + ss / 3600) % 24;
		return isNegative ? (24.0 - raHr) : raHr;
	}

	
	/**
	 * Convert dec in sexagesimal format to numeric value (dd.dddd).
	 * 
	 * @param decDms object declination in sexagesimal format DD:MM:SS.SS
	 * @throws Exception non-specific exception
	 * @return numeric dec in units deg (±dd.dddd)
	 */
	public static Double decDmsToDecDeg(String decDms) throws Exception {
		int sign = (decDms.charAt(0) == '-') ? -1 : 1;

		// split input at ':' delim and coerce elements into appropriate range
		String[] el = decDms.split(":");
		double dd = Math.abs(Double.valueOf(el[0]));

		// clip |dec| > 90 to 90.0
		if (dd > 90) {
			return sign * 90.0;
		}
		double mm = Double.valueOf(el[1]) % 60;
		double ss = Double.valueOf(el[2]) % 60;
		return sign * (dd + mm / 60 + ss / 3600);
	}

	/**
	 * Surrounds string with double quotations.
	 * Enables path with spaces to be used in in ProcessBuilder expressions
	 */
	public static String applyEscQuotes(String line) {
		var escQuote = "\"";
		return escQuote + line + escQuote;
	}
	
	/*
	 * Compiles RA and Dec ASTAP command line fragment 
	 */
	private static String getUserCoords(String raHms, String decDms) {
		double raHr = 0.0;
		double spd = 0.0;
		try {
			raHr = AstapUtils.raHmsToRaHr(raHms);
			spd = AstapUtils.decDmsToDecDeg(decDms) + 90.0;
		} catch (Exception e) {
			var message = "Error compiling RA or Dec to ASTAP command line";
			JOptionPane.showMessageDialog(null,  message);
		}
		return String.format("-ra %.6f -spd %.6f", raHr, spd);
	}

	public static void main(String[] args) {
		String raHms = "10:42:24.602";
		String decDms = "+07:26:06.29";		
		String x = getUserCoords(raHms, decDms);
		System.out.println(x);
		
		raHms = "10 42:24.602";
		decDms = "+07:26:06.29";
		x = getUserCoords(raHms, decDms);
		System.out.println(x);
	}
}
