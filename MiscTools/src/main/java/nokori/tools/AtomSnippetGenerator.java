package nokori.tools;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
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
	
	private static final String PROJECT_PATH = "D:/Projects/Eclipse Workspace/Git Projects/RobotFarm/RobotFarmGame/src/main/java/nokori/robotfarm/";
	private static final String LUA_TOOLS_PACKAGE = PROJECT_PATH + "lua/tools/";
	private static final String ENTITY_DESCRIPTOR_PACKAGE = PROJECT_PATH + "entity/descriptors/";
	
	public static final void main(String[] args) {
		/*
		Configuration settings for the generator. Add all desired classes you want the snippets contain into the arrays.
		 */
		//Globals names paired with their respective files
		//Instantiated globals start with lowercase letters, static ones start with capital letters
		String[][] globals = new String[][]{
			//Lua tools
			{"entityTools", LUA_TOOLS_PACKAGE + "LuaEntityTools.java"},
			{"gameTools", LUA_TOOLS_PACKAGE + "LuaGameTools.java"},
			{"hudTools", LUA_TOOLS_PACKAGE + "LuaHUDTools.java"},
			{"itemTools", LUA_TOOLS_PACKAGE + "LuaItemTools.java"},
			{"playerTools", LUA_TOOLS_PACKAGE + "LuaPlayerTools.java"},
			{"worldTools", LUA_TOOLS_PACKAGE + "LuaWorldTools.java"},
			{"itemDatabase", PROJECT_PATH + "item/ItemDatabase.java"},
			
			//Java utilities
			{"JavaArray", LUA_TOOLS_PACKAGE + "JavaArray.java"},
			{"JavaUtil", LUA_TOOLS_PACKAGE + "JavaUtil.java"},
			
			//Text keys
			{"GlobalTextKey", PROJECT_PATH + "textkeys/GlobalTextKey.java"},
			
			//Combat
			{"Element", PROJECT_PATH + "battle/element/Element.java"},
			{"ElementalAffinity", PROJECT_PATH + "battle/element/ElementalAffinity.java"},
			
			//Items
			{"ItemCategoryID", PROJECT_PATH + "item/ItemCategoryID.java"},
			{"Property", PROJECT_PATH + "bestiary/Property.java"},
			{"TriggerEffect", PROJECT_PATH + "item/TriggerEffect.java"},
			{"AgriculturalToolType", PROJECT_PATH + "/item/AgriculturalToolType.java"},
			
			//Personality entity component
			{"Personality", PROJECT_PATH + "entity/components/Personality.java"},
			
			//WorldRegistry
			{"SettingFlag", PROJECT_PATH + "world/WorldRegistry.java"},
			
			//Entity descriptors
			{"Alignment", ENTITY_DESCRIPTOR_PACKAGE + "Alignment.java"},
			{"EntityType", ENTITY_DESCRIPTOR_PACKAGE + "EntityType.java"},
			{"Gender", ENTITY_DESCRIPTOR_PACKAGE + "Gender.java"},
			{"Job", ENTITY_DESCRIPTOR_PACKAGE + "Job.java"},
			{"UniquePointer", ENTITY_DESCRIPTOR_PACKAGE + "UniquePointer.java"}
		};

		//Snippets that are just generated as-is from the below array. These are for classes like built-in java ones where we can't read the source file.
		//Array Order: SnippetName, SnippetPrefix, SnippetBody, SnippetDescription
		String[][] manualSnippets = new String[][]{
                {"random", "random", "random", "A coerced copy of a Java Random object, allowing access to its arguably better tools than Lua's counterpart. "
                		+ "Look up its own documentation for more information."},
		};

		//Create filter for functions I want to be ignored - such as inner anonymous callbacks & functions
		ArrayList<String> functionFilter = new ArrayList<>();
		functionFilter.add("callback()");
		functionFilter.add("select()");
		functionFilter.add("select(index)");
		functionFilter.add("getName()");
		functionFilter.add("getDesc()");

		//This will be adding to the front of the output. Adding in the header directly to the generator allows for me to just use CTRL-A, CTRL-V for pasing into the snippets CSON file.
		String fileHeader = "'.source.lua':\n" +
				"\n" +
				"\t#This snippets file will allow the user to shortcut all of the functions available in the Robot Farm API\n" +
				"\t#I'd normally go all out and make some sort of proper auto-complete system, but I don't think it's worth the effort when this works good enough\n" +
				"\t#I use underscores to denote the usual lua colon (denoting functions) because Atom's autocomplete system cancels itself out if you actually use a colon, sorry about that\n";

		/*
		 * Begin Generation
		 */

		String snippetOutput = fileHeader;

		//Generated snippets from java files
		for (int i = 0; i < globals.length; i++){
			snippetOutput += "\t#" + globals[i][0] + " Globals\n\n";
			snippetOutput += run(globals[i][1], globals[i][0], functionFilter);
		}

		//Manually created snippets
		for (int i = 0; i < manualSnippets.length; i++){
			snippetOutput += "\t#" + manualSnippets[i][0] + " Globals\n\n";
			snippetOutput += generateSnippet(manualSnippets[i][0], manualSnippets[i][1], manualSnippets[i][2], manualSnippets[i][3]);
		}
			
		/*
		 * Copy and paste the results into the clipboard.
		 */
		
		StringSelection selection = new StringSelection(snippetOutput);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(selection, selection);

		System.out.println("\nCopied snippets to clipboard.");
	}

	public static String run(String classPath, String globalsName, ArrayList<String> functionFilter){
		// I just input the settings manually like this, it's faster
		File classFile = new File(classPath);
		
		try {
			String content = new String(Files.readAllBytes(classFile.toPath()));
			
			/*
			 * Build enumerator key snippets for the given content
			 */
			
			String[][] enumKeys = getEnumKeys(classPath, content);
			
			/*
			 * Build function snippets for the given content
			 */
			
			//Fetch function names and put them into Stacks
			//The indices of each Stack correspond to each other
			Stack<String> rawFunctions = new Stack<>();
			Stack<String> snippetFunctions = new Stack<>();
			Stack<String> descriptions = new Stack<>();

			getFunctions(content, rawFunctions, snippetFunctions, descriptions, functionFilter);

			Collections.reverse(rawFunctions);
			Collections.reverse(snippetFunctions);
			Collections.reverse(descriptions);

			String snippets = "";
			
			if (!rawFunctions.isEmpty()) {
				snippets = buildSnippets(globalsName, enumKeys, rawFunctions, snippetFunctions, descriptions);
			}
			
			return snippets;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Fetches an array of the key entries and their comments (if applicable) 
	 */
	public static String[][] getEnumKeys(String classPath, String content) {
		System.out.println("Starting getEnumKeys() ------------------------------------------------------------------------");

		String header = content.substring(0, content.indexOf("{") + 1);
		
		if (header.contains("enum")) {
			System.out.println(classPath.replace(PROJECT_PATH, "") + " verified as an enumerator. Extracting keys into Atom snippets...");

			//Remove the header, leaving the enum keys at the top of the string
			String headerlessContent = content.replace(header, "");
			
			//Process the headerlessContent into the "raw keys," or the section of the class containing all of the enum keys 
			int keysEnd = headerlessContent.indexOf(";");
			
			if (keysEnd == -1) {
				//Some enum classes don't use a semi-colon to end them, but rather the whole class is just a set of enum keys. In which case, we'll use the ending bracket to find the end.
				keysEnd = headerlessContent.indexOf("}");
			}
			
			String rawKeys = headerlessContent.substring(0, keysEnd).replace("\t", "");
			
			//Check the enum uses constructors. Remove them to prevent the following split() call from getting confused by constructor function arguments
			while (rawKeys.contains("(") && rawKeys.contains(")")) {
				String constructor = rawKeys.substring(rawKeys.indexOf("("), rawKeys.indexOf(")") + 1);
				rawKeys = rawKeys.replace(constructor, "");
			}
			
			//Finally, build the array of isolated keys & comments
			String[] keys = rawKeys.split(",");
			String[] comments = new String[keys.length];
			
			for (int i = 0; i < keys.length; i++) {
				//Extract comments 
				if (keys[i].startsWith("//")) {
					comments[i] = keys[i].substring(2, keys[i].indexOf(System.lineSeparator()));
					keys[i] = keys[i].replace("//" + comments[i], "");
				} else {
					comments[i] = "";
				}
				
				//Trim keys and comments and remove unnecessary spaces
				keys[i] = keys[i].replace(System.lineSeparator(), "").trim();
				comments[i] = comments[i].trim();
			}
			
			//Print the result
			System.out.println("Result:");
			for (int i = 0; i < keys.length; i++) {
				System.out.println("Index: " + i + " Key: " + keys[i] + " Comment: " + comments[i]);
			}
			
			System.out.println("");
			
			return new String[][] {keys, comments};
		} else {
			System.out.println(classPath.replace(PROJECT_PATH, "") + " is not an enumerator. Aborting.\n");
		}
		
		return null;
	}
	
	/**
	 * Fetches a list of functions from a class file that can be inserted into a .txt file for use by <code>buildSnippets()</code>
	 * 
	 * Keep in mind that it's not perfect and may need human trimming.
	 */
	public static void getFunctions(String content, Stack<String> rawFunctions, Stack<String> snippetFunctions, Stack<String> descriptions, ArrayList<String> functionFilter) {
		System.out.println("Starting getFunctions() ------------------------------------------------------------------------");

		/*
		 * Split the file by new line and read each line for function names
		 */
		String[] splitContent = content.split("\n");
		
		for (int i = 0; i < splitContent.length; i++) {

			/*
			 * 
			 * STEP 1: Begin basic setup & weed out undesirable content
			 * 
			 */

			String f = splitContent[i];

			//These originally were : \tpublic static & \tpublic, but some classes don't have this formatting, so I'm removing the tab characters for now.
			String startTag = f.contains("static") ? "public static " : "public ";
			String endTag = "{";

			int start = f.indexOf(startTag);
			int end = f.indexOf(endTag, start);

			// This weeds out undesirable content (non-functions)
			if (start == -1 || end == -1) {
				continue;
			}

			/*
			 * STEP 2: Fetches the comment above the function if there is one and use it as
			 * a description for the snippet
			 */

			String description = "";

			int upIterator = i - 1;
			
			String singleLineCommentTag = "//";
			
			String multiLineCommentStartTag = "*/";
			String multiLineCommentBodyTag = "\t *";
			String multiLineCommentEndTag = "*/";
			String multiLineCommentParameter = "* @";
			
			descIterator:
			while (upIterator >= 0) {
				String d = splitContent[upIterator];

				//Checks the current line for a comment. If no comment is found, end the description search.
				if (d.startsWith(singleLineCommentTag)) {
					//One line comment
					description += d.replace(singleLineCommentTag, "");
				} else if (d.contains(multiLineCommentStartTag) || d.contains(multiLineCommentBodyTag) || d.contains(multiLineCommentEndTag)) {
					//Multi-line comment (ignores parameter/return-type lines)
					if (!d.contains(multiLineCommentParameter)) {
						description += d.replace(multiLineCommentStartTag, "").replace(multiLineCommentBodyTag, "").replace(multiLineCommentEndTag, "");
					}
				} else {
					break descIterator;
				}
				
				upIterator--;
			}
			
			description = description.replace("\t", "").replace("\n", "").replace("\r", "").trim();

			/*
			 * 
			 * 
			 * STEP 3: Find public functions and isolate them
			 * 
			 * 
			 */

			// We keep two versions of the function on hand:
			// function: a version without Atom tab stops (used for naming the snippets
			// later)
			// snippetFunction: a version with Atom tab stops (e.g. the ${1:argument} bits)
			// (for making the snippets later)
			String function, snippetFunction;

			// 1) Find the "public void functionName()" bit, remove the "public", this
			// leaves us with "void functionName()"
			// 2) Jump to the first space after that to isolate and remove the return type,
			// ideally leaving us with "functionName()"
			function = f.substring(start, end).replace(startTag, "");
			function = function.substring(function.indexOf(" ") + 1);

			/*
			 * 
			 * STEP 4: Next, we isolate the arguments and simplify it down to a prettier
			 * form for the Atom snippet
			 * 
			 */

			int parenthesisStart = function.indexOf("(");
			int parenthesisEnd = function.indexOf(")");

			// If there are no parenthesis, it's not a function (it might be a class name
			// that made it through)
			if (parenthesisStart == -1 || parenthesisEnd == -1) {
				continue;
			}

			// This will make the following edits easier
			parenthesisStart++;
			parenthesisEnd++;

			String arguments = function.substring(parenthesisStart, parenthesisEnd);
			String[] argumentsArray = arguments.split(" ");
			snippetFunction = function = function.replace(arguments, "").trim();

			/*
			 * 
			 * STEP 5: Re-add the variable names back in without the types (shortens the
			 * name and cleans it up)
			 * 
			 * From here we also start separating the snippetFunction/rawFunction operations
			 * 
			 */

			int modulo = 2;
			int tabStops = 1;

			for (int j = 0; j < argumentsArray.length; j++) {
				if (j % modulo == 0) {
					continue;
				}

				// Create the raw function without the snippet syntax
				function += argumentsArray[j].replace("(", "").replace(")", "");

				// Create a version of the function with snippet tab stops for ease of use
				snippetFunction += "${" + tabStops + ":"
						+ argumentsArray[j].replace("(", "").replace(")", "").replace(",", "},");
				tabStops++;

				// Clean up the functions after
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

			/*
			 * 
			 * 
			 * STEP 6: Finalization and cleanup
			 * 
			 * 
			 */

			// Add the final ending parenthesis in and close the tab stops (if there's more
			// than just the one at the end)
			function += ")";

			if (tabStops > 1) {
				snippetFunction += "}";
			}

			snippetFunction += ")$" + tabStops;

			// If the final function is in the filter, don't add it
			if (functionFilter.contains(function)) {
				System.out.println(function + " is in the function filter. Skipping...\n");
				continue;
			}
			
			System.out.println("Result: ");
			System.out.println("Raw Function: " + function);
			System.out.println("Snippet Function: " + snippetFunction);
			System.out.println("Description: " + description);

			if (!rawFunctions.contains(function)) {
				// Add function to the list if its not already in there 
				rawFunctions.push(function);
				snippetFunctions.push(snippetFunction);
				descriptions.push(description);
				System.out.println("Added to list. Continuing...");
			} else {
				System.out.println("Duplicate entry detected. Continuing...");
			}
			
			System.out.println();
		}

		if (rawFunctions.isEmpty()) {
			System.out.println("No functions found. Continuing...");
		}
	}

	/**
	 * Automatically generates a snippets .cson file beside the given .txt file of functionNames. The functions must be separated by new lines (\n), which is
	 * done automatically by <code>getFunctions()</code>. The filename is the assumed name of the globals that the snippets are being generated for.
	 */
	public static String buildSnippets(String globalsName, String[][] enumKeys, Stack<String> rawFunctions, Stack<String> snippetFunctions, Stack<String> descriptions) {
		
		System.out.println("\nStarting buildSnippets(): ------------------------------------------------------------------------");
		
		String snippets = "";
		
		//Create enum key snippets (if applicable)
		if (enumKeys != null) {
			for (int i = 0; i < enumKeys.length; i++) {
				snippets += generateSnippet(globalsName + "." + enumKeys[0][i], globalsName + "_" + enumKeys[0][i], globalsName + "." + enumKeys[0][i], enumKeys[1][i]);
			}
		}
		
		//Create function snippets (if applicable)
		while(!rawFunctions.isEmpty()) {
			String rawFunction = rawFunctions.pop();
			String snippetFunction = snippetFunctions.pop();
			String description = descriptions.pop();
			
			snippets += generateSnippet(globalsName + "." + rawFunction, globalsName + "_" + rawFunction, globalsName + "." + snippetFunction, description);
		}
		
		System.out.print(snippets);

		return snippets;
	}

	public static String generateSnippet(String snippetName, String snippetPrefix, String snippetBody, String snippetDescription){
		String snippet = "";

		snippet += "\t'" + snippetName + "':";
		snippet += "\n\t\t'prefix': '" + snippetPrefix + "'";
		snippet += "\n\t\t'body': '" + snippetBody + "'";
		snippet += "\n\t\t'description': '" + parseCSONSpecialCharacters(snippetDescription) + "'";
		snippet += "\n\n";

		return snippet;
	}

	public static String parseCSONSpecialCharacters(String s){
		StringBuilder b = new StringBuilder(s);

		boolean quoteSwitch = false;

		for (int i = 0; i < s.length(); i++){
			char c = b.charAt(i);

			if (c == '\'') {
				b.setCharAt(i, '’');
			}

			if (c == '\"'){
				if (quoteSwitch){
					b.setCharAt(i, '”');
				} else{
					b.setCharAt(i, '“');
				}

				quoteSwitch = !quoteSwitch;
			}
		}

		return b.toString();
	}
}
