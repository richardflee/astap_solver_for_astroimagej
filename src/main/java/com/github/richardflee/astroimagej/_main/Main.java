package com.github.richardflee.astroimagej._main;

import java.awt.EventQueue;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLightLaf;

/**
 * Main class for ASTAP plate solve
 * 
 * @author rfl
 *
 */
public class Main {
	
	public final static String ASTAP_TITLE = "Run ASTAP Astrometric Solver for AstroImageJ";
	public final static String ASTAP_VERSION = "1.00a";
	
	public static void runApp() {
		var astapUi = new AstapUi();	
		var version = String.format("%s - %s", ASTAP_TITLE, ASTAP_VERSION); 
		astapUi.setTitle(version);
		astapUi.setVisible(true);		
	}

	/**
	 * Configures FlatLightLaf look and feel
	 * Runs main app in EDT
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
			// dashing flat laf light theme
			UIManager.setLookAndFeel(new FlatLightLaf());
		} catch (Exception ex) {
			var message = "Failed to initialize FlatLightLaf";
			JOptionPane.showMessageDialog(null,  message);
		}
		
		// runs app in EDT (event dispatching thread)
		EventQueue.invokeLater(() -> {
			runApp();
		});
	}

}
