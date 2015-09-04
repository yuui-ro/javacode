package eng;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A TokenIdConverter convert the tokens in a single file or the files in a
 * directory to their ids which are separated by a delimiter "|" which stand for
 * the separators in the original text, e.g. commas, newline characters etc., to
 * identify segments in the resulted id sequence; the id of a token is the line
 * number whose line begins with the token in the token list file, with the line
 * number counted from zero.
 * <p>
 * 
 * Because some words in the files can be missing from the token list,
 * extracting all separators in a file may result in some empty segments. This
 * situation is considered and is handled by automatically removing these empty
 * segments.
 * <p>
 * 
 * When the input is a single text file, the output will also be a text file. If
 * the specified output is an existing directory, then an error will occur;
 * however, when the input is a directory consisting of multiple text files, the
 * specified output stands for a directory to take in the resulted files. For
 * each token file in the input directory, a file with the same filename will be
 * created in the output directory to represent the resulted id sequence by
 * processing the token file.
 * 
 * @author Youwei Lu
 */
public class TokenIdConverter {

	/**
	 * The filename of the token list file
	 */
	private String tokenListFile;
	/**
	 * The map between the token and its id. A token's id is the line number
	 * whose corresponding line begins with the token
	 */
	private Map<String, Integer> tokenIdMap;
	/**
	 * A regular expression for matching the separators in files
	 */
	private String separatorPattern;

	public TokenIdConverter(String tokenListFile, String separatorPattern) {
		this.tokenListFile = tokenListFile;
		this.separatorPattern = separatorPattern;
		tokenIdMap = new HashMap<String, Integer>();
	}

	public void loadTokenList() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(tokenListFile));
		String line, token;
		int i = 0;
		try {
			while ((line = reader.readLine()) != null) {
				token = line.split("\\s+", 2)[0];
				if (tokenIdMap.put(token, i++) != null) {
					throw new RuntimeException(
							"Token " + token + " has multiple ids in token list file " + tokenListFile);
				}
			}
		} catch (IOException e) {
			throw e;
		} finally {
			if (reader != null)
				reader.close();
		}
	}

	/**
	 * Read tokens from an input file, convert the tokens to corresponding ids
	 * in the token list, and write the results to the output file. If
	 * separators are detected, then insert a delimiter "|" in the id sequence
	 * to denote segments; however, empty segments are avoided.
	 */
	private void convertTokenFile(File infile, File outfile) throws IOException {
		String line, tokens[], token;
		int prevId = -1, id = -1, i, j;
		BufferedReader read = null;
		BufferedWriter write = null;

		try {
			read = new BufferedReader(new FileReader(infile));
			write = new BufferedWriter(new FileWriter(outfile));

			while ((line = read.readLine()) != null) {
				tokens = line.split("\\s+");
				for (i = 0; i < tokens.length; i++) {
					token = tokens[i];
					j = i + 1;
					if (Pattern.matches(separatorPattern, token)) {
						/*
						 * if the current token is a separator, it is written to
						 * the output only when it is between two ids
						 */
						for (; j < tokens.length; j++) {
							if (tokenIdMap.containsKey(tokens[j])) {
								id = tokenIdMap.get(tokens[j]);
								break;
							}
						}
						if (i > 0 && j > 0 && j < tokens.length) {
							if (prevId > 0) {
								write.write("| " + id + "("+tokens[j]+")" +" ");
							} else {
								write.write(id + "("+tokens[j]+")"+" ");
							}
						}
						else if( prevId > 0 && j==tokens.length) {
							write.write("| ");
						}
						prevId = id;
						i = j;
					} else if (tokenIdMap.containsKey(token)) {
						prevId = id;
						id = tokenIdMap.get(token);
						write.write(id +"("+token+")"+ " ");
					}
				}
			}
		} catch (IOException e) {
			throw e;
		} finally {
			write.close();
			read.close();
		}
	}

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		TokenIdConverter convert = new TokenIdConverter("token_freq", "\\.|,|\\n|:|\\?|!");
		convert.loadTokenList();
		convert.convertTokenFile(new File("test"), new File("12206.txt.ids"));
	}

}
