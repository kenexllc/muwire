package com.muwire.core.files

import com.muwire.core.InfoHash
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class FileHasher {

	/** max size of shared file is 128 GB */
	public static final long MAX_SIZE = 0x1L << 37
	
	/**
	 * @param size of the file to be shared
	 * @return the size of each piece in power of 2
	 */
	static int getPieceSize(long size) {
		if (size <= 0x1 << 25)
			return 18
		
		for (int i = 26; i <= 37; i++) {
			if (size <= 0x1L << i) {
				return i-7
			}
		}
		
		throw new IllegalArgumentException("File too large $size")
	}
	
	final MessageDigest digest
	
	FileHasher() {
		try {
			digest = MessageDigest.getInstance("SHA-256")
		} catch (NoSuchAlgorithmException impossible) {
			digest = null
			System.exit(1)
		}
	}
	
	InfoHash hashFile(File file) {
		final long length = file.length()
		final int size = 0x1 << getPieceSize(length)
		int numPieces = (int) (length / size)
		if (numPieces * size < length)
			numPieces++
			
		def output = new ByteArrayOutputStream()
		RandomAccessFile raf = new RandomAccessFile(file, "r")
		try {
			MappedByteBuffer buf
			for (int i = 0; i < numPieces - 1; i++) {
				buf = raf.getChannel().map(MapMode.READ_ONLY, size * i, size)
				digest.update buf
				output.write(digest.digest(), 0, 32)
			}
			def lastPieceLength = length - (numPieces - 1) * size
			buf = raf.getChannel().map(MapMode.READ_ONLY, length - lastPieceLength, lastPieceLength)
			digest.update buf
			output.write(digest.digest(), 0, 32)
		} finally {
			raf.close()
		}
		
		byte [] hashList = output.toByteArray()
		InfoHash.fromHashList(hashList)
	}
}