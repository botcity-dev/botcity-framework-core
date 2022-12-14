package dev.botcity.framework.web.parsers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class TableParser {
	
	/**
	 * Format a label name to remove invalid characters.
	 * 
	 * @param label The label name.
	 * @return The formatted label name.
	 */
	private static String formatLabel(String label) {
		StringBuffer buffer = new StringBuffer();
		for (Character c : label.toCharArray()) {
			if(Character.isLetterOrDigit(c) || Character.isSpaceChar(c))
				buffer.append(c);
		}
		return buffer.toString();
	}
	
	/**
	 * Extract data from a row and return it as a list.
	 * 
	 * @param row The row element.
	 * @param cellTag The HTML tag associated with the row cells.
	 * @param cellXpath The XPath expression associated with the row cels. Defaults to null.
            If informed, overwrites the `cellTag` definition.
	 * @return List of strings with the contents.
	 */
	public static List<String> dataFromRow(WebElement row, String cellTag, String cellXpath) {
		List<String> rowData = new ArrayList<String>(); 
		
		if(cellXpath != null && !cellXpath.isEmpty()) {
			for(WebElement col : row.findElements(By.xpath(cellXpath)))
				rowData.add(col.getText());
			return rowData;
		}
		
		for(WebElement col : row.findElements(By.tagName(cellTag)))
			rowData.add(col.getText());
		return rowData;
	}
	
	/**
	 * Sanitize header labels.
	 * 
	 * @param labels The labels to format.
	 * @return The List with the formatted labels.
	 */
	public static List<String> sanitizeHeader(List<String> labels) {
		List<String> formattedLabels = new ArrayList<String>();
		
		// Handle Treat Empty Header
		for(int index=0; index<labels.size(); index++) {
			String label = labels.get(index);
			if(!label.trim().isEmpty()) {
				// make it lowercase
				label = label.toLowerCase();
				
				// remove punctuations
				label = formatLabel(label);
				
				// replace spaces with underscores
				label = label.replace(" ", "_");
			}
			else {
				label = String.format("col_%s", Integer.toString(index));
			}
			formattedLabels.add(label);
		}
		// Deduplicate by adding _1, _2, _3 to repeated labels
		Map<String, Long> repeatedLabels = formattedLabels.stream()
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		repeatedLabels.values().removeIf(count -> count <= 1);
		
		for(int i=formattedLabels.size()-1; i>=0; i--) {
			String label = formattedLabels.get(i);
			Long labelCount = repeatedLabels.get(label);
			if(labelCount != null && labelCount >= 1) {
				String newLabel = String.format("%s_%s", label, Long.toString(labelCount));
				formattedLabels.set(i, newLabel);
				repeatedLabels.replace(label, labelCount - 1);
			}
		}
		return formattedLabels;
	}
	
	/**
	 * Convert a table WebElement to a list of maps
	 * 
	 * @param table The table element.
	 * @param hasHeader Whether or not to parse a header. Defaults to true.
	 * @param skipRows Number of rows to skip from the top. Defaults to 0.
	 * @param headerTag The HTML tag associated with the header cell. Defaults to "th".
	 * @param cellXpath Optional cell XPath selector for complex row constructions.
	 *  If `cellXpath` is not informed, the row data will come from "td" elements.
	 * @return The List with a Map for each table row.
	 */
	public static List<Map<String, String>> tableToMapArray(WebElement table, boolean hasHeader, int skipRows, String headerTag, String cellXpath) {
		List<String> labels = new ArrayList<String>();
		
		//Collect all rows from table
		List<WebElement> rows = table.findElements(By.tagName("tr"));
		
		// Skip rows if informed
		if(skipRows > 0)
			rows = rows.subList(skipRows, rows.size());
		
		// Convert into relative xpath
		if(cellXpath != null && !cellXpath.startsWith("."))
			cellXpath = String.format(".%s", cellXpath);
		
		// Parse header labels
		if(hasHeader) {
			// Read header labels
			labels = dataFromRow(rows.get(0), headerTag, null);
			// Sanitize headers
			labels = sanitizeHeader(labels);
			// Skip the header
			rows.remove(0);
		}
		else {
			// Make up header labels
			List<WebElement> cols = new ArrayList<WebElement>();
			if(cellXpath != null)
				cols = rows.get(0).findElements(By.xpath(cellXpath));
			else
				cols = rows.get(0).findElements(By.tagName("td"));
		
			for(int i=0; i<cols.size(); i++)
				labels.add(String.format("col_%s", Integer.toString(i)));
		}
		// Assemble output dictionary
		List<Map<String, String>> outputList = new ArrayList<Map<String, String>>();
		for(WebElement row : rows) {
			List<String> rowData = dataFromRow(row, "td", cellXpath);
			Map<String, String> rowMap = new LinkedHashMap<String, String>();
			
			for(int colIndex=0; colIndex<rowData.size(); colIndex++)	
				rowMap.put(labels.get(colIndex), rowData.get(colIndex));
			
			outputList.add(rowMap);
		}
		return outputList;
	}
	
	/**
	 * Convert a table WebElement to a list of maps
	 * 
	 * @param table The table element.
	 * @return The List with a Map for each table row.
	 */
	public static List<Map<String, String>> tableToMapArray(WebElement table) {
		return tableToMapArray(table, true, 0, "th", null);
	}
	
	/**
	 * Convert a table WebElement to a list of maps
	 * 
	 * @param table The table element.
	 * @param hasHeader Whether or not to parse a header. Defaults to true.
	 * @param skipRows Number of rows to skip from the top. Defaults to 0.
	 * @return The List with a Map for each table row.
	 */
	public static List<Map<String, String>> tableToMapArray(WebElement table, boolean hasHeader, int skipRows) {
		return tableToMapArray(table, hasHeader, skipRows, "th", null);
	}
	
	/**
	 * Convert a table WebElement to a list of maps
	 * 
	 * @param table The table element.
	 * @param hasHeader Whether or not to parse a header. Defaults to true.
	 * @param skipRows Number of rows to skip from the top. Defaults to 0.
	 * @param headerTag The HTML tag associated with the header cell. Defaults to "th".
	 * @return The List with a Map for each table row.
	 */
	public static List<Map<String, String>> tableToMapArray(WebElement table, boolean hasHeader, int skipRows, String headerTag) {
		return tableToMapArray(table, hasHeader, skipRows, headerTag, null);
	}
}
