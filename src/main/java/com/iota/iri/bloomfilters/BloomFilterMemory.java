package com.iota.iri.bloomfilters;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.BitSet;

public class BloomFilterMemory<T> implements BloomFilter<T> {
    private static final long serialVersionUID = -5962895807963838856L;
    private final FilterBuilder config;
    protected BitSet blooms[] = new BitSet[5];
    protected BitSet bloom;
    public BloomFilterMemory(FilterBuilder config) {
        config.complete();
        bloom = new BitSet(config.size());
        this.config = config;    
    }

    @Override
    public FilterBuilder config() {
        return config;
    }

    @Override
    public synchronized boolean addRaw(byte[] element) {
        boolean added = false;
        for (int position : hash(element)) {
            if (!getBit(position)) {
                added = true;
                setBit(position, true);
            }
        }
        return added;
    }

    @Override
    public synchronized void clear() {
        bloom.clear();
    }

    @Override
    public synchronized boolean contains(byte[] element) {
        for (int position : hash(element)) {
            if (!getBit(position)) {
                return false;
            }
        }
        return true;
    }

    protected boolean getBit(int index) {
        return bloom.get(index);
    }

    protected void setBit(int index, boolean to) {
        bloom.set(index, to);
    }

    @Override
    public synchronized BitSet getBitSet() {
        return (BitSet) bloom.clone();
    }

    @Override
    public synchronized boolean union(BloomFilter<T> other) {
        if (compatible(other)) {
            bloom.or(other.getBitSet());
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean intersect(BloomFilter<T> other) {
        if (compatible(other)) {
            bloom.and(other.getBitSet());
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean isEmpty() {
        return bloom.isEmpty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized BloomFilter<T> clone() {
        BloomFilterMemory<T> o = null;
        try {
            o = (BloomFilterMemory<T>) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        o.bloom = (BitSet) bloom.clone();
        return o;
    }

    @Override
    public synchronized String toString() {
        return asString();
    }

    public synchronized void setBitSet(BitSet bloom) {
        this.bloom = bloom;
    }

    @Override
    public synchronized boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BloomFilterMemory)) {
            return false;
        }

        BloomFilterMemory that = (BloomFilterMemory) o;

        if (bloom != null ? !bloom.equals(that.bloom) : that.bloom != null) {
            return false;
        }
        if (config != null ? !config.isCompatibleTo(that.config) : that.config != null) {
            return false;
        }

        return true;
    }

    @Override
    public void save(String path) {
        try {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(path));
            out.write(bitSet2ByteArray(bloom));
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void load(String path) {
        long start = System.nanoTime();
        try (InputStream in = new BufferedInputStream(new FileInputStream(path));
                ByteArrayOutputStream out = new ByteArrayOutputStream();) {
            byte[] tempbytes = new byte[in.available()];
            for (int i = 0; (i = in.read(tempbytes)) != -1;) {
                out.write(tempbytes, 0, i);
            }
            long used = System.nanoTime() - start;
            System.out.println("read a file use :" + used / 1000 + "us");
            bloom = byteArray2BitSet(out.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * from file "path",at "offset",read "size" bytes into memory,and build BF with it
     * @param path
     * @param offset
     * @param size
     */
    public void load(String path, int offset,int size) {
        try {
            File f=new File(path);
            RandomAccessFile rf=new RandomAccessFile(f, "r");
            
            rf.seek(offset);
            byte[] tmp=new byte[size];
            //rf.readFully(tmp, offset, size);
            rf.read(tmp);
            bloom=byteArray2BitSet(tmp);
            rf.close();
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    public void save(String path,int offset,int size){
        try {
            File f=new File(path);
            if(!f.exists()){
                f.createNewFile();
            }

            RandomAccessFile rf=new RandomAccessFile(f, "rw");
            rf.seek(offset);
            rf.write(bitSet2ByteArray(bloom));
            rf.close();
        } catch (Exception e) {
            //TODO: handle exception
            e.printStackTrace();
        }
    }

    public static byte[] bitSet2ByteArray(BitSet bitSet) {
        byte[] bytes = new byte[bitSet.size() / 8];
        for (int i = 0; i < bitSet.size(); i++) {
            int index = i / 8;
            int offset = 7 - i % 8;
            bytes[index] |= (bitSet.get(i) ? 1 : 0) << offset;
        }
        return bytes;
    }

    public static BitSet byteArray2BitSet(byte[] bytes) {
        BitSet bitSet = new BitSet(bytes.length * 8);
        int index = 0;
        for (int i = 0; i < bytes.length; i++) {
            for (int j = 7; j >= 0; j--) {
                bitSet.set(index++, (bytes[i] & (1 << j)) >> j == 1 ? true : false);
            }
        }
        return bitSet;
    }
}