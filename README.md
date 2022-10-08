## astap_solver_for_astroimagej

## Integrates ASTAP astrometric solver with AstroImageJ 

README file for AstroImageJ plugin to batch process fits image files using the ASTAP astrometric solver.
<br/>See the User Guide pdf (below) for software setup and user instructions.
<br/>Limitations: current version (v1.00a Oct 2022) is Windows only; target date Dec 2022 for Linux compatibility.

<br/><br/>
**Download files:**
- astro_plugins-1.0x.jar: java-8 jar, compiled as an ImageJ plugin to link astap.jar to AIJ plugins menu options.
- astap.jar: java-17 jar, compiled as a java exe with no external dependencies (uber-jar)
-  astap data.zip: compressed folder containing sample fits files
- User Guide Astap Solver for AstroImageJ.pdf

Click 'Releases' link on the right side of the repo Code page<br/>
In Releases page, select latest release
<br/>Click links to astap data.zip and User Guide Astap Solver for AstroImageJ.pdf
<br/>Open Assets, click links to astro_plugins-1.0x.jar and astap.jar: java-17 jar to download jar files<br/>

**Software Notes**

astro_plugins-1.0x.jar is an ImageJ plugin, ref: https://imagej.net/Developing_Plugins_for_ImageJ_1.x.. 
The plugin is coped to folder  ./AstroImageJ/Plugins/Astro Apps and invoked through AIJ plugins menu. 
Selecting 'Run Astap App' invokes exe jar: astap.jar
<br/>

**ASTAP.JAR**
- Java 17 application copied to .AIJ plugins folder
- Maven build, compiles all dependencies into a single exe jar (uber jar)
- Swing application with FlatLightLaf Look and Feel, ref: https://www.formdev.com/flatlaf/
- currently Windows only, tested on WIN7 and WIN10
- next development stage is to extend to Linux.



