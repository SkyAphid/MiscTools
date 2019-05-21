package nokori.tools;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;

/**
 * This is a mass find and replace program. The 2D Array "FIND_AND_REPLACE" at the bottom contains the content to be found and replaced. 
 * 
 * <br><br>The structure of that array works like this:
 * <br>{
 * <br>		{$FIND_ENTRY_1, $REPLACE_ENTRY_1},
 * <br>		{$FIND_ENTRY_2, $REPLACE_ENTRY_2}, 
 * <br>}
 * 
 */
public class AutomatedFindAndReplaceProgram {
	
	public static final String EXTENSION = ".json";
	
	public static final void main(String[] args) {
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.showOpenDialog(null);
		
		File location = fc.getSelectedFile();
		
		System.out.println("Reading from directory: " + location.toString());
		
		try {
			recursiveRead(location.listFiles());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void recursiveRead(File[] files) throws Exception{
		for(int i = 0; i < files.length; i++){
			File f = files[i];
			
			if(f.isDirectory()){
				recursiveRead(f.listFiles());
			}else{
				String n = f.getName();
				
				System.out.println("Checking " + f.getName());
				
				if (n.endsWith(EXTENSION)) {
					System.out.println("Finding & Replacing in: " + n);
					
					String contents = new String(Files.readAllBytes(f.toPath()));
					
					for (int j = 0; j < FIND_AND_REPLACE.length; j++) {
						String[] set = FIND_AND_REPLACE[j];
						
						contents = contents.replace(set[0], set[1]);
					}
					
					FileWriter fileWriter = new FileWriter(f);
					fileWriter.write(contents);
					fileWriter.close();
					
					System.out.println("File saved. Continuing...\n");
				}
			}
		}
		
		System.out.println("Find & Replace complete. Closing program.");
	}
	
	public static final String[][] FIND_AND_REPLACE = {
			{ "SmallTalkLines", "Lines" }
	};
}
