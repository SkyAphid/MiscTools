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
		/*
		Configuration settings for the generator. Add all desired classes you want the snippets contain into the arrays.
		 */
		
		String pathToLuaToolsPackage = "D:/Projects/Eclipse Workspace/Git Projects/RobotFarm/RobotFarmGame/src/main/java/nokori/robotfarm/lua/tools/";

		//The classes to convert into snippets
		String[] classPaths = new String[]{
				pathToLuaToolsPackage + "LuaEntityTools.java",
				pathToLuaToolsPackage + "LuaGameTools.java",
				pathToLuaToolsPackage + "LuaHUDTools.java",
				pathToLuaToolsPackage + "LuaItemTools.java",
				pathToLuaToolsPackage + "LuaPlayerTools.java",
				pathToLuaToolsPackage + "LuaWorldTools.java"
		};

		//the corresponding globals names for the above files
		String[] globalsNames = new String[]{
				"entityTools",
				"gameTools",
				"hudTools",
				"itemTools",
				"playerTools",
				"worldTools"
		};

		//Snippets that are just generated as-is from the below array
		String[][] manualSnippets = new String[][]{
				{"GlobalTextKey", "GlobalTextKey", "GlobalTextKey", "Gets the container for the UI text keys. The built-in text keys can be accessed, but custom ones added can also be accessed if needed."},
				{"Element", "Element", "Element", "Robot Farm's Element enumerator, containing the keys for the various battle Elements."},
				{"ElementalAffinity", "ElementalAffinity", "ElementalAffinity", "Robot Farm's ElementalAffinity enumerator, containing the keys for the various affinity types for battle Elements."},

				{"ItemCategoryID", "ItemCategoryID", "ItemCategoryID", "The enumerator containing a list of IDs for Robot Farm's built-in item categories."},
				{"Property", "Property", "Property", "This enumerator contains all the Property-types for items."},
				{"TriggerEffect", "TriggerEffect", "TriggerEffect", "This enumerator contains all the Trigger Effect-types for items."},
				{"AgriculturalEquipmentType", "AgriculturalEquipmentType", "AgriculturalEquipmentType", "This enumerator contains all the types available for Agricultural Equipment (axes, hammers, etc)."},

				{"SpriteSheetID", "SpriteSheetID", "SpriteSheetID", "This class contains all of the accessible SpriteSheetIDs for Entities."},
				{"AokobotVariant", "AokobotVariant", "AokobotVariant", "This enumerator contains all the Aokobot variant types."},
				{"MonsterVariant", "MonsterVariant", "MonsterVariant", "This enumerator contains all the Monster variant types."},

				{"Personality", "Personality", "Personality", "This class allows access to the various Personality configurations and traits available for NPCs."},

				{"SettingFlag", "SettingFlag", "SettingFlag", "This class contains all of the SettingFlags that can be configured and accessed in the WorldRegistry."},

				{"EntityType", "EntityType", "EntityType", "This class contains all of the EntityTypes that are available in Robot Farm. Check the documentation for more information."},
				{"UniquePointer", "UniquePointer", "UniquePointer", "This class contains a list of IDs that are set on special entities. Check the documentation for more information."},

                {"random", "random", "random", "A coerced copy of a Java Random class, allowing access to its arguably better tools than Lua's counterpart."},
				{"JavaArray", "JavaArray", "JavaArray", "This utility class will allow you to access arrays as they are in Java and other programming languages. Meaning that the starting index will be 0 (Arrays) instead of 1 (Lua tables)."},
				{"JavaUtil", "JavaUtil", "JavaUtil", "This utility class contains various tools from Java for use in your Lua code."}
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
		Begin generation.
		 */

		String snippetOutput = fileHeader;

		for (int i = 0; i < classPaths.length; i++){
			snippetOutput += "\t#" + globalsNames[i] + " globals\n\n";
			snippetOutput += run(classPaths[i], globalsNames[i], functionFilter);
		}

		for (int i = 0; i < manualSnippets.length; i++){
			snippetOutput += generateSnippet(manualSnippets[i][0], manualSnippets[i][1], manualSnippets[i][2], manualSnippets[i][3]);
		}

		/*
		Copy and paste the results into the clipboard.
		 */
		StringSelection selection = new StringSelection(snippetOutput);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(selection, selection);

		System.out.println("\nCopied snippets to clipboard. Remember that you'll need to post-process these by hand.");
	}

	public static String run(String classPath, String globalsName, ArrayList<String> functionFilter){
		// I just input the settings manually like this, it's faster
		File classFile = new File(classPath);

		//Begin conversion
		Stack<String> rawFunctions = new Stack<>();
		Stack<String> snippetFunctions = new Stack<>();
		Stack<String> descriptions = new Stack<>();

		getFunctions(classFile, rawFunctions, snippetFunctions, descriptions, functionFilter);

		Collections.reverse(rawFunctions);
		Collections.reverse(snippetFunctions);
		Collections.reverse(descriptions);

		String snippets = buildSnippets(globalsName, rawFunctions, snippetFunctions, descriptions);

		return snippets;
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
	public static String buildSnippets(String globalsName, Stack<String> rawFunctions, Stack<String> snippetFunctions, Stack<String> descriptions) {
		
		System.out.println("\nStarting buildSnippets():");
		
		String snippets = "";
		
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
