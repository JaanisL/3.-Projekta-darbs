//211REC027 Jānis Lejnieks 4.grupa
//231RDB255 Mārtiņš Rimša 4.grupa
//231RDB247 Reinis Kurtišs 4.grupa

import java.util.Scanner;
import java.io.*;
import java.util.HashMap;
import java.util.PriorityQueue;

public class Main {
    //Represents a node used in a HuffmanTree. Compares nodes based on their frequencies to construct a tree.
    static class HuffmanTree implements Comparable<HuffmanTree> {
        public final int frequency; // frequency refers to count of symbols in input data
                                    // symbols with higher frequencies recieve shorter codewords during compression // optimazes space
        public HuffmanTree(int frequency) {
            this.frequency = frequency;
        }
        public int compareTo(HuffmanTree tree) {
            return frequency - tree.frequency;
        }
    }

    //For Leaf node in huffmantree to compress data
    static class HuffmanLeaf extends HuffmanTree {
        public final byte symbol;

        public HuffmanLeaf(byte symbol, int frequency) {
            super(frequency);
            this.symbol = symbol;
        }
    }
    //Represents internal node used for data compression --> combines 2 child nodes in one frequency
    static class HuffmanNode extends HuffmanTree {
        public final HuffmanTree left;
        public final HuffmanTree right;

        public HuffmanNode(HuffmanTree left, HuffmanTree right) {
            super(left.frequency + right.frequency);
            this.left = left;
            this.right = right;
        }
    }
    //Reads binary data from input. Makes buffer(temp.storage) to store bytes. Tracks number of bits remaining in current byte
    static class BitInputStream {
        private InputStream inputStream;
        private int currentByte;
        private int numBitsRemaining;

        public BitInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        this.currentByte = 0;
        this.numBitsRemaining = 0;
    }

    public int readBit() throws IOException {  //reads and returns next bit from input 
        if (numBitsRemaining == 0) {
            currentByte = inputStream.read();
            if (currentByte == -1) {
                return -1; // In the end
            }
            numBitsRemaining = 8;  //in byte
        }
        int bit = (currentByte >> (numBitsRemaining - 1)) & 1; //extracts next bit from current byte -> to right
        numBitsRemaining--;
        return bit;
    }

    public void close() throws IOException {   //closes input stream
        inputStream.close();
    }
}

    //writes binary data to output stream. Writes in buffer(temp.storage) then outputs it all.
    static class BitOutputStream {
        private OutputStream outputStream;
        private int currentByte;
        private int numBitsFilled;

        public BitOutputStream(OutputStream outputStream) {
            this.outputStream = outputStream;
            this.currentByte = 0;
            this.numBitsFilled = 0;
        }

        //writes specified number of bits out  
        public void writeBits(int numBits, int value) throws IOException {
            for (int i = numBits - 1; i >= 0; i--) {
                int bit = (value >> i) & 1;
                currentByte = (currentByte << 1) | bit;
                numBitsFilled++;
                if (numBitsFilled == 8) {
                    outputStream.write(currentByte);
                    currentByte = 0;
                    numBitsFilled = 0;
                }
            }
        }
        //closes writing
        public void close() throws IOException {
            while (numBitsFilled != 0) {
                writeBits(1, 0);
            }
            outputStream.close();
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String choiseStr;
        String sourceFile, resultFile, firstFile, secondFile;

        loop: while (true) {

            choiseStr = sc.next();
            // TO OPEM FILE TO (comp, decomp...) WRITE FULL PATH NAME.
            switch (choiseStr) {
                case "comp":
                    System.out.print("source file name: ");
                    sourceFile = sc.next();
                    System.out.print("archive name: ");
                    resultFile = sc.next();
                    comp(sourceFile, resultFile);
                    break;
                case "decomp":
                    System.out.print("archive name: ");
                    sourceFile = sc.next();
                    System.out.print("file name: ");
                    resultFile = sc.next();
                    decomp(sourceFile, resultFile);
                    break;
                case "size":
                    System.out.print("file name: ");
                    sourceFile = sc.next();
                    size(sourceFile);
                    break;
                case "equal":
                    System.out.print("first file name: ");
                    firstFile = sc.next();
                    System.out.print("second file name: ");
                    secondFile = sc.next();
                    System.out.println(equal(firstFile, secondFile));
                    break;
                case "about":
                    about();
                    break;
                case "exit":
                    break loop;
            }
        }

        sc.close();
    }

    //encodes file data with Huffman coding and writes compressed data to new output file with huffman tree structure.
    public static void comp(String sourceFileName, String archiveName) {
        try {
            FileInputStream fileInputStream = new FileInputStream(sourceFileName);
            HashMap<Byte, Integer> frequencyTable = buildFrequencyTable(fileInputStream);
            fileInputStream.close();

            HuffmanTree huffmanTree = buildHuffmanTree(frequencyTable);

            HashMap<Byte, String> codeTable = buildCodeTable(huffmanTree);

            FileOutputStream fileOutputStream = new FileOutputStream(archiveName);
            BitOutputStream bitOutputStream = new BitOutputStream(fileOutputStream);
            writeHuffmanTree(huffmanTree, bitOutputStream);
            writeCompressedData(sourceFileName, codeTable, bitOutputStream);
            bitOutputStream.close();

            System.out.println("Files compressed");

        } catch (IOException e) {
            System.out.println("Error compressing file: " + e.getMessage());
        }
    }

    // decompresses file from huffman compressed file - decodes data and writes in new output file the original file.
    public static void decomp(String archiveName, String fileName) {
        try {
            FileInputStream fileInputStream = new FileInputStream(archiveName);
            BitInputStream bitInputStream = new BitInputStream(fileInputStream);
            HuffmanTree huffmanTree = readHuffmanTree(bitInputStream);
        
            FileOutputStream fileOutputStream = new FileOutputStream(fileName);
            decodeHuffmanData(huffmanTree, bitInputStream, fileOutputStream);
        
            fileInputStream.close();
            fileOutputStream.close();
            bitInputStream.close();
            System.out.println("File decompressed successfully.");
        
        } catch (IOException e) {
            System.out.println("Error decompressing file: " + e.getMessage());
        }
    }

    //gets available bytes and prints size to console
    public static void size(String sourceFile) {
        try {
            FileInputStream f = new FileInputStream(sourceFile);
            System.out.println("size: " + f.available());
            f.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

    }

    //compares 2 files by reading bytes and comparing them 
    public static boolean equal(String firstFile, String secondFile) {
        try {
            FileInputStream f1 = new FileInputStream(firstFile);
            FileInputStream f2 = new FileInputStream(secondFile);

            int k1, k2;
            byte[] buf1 = new byte[1000];
            byte[] buf2 = new byte[1000];
            do {
                k1 = f1.read(buf1);
                k2 = f2.read(buf2);
                if (k1 != k2) {
                    f1.close();
                    f2.close();
                    return false;
                }

                for (int i = 0; i < k1; i++) {
                    if (buf1[i] != buf2[i]) {
                        f1.close();
                        f2.close();
                        return false;
                    }

                }
            } while (!(k1 == -1 && k2 == -1));
            f1.close();
            f2.close();
            return true;

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
    }

    public static void about() {
        // TODO insert information about authors
        System.out.println("211REC027 Jānis Lejnieks 4.grupa");
        System.out.println("231RDB255 Mārtiņš Rimša 4.grupa");
        System.out.println("231RDB247 Reinis Kurtišs 4.grupa");
    }

    //buildFrequencyTable, buildHuffmanTree, buildCodeTable, buildCodeTableHelper, writeHuffmanTree, readHuffmanTree, writeCompressedData, decodeHuffmanData.

    //Count byte frequency
    private static HashMap<Byte, Integer> buildFrequencyTable(InputStream inputStream) throws IOException {
        HashMap<Byte, Integer> frequencyTable = new HashMap<>();
        int byteRead;

        while ((byteRead = inputStream.read()) != -1) {
            frequencyTable.put((byte) byteRead, frequencyTable.getOrDefault((byte) byteRead, 0) + 1);
        }
        return frequencyTable;
    }

    //Constructs Huffman tree from byte frequency
    private static HuffmanTree buildHuffmanTree(HashMap<Byte, Integer> frequencyTable) {
        PriorityQueue<HuffmanTree> priorityQueue = new PriorityQueue<>();

        for (Byte symbol : frequencyTable.keySet()) {
            priorityQueue.offer(new HuffmanLeaf(symbol, frequencyTable.get(symbol)));
        }

        while (priorityQueue.size() > 1) {
            HuffmanTree left = priorityQueue.poll();
            HuffmanTree right = priorityQueue.poll();
            priorityQueue.offer(new HuffmanNode(left, right));
        }
        return priorityQueue.poll();
    }

    //Creates mapping of bytes to Huffman codewords for compression
    private static HashMap<Byte, String> buildCodeTable(HuffmanTree huffmanTree) {
        HashMap<Byte, String> codeTable = new HashMap<>();
        buildCodeTableHelper(huffmanTree, "", codeTable);
        return codeTable;
    }

    //Helps to build byte-to-codeword mapping for compression
    private static void buildCodeTableHelper(HuffmanTree huffmanTree, String code, HashMap<Byte, String> codeTable) {

        if (huffmanTree instanceof HuffmanLeaf) {
            HuffmanLeaf leaf = (HuffmanLeaf) huffmanTree;
            codeTable.put(leaf.symbol, code);

        } else if (huffmanTree instanceof HuffmanNode) {
            HuffmanNode node = (HuffmanNode) huffmanTree;
            buildCodeTableHelper(node.left, code + "0", codeTable);
            buildCodeTableHelper(node.right, code + "1", codeTable);
        }
    }
    //Writes Huffman tree structure for compression output (comp)
    private static void writeHuffmanTree(HuffmanTree huffmanTree, BitOutputStream bitOutputStream) throws IOException {

        if (huffmanTree instanceof HuffmanLeaf) {
            bitOutputStream.writeBits(1, 0);
            bitOutputStream.writeBits(8, ((HuffmanLeaf) huffmanTree).symbol);

        } else if (huffmanTree instanceof HuffmanNode) {
            bitOutputStream.writeBits(1, 1);
            writeHuffmanTree(((HuffmanNode) huffmanTree).left, bitOutputStream);
            writeHuffmanTree(((HuffmanNode) huffmanTree).right, bitOutputStream);
        }
    }
    //Reads Huffman tree structure during decompression
    private static HuffmanTree readHuffmanTree(BitInputStream bitInputStream) throws IOException {
        int bit = bitInputStream.readBit();
        if (bit == 0) {
            // Internal node
            HuffmanTree left = readHuffmanTree(bitInputStream);
            HuffmanTree right = readHuffmanTree(bitInputStream);
            return new HuffmanNode(left, right);

        } else {
            // Leaf node
            byte[] symbolBytes = new byte[1];

            for (int i = 7; i >= 0; i--) {
                int bitValue = bitInputStream.readBit();
                symbolBytes[0] |= (bitValue << i);
            }
            return new HuffmanLeaf(symbolBytes[0], 0); // Frequency wont matter 
        }
    }
    //WritesCompressedData in new file
    private static void writeCompressedData(String sourceFileName, HashMap<Byte, String> codeTable, BitOutputStream bitOutputStream) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(sourceFileName);
        int byteRead;

        while ((byteRead = fileInputStream.read()) != -1) {
            String code = codeTable.get((byte) byteRead);

            for (char bit : code.toCharArray()) {
                bitOutputStream.writeBits(1, bit == '1' ? 1 : 0);
            }
        }
        fileInputStream.close();

    // decodes compressed data during dcompression
    }
    private static void decodeHuffmanData(HuffmanTree huffmanTree, BitInputStream bitInputStream, FileOutputStream fileOutputStream) throws IOException {
        HuffmanTree current = huffmanTree;
        while (true) {
            if (current instanceof HuffmanLeaf) {
                HuffmanLeaf leaf = (HuffmanLeaf) current;
                if (leaf.symbol == -1) { // End of stream marker
                    break;
                }
                fileOutputStream.write(leaf.symbol);
                current = huffmanTree;
            } else if (current instanceof HuffmanNode) {
                HuffmanNode node = (HuffmanNode) current;
                int bit = bitInputStream.readBit();

                if (bit == -1) {
                    throw new IOException("Unexpected end of input stream");
                }

                current = (bit == 0) ? node.left : node.right; // Traverse left for 0 right for 1 -- Tree searching
            }
        }
    }
}