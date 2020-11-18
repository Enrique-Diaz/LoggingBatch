package com.batch.processor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.batch.model.Log;

@Component
public class ProcessLogs {
	
	@Autowired
	private Logger logger;

	private Map<String, RandomAccessFile> filesMap = new HashMap<>();
	private Map<String, Long> indexMap = new HashMap<>();
	private List<Log> orderedLog = new ArrayList<>();
	
	private final String DIRECTORY_PATH = "/temp/";
	private final int NUMBER_OF_LINES = 1000;
	private final int MINUS_X_SECONDS = 10;
	private final String ALL_LOGS_FILE_NAME = "all_logs.txt";

	public void runLogs() {
		logger.info("Starts merging logs");
		listFilesForFolder(new File(DIRECTORY_PATH));

		while (!filesMap.isEmpty()) {
			readXAmountofLines(NUMBER_OF_LINES);
			//printLinesToConsole();
			writeToFile();
		}
		
		logger.info("Writting left over logs");
		writeLeftOver(orderedLog);
		
		logger.info("Finish merging logs");
	}

	private void writeToFile() {
		if (!orderedLog.isEmpty()) {
			List<Log> tempLogs = new ArrayList<>();
			List<Log> keepLogs = new ArrayList<>();

			// Getting last object on the list
			Log log = orderedLog.get(orderedLog.size() - 1);

			// Set minus 10 seconds on the latest element to search last_time minus 10 seconds
			log.setTime(log.getTime().minusSeconds(MINUS_X_SECONDS));

			// Search last_time minus 10 seconds
			int logIndex = Collections.binarySearch(orderedLog, log, Comparator.comparing(Log::getTime));

			// If index found get previous element
			if (logIndex > 0) {
				tempLogs.addAll(orderedLog.subList(0, logIndex));
				keepLogs.addAll(orderedLog.subList(logIndex, orderedLog.size()));

				orderedLog.clear();
				orderedLog.addAll(keepLogs);
				keepLogs.clear();
				
				writeFile(tempLogs);
			} else { // If index negavite element was not found get 2th element behind the end
				tempLogs.addAll(orderedLog.subList(0, orderedLog.size() - 2));
				keepLogs.addAll(orderedLog.subList(orderedLog.size() - 2, orderedLog.size()));
				
				orderedLog.clear();
				orderedLog.addAll(keepLogs);
				keepLogs.clear();
				
				writeFile(tempLogs);
			}
		}
	}
	
	private void writeLeftOver(List<Log> leftOverLogs) {
		writeFile(leftOverLogs);
	}

	private void writeFile(List<Log> tempLogs) {
		try {
			// If file exists write at the end
			FileWriter myWriter = new FileWriter(DIRECTORY_PATH + ALL_LOGS_FILE_NAME, true);
			for (Log log: tempLogs) {
				myWriter.write(log.getLine() + "\n");
			}
			myWriter.close();
			tempLogs.clear();
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	private void printLinesToConsole() {
		for (Log log : orderedLog) {
			System.out.println(log.getLine());
		}
	}

	private void readXAmountofLines(int numberOfLines) {
		Iterator<Entry<String, RandomAccessFile>> it = filesMap.entrySet().iterator();
		
		// Iterate over all possible files
		while (it.hasNext()) {
			Entry<String, RandomAccessFile> entry = it.next();
			
			// Reference to the log file
			RandomAccessFile file = (RandomAccessFile) entry.getValue();
			
			// If Index doesn't exist return 0
			// If Index exists get that index plus 1
			long fileIndex = indexMap.containsKey(entry.getKey()) ? indexMap.get(entry.getKey()) + 1 : 0;
			String line;
			
			try {
				// Position over the index for the given file
				file.seek(fileIndex);
				for (int i = 0; i < numberOfLines; i++) {

					// If reached null file is over and remove it from the Map
					if ((line = file.readLine()) == null) {
						it.remove();
						break;
					}

					String splitted[] = line.split(",", 2);
					Log log = new Log();

					log.setLine(line);
					log.setTime(Instant.parse(splitted[0]));

					// Checks the index where to store the next log to maintain order.
					int logIndex = Collections.binarySearch(orderedLog, log, Comparator.comparing(Log::getTime));
					if (logIndex < 0) {
						logIndex = -logIndex - 1;
					}
					
					orderedLog.add(logIndex, log);
				}

				// Save the index of last line readed
				indexMap.put((String) entry.getKey(), file.getFilePointer());
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
	}

	private void listFilesForFolder(final File folder) {
		// Get all files and ommit all_logs if exists
		for (final File fileEntry : folder.listFiles()) {
			if (fileEntry.isFile() && !fileEntry.getName().equalsIgnoreCase(ALL_LOGS_FILE_NAME)) {
				try {
					filesMap.put(fileEntry.getName(), new RandomAccessFile(fileEntry.getAbsolutePath(), "r"));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				// For recursiveness i.e. folder/folder/file
				// } else {
				// listFilesForFolder(fileEntry);
			}
		}
	}
}