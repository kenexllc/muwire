package com.muwire.core.upload

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import com.muwire.core.InfoHash
import com.muwire.core.connection.Endpoint

class HashListUploader extends Uploader {
    private final InfoHash infoHash
    private final HashListRequest request
    
    HashListUploader(Endpoint endpoint, InfoHash infoHash, HashListRequest request) {
        super(endpoint)
        this.infoHash = infoHash
        mapped = ByteBuffer.wrap(infoHash.getHashList())
    }
    
    void respond() {
        OutputStream os = endpoint.getOutputStream()
        os.write("200 OK\r\n".getBytes(StandardCharsets.US_ASCII))
        os.write("Content-Range: 0-${mapped.remaining()}\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
        
        byte[]tmp = new byte[0x1 << 13]
        while(mapped.hasRemaining()) {
            int start = mapped.position()
            synchronized(this) {
                mapped.get(tmp, 0, Math.min(tmp.length, mapped.remaining()))
            }
            int read = mapped.position() - start
            endpoint.getOutputStream().write(tmp, 0, read)
        }
        endpoint.getOutputStream().flush()
    }
}