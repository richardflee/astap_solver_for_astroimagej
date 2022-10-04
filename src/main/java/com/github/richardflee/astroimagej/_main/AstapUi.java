/*
 * Created by JFormDesigner on Sat Oct 01 17:01:45 BST 2022
 */

package com.github.richardflee.astroimagej._main;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;


/**
 * User interface to run astap astrometry solver for user selected folder of fits files.
 */
public class AstapUi extends JFrame {
	private static final long serialVersionUID = 1L;
	
	private enum StatusEnum {
		ASTAP_STARTED, ASTAP_STOPPED;
	}
	
	private enum SolveEnum {
		TEST_SOLVE_FITS, SOLVE_ALL_FITS;
	}
	
	
	// sub-folder to Solved folder, contains failed astap solve fits files
	private final String SOLVED_FOLDER = "Solved";
	private final String FAILED_FOLDER = "SOLVE_FAILED";
	
	// run button text
	private final String START = "Run ASTAP";
	private final String STOP = "Stop ASTAP";
	
	private final String INI = ".ini";
	private final String WCS = ".wcs";
	
	// user-selected folder
	private String pathToFitsFolder = ""; 
	
	// lists all files in user-selected folder with .fit, .fits or .fts extension 
	private List<String> fitsFilePaths = null;
	
	private double raHr = 0.0;
	private double spdDeg = 0.0;
	
	
	private boolean isAstapRunning;
	private AstapTask astapTask = null;

	public AstapUi() {
		initComponents();
		
		// button and checkbox event listeners
		initActionListeners();
		
		// start in astroimagej.exe folder
		this.pathToFitsFolder = System.getProperty("user.dir");
		setFitsFilesList(SolveEnum.SOLVE_ALL_FITS);
		
		// astap not running at start-up
		this.isAstapRunning = false;
	}

	/*
	 * Opens chooser dialog and loads all fit files in selected folder
	 */
	private void doImportFits() {
		var jfc = AstapUtils.configFileChooser(this.pathToFitsFolder);
		if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			// update current file path
			this.pathToFitsFolder = jfc.getSelectedFile().toString();
			
			// update list of fits file paths
			setFitsFilesList(SolveEnum.SOLVE_ALL_FITS);
		}
	}
	
	/*
	 * Deletes old solved folders and runs astap solver on selected folder.
	 * After solver run, deletes empty failed folder
	 * 
	 * <p>Task runs in a SwingWorker thread</p>
	 */
	private void doRunAstap(SolveEnum en) {
		// AstapUtils.deleteDirectory(Path.of(getPathToSolvedFolder()));
		createSolvedFolders();
		setFitsFilesList(en);
		try {
			setCentreCoords();
			this.astapTask = new AstapTask();
			this.astapTask.execute();			
		} catch (Exception e) {
			var message = "RA or Dec format error";
			JOptionPane.showMessageDialog(null,  message);
			configRunSettings(StatusEnum.ASTAP_STOPPED);			
		}
	}
	
	/*
	 * Stops astap processing in response to user abort
	 */
	private void doStopAstap() {
		astapTask.cancel(true);
		astapTask = null;		
	}
	
	/*
	 * Configures buttons and check-box event listeners
	 */
	private void initActionListeners() {
		// button action listeners
		importButton.addActionListener(e -> {
			doImportFits();
		});

		// sets button text and enabled status then toggle START / STOP astap solver
		runButton.addActionListener(e -> {
			if (!isAstapRunning) {
				configRunSettings(StatusEnum.ASTAP_STARTED);
				doRunAstap(SolveEnum.SOLVE_ALL_FITS);				
			} else {
				configRunSettings(StatusEnum.ASTAP_STOPPED);
				doStopAstap();
			}
		});
		
		testButton.addActionListener(e -> {
			System.out.println("testing");
			configRunSettings(StatusEnum.ASTAP_STARTED);
			setLogSelected(true);
			doRunAstap(SolveEnum.TEST_SOLVE_FITS);
		});

		// If Auto check-box selected (default state) astap reads ra and dec coordinates from fits header 
		// de-selecting Auto, enables text fields for user to input sexagesimal format coordinates
		autoCheckBox.addActionListener(e -> {
			if (autoCheckBox.isSelected()) {
				raTextField.setEditable(false);
				decTextField.setEditable(false);
			} else {
				raTextField.setEditable(true);
				decTextField.setEditable(true);
			}
		});
		
		// if running, terminates SwingWorker when ui is closed
		this.addWindowListener(new WindowAdapter() {			
			@Override
			public void windowClosing(WindowEvent e) {
				if (isAstapRunning) {
					isAstapRunning = false;
					astapTask.cancel(true);
					astapTask = null;	
				}
			}
		});
	}
	
	private void configRunSettings(StatusEnum en) {
		
		switch (en) {
			case ASTAP_STARTED:
				runButton.setText(STOP);
				testButton.setEnabled(false);
				importButton.setEnabled(false);
				isAstapRunning = true;			
				break;
				
			case ASTAP_STOPPED:
				runButton.setText(START);
				testButton.setEnabled(true);
				importButton.setEnabled(true);
				setLogSelected(false);
				isAstapRunning = false;			
				break;
		}
	}
	
	/**
	 * compiles a list of path strings for all fits files found in selected folder
	 */
	private void setFitsFilesList(SolveEnum en) {	
		
		var paths =  getFitsFilePaths();
		this.fitsFilePaths = getFitsFilePaths();
		
		// update ui field with name of selected folder and file counts
		folderTextField.setText(this.pathToFitsFolder);
		passTextField.setText("0");
		failTextField.setText("0");
		totalTextField.setText(String.format("%d", paths.size()));
		switch (en) {
			case SOLVE_ALL_FITS:
				this.fitsFilePaths = paths;
				break;
			case TEST_SOLVE_FITS:
				this.fitsFilePaths = Arrays.asList(paths.get(0));				
				break;		
		}
	}


	private List<String> getFitsFilePaths() {
		List<String> list = null;
		try (Stream<Path> stream = Files.list(Paths.get(this.pathToFitsFolder))) {
			 list = stream.filter(file -> AstapUtils.isFitsFile(file))
							.map(Path::toString)
							.collect(Collectors.toList());
		} catch (IOException e) {
			var message = "Error reading fits files";
			JOptionPane.showMessageDialog(null,  message);
		}
		return list;
	}
	
	/*
	 * Returns path string to folder containing plate solved fits files
	 * Defaults to "Solved" on start up or empty text field 
	 */
	private String getPathToSolvedFolder() {
		var solvedFolder = solvedTextField.getText().trim();
		if (solvedFolder.length() == 0) {
			solvedFolder = SOLVED_FOLDER;
			solvedTextField.setText(SOLVED_FOLDER);
		}
		return this.pathToFitsFolder + File.separator + solvedFolder;
	}
	
	/*
	 * Returns path string to folder containing failed plate solve fits files
	 */
	private String getPathToFailedFolder() {		
		return getPathToSolvedFolder() + File.separator + FAILED_FOLDER;
	}
	
	/*
	 * Deletes existing solved folders and creates ./Solved and ./Solver/SOLVE_FAILED sub-folders
	 */
	private void createSolvedFolders() {
		// delete existing solved folder, sub-folders + files
		AstapUtils.deleteDirectory(Path.of(getPathToSolvedFolder()));
		
		// create empty solved & failed solve folders
		try {
			Files.createDirectories(Path.of(getPathToFailedFolder()));
		} catch (IOException e1) {
			var message = String.format("Error creating folder: %s\n", getPathToFailedFolder());
			JOptionPane.showMessageDialog(null,  message);
		}
	}
	
	/*
	 * Astap processing in SwingWorker thread, runs independent of swing EDT
	 * 
	 *  Parameters:
	 *  <Void> no return value (process returns null)
	 *  <AstapData> current fits file and pass/fail tally to update ui
	 *
	 */
	private class AstapTask extends SwingWorker<Void, AstapData> {
		
		@Override
		protected Void doInBackground() {
			// solve pass/fial counters
			var passCount = 0;
			var failCount = 0;
			
			// paths to raw, solved and failed-solve fits files
			var parent = Path.of(pathToFitsFolder);
			var solved = Path.of(getPathToSolvedFolder());
			var failed = Path.of(getPathToFailedFolder());
			
			for (String spath : fitsFilePaths) {
				// file copy source and destination paths
				var filename =  Paths.get(spath).getFileName().toString();				
				var src = parent.resolve(filename);
				var dst = solved.resolve(filename);
				var solvedFits = dst.toString();
				// copy raw fits to Solved sub-folder and run astap plate solve
				// runAstap returns 0 if solve is successful otherwise solve failed
				try {
					Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);				
					if (runAstap(solvedFits) == 0) {
						// increment pass count and deletes redundant ini and wcs files
						passCount++;
						AstapUtils.deleteFile(dst, INI);
						AstapUtils.deleteFile(dst, WCS);
					} else {
						// increment fail count and moves fits file to failed sub-folder
						failCount++;
						Files.move(dst, failed.resolve(filename));
					}
				} catch (IOException e) {
					var message = String.format("Error processing file:\n%s", solvedFits);
					// skip error message if user clicked close window
					if (isAstapRunning) {
						JOptionPane.showMessageDialog(null,  message);
					}
				}
				// ui updates 
				publish(new AstapData(filename, passCount, failCount));
				// user abort
				if (isCancelled()) {
					break;
				}
			}			
			return null;
		}
		
		
		/*
		 * updates ui with current filename and pass / fail counts
		 */
		@Override
		protected void process(List<AstapData> astapList) {
			AstapData data = astapList.get(astapList.size() - 1);			
			passTextField.setText(String.format("%d", data.getPassCount()));
			failTextField.setText(String.format("%d", data.getFailCount()));
			fitsTextField.setText(data.getFilename());
		}
		
		/*
		 * resets astap running flag and resets buttons to run a new process
		 */
		@Override
		protected void done() {
			configRunSettings(StatusEnum.ASTAP_STOPPED);
			
			// deletes solve failed folder if empty, i.e. all fits files were solved
			AstapUtils.deleteFolderIfEmpty(getPathToFailedFolder());
		}
		
		
		/*
		 * Runs Astap plate solve command line options in java ProcessBuilder task
		 * NOTE: configured for WIN only 
		 * 
		 * refer: http://www.hnsky.org/astap.htm#command_line
		 * 
		 * @param spath path to fits file
		 * @return code = 0 if plate solve was successful, other codes indicates plate solve failed
		 */
		private int runAstap(String spath) {
			// compile WIN process to runatap
			ProcessBuilder processBuilder = new ProcessBuilder();
			processBuilder.command("cmd.exe", "/c", compileAstapCmdLine(spath));
			
			int exitCode = -1;
			try {
				Process process = processBuilder.start();
				
				// consumes any output from process (otherwise process blocks)
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				while ((reader.readLine()) != null) { }	
				
				exitCode = process.waitFor();
			}  catch (IOException e) {
				var message = String.format("Astap error processing file:: %s\n", spath);
				JOptionPane.showMessageDialog(null,  message);
			} catch (InterruptedException e) {
				// user clicked STOP
				var message = "Astap processing stopped";
				JOptionPane.showMessageDialog(null,  message);
			}
			return exitCode;
		}
		
		/*
		 * Compiles command line to process single fits file.
		 * Command line formats
		 * Auto (fits header coords): astap.exe -f <path_to_fits_file> -r 10 -m <min star width> [ -log]
		 * User coords: TBD
		 */
		private String compileAstapCmdLine(String spath) {
			spath = "\"" + spath + "\"";
			String line = String.format("astap.exe -f %s -r 10 -m %.3f -update", spath, getMinStarWidth());
			
			// option to specify ra spd (dec+90) coords
			line = (isAutoSelected()) ? line : line.concat(compileUserCoords());
			
			// option to append -log flag
			line = (isLogSelected() == false) ? line : line.concat(" -log");
			return line;
		}
		
	} // AstapTask 
	
	/*
	 * Compiles and returns line fragment comprising use-specified centre ra spd/dec coordinates
	 */
	private String compileUserCoords() {		
		return String.format(" -ra %.6f -spd %.6f ", this.raHr, this.spdDeg);
	}
	
	private void setCentreCoords() throws Exception {
		if (! isAutoSelected()) {
			String raHms = raTextField.getText();
			this.raHr = AstapUtils.raHmsToRaHr(raHms);
			
			String decDms = decTextField.getText();
			this.spdDeg = AstapUtils.decDmsToDecDeg(decDms) + 90.0;
		}
	}
		
	private double getMinStarWidth() {
		return (double) widthSpinner.getValue();
	}
	
	private boolean isLogSelected() {
		return logCheckBox.isSelected();
	}
	
	private boolean isAutoSelected() {
		return autoCheckBox.isSelected();
	}
	
	private void setLogSelected(boolean isSelected) {
		logCheckBox.setSelected(isSelected);
	}
	
	

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY
		// //GEN-BEGIN:initComponents
		// Generated using JFormDesigner non-commercial license
		dialogPane = new JPanel();
		contentPanel = new JPanel();
		fitsTextField = new JTextField();
		totalTextField = new JTextField();
		failTextField = new JTextField();
		passTextField = new JTextField();
		folderTextField = new JTextField();
		panel1 = new JPanel();
		autoCheckBox = new JCheckBox();
		label1 = new JLabel();
		label2 = new JLabel();
		raTextField = new JTextField();
		decTextField = new JTextField();
		label3 = new JLabel();
		label4 = new JLabel();
		panel2 = new JPanel();
		label5 = new JLabel();
		widthSpinner = new JSpinner();
		logCheckBox = new JCheckBox();
		solvedTextField = new JTextField();
		label6 = new JLabel();
		panel3 = new JPanel();
		importButton = new JButton();
		runButton = new JButton();
		testButton = new JButton();

		//======== this ========
		setTitle("ASTAP for AstroImageJ");
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		var contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());

		//======== dialogPane ========
		{
			dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
			dialogPane.setLayout(new BorderLayout());

			//======== contentPanel ========
			{
				contentPanel.setBorder(null);

				//---- fitsTextField ----
				fitsTextField.setEditable(false);
				fitsTextField.setToolTipText("Current Fits file");

				//---- totalTextField ----
				totalTextField.setText("0");
				totalTextField.setEditable(false);
				totalTextField.setHorizontalAlignment(SwingConstants.CENTER);
				totalTextField.setToolTipText("TOTAL count");

				//---- failTextField ----
				failTextField.setText("0");
				failTextField.setEditable(false);
				failTextField.setHorizontalAlignment(SwingConstants.CENTER);
				failTextField.setToolTipText("FAIL count");

				//---- passTextField ----
				passTextField.setText("0");
				passTextField.setEditable(false);
				passTextField.setHorizontalAlignment(SwingConstants.CENTER);
				passTextField.setToolTipText("PASS count");

				//---- folderTextField ----
				folderTextField.setEditable(false);
				folderTextField.setToolTipText("Path to Fits files folder");

				//======== panel1 ========
				{
					panel1.setBorder(new TitledBorder("RA / Dec Coordinates"));

					//---- autoCheckBox ----
					autoCheckBox.setText("Auto (get from fits header)");
					autoCheckBox.setSelected(true);
					autoCheckBox.setToolTipText("<html>\nChecked: solver reads read Ra and Dec coordinates from Fits file header\n<p>\nUnchecked: user enters coordinates in sexagesimal format\n</p>\n</html>");

					//---- label1 ----
					label1.setText("RA:");
					label1.setHorizontalAlignment(SwingConstants.RIGHT);

					//---- label2 ----
					label2.setText("Dec:");
					label2.setHorizontalAlignment(SwingConstants.RIGHT);

					//---- raTextField ----
					raTextField.setEditable(false);
					raTextField.setToolTipText("<html>\nEnter RA in range 00:00:00 to 23:59:59\n<p>\nNote: ':' delimiter\n</p>\n</html>");

					//---- decTextField ----
					decTextField.setEditable(false);
					decTextField.setToolTipText("<html>\nEnter Dec in range 00:00:00 to \u00b189:59:59\n<p>\nNote: ':' delimiter\n</p>\n</html>");

					//---- label3 ----
					label3.setText("HH:MM:SS");

					//---- label4 ----
					label4.setText("\u00b1DD:MM:SS");

					GroupLayout panel1Layout = new GroupLayout(panel1);
					panel1.setLayout(panel1Layout);
					panel1Layout.setHorizontalGroup(
						panel1Layout.createParallelGroup()
							.addGroup(panel1Layout.createSequentialGroup()
								.addGroup(panel1Layout.createParallelGroup()
									.addGroup(panel1Layout.createSequentialGroup()
										.addContainerGap()
										.addGroup(panel1Layout.createParallelGroup()
											.addComponent(label1, GroupLayout.PREFERRED_SIZE, 38, GroupLayout.PREFERRED_SIZE)
											.addComponent(label2, GroupLayout.PREFERRED_SIZE, 38, GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
											.addComponent(raTextField, GroupLayout.DEFAULT_SIZE, 113, Short.MAX_VALUE)
											.addComponent(decTextField, GroupLayout.DEFAULT_SIZE, 113, Short.MAX_VALUE))
										.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(panel1Layout.createParallelGroup()
											.addComponent(label3)
											.addComponent(label4)))
									.addGroup(panel1Layout.createSequentialGroup()
										.addGap(10, 10, 10)
										.addComponent(autoCheckBox)))
								.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
					);
					panel1Layout.setVerticalGroup(
						panel1Layout.createParallelGroup()
							.addGroup(panel1Layout.createSequentialGroup()
								.addContainerGap()
								.addComponent(autoCheckBox)
								.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
								.addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
									.addComponent(label1)
									.addComponent(raTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
									.addComponent(label3))
								.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
								.addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
									.addComponent(label2)
									.addComponent(decTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
									.addComponent(label4))
								.addContainerGap(28, Short.MAX_VALUE))
					);
				}

				//======== panel2 ========
				{
					panel2.setBorder(new TitledBorder("Solver Settings"));

					//---- label5 ----
					label5.setText("Minimum star size [sec]:");

					//---- widthSpinner ----
					widthSpinner.setModel(new SpinnerNumberModel(1.5, 0.5, 4.0, 0.5));
					widthSpinner.setToolTipText("<html>\nSet minimum star size in range 0.5\" to 4.0\"\n<p>\n(Hot pixel filter)\n</p>\n</html>");

					//---- logCheckBox ----
					logCheckBox.setText("Save solver log files");
					logCheckBox.setToolTipText("<html>\nChecked: saves plate solve log file\n<p>\nUnchecked: log file not saved (default)\n</p>\n</html>");

					//---- solvedTextField ----
					solvedTextField.setText("Solved");
					solvedTextField.setToolTipText("<html>\nSub-folder to save solved Fits files\n<p>\nDefault: 'Solved'\n</p>\n</html>");

					//---- label6 ----
					label6.setText("Solver subfolder:");

					GroupLayout panel2Layout = new GroupLayout(panel2);
					panel2.setLayout(panel2Layout);
					panel2Layout.setHorizontalGroup(
						panel2Layout.createParallelGroup()
							.addGroup(panel2Layout.createSequentialGroup()
								.addContainerGap()
								.addGroup(panel2Layout.createParallelGroup()
									.addComponent(logCheckBox)
									.addGroup(panel2Layout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
										.addGroup(panel2Layout.createSequentialGroup()
											.addComponent(label6)
											.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
											.addComponent(solvedTextField, GroupLayout.PREFERRED_SIZE, 94, GroupLayout.PREFERRED_SIZE))
										.addGroup(panel2Layout.createSequentialGroup()
											.addComponent(label5)
											.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
											.addComponent(widthSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))))
								.addContainerGap(11, Short.MAX_VALUE))
					);
					panel2Layout.setVerticalGroup(
						panel2Layout.createParallelGroup()
							.addGroup(panel2Layout.createSequentialGroup()
								.addContainerGap()
								.addGroup(panel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
									.addComponent(label5)
									.addComponent(widthSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
								.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
								.addComponent(logCheckBox)
								.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
								.addGroup(panel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
									.addComponent(label6)
									.addComponent(solvedTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
								.addContainerGap(27, Short.MAX_VALUE))
					);
				}

				//======== panel3 ========
				{

					//---- importButton ----
					importButton.setText("Select Folder");
					importButton.setToolTipText("Opens dialog to select Fits files folder");

					//---- runButton ----
					runButton.setText("Run ASTAP");
					runButton.setToolTipText("Runs ASTAP astrometric solver");

					//---- testButton ----
					testButton.setText("Test Solve");
					testButton.setToolTipText("Test solve a single Fits file");

					GroupLayout panel3Layout = new GroupLayout(panel3);
					panel3.setLayout(panel3Layout);
					panel3Layout.setHorizontalGroup(
						panel3Layout.createParallelGroup()
							.addGroup(GroupLayout.Alignment.TRAILING, panel3Layout.createSequentialGroup()
								.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(importButton)
								.addContainerGap())
							.addGroup(panel3Layout.createSequentialGroup()
								.addContainerGap()
								.addGroup(panel3Layout.createParallelGroup()
									.addComponent(runButton, GroupLayout.PREFERRED_SIZE, 93, GroupLayout.PREFERRED_SIZE)
									.addComponent(testButton, GroupLayout.PREFERRED_SIZE, 93, GroupLayout.PREFERRED_SIZE))
								.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
					);
					panel3Layout.setVerticalGroup(
						panel3Layout.createParallelGroup()
							.addGroup(panel3Layout.createSequentialGroup()
								.addContainerGap()
								.addComponent(runButton)
								.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
								.addComponent(testButton)
								.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 101, Short.MAX_VALUE)
								.addComponent(importButton))
					);
				}

				GroupLayout contentPanelLayout = new GroupLayout(contentPanel);
				contentPanel.setLayout(contentPanelLayout);
				contentPanelLayout.setHorizontalGroup(
					contentPanelLayout.createParallelGroup()
						.addGroup(contentPanelLayout.createSequentialGroup()
							.addContainerGap()
							.addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
								.addGroup(contentPanelLayout.createSequentialGroup()
									.addComponent(panel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
									.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
									.addComponent(panel2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
								.addGroup(contentPanelLayout.createSequentialGroup()
									.addComponent(fitsTextField)
									.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
									.addComponent(passTextField, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
									.addComponent(failTextField, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
									.addComponent(totalTextField, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE))
								.addComponent(folderTextField, GroupLayout.DEFAULT_SIZE, 466, Short.MAX_VALUE))
							.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
							.addComponent(panel3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addContainerGap())
				);
				contentPanelLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {failTextField, passTextField, totalTextField});
				contentPanelLayout.setVerticalGroup(
					contentPanelLayout.createParallelGroup()
						.addGroup(contentPanelLayout.createSequentialGroup()
							.addContainerGap()
							.addGroup(contentPanelLayout.createParallelGroup()
								.addComponent(panel3, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addGroup(contentPanelLayout.createSequentialGroup()
									.addGroup(contentPanelLayout.createParallelGroup()
										.addComponent(panel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
										.addComponent(panel2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
									.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
									.addGroup(contentPanelLayout.createParallelGroup()
										.addComponent(fitsTextField, GroupLayout.Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
										.addGroup(GroupLayout.Alignment.TRAILING, contentPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
											.addComponent(totalTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
											.addComponent(failTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
											.addComponent(passTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
									.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
									.addComponent(folderTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
							.addContainerGap())
				);
			}
			dialogPane.add(contentPanel, BorderLayout.CENTER);
		}
		contentPane.add(dialogPane, BorderLayout.CENTER);
		pack();
		setLocationRelativeTo(getOwner());
		// JFormDesigner - End of component initialization //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY //GEN-BEGIN:variables
	// Generated using JFormDesigner non-commercial license
	private JPanel dialogPane;
	private JPanel contentPanel;
	private JTextField fitsTextField;
	private JTextField totalTextField;
	private JTextField failTextField;
	private JTextField passTextField;
	private JTextField folderTextField;
	private JPanel panel1;
	private JCheckBox autoCheckBox;
	private JLabel label1;
	private JLabel label2;
	private JTextField raTextField;
	private JTextField decTextField;
	private JLabel label3;
	private JLabel label4;
	private JPanel panel2;
	private JLabel label5;
	private JSpinner widthSpinner;
	private JCheckBox logCheckBox;
	private JTextField solvedTextField;
	private JLabel label6;
	private JPanel panel3;
	private JButton importButton;
	private JButton runButton;
	private JButton testButton;
	// JFormDesigner - End of variables declaration //GEN-END:variables
}
