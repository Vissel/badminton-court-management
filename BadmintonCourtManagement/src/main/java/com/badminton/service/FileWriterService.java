package com.badminton.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import jakarta.servlet.ServletContext;

//@Component
/**
 * not use yet
 */
public class FileWriterService {

	private final ServletContext servletContext;
	private final String folder;

	public FileWriterService(ServletContext servletContext) {
		this.servletContext = servletContext;
		folder = "database-data";
	}

	/**
	 * Writes content to a file in Tomcat's deployment folder under "database-data".
	 * Creates file and directories if not exist.
	 *
	 * @param playerName Name of the player
	 * @param services   Services string
	 * @param number1    An integer value to include in filename
	 * @param content    The string content to write
	 * @return Absolute file path
	 * @throws IOException if writing fails
	 */
	public String writeAvailableFile(String playerName, String services, int number1, String content)
			throws IOException {
		// Detect Tomcat deployment root path
		String deploymentPath = servletContext.getRealPath("/");

		Path folderPath;
		if (deploymentPath != null) {
			// Production (external Tomcat)
			folderPath = Paths.get(deploymentPath, folder);
		} else {
			// Development (Spring Boot project root)
			String projectRoot = System.getProperty("user.dir");
			folderPath = Paths.get(projectRoot, folder);
		}

		Files.createDirectories(folderPath);

		// Build file name: available<PlayerName><Services>_<yyMMddHHmm>_<number1>.txt
		String timestamp = new SimpleDateFormat("yyMMddHHmm").format(new Date());
		String fileName = String.format("available%s%s_%s_%d.txt", sanitize(playerName), sanitize(services), timestamp,
				number1);

		Path filePath = folderPath.resolve(fileName);

		// Write the content into the file (create if not exist)
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile(), false))) {
			writer.write(content);
		}

		return filePath.toAbsolutePath().toString();
	}

	// Ensure file name is safe for all OS
	private String sanitize(String input) {
		return input == null ? "" : input.replaceAll("[^a-zA-Z0-9_-]", "");
	}
}
