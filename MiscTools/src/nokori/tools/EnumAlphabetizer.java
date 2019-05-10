package nokori.tools;

import java.io.File;
import java.nio.file.Files;

import javax.swing.JFileChooser;

public class EnumAlphabetizer {
	
	@SuppressWarnings("unused")
	public static final void main(String[] args) {
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Select Enum .java file");
		fc.showOpenDialog(null);
		
		File f = fc.getSelectedFile();
		
		try {
			String contents = new String(Files.readAllBytes(f.toPath()));
			
			//contents.substring(contents.indexOf("{"), contents.)
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
