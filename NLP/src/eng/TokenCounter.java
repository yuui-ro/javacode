package eng;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import util.SProperties;

/**
 * A TokenCounter object counts the appearance times of different tokens in
 * files; however, there are two regular expressions for token selection, one
 * for collecting tokens and the other for scraping tokens, and only those
 * tokens that conform to the {@linkplain #countPattern collecting regular
 * expression} and do not match the {@linkplain #ignorePattern scraping one} are
 * counted.
 * 
 * TokenCounter can be executed from command line with options to specify the
 * target files whose tokens are counted, and output the frequency of tokens to
 * a file. Use the following options:
 * 
 * <ul>
 * <li>-input: input file or directory</li>
 * <li>-output: the output file recording the counting results</li>
 * <li>-countpattern: the regular expression for tokens to be collected for
 * counting</li>
 * <li>-ignorepattern: the regular expression for keeping tokens from being
 * counted</li>
 * <li>-outputfilter: the mode for output token selection. <br>
 * Use outputfilter.name to specify the filter to use. At the moment, it can be
 * "fixedsize" or "largerthan", and outputfilter.value is used to specify the
 * arguments for the specified filter.</li>
 * </ul>
 * 
 * @author Youwei Lu
 */
public class TokenCounter {

	/**
	 * Specified counting target, which may be a file name or a directory. When
	 * it is a single file, the tokens in the file will be counted; however,
	 * when it is a directory, tokens in all files in the directory will be
	 * counted.
	 */
	String inputf;
	/**
	 * The name of file to which the counting result will be written.
	 */
	String countf;
	/**
	 * A regular expression for matched tokens to be filtered out from counting
	 */
	String ignorePattern;
	/**
	 * A regular expression for matched tokens to be counted
	 */
	String countPattern;
	/**
	 * A counting map between tokens and their frequencies
	 */
	private Map<String, Integer> count;
	/**
	 * Constant standing for sorting in ascending order.
	 */
	static public final int ASC = 0;
	/**
	 * Constant standing for sorting in descending order.
	 */
	static public final int DESC = 1;

	private TokenOutputSelector outputSelector;

	public TokenCounter(String inputf, String countf) {
		this(inputf, countf, ".+", "");
	}

	private TokenCounter(String inputf, String countf, String countPattern, String ignorePattern) {
		this.inputf = inputf;
		this.countf = countf;
		this.countPattern = countPattern;
		this.ignorePattern = ignorePattern;
		count = new HashMap<String, Integer>();
	}

	public void setOutputTokenSelector(String selectorName, String selectorValue) {
		if (selectorName.equals("fixedsize")) {
			outputSelector = new FixedVocabularySizeTokenSelector(Integer.valueOf(selectorValue));
		} else if (selectorName.equals("largerthan")) {
			outputSelector = new LargerThanTokenSelector(Integer.valueOf(selectorValue));
		} else {
			throw new RuntimeException("Unkown output token selector " + selectorName);
		}
	}

	/**
	 * Increase the frequency of a specific token by one, if the token matches
	 * the collecting regular expression and does not match the scraping regular
	 * expression.
	 * 
	 * @param token
	 *            the token to be added; if it is a token that only matches the
	 *            collecting pattern, then its frequency will be increased by
	 *            one.
	 */
	public void addToken(String token) {
		if (Pattern.matches(this.ignorePattern, token) == false && Pattern.matches(this.countPattern, token) == true) {
			if (count.containsKey(token)) {
				count.put(token, count.get(token) + 1);
			} else {
				count.put(token, 1);
			}
		}
	}

	/**
	 * Output a list of token-frequency pairs which are sorted in ascending or
	 * descending order, based on tokens' frequency.
	 * 
	 * @param order
	 *            Mode of sorting, which is either {@link #ASC} or {@link #DESC}
	 * @return A sorted list of token-frequency pairs.
	 */
	public List<Map.Entry<String, Integer>> sortByFrequency(int order) {
		List<Map.Entry<String, Integer>> list = new LinkedList<>(count.entrySet());
		if (order == ASC) {
			Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
				@Override
				public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
					return o1.getValue().compareTo(o2.getValue());
				}
			});
		} else if (order == DESC) {
			Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
				@Override
				public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
					return -o1.getValue().compareTo(o2.getValue());
				}
			});
		} else {
			throw new RuntimeException("Unknown sorting order.");
		}
		return list;
	}

	public void docount() {
		File input = new File(this.inputf);
		BufferedReader reader = null;
		String line = null;
		if (input.isDirectory()) {
			File[] directoryListing = input.listFiles();
			for (File child : directoryListing) {
				System.out.println("processing file " + child.getName());
				try {
					reader = new BufferedReader(new FileReader(child));
					while ((line = reader.readLine()) != null) {
						String tokens[] = line.split("\\s+");
						for (String token : tokens) {
							addToken(token);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if (reader != null)
						try {
							reader.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
				}
			}
		} else {
			try {
				reader = new BufferedReader(new FileReader(input));
				while ((line = reader.readLine()) != null) {
					String tokens[] = line.split("\\s+");
					for (String token : tokens) {
						addToken(token);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (reader != null)
					try {
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		}
	}
	public void outputCountMap(List<Map.Entry<String, Integer>> list) throws IOException {
		if(this.outputSelector == null) {
			outputWholeCountMap(list);
		}
		else {
			outputFilteredCountMap(list);
		}
	}
	
	public void outputWholeCountMap(List<Map.Entry<String, Integer>> list) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(this.countf));
		for (Map.Entry<String, Integer> entry : list) {
			writer.write(entry.getKey() + " " + entry.getValue());
			writer.newLine();
		}
		writer.close();
	}

	public void outputFilteredCountMap(List<Map.Entry<String, Integer>> list) throws IOException {
		outputSelector.clearMap();
		outputSelector.runTokenOutputSelection();
		BufferedWriter writer = new BufferedWriter(new FileWriter(this.countf));
		for (Map.Entry<String, Integer> entry : list) {
			if (outputSelector.outputToken(entry.getKey())) {
				writer.write(entry.getKey() + " " + entry.getValue());
				writer.newLine();
			}
		}
		writer.close();
	}

	public String getIgnorePattern() {
		return ignorePattern;
	}

	public void setIgnorePattern(String ignorePattern) {
		this.ignorePattern = ignorePattern;
	}

	public String getCountPattern() {
		return countPattern;
	}

	public void setCountPattern(String countPattern) {
		this.countPattern = countPattern;
	}

	/**
	 * An inner abstract class defined for the purpose of allowing its
	 * subclasses' can implement the logics of determining whether or not a
	 * token should be written to the output file based on the state of the
	 * words' frequencies.
	 */
	protected abstract class TokenOutputSelector {
		/**
		 * map between tokens and the flags denoting whether or not the token
		 * should be written to external files
		 */
		Map<String, Boolean> outputMap;

		abstract void runTokenOutputSelection();

		boolean outputToken(String token) {
			return outputMap.get(token);
		}

		void clearMap() {
			for (String key : outputMap.keySet()) {
				outputMap.put(key, false);
			}
		}
	}

	/**
	 * A subclass of {@link TokenOutputSelector}, which implements the selection
	 * of tokens that only a fixed number of tokens are to be output based on
	 * their frequencies. Each token to be output is one with its frequency
	 * large enough to make it among the specified number of the
	 * highest-frequency tokens.
	 */
	protected class FixedVocabularySizeTokenSelector extends TokenOutputSelector {
		int vocabularySize;

		public FixedVocabularySizeTokenSelector(int vocabularySize) {
			this.outputMap = new HashMap<String, Boolean>();
			this.vocabularySize = vocabularySize;
		}

		void runTokenOutputSelection() {
			clearMap();
			List<Map.Entry<String, Integer>> list = sortByFrequency(TokenCounter.DESC);
			int i = 0;
			for (Map.Entry<String, Integer> entry : list) {
				if (i < vocabularySize) {
					outputMap.put(entry.getKey(), true);
					i++;
				} else {
					outputMap.put(entry.getKey(), false);
				}
			}
		}
	}

	/**
	 * A subclass of {@link TokenOutputSelector}, which implements the selection
	 * of tokens that a token is output as long as its frequency is larger than
	 * a specific number.
	 */
	protected class LargerThanTokenSelector extends TokenOutputSelector {
		int tokenFrequency;

		public LargerThanTokenSelector(int tokenFrequency) {
			this.outputMap = new HashMap<String, Boolean>();
			this.tokenFrequency = tokenFrequency;
		}

		void runTokenOutputSelection() {
			clearMap();
			List<Map.Entry<String, Integer>> list = sortByFrequency(TokenCounter.DESC);
			for (Map.Entry<String, Integer> entry : list) {
				if (entry.getValue() > tokenFrequency) {
					outputMap.put(entry.getKey(), true);
				} else {
					outputMap.put(entry.getKey(), false);
				}
			}
		}
	}

	public static void main(String[] args) throws IOException {
		String input, output, countPattern, ignorePattern, outputFilterName, outputFilterValue;
		TokenCounter counter;
		SProperties props = new SProperties();
		props.load(args);
		if ((input = props.getProperty("input")) == null) {
			throw new RuntimeException("Use -input=<file name> option to specify the input file/directory.");
		}
		if ((output = props.getProperty("output")) == null) {
			throw new RuntimeException("Use -output=<file name> option to specify the output file.");
		}

		counter = new TokenCounter(input, output);

		if ((countPattern = props.getProperty("countpattern")) != null) {
			counter.setCountPattern(countPattern);
		}

		if ((ignorePattern = props.getProperty("ignorepattern")) != null) {
			counter.setIgnorePattern(ignorePattern);
		}

		if ((outputFilterName = props.getProperty("outputfilter.name")) != null) {
			if((outputFilterValue = props.getProperty("outputfilter.value")) == null) {
				throw new RuntimeException("Use -outputfilter.value option to specify the value for output filter " + outputFilterName);
			}
			counter.setOutputTokenSelector(outputFilterName, outputFilterValue);
		}

		counter.docount();
		counter.outputCountMap(counter.sortByFrequency(TokenCounter.DESC));
		counter.outputFilteredCountMap(counter.sortByFrequency(TokenCounter.DESC));
	}

}
