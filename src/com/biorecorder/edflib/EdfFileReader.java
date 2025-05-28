package com.biorecorder.edflib;

import com.biorecorder.edflib.exceptions.EdfHeaderRuntimeException;
import com.biorecorder.edflib.exceptions.FileNotFoundRuntimeException;
import com.biorecorder.edflib.exceptions.EdfRuntimeException;

import java.io.*;
import java.nio.ByteBuffer;
import java.text.MessageFormat;

/**
 * Permits to read data samples from EDF or BDF file. Also it
 * reads information from the file header and saves it in special {@link HeaderConfig} object, that we
 * can get by method {@link #getHeader()}
 * <p>
 * EDF/BDF files contains "row" digital (int) data but they can be converted to corresponding
 * real physical floating point data on the base of header information (physical maximum and minimum
 * and digital maximum and minimum specified for every channel (signal)).
 * So we can "read" both digital or physical values.
 * See: {@link #readDigitalSamples(int, int[], int, int)}, {@link #readPhysicalSamples(int, double[], int, int)}
 */
public class EdfFileReader {
    private HeaderConfig headerConfig;
    private FileInputStream fileInputStream;
    private File file;
    private long[] samplesPositionList;
    private int recordPosition = 0;

    /**
     * Creates EdfFileReader to read data from the file represented by the specified
     * File object. Before create EdfFileReader you can check if the file is valid EdF/Bdf file:
     *
     * @param file Edf or Bdf file to be opened for reading
     * @throws FileNotFoundRuntimeException   if the file does not exist,
     *                                        is a directory rather than a regular file,
     *                                        or for some other reason cannot be opened for reading.
     * @throws EdfHeaderRuntimeException if the the file is not valid EDF/BDF file
     *                                        due to some errors in its header record
     */
    public EdfFileReader(File file) throws FileNotFoundRuntimeException, EdfHeaderRuntimeException {
        this.file = file;
        try {
            fileInputStream = new FileInputStream(file);
            headerConfig = new HeaderConfig(file);
        } catch (EdfHeaderRuntimeException e) {
            throw e;
        } catch (Exception e) {
            String errMsg = MessageFormat.format("File: {0} can not be opened for reading", file);
            throw new FileNotFoundRuntimeException(errMsg, e);
        }
        samplesPositionList = new long[headerConfig.getNumberOfSignals()];
    }

    /**
     * Set the sample position indicator of the given channel (signal)
     * to the given new position. The position is measured in samples.
     * <p>
     * Note that every signal has it's own independent sample position indicator and
     * setSamplePosition() affects only one of them.
     * Methods {@link #readDigitalSamples(int, int[], int, int)} and
     * {@link #readPhysicalSamples(int, double[], int, int)} will start reading
     * samples belonging to a channel from the specified for that channel position.
     *
     * @param signalNumber channel (signal) number whose sample position we change. Numbering starts from 0!
     * @param newPosition  the new sample position, a non-negative integer counting
     *                     the number of samples belonging to the specified
     *                     channel from the beginning of the file
     */
    public void setSamplePosition(int signalNumber, long newPosition) {
        samplesPositionList[signalNumber] = newPosition;
    }


    /**
     * Helper method that reads only samples belonging to the given channel within the DataRecord at the given position.
     * Read samples will be saved in the specified ByteBuffer.
     * The values are the "raw" digital (integer) values.
     * Note the this method does note affect the DataRecord position used by the methods
     * {@link #readDigitalSamples(int, int[], int, int)} and
     * {@link #readPhysicalSamples(int, double[], int, int)} (int, int)}
     *
     * @param signalNumber   channel (signal) number whose samples must be read within the given DataRecord
     * @param recordPosition position of the DataRecord
     * @param buffer         buffer where read samples will be saved
     * @return amount of read samples
     * @throws EdfRuntimeException if data can not be read
     */
    private int readSamplesFromRecord(int signalNumber, int recordPosition, ByteBuffer buffer) throws EdfRuntimeException {
        long signalPosition = headerConfig.getDataRecordLength() * recordPosition;
        for (int i = 0; i < signalNumber; i++) {
            signalPosition += headerConfig.getNumberOfSamplesInEachDataRecord(i);
        }
        signalPosition = signalPosition * headerConfig.getFileType().getNumberOfBytesPerSample() + headerConfig.getNumberOfBytesInHeaderRecord();
        int readByteNumber = 0;
        try {
            readByteNumber = fileInputStream.getChannel().read(buffer, signalPosition);
        } catch (IOException e) {
            String errMsg = MessageFormat.format("Error while reading data from the file: {0}.", file);
            throw new EdfRuntimeException(errMsg, e);
        }
        return readByteNumber;
    }


    /**
     * Read the given number of samples belonging to the  channel
     * starting from the current sample position indicator.
     * The values are the "raw" digital (integer) values.
     * The sample position indicator of that channel will be increased with the amount of samples read.
     * Read samples are saved in the specified array starting at the specified offset.
     * Return the amount of read samples (this can be less than given numberOfSamples or zero!)
     *
     * @param signalNumber    channel (signal) number whose samples must be read. Numbering starts from 0!
     * @param buffer          buffer where read samples are saved
     * @param offset          offset within the buffer array at which saving starts
     * @param numberOfSamples number of samples to read
     * @return the amount of read samples (this can be less than given numberOfSamples or zero!)
     * @throws EdfRuntimeException if data can not be read
     */
    public int readDigitalSamples(int signalNumber, int[] buffer, int offset, int numberOfSamples) throws EdfRuntimeException {
        int readTotal = 0;
        int samplesPerRecord = headerConfig.getNumberOfSamplesInEachDataRecord(signalNumber);
        byte[] rowData = new byte[samplesPerRecord * headerConfig.getFileType().getNumberOfBytesPerSample()];
        ByteBuffer byteBuffer = ByteBuffer.wrap(rowData);
        int recordNumber = (int) (samplesPositionList[signalNumber] / samplesPerRecord);
        int positionInRecord = (int) (samplesPositionList[signalNumber] % samplesPerRecord);

        while (readTotal < numberOfSamples) {
            if (readSamplesFromRecord(signalNumber, recordNumber, byteBuffer) < byteBuffer.capacity()) {
                break;
            }
            int readInRecord = Math.min(numberOfSamples - readTotal, samplesPerRecord - positionInRecord);
            EndianBitConverter.littleEndianByteArrayToIntArray(rowData, positionInRecord * headerConfig.getFileType().getNumberOfBytesPerSample(), buffer, offset + readTotal, readInRecord, headerConfig.getFileType().getNumberOfBytesPerSample());
            readTotal += readInRecord;
            byteBuffer.clear();
            recordNumber++;
            positionInRecord = 0;
        }
        samplesPositionList[signalNumber] += readTotal;
        return readTotal;
    }


    /**
     * Read the given number of samples belonging to the channel
     * starting from the current sample position indicator. Converts the read samples
     * to their physical values (e.g. microVolts, beats per minute, etc) and
     * saves them in the specified buffer array starting at the specified offset.
     * The sample position indicator of that channel will be increased with the
     * amount of samples read.
     * Return the amount of read samples (this can be less than given numberOfSamples or zero!)
     *
     * @param signalNumber    channel (signal) number whose samples must be read. Numbering starts from 0!
     * @param buffer          buffer where resultant values are saved
     * @param offset          offset within the buffer array at which saving starts
     * @param numberOfSamples number of samples to read
     * @return the amount of read samples (this can be less than given numberOfSamples or zero!)
     * @throws EdfRuntimeException if data can not be read
     */
    public int readPhysicalSamples(int signalNumber, double[] buffer, int offset, int numberOfSamples) throws EdfRuntimeException {
        int[] digSamples = new int[numberOfSamples];
        int numberOfReadSamples = readDigitalSamples(signalNumber, digSamples, 0, numberOfSamples);
        for (int i = 0; i < numberOfReadSamples; i++) {
            buffer[i + offset] = headerConfig.digitalValueToPhysical(signalNumber, digSamples[i]);
        }
        return numberOfReadSamples;
    }


    /**
     * Return the information from the file header stored in the HeaderConfig object
     *
     * @return the object containing EDF/BDF header information
     */
    public HeaderConfig getHeader() {
        return headerConfig;
    }


    /**
     * Calculate and get the total number of  DataRecords in the file.
     * <br>getNumberOfDataRecords() = availableDataRecords() + getDataRecordPosition();
     *
     * @return total number of DataRecords in the file
     */
    public int getNumberOfDataRecords() {
        return (int) (file.length() - headerConfig.getNumberOfBytesInHeaderRecord()) / (headerConfig.getDataRecordLength() * headerConfig.getFileType().getNumberOfBytesPerSample());
    }

    /**
     * Calculate and get the total number of samples of the given channel (signal)
     * in the file.
     * <br>getNumberOfSamples(sampleNumberToSignalNumber) = availableSamples(sampleNumberToSignalNumber) + getSamplePosition(sampleNumberToSignalNumber);
     *
     * @return total number of samples of the given signal in the file
     */
    public long getNumberOfSamples(int signalNumber) {
        return (long) getNumberOfDataRecords() * (long) headerConfig.getNumberOfSamplesInEachDataRecord(signalNumber);
    }


    /**
     * Close this EdfFileReader and releases any system resources associated with
     * it. This method MUST be called after finishing reading DataRecords.
     * Failing to do so will cause unnessesary memory usage and corrupted and incomplete data writing.
     *
     * @throws EdfRuntimeException if an I/O  occurs while closing the file reader
     */
    public void close() throws EdfRuntimeException {
        try {
            fileInputStream.close();
        } catch (IOException e) {
            String errMsg = MessageFormat.format("Error while closing the file: {0}.", file);
            new EdfRuntimeException(errMsg, e);
        }
    }
}
