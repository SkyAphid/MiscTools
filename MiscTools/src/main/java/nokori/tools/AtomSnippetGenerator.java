package nokori.tools;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;

/**
 * This class will allow you to select a Java class file and generate corresponding Atom snippets for them. It's intended to be used with LuaJava implementations where 
 * you'll frequently be calling Java code from Lua, and wanted a documented method of calling them from Atom. This is so that you're not constantly referring to your Java 
 * code when working in Lua, and you can get auto-complete suggestions on the fly.
 */
public class AtomSnippetGenerator {
	
	public static final void main(String[] args) {
		
		// I just input the settings manually like this, it's faster
		File classFile = new File("D:/Projects/Git/RobotFarm/RobotFarmGame/src/nokori/robotfarm/lua/tools/LuaHUDTools.java");
		String globalsName = "hudTools";
		
		//Create filter for functions I want to be ignored - such as inner anonymous callbacks
		ArrayList<String> functionFilter = new ArrayList<>();
		functionFilter.add("callback()");
		functionFilter.add("select()");
		functionFilter.add("select(index)");
		
		//Begin conversion
		Stack<String> rawFunctions = new Stack<>();
		Stack<String> snippetFunctions = new Stack<>();
		Stack<String> descriptions = new Stack<>();
		
		getFunctions(classFile, rawFunctions, snippetFunctions, descriptions, functionFilter);
		
		Collections.reverse(rawFunctions);
		Collections.reverse(snippetFunctions);
		Collections.reverse(descriptions);
		
		buildSnippets(globalsName, rawFunctions, snippetFunctions, descriptions);
	}
	
	/**
	 * Fetches a list of functions from a class file that can be inserted into a .txt file for use by <code>buildSnippets()</code>
	 * 
	 * Keep in mind that it's not perfect and may need human trimming.
	 */
	public static void getFunctions(File classFile, Stack<String> rawFunctions, Stack<String> snippetFunctions, Stack<String> descriptions, ArrayList<String> functionFilter) {
		System.out.println("Starting getFunctions():");

		try {
			String content = new String(Files.readAllBytes(classFile.toPath()));

			// Split the file by new line and read each line for function names
			String[] splitContent = content.split("\n");

			for (int i = 0; i < splitContent.length; i++) {
				String f = splitContent[i];

				String startTag = "\tpublic ";
				String endTag = "{";

				int start = f.indexOf(startTag);
				int end = f.indexOf(endTag, start);

				//This weeds out undesirable content (non-functions)
				if (start == -1 || end == -1) {
					continue;
				}
				
				//Fetches the comment above if there is one (uses it as the description)
				String description = "";
				
				if (i - 1 >= 0) {
					String d = splitContent[i-1].replace("\t", "").replace("\n", "").replace("\r", "");
					
					if (d.startsWith("//")) {
						description = d.replace("//", "");
					}
				}

				//Find public functions and isolate them
				//Step 1: find the "public void functionName()" bit, remove the "public", this leaves us with "void functionName()"
				//Step 2: jump to the first space after that to isolate and remove the return type
				//Note: we also keep two versions of the function on hand: one with tab stops (for making the snippets later) and one without (for naming the snippets)
				String function = f.substring(start, end).replace(startTag, "");
				String snippetFunction;
				
				function = function.substring(function.indexOf(" ") + 1);
				
				//Next, we isolate the arguments and simplify it down to a prettier form for the Atom snippet
				int parenthesisStart = function.indexOf("(");
				int parenthesisEnd = function.indexOf(")");
				
				//If there are no parenthesis, it's not a function (it might be a class name that made it through)
				if (parenthesisStart == -1 || parenthesisEnd == -1) {
					continue;
				}
				
				//This will make the following edits easier
				parenthesisStart++;
				parenthesisEnd++;
				
				String arguments = function.substring(parenthesisStart, parenthesisEnd);
				String[] argumentsArray = arguments.split(" ");
				snippetFunction = function = function.replace(arguments, "").trim();
				
				//Re-add the variable names back in without the types (shortens the name and cleans it up)
				//From here we also start separating the snippetFunction/rawFunction operations
				int modulo = 2;
				int tabStops = 1;
				
				for (int j = 0; j < argumentsArray.length; j++) {
					if (j % modulo == 0) {
						continue;
					}
					
					//Create the raw function without the snippet syntax
					function += argumentsArray[j].replace("(", "").replace(")", "");
					
					//Create a version of the function with snippet tab stops for ease of use
					snippetFunction += "${" + tabStops + ":" + argumentsArray[j].replace("(", "").replace(")", "").replace(",", "},");
					tabStops++;
					
					//Clean up the functions after
					if (function.endsWith(",")) {
						if (j + modulo < argumentsArray.length) {
							function += " ";
							snippetFunction += " ";
						} else {
							function = function.substring(0, function.length() - 1);
							snippetFunction = snippetFunction.substring(0, snippetFunction.length() - 1);
						}
					}
				}

				//And finally, add the final ending parenthesis in
				function += ")";
				snippetFunction += "})$" + tabStops;
				
				//If the final function is in the filter, don't add it
				if (functionFilter.contains(function)) {
					System.out.println(function + " is in the function filter. Skipping...\n");
					continue;
				}
				
				System.out.println("Result: ");
				System.out.println("Raw Function: " + function);
				System.out.println("Snippet Function: " + snippetFunction);
				System.out.println("Description: " + description);
				System.out.println();
				
				//Add function to the list
				rawFunctions.push(function);
				snippetFunctions.push(snippetFunction);
				descriptions.push(description);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Automatically generates a snippets .cson file beside the given .txt file of functionNames. The functions must be separated by new lines (\n), which is
	 * done automatically by <code>getFunctions()</code>. The filename is the assumed name of the globals that the snippets are being generated for.
	 */
	public static void buildSnippets(String globalsName, Stack<String> rawFunctions, Stack<String> snippetFunctions, Stack<String> descriptions) {
		
		System.out.println("\nStarting buildSnippets():");
		
		String snippets = "";
		
		while(!rawFunctions.isEmpty()) {
			String rawFunction = rawFunctions.pop();
			String snippetFunction = snippetFunctions.pop();
			String description = descriptions.pop();
			
			snippets += "\t'" + globalsName + ":" + rawFunction + "':";
			snippets += "\n\t\t'prefix': '" + globalsName + "_" + rawFunction + "'";
			snippets += "\n\t\t'body': '" + globalsName + ":" + snippetFunction + "'";
			snippets += "\n\t\t'description': '" + description + "'";
			snippets += "\n\n";
		}
		
		System.out.print(snippets);
		
		StringSelection selection = new StringSelection(snippets);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(selection, selection);
		
		System.out.println("\nCopied snippets to clipboard. Remember that you'll need to post-process these by hand.");
	}
}
