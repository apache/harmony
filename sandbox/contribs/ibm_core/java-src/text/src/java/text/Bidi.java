/* Copyright 2005 The Apache Software Foundation or its licensors, as applicable
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.text;


import java.util.Arrays;
import java.util.LinkedList;

import com.ibm.text.BidiRun;
import com.ibm.text.BidiWrapper;

/**
 * Bidi is the class providing the bidirectional algorithm. The algorithm is
 * defined in the Unicode Standard Annex #9, version 13, also described in The
 * Unicode Standard, Version 4.0 .
 * 
 * Use a Bidi object to get the infomation on the position reordering of a
 * bidirectional text, such as Arabic or Hebrew. The natural display ordering of
 * horizontal text in these languages is from right to left, while they order
 * numbers from left to right.
 * 
 * If the text contains multiple runs, the information of each run can be
 * obtained from the run index. The level of any particular run indicates the
 * direction of the text as well as the nesting level. Left-to-right runs have
 * even levels while right-to-left runs have odd levels.
 * 
 */
public final class Bidi {
	/**
	 * Constant that indicates the default base level. If there is no strong
	 * character, then set the paragraph level to 0 (left-to-right).
	 */
	public static final int DIRECTION_DEFAULT_LEFT_TO_RIGHT = -2;

	/**
	 * Constant that indicates the default base level. If there is no strong
	 * character, then set the paragraph level to 1 (right-to-left).
	 */
	public static final int DIRECTION_DEFAULT_RIGHT_TO_LEFT = -1;

	/**
	 * Constant that specifies the default base level as 0 (left-to-right).
	 */
	public static final int DIRECTION_LEFT_TO_RIGHT = 0;

	/**
	 * Constant that specifies the default base level as 1 (right-to-left).
	 */
	public static final int DIRECTION_RIGHT_TO_LEFT = 1;

	/**
	 * Create a Bidi object from the AttributedCharacterIterator of a paragraph
	 * text.
	 * 
	 * The RUN_DIRECTION attribute determines the base direction of the
	 * bidirectional text. If it's not specified explicitly, the algorithm uses
	 * DIRECTION_DEFAULT_LEFT_TO_RIGHT by default.
	 * 
	 * The BIDI_EMBEDDING attribute specifies the level of embedding for each
	 * character. Values between -1 and -62 denote overrides at the level's
	 * absolute value, values from 1 to 62 indicate embeddings, and the
	 * 0 value indicates the level is calculated by the algorithm automatically.
	 * For the character with no BIDI_EMBEDDING attribute or with a improper
	 * attribute value, such as a null value, the algorithm treats its embedding
	 * level as 0.
	 * 
	 * The NUMERIC_SHAPING attribute specifies the instance of NumericShaper
	 * used to convert European digits to other decimal digits before performing
	 * the bidi algorithm.
	 * 
	 * @param paragraph
	 * 
	 * @see TextAttribute.BIDI_EMBEDDING
	 * @see TextAttribute.NUMERIC_SHAPING
	 * @see TextAttribute.RUN_DIRECTION
	 */
	public Bidi(AttributedCharacterIterator paragraph) {
		/*
		 * TODO: dependency on java.awt.font.TextAttribute and
		 * java.awt.font.NumericShaper which is not implemented yet.
		 */
	}

	/**
	 * Create a Bidi object.
	 * 
	 * @param text
	 *            the char array of the paragraph text.
	 * @param textStart
	 *            the start offset of the text array to perform the algorithm.
	 * @param embeddings
	 *            the embedding level array of the paragraph text, specifying
	 *            the embedding level infomation for each character. Values
	 *            between -1 and -62 denote overrides at the level's absolute
	 *            value, values from 1 to 62 indicate embeddings, and the 0
	 *            value indicates the level is calculated by the algorithm
	 *            automatically.
	 * @param embStart
	 *            the start offset of the embeddings array to perform the
	 *            algorithm.
	 * @param paragraphLength
	 *            the length of the text to perform the algorithm. It must be
	 *            text.length >= textStart + paragraphLength, and
	 *            embeddings.length >= embStart + paragraphLength.
	 * @param flags
	 *            indicates the base direction of the bidirectional text. It is
	 *            expected that this will be one of the direction constant
	 *            values defined in this class. An unknown value is treated as
	 *            DIRECTION_DEFAULT_LEFT_TO_RIGHT.
	 * 
	 * @see #DIRECTION_LEFT_TO_RIGHT
	 * @see #DIRECTION_RIGHT_TO_LEFT
	 * @see #DIRECTION_DEFAULT_RIGHT_TO_LEFT
	 * @see #DIRECTION_DEFAULT_LEFT_TO_RIGHT
	 */
	public Bidi(char[] text, int textStart, byte[] embeddings, int embStart,
			int paragraphLength, int flags) {
		long pBidi = createUBiDi(text, textStart, embeddings, embStart,
				paragraphLength, flags);
		readBidiInfo(pBidi);
		BidiWrapper.ubidi_close(pBidi);
	}

	/**
	 * Create a Bidi object.
	 * 
	 * @param paragraph
	 *            the String containing the paragraph text to perform the
	 *            algorithm.
	 * @param flags
	 *            indicates the base direction of the bidirectional text. It is
	 *            expected that this will be one of the direction constant
	 *            values defined in this class. An unknown value is treated as
	 *            DIRECTION_DEFAULT_LEFT_TO_RIGHT.
	 * 
	 * @see #DIRECTION_LEFT_TO_RIGHT
	 * @see #DIRECTION_RIGHT_TO_LEFT
	 * @see #DIRECTION_DEFAULT_RIGHT_TO_LEFT
	 * @see #DIRECTION_DEFAULT_LEFT_TO_RIGHT
	 */
	public Bidi(String paragraph, int flags) {
		this((paragraph == null ? null : paragraph.toCharArray()), 0, null, 0,
				(paragraph == null ? 0 : paragraph.length()), flags);
	}

	// create the native UBiDi struct, need to be closed with ubidi_close().
	private static long createUBiDi(char[] text, int textStart,
			byte[] embeddings, int embStart, int paragraphLength, int flags) {
		char[] realText = null;

		byte[] realEmbeddings = null;

		if (text == null || text.length < textStart + paragraphLength) {
			throw new IllegalArgumentException();
		}
		realText = new char[paragraphLength];
		System.arraycopy(text, textStart, realText, 0, paragraphLength);

		if (embeddings != null) {
			if (embeddings.length < embStart + paragraphLength) {
				throw new IllegalArgumentException();
			}
			Bidi temp = new Bidi(text, textStart, null, 0, paragraphLength,
					flags);
			realEmbeddings = new byte[paragraphLength];
			System.arraycopy(temp.offsetLevel, 0, realEmbeddings, 0,
					paragraphLength);
			for (int i = 0; i < paragraphLength; i++) {
				byte e = embeddings[i];
				if (e < 0) {
					realEmbeddings[i] = (byte) (BidiWrapper.UBIDI_LEVEL_OVERRIDE - e);
				} else if (e > 0) {
					realEmbeddings[i] = e;
				} else {
					realEmbeddings[i] |= (byte) BidiWrapper.UBIDI_LEVEL_OVERRIDE;
				}
			}
		}

		if (flags > 1 || flags < -2) {
			flags = 0;
		}

		long bidi = BidiWrapper.ubidi_open();
		BidiWrapper.ubidi_setPara(bidi, realText, paragraphLength,
				(byte) flags, realEmbeddings);
		return bidi;
	}

	// private constructor, used by createLineBidi()
	private Bidi(long pBidi) {
		readBidiInfo(pBidi);
	}

	// read info from the native UBiDi struct
	private void readBidiInfo(long pBidi) {

		length = BidiWrapper.ubidi_getLength(pBidi);

		offsetLevel = (length == 0) ? null : BidiWrapper.ubidi_getLevels(pBidi);

		baseLevel = BidiWrapper.ubidi_getParaLevel(pBidi);

		int runCount = BidiWrapper.ubidi_countRuns(pBidi);
		if (runCount == 0) {
			runCount = 1;
			runs = new BidiRun[runCount];
			runs[0] = new BidiRun(0, 0, baseLevel);
		} else if (runCount < 0) {
			runCount = 0;
			runs = null;
		} else {
			runs = new BidiRun[runCount];
			for (int i = 0; i < runs.length; i++) {
				runs[i] = BidiWrapper.ubidi_getRun(pBidi, i);
			}
		}

		direction = BidiWrapper.ubidi_getDirection(pBidi);
	}

	private int baseLevel;

	private int length;

	private byte[] offsetLevel;

	private BidiRun[] runs;

	private int direction;

	/**
	 * Return whether the base level is from left to right.
	 * 
	 * @return true if the base level is from left to right.
	 */
	public boolean baseIsLeftToRight() {
		return baseLevel % 2 == 0 ? true : false;
	}

	/**
	 * Create a new Bidi object containing the infomation of one line from this
	 * object.
	 * 
	 * @param lineStart
	 *            the start offset of the line.
	 * @param lineLimit
	 *            the limit of the line.
	 * @return the new line Bidi object. In this new object, the indices will
	 *         range from 0 to (limit - start - 1).
	 */
	public Bidi createLineBidi(int lineStart, int lineLimit) {
		char[] text = new char[this.length];
		Arrays.fill(text, 'a');
		byte[] embeddings = new byte[this.length];
		for (int i = 0; i < embeddings.length; i++) {
			embeddings[i] = (byte) -this.offsetLevel[i];
		}

		int dir = this.baseIsLeftToRight() ? Bidi.DIRECTION_LEFT_TO_RIGHT
				: Bidi.DIRECTION_RIGHT_TO_LEFT;

		long parent = createUBiDi(text, 0, embeddings, 0, this.length, dir);

		long line = BidiWrapper.ubidi_setLine(parent, lineStart, lineLimit);
		Bidi result = new Bidi(line);
		BidiWrapper.ubidi_close(line);
		BidiWrapper.ubidi_close(parent);
		return result;
	}

	/**
	 * Return the base level.
	 * 
	 * @return the int value of the base level.
	 */
	public int getBaseLevel() {
		return baseLevel;
	}

	/**
	 * Return the length of the text in the Bidi object.
	 * 
	 * @return the int value of the length.
	 */
	public int getLength() {
		return length;
	}

	/**
	 * Return the level of a specified character.
	 * 
	 * @param offset
	 *            the offset of the character.
	 * @return the int value of the evel.
	 */
	public int getLevelAt(int offset) {
		try {
			return offsetLevel[offset] & ~BidiWrapper.UBIDI_LEVEL_OVERRIDE;
		} catch (RuntimeException e) {
			return baseLevel;
		}
	}

	/**
	 * Return the number of runs in the bidirectional text.
	 * 
	 * @return the int value of runs, at least 1.
	 */
	public int getRunCount() {
		return runs.length;
	}

	/**
	 * Return the level of a specified run.
	 * 
	 * @param run
	 *            the index of the run.
	 * @return the level of the run.
	 */
	public int getRunLevel(int run) {
		return runs[run].getLevel();
	}

	/**
	 * Return the limit offset of a specified run.
	 * 
	 * @param run
	 *            the index of the run.
	 * @return the limit offset of the run.
	 */
	public int getRunLimit(int run) {
		return runs[run].getLimit();
	}

	/**
	 * Return the start offset of a specified run.
	 * 
	 * @param run
	 *            the index of the run.
	 * @return the start offset of the run.
	 */
	public int getRunStart(int run) {
		return runs[run].getStart();
	}

	/**
	 * Return whether the text is from left to right, that is, both the base
	 * direction and the text direction is from left to right.
	 * 
	 * @return true if the text is from left to right.
	 */
	public boolean isLeftToRight() {
		return direction == BidiWrapper.UBiDiDirection_UBIDI_LTR;
	}

	/**
	 * Return whether the text direction is mixed.
	 * 
	 * @return true if the text direction is mixed.
	 */
	public boolean isMixed() {
		return direction == BidiWrapper.UBiDiDirection_UBIDI_MIXED;
	}

	/**
	 * Return whether the text is from right to left, that is, both the base
	 * direction and the text direction is from right to left.
	 * 
	 * @return true if the text is from right to left.
	 */
	public boolean isRightToLeft() {
		return direction == BidiWrapper.UBiDiDirection_UBIDI_RTL;
	}

	/**
	 * Reorder a range of objects according to their spefied levels. This is a
	 * convenience function that does not use a Bidi object. The range of
	 * objects at index from objectStart to objectStart + count will be
	 * reordered according to the range of levels at index from levelStart to
	 * levelStart + count.
	 * 
	 * @param levels
	 *            the level array, which is already determined.
	 * @param levelStart
	 *            the start offset of the range of the levels.
	 * @param objects
	 *            the object array to reoeder.
	 * @param objectStart
	 *            the start offset of the range of objects.
	 * @param count
	 *            the count of the range of objects to reorder.
	 */
	public static void reorderVisually(byte[] levels, int levelStart,
			Object[] objects, int objectStart, int count) {
		try {
			byte[] realLevels = new byte[count];
			System.arraycopy(levels, levelStart, realLevels, 0, count);

			int[] indices = BidiWrapper.ubidi_reorderVisual(realLevels, count);

			LinkedList result = new LinkedList();
			for (int i = 0; i < count; i++) {
				result.addLast(objects[objectStart + indices[i]]);
			}

			System.arraycopy(result.toArray(), 0, objects, objectStart, count);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Return whether a range of characters of a text requires a Bidi object to
	 * display properly.
	 * 
	 * @param text
	 *            the char array of the text.
	 * @param start
	 *            the start offset of the range of characters.
	 * @param limit
	 *            the limit offset of the range of characters.
	 * @return true if the range of characters requires a Bidi object.
	 */
	public static boolean requiresBidi(char[] text, int start, int limit) {
		if (limit < 0 || start >= limit) {
			return false;
		} else if (start < 0 || start > text.length || limit > text.length) {
			throw new ArrayIndexOutOfBoundsException();
		}

		Bidi bidi = new Bidi(text, start, null, 0, limit - start, 0);

		if (bidi.isLeftToRight()) {
			return false;
		}

		return true;
	}

	/**
	 * Return the internal message of the Bidi object, used in debugging.
	 * 
	 * @return a string containing the internal messsage.
	 */
	public String toString() {
		// simply return nothing
		return "";
	}
}
