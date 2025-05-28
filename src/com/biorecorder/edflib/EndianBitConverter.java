package com.biorecorder.edflib;


/**
 * Contains several useful static methods to convert Java integer (32-bit, signed, BIG_ENDIAN)
 * to an array of bytes (BIG_ENDIAN or LITTLE_ENDIAN ordered) and vice versa
 */
public class EndianBitConverter {


    /**
     * Convert specified number of bytes (4, 3, 2 or 1) of the LITTLE_ENDIAN ordered byte array
     * to BIG_ENDIAN java integer.
     * Сonversion begins from the specified offset position.
     *
     * @param byteArray           byte array (LITTLE_ENDIAN ordered) from which 4, 3, 2 or 1 bytes
     *                            are taken to be converted to int
     * @param numberOfBytesPerInt number of bytes that should be converted to int. Can be: 4, 3, 2 or 1.
     * @param offset              the offset within the array of the first byte to be converted
     * @return standard 32-bit signed java int (BIG_ENDIAN)
     */
    public static int littleEndianBytesToInt(byte[] byteArray, int offset, int numberOfBytesPerInt) {
        switch (numberOfBytesPerInt) {
            case 1:
                return byteArray[offset];
            case 2:
                return (byteArray[offset + 1] << 8) | (byteArray[offset] & 0xFF);
            case 3:
                return (byteArray[offset + 2] << 16) | (byteArray[offset + 1] & 0xFF) << 8 | (byteArray[offset] & 0xFF);
            case 4:
                return (byteArray[offset + 3] << 24) | (byteArray[offset + 2] & 0xFF) << 16 | (byteArray[offset + 1] & 0xFF) << 8 | (byteArray[offset] & 0xFF);
            default:
                String errMsg = "Wrong «number of bytes per int» = " + numberOfBytesPerInt +
                        "! Available «number of bytes per int»: 4, 3, 2 or 1.";
                throw new IllegalArgumentException(errMsg);
        }
    }


    /**
     * Convert specified number of elements from LITTLE_ENDIAN ordered byte array
     * (starting from byteArrayOffset position) to ints and
     * write resultant ints to the given int array (starting from intArrayOffset position)
     *
     * @param byteArray           byte array (LITTLE_ENDIAN ordered) to be converted to int array
     * @param byteArrayOffset     the offset within the byte array of the first byte to be converted
     * @param intArray            int array to write resultant ints
     * @param intArrayOffset      the offset within the int array of the first int to be written
     * @param lengthInInts        number of resultant ints.
     *                            Number of bytes that will be converted to ints = lengthInInts * numberOfBytesPerInt
     * @param numberOfBytesPerInt number of bytes converted to ONE int. Can be: 4, 3, 2 or 1.
     */
    public static void littleEndianByteArrayToIntArray(byte[] byteArray, int byteArrayOffset, int[] intArray, int intArrayOffset, int lengthInInts, int numberOfBytesPerInt) {
        for (int index = 0; index < lengthInInts; index++) {
            intArray[index + intArrayOffset] = littleEndianBytesToInt(byteArray, index * numberOfBytesPerInt + byteArrayOffset, numberOfBytesPerInt);
        }
    }
}
