package org.danielli.xultimate.core.io;

import java.io.IOException;
import java.io.OutputStream;

public class DataOutput extends OutputStream {
	
	private int maxCapacity, capacity, position, total;
	private byte[] buffer;
	private OutputStream outputStream;

	/** Creates a new Output for writing to a byte array.
	 * @param bufferSize The initial and maximum size of the buffer. An exception is thrown if this size is exceeded. */
	public DataOutput (int bufferSize) {
		this(bufferSize, bufferSize);
	}

	/** Creates a new Output for writing to a byte array.
	 * @param bufferSize The initial size of the buffer.
	 * @param maxBufferSize The buffer is doubled as needed until it exceeds maxBufferSize and an exception is thrown. Can be -1
	 *           for no maximum. */
	public DataOutput (int bufferSize, int maxBufferSize) {
		if (maxBufferSize < -1) throw new IllegalArgumentException("maxBufferSize cannot be < -1: " + maxBufferSize);
		this.capacity = bufferSize;
		this.maxCapacity = maxBufferSize == -1 ? Integer.MAX_VALUE : maxBufferSize;
		buffer = new byte[bufferSize];
	}

	/** Creates a new Output for writing to a byte array.
	 * @see #setBuffer(byte[]) */
	public DataOutput (byte[] buffer) {
		this(buffer, buffer.length);
	}

	/** Creates a new Output for writing to a byte array.
	 * @see #setBuffer(byte[], int) */
	public DataOutput (byte[] buffer, int maxBufferSize) {
		if (buffer == null) throw new IllegalArgumentException("buffer cannot be null.");
		setBuffer(buffer, maxBufferSize);
	}

	/** Creates a new Output for writing to an OutputStream. A buffer size of 256 is used. */
	public DataOutput (OutputStream outputStream) {
		this(256, 256);
		if (outputStream == null) throw new IllegalArgumentException("outputStream cannot be null.");
		this.outputStream = outputStream;
	}

	/** Creates a new Output for writing to an OutputStream. */
	public DataOutput (OutputStream outputStream, int bufferSize) {
		this(bufferSize, bufferSize);
		if (outputStream == null) throw new IllegalArgumentException("outputStream cannot be null.");
		this.outputStream = outputStream;
	}

	public OutputStream getOutputStream () {
		return outputStream;
	}

	/** Sets a new OutputStream. The position and total are reset, discarding any buffered bytes.
	 * @param outputStream May be null. */
	public void setOutputStream (OutputStream outputStream) {
		this.outputStream = outputStream;
		position = 0;
		total = 0;
	}

	/** Sets the buffer that will be written to. {@link #setBuffer(byte[], int)} is called with the specified buffer's length as the
	 * maxBufferSize. */
	public void setBuffer (byte[] buffer) {
		setBuffer(buffer, buffer.length);
	}

	/** Sets the buffer that will be written to. The position and total are reset, discarding any buffered bytes. The
	 * {@link #setOutputStream(OutputStream) OutputStream} is set to null.
	 * @param maxBufferSize The buffer is doubled as needed until it exceeds maxBufferSize and an exception is thrown. */
	public void setBuffer (byte[] buffer, int maxBufferSize) {
		if (buffer == null) throw new IllegalArgumentException("buffer cannot be null.");
		if (maxBufferSize < -1) throw new IllegalArgumentException("maxBufferSize cannot be < -1: " + maxBufferSize);
		this.buffer = buffer;
		this.maxCapacity = maxBufferSize == -1 ? Integer.MAX_VALUE : maxBufferSize;
		capacity = buffer.length;
		position = 0;
		total = 0;
		outputStream = null;
	}

	/** Returns the buffer. The bytes between zero and {@link #position()} are the data that has been written. */
	public byte[] getBuffer () {
		return buffer;
	}

	/** Returns a new byte array containing the bytes currently in the buffer between zero and {@link #position()}. */
	public byte[] toBytes () {
		byte[] newBuffer = new byte[position];
		System.arraycopy(buffer, 0, newBuffer, 0, position);
		return newBuffer;
	}

	/** Returns the current position in the buffer. This is the number of bytes that have not been flushed. */
	public int position () {
		return position;
	}

	/** Sets the current position in the buffer. */
	public void setPosition (int position) {
		this.position = position;
	}

	/** Returns the total number of bytes written. This may include bytes that have not been flushed. */
	public int total () {
		return total + position;
	}

	/** Sets the position and total to zero. */
	public void clear () {
		position = 0;
		total = 0;
	}

	/** @return true if the buffer has been resized. */
	private boolean require (int required) throws IOException {
		if (capacity - position >= required) return false;
		if (required > maxCapacity)
			throw new IOException("Buffer overflow. Max capacity: " + maxCapacity + ", required: " + required);
		flush();
		while (capacity - position < required) {
			if (capacity == maxCapacity)
				throw new IOException("Buffer overflow. Available: " + (capacity - position) + ", required: " + required);
			// Grow buffer.
			capacity = Math.min(capacity * 2, maxCapacity);
			if (capacity < 0) capacity = maxCapacity;
			byte[] newBuffer = new byte[capacity];
			System.arraycopy(buffer, 0, newBuffer, 0, position);
			buffer = newBuffer;
		}
		return true;
	}

	// OutputStream

	/** Writes the buffered bytes to the underlying OutputStream, if any. */
	public void flush () throws IOException {
		if (outputStream == null) return;
		try {
			outputStream.write(buffer, 0, position);
		} catch (IOException ex) {
			throw new IOException(ex);
		}
		total += position;
		position = 0;
	}

	/** Flushes any buffered bytes and closes the underlying OutputStream, if any. */
	public void close () throws IOException {
		flush();
		if (outputStream != null) {
			try {
				outputStream.close();
			} catch (IOException ignored) {
			}
		}
	}

	/** Writes a byte. */
	public void write (int value) throws IOException {
		if (position == capacity) require(1);
		buffer[position++] = (byte)value;
	}

	/** Writes the bytes. Note the byte[] length is not written. */
	public void write (byte[] bytes) throws IOException {
		if (bytes == null) throw new IllegalArgumentException("bytes cannot be null.");
		writeBytes(bytes, 0, bytes.length);
	}

	/** Writes the bytes. Note the byte[] length is not written. */
	public void write (byte[] bytes, int offset, int length) throws IOException {
		writeBytes(bytes, offset, length);
	}

	// byte

	public void writeByte (byte value) throws IOException {
		if (position == capacity) require(1);
		buffer[position++] = value;
	}

	public void writeByte (int value) throws IOException {
		if (position == capacity) require(1);
		buffer[position++] = (byte)value;
	}

	/** Writes the bytes. Note the byte[] length is not written. */
	public void writeBytes (byte[] bytes) throws IOException {
		if (bytes == null) throw new IllegalArgumentException("bytes cannot be null.");
		writeBytes(bytes, 0, bytes.length);
	}

	/** Writes the bytes. Note the byte[] length is not written. */
	public void writeBytes (byte[] bytes, int offset, int count) throws IOException {
		if (bytes == null) throw new IllegalArgumentException("bytes cannot be null.");
		int copyCount = Math.min(capacity - position, count);
		while (true) {
			System.arraycopy(bytes, offset, buffer, position, copyCount);
			position += copyCount;
			count -= copyCount;
			if (count == 0) return;
			offset += copyCount;
			copyCount = Math.min(capacity, count);
			require(copyCount);
		}
	}

	// int

	/** Writes a 4 byte int. */
	public void writeInt (int value) throws IOException {
		require(4);
		byte[] buffer = this.buffer;
		buffer[position++] = (byte)(value >> 24);
		buffer[position++] = (byte)(value >> 16);
		buffer[position++] = (byte)(value >> 8);
		buffer[position++] = (byte)value;
	}

	/** Writes a 1-5 byte int.
	 * @param optimizePositive If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
	 *           inefficient (5 bytes). */
	public int writeInt (int value, boolean optimizePositive) throws IOException {
		if (!optimizePositive) value = (value << 1) ^ (value >> 31);
		if (value >>> 7 == 0) {
			require(1);
			buffer[position++] = (byte)value;
			return 1;
		}
		if (value >>> 14 == 0) {
			require(2);
			buffer[position++] = (byte)((value & 0x7F) | 0x80);
			buffer[position++] = (byte)(value >>> 7);
			return 2;
		}
		if (value >>> 21 == 0) {
			require(3);
			buffer[position++] = (byte)((value & 0x7F) | 0x80);
			buffer[position++] = (byte)(value >>> 7 | 0x80);
			buffer[position++] = (byte)(value >>> 14);
			return 3;
		}
		if (value >>> 28 == 0) {
			require(4);
			buffer[position++] = (byte)((value & 0x7F) | 0x80);
			buffer[position++] = (byte)(value >>> 7 | 0x80);
			buffer[position++] = (byte)(value >>> 14 | 0x80);
			buffer[position++] = (byte)(value >>> 21);
			return 4;
		}
		require(5);
		buffer[position++] = (byte)((value & 0x7F) | 0x80);
		buffer[position++] = (byte)(value >>> 7 | 0x80);
		buffer[position++] = (byte)(value >>> 14 | 0x80);
		buffer[position++] = (byte)(value >>> 21 | 0x80);
		buffer[position++] = (byte)(value >>> 28);
		return 5;
	}

	// string

	/** Writes the length and string, or null. Short strings are checked and if ASCII they are written more efficiently, else they
	 * are written as UTF8. If a string is known to be ASCII, {@link #writeAscii(String)} may be used. The string can be read using
	 * {@link DataInput#readString()} or {@link DataInput#readStringBuilder()}.
	 * @param value May be null. */
	@SuppressWarnings("deprecation")
	public void writeString (String value) throws IOException {
		if (value == null) {
			writeByte(0x80); // 0 means null, bit 8 means UTF8.
			return;
		}
		int charCount = value.length();
		if (charCount == 0) {
			writeByte(1 | 0x80); // 1 means empty string, bit 8 means UTF8.
			return;
		}
		// Detect ASCII.
		boolean ascii = false;
		if (charCount > 1 && charCount < 64) {
			ascii = true;
			for (int i = 0; i < charCount; i++) {
				int c = value.charAt(i);
				if (c > 127) {
					ascii = false;
					break;
				}
			}
		}
		if (ascii) {
			if (capacity - position < charCount)
				writeAscii_slow(value, charCount);
			else {
				value.getBytes(0, charCount, buffer, position);
				position += charCount;
			}
			buffer[position - 1] |= 0x80;
		} else {
			writeUtf8Length(charCount + 1);
			int charIndex = 0;
			if (capacity - position >= charCount) {
				// Try to write 8 bit chars.
				byte[] buffer = this.buffer;
				int position = this.position;
				for (; charIndex < charCount; charIndex++) {
					int c = value.charAt(charIndex);
					if (c > 127) break;
					buffer[position++] = (byte)c;
				}
				this.position = position;
			}
			if (charIndex < charCount) writeString_slow(value, charCount, charIndex);
		}
	}

	/** Writes the length and CharSequence as UTF8, or null. The string can be read using {@link DataInput#readString()} or
	 * {@link DataInput#readStringBuilder()}.
	 * @param value May be null. */
	public void writeString (CharSequence value) throws IOException {
		if (value == null) {
			writeByte(0x80); // 0 means null, bit 8 means UTF8.
			return;
		}
		int charCount = value.length();
		if (charCount == 0) {
			writeByte(1 | 0x80); // 1 means empty string, bit 8 means UTF8.
			return;
		}
		writeUtf8Length(charCount + 1);
		int charIndex = 0;
		if (capacity - position >= charCount) {
			// Try to write 8 bit chars.
			byte[] buffer = this.buffer;
			int position = this.position;
			for (; charIndex < charCount; charIndex++) {
				int c = value.charAt(charIndex);
				if (c > 127) break;
				buffer[position++] = (byte)c;
			}
			this.position = position;
		}
		if (charIndex < charCount) writeString_slow(value, charCount, charIndex);
	}

	/** Writes a string that is known to contain only ASCII characters. Non-ASCII strings passed to this method will be corrupted.
	 * Each byte is a 7 bit character with the remaining byte denoting if another character is available. This is slightly more
	 * efficient than {@link #writeString(String)}. The string can be read using {@link DataInput#readString()} or
	 * {@link DataInput#readStringBuilder()}.
	 * @param value May be null. */
	@SuppressWarnings("deprecation")
	public void writeAscii (String value) throws IOException {
		if (value == null) {
			writeByte(0x80); // 0 means null, bit 8 means UTF8.
			return;
		}
		int charCount = value.length();
		if (charCount == 0) {
			writeByte(1 | 0x80); // 1 means empty string, bit 8 means UTF8.
			return;
		}
		if (capacity - position < charCount)
			writeAscii_slow(value, charCount);
		else {
			value.getBytes(0, charCount, buffer, position);
			position += charCount;
		}
		buffer[position - 1] |= 0x80; // Bit 8 means end of ASCII.
	}

	/** Writes the length of a string, which is a variable length encoded int except the first byte uses bit 8 to denote UTF8 and
	 * bit 7 to denote if another byte is present. 
	 */
	private void writeUtf8Length (int value) throws IOException {
		if (value >>> 6 == 0) {
			require(1);
			buffer[position++] = (byte)(value | 0x80); // Set bit 8.
		} else if (value >>> 13 == 0) {
			require(2);
			byte[] buffer = this.buffer;
			buffer[position++] = (byte)(value | 0x40 | 0x80); // Set bit 7 and 8.
			buffer[position++] = (byte)(value >>> 6);
		} else if (value >>> 20 == 0) {
			require(3);
			byte[] buffer = this.buffer;
			buffer[position++] = (byte)(value | 0x40 | 0x80); // Set bit 7 and 8.
			buffer[position++] = (byte)((value >>> 6) | 0x80); // Set bit 8.
			buffer[position++] = (byte)(value >>> 13);
		} else if (value >>> 27 == 0) {
			require(4);
			byte[] buffer = this.buffer;
			buffer[position++] = (byte)(value | 0x40 | 0x80); // Set bit 7 and 8.
			buffer[position++] = (byte)((value >>> 6) | 0x80); // Set bit 8.
			buffer[position++] = (byte)((value >>> 13) | 0x80); // Set bit 8.
			buffer[position++] = (byte)(value >>> 20);
		} else {
			require(5);
			byte[] buffer = this.buffer;
			buffer[position++] = (byte)(value | 0x40 | 0x80); // Set bit 7 and 8.
			buffer[position++] = (byte)((value >>> 6) | 0x80); // Set bit 8.
			buffer[position++] = (byte)((value >>> 13) | 0x80); // Set bit 8.
			buffer[position++] = (byte)((value >>> 20) | 0x80); // Set bit 8.
			buffer[position++] = (byte)(value >>> 27);
		}
	}

	private void writeString_slow (CharSequence value, int charCount, int charIndex) throws IOException {
		for (; charIndex < charCount; charIndex++) {
			if (position == capacity) require(Math.min(capacity, charCount - charIndex));
			int c = value.charAt(charIndex);
			if (c <= 0x007F) {
				buffer[position++] = (byte)c;
			} else if (c > 0x07FF) {
				buffer[position++] = (byte)(0xE0 | c >> 12 & 0x0F);
				require(2);
				buffer[position++] = (byte)(0x80 | c >> 6 & 0x3F);
				buffer[position++] = (byte)(0x80 | c & 0x3F);
			} else {
				buffer[position++] = (byte)(0xC0 | c >> 6 & 0x1F);
				require(1);
				buffer[position++] = (byte)(0x80 | c & 0x3F);
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void writeAscii_slow (String value, int charCount) throws IOException {
		byte[] buffer = this.buffer;
		int charIndex = 0;
		int charsToWrite = Math.min(charCount, capacity - position);
		while (charIndex < charCount) {
			value.getBytes(charIndex, charIndex + charsToWrite, buffer, position);
			charIndex += charsToWrite;
			position += charsToWrite;
			charsToWrite = Math.min(charCount - charIndex, capacity);
			if (require(charsToWrite)) buffer = this.buffer;
		}
	}

	// float

	/** Writes a 4 byte float. */
	public void writeFloat (float value) throws IOException {
		writeInt(Float.floatToIntBits(value));
	}

	/** Writes a 1-5 byte float with reduced precision.
	 * @param optimizePositive If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
	 *           inefficient (5 bytes). */
	public int writeFloat (float value, float precision, boolean optimizePositive) throws IOException {
		return writeInt((int)(value * precision), optimizePositive);
	}

	// short

	/** Writes a 2 byte short. */
	public void writeShort (int value) throws IOException {
		require(2);
		buffer[position++] = (byte)(value >>> 8);
		buffer[position++] = (byte)value;
	}

	// long

	/** Writes an 8 byte long. */
	public void writeLong (long value) throws IOException {
		require(8);
		byte[] buffer = this.buffer;
		buffer[position++] = (byte)(value >>> 56);
		buffer[position++] = (byte)(value >>> 48);
		buffer[position++] = (byte)(value >>> 40);
		buffer[position++] = (byte)(value >>> 32);
		buffer[position++] = (byte)(value >>> 24);
		buffer[position++] = (byte)(value >>> 16);
		buffer[position++] = (byte)(value >>> 8);
		buffer[position++] = (byte)value;
	}

	/** Writes a 1-9 byte long.
	 * @param optimizePositive If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
	 *           inefficient (9 bytes). */
	public int writeLong (long value, boolean optimizePositive) throws IOException {
		if (!optimizePositive) value = (value << 1) ^ (value >> 63);
		if (value >>> 7 == 0) {
			require(1);
			buffer[position++] = (byte)value;
			return 1;
		}
		if (value >>> 14 == 0) {
			require(2);
			buffer[position++] = (byte)((value & 0x7F) | 0x80);
			buffer[position++] = (byte)(value >>> 7);
			return 2;
		}
		if (value >>> 21 == 0) {
			require(3);
			buffer[position++] = (byte)((value & 0x7F) | 0x80);
			buffer[position++] = (byte)(value >>> 7 | 0x80);
			buffer[position++] = (byte)(value >>> 14);
			return 3;
		}
		if (value >>> 28 == 0) {
			require(4);
			buffer[position++] = (byte)((value & 0x7F) | 0x80);
			buffer[position++] = (byte)(value >>> 7 | 0x80);
			buffer[position++] = (byte)(value >>> 14 | 0x80);
			buffer[position++] = (byte)(value >>> 21);
			return 4;
		}
		if (value >>> 35 == 0) {
			require(5);
			buffer[position++] = (byte)((value & 0x7F) | 0x80);
			buffer[position++] = (byte)(value >>> 7 | 0x80);
			buffer[position++] = (byte)(value >>> 14 | 0x80);
			buffer[position++] = (byte)(value >>> 21 | 0x80);
			buffer[position++] = (byte)(value >>> 28);
			return 5;
		}
		if (value >>> 42 == 0) {
			require(6);
			buffer[position++] = (byte)((value & 0x7F) | 0x80);
			buffer[position++] = (byte)(value >>> 7 | 0x80);
			buffer[position++] = (byte)(value >>> 14 | 0x80);
			buffer[position++] = (byte)(value >>> 21 | 0x80);
			buffer[position++] = (byte)(value >>> 28 | 0x80);
			buffer[position++] = (byte)(value >>> 35);
			return 6;
		}
		if (value >>> 49 == 0) {
			require(7);
			buffer[position++] = (byte)((value & 0x7F) | 0x80);
			buffer[position++] = (byte)(value >>> 7 | 0x80);
			buffer[position++] = (byte)(value >>> 14 | 0x80);
			buffer[position++] = (byte)(value >>> 21 | 0x80);
			buffer[position++] = (byte)(value >>> 28 | 0x80);
			buffer[position++] = (byte)(value >>> 35 | 0x80);
			buffer[position++] = (byte)(value >>> 42);
			return 7;
		}
		if (value >>> 56 == 0) {
			require(8);
			buffer[position++] = (byte)((value & 0x7F) | 0x80);
			buffer[position++] = (byte)(value >>> 7 | 0x80);
			buffer[position++] = (byte)(value >>> 14 | 0x80);
			buffer[position++] = (byte)(value >>> 21 | 0x80);
			buffer[position++] = (byte)(value >>> 28 | 0x80);
			buffer[position++] = (byte)(value >>> 35 | 0x80);
			buffer[position++] = (byte)(value >>> 42 | 0x80);
			buffer[position++] = (byte)(value >>> 49);
			return 8;
		}
		require(9);
		buffer[position++] = (byte)((value & 0x7F) | 0x80);
		buffer[position++] = (byte)(value >>> 7 | 0x80);
		buffer[position++] = (byte)(value >>> 14 | 0x80);
		buffer[position++] = (byte)(value >>> 21 | 0x80);
		buffer[position++] = (byte)(value >>> 28 | 0x80);
		buffer[position++] = (byte)(value >>> 35 | 0x80);
		buffer[position++] = (byte)(value >>> 42 | 0x80);
		buffer[position++] = (byte)(value >>> 49 | 0x80);
		buffer[position++] = (byte)(value >>> 56);
		return 9;
	}

	// boolean

	/** Writes a 1 byte boolean. */
	public void writeBoolean (boolean value) throws IOException {
		require(1);
		buffer[position++] = (byte)(value ? 1 : 0);
	}

	// char

	/** Writes a 2 byte char. */
	public void writeChar (char value) throws IOException {
		require(2);
		buffer[position++] = (byte)(value >>> 8);
		buffer[position++] = (byte)value;
	}

	// double

	/** Writes an 8 byte double. */
	public void writeDouble (double value) throws IOException {
		writeLong(Double.doubleToLongBits(value));
	}

	/** Writes a 1-9 byte double with reduced precision.
	 * @param optimizePositive If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
	 *           inefficient (9 bytes). */
	public int writeDouble (double value, double precision, boolean optimizePositive) throws IOException {
		return writeLong((long)(value * precision), optimizePositive);
	}

	/** Returns the number of bytes that would be written with {@link #writeInt(int, boolean)}. */
	static public int intLength (int value, boolean optimizePositive) {
		if (!optimizePositive) value = (value << 1) ^ (value >> 31);
		if (value >>> 7 == 0) return 1;
		if (value >>> 14 == 0) return 2;
		if (value >>> 21 == 0) return 3;
		if (value >>> 28 == 0) return 4;
		return 5;
	}

	/** Returns the number of bytes that would be written with {@link #writeLong(long, boolean)}. */
	static public int longLength (long value, boolean optimizePositive) {
		if (!optimizePositive) value = (value << 1) ^ (value >> 63);
		if (value >>> 7 == 0) return 1;
		if (value >>> 14 == 0) return 2;
		if (value >>> 21 == 0) return 3;
		if (value >>> 28 == 0) return 4;
		if (value >>> 35 == 0) return 5;
		if (value >>> 42 == 0) return 6;
		if (value >>> 49 == 0) return 7;
		if (value >>> 56 == 0) return 8;
		return 9;
	}
}