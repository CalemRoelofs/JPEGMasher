import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;

public class JPEGMasher {
    public static void main(String[] args) {
        try {
            // Serialize image file into byte buffer
            byte[] image = Files.readAllBytes(Paths.get("C:\\Users\\BigBoii\\IdeaProjects\\ImageFucker\\out\\production\\JPEGMasher\\image.jpg"));
            // Need to create a 14 byte buffer to find the string FF DA 00 0C 03 01 00 02 11 03 11 00 3F 00
            // Everything after this is the Image data, with a trailing FF D9 at the end for "End Of Image"
            // So initialize the byte array "buffer" to keep track of the last 14 bytes
            Byte[] buffer = new Byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
            // These headers are jank because they only work if the length of the Quantization table is 64
            // Next up is to just parse the flag 0xFF 0xDB and then create the array with the proceeding length
            Byte[] quantTableLumenHeaderList = new Byte[] { -1, -37, 0, 67, 0 };
            byte[] quantTableLumenHeader = new byte[] { -1, -37, 0, 67, 0 };
            Byte[] quantTableChromaHeaderList = new Byte[] { -1, -37, 0, 67, 1 };
            byte[] quantTableChromaHeader = new byte[] { -1, -37, 0, 67, 1 };
            Byte[] imageDataHeader = new Byte[] { -1, -38, 0, 12, 3, 1, 0, 2, 17, 3, 17, 0, 63, 0 };
            byte[] quantTableLumenData = new byte[64];
            byte[] quantTableChromaData = new byte[64];
            int startOfFrameLocation = 0;
            int index = 0;

            // For each byte in the byte array "image"
            for(byte b : image){
                // Left shift everything by 1
                for(int i = 1; i < buffer.length; i++) {
                    buffer[i-1] = buffer[i];
                }
                // Set the last element of the buffer to the newest byte
                buffer[13] = b;
                // Comparison check to see if we've reached the desired index
                if(Arrays.asList(buffer).subList(9,buffer.length).equals(Arrays.asList(quantTableLumenHeaderList))){
                    quantTableLumenData = Arrays.copyOfRange(image, index + 1, index + 65);
                    System.out.println("Extracted Quantization Table for Luminance (0xFF 0xDB .. 0x00)");
                    System.out.println(Arrays.toString(buffer));
                    System.out.println(Arrays.toString(quantTableLumenData));
                }
                if(Arrays.asList(buffer).subList(9,buffer.length).equals(Arrays.asList(quantTableChromaHeaderList))){
                    quantTableLumenData = Arrays.copyOfRange(image, index + 1, index + 65);
                    System.out.println("Extracted Quantization Table for Chrominance (0xFF 0xDB .. 0x00)");
                    System.out.println(Arrays.toString(buffer));
                    System.out.println(Arrays.toString(quantTableLumenData));
                    startOfFrameLocation = index + 65;
                }
                if(Arrays.equals(buffer, imageDataHeader)){
                    System.out.println("Extracted Image Data (0xFF 0xDA)");
                    System.out.println(Arrays.toString(buffer));
                    break;
                }
                index++;
            }

            // Split off into 3 arrays, one with all the meta data and tables,
            byte[] app0Data = Arrays.copyOfRange(image, 0, 20);
            // and then finally the last 2 trailing bytes
            byte[] sofData = Arrays.copyOfRange(image, startOfFrameLocation, index);
            byte[] imageData = Arrays.copyOfRange(image, index + 1, image.length - 2);
            byte[] imageEnd = Arrays.copyOfRange(image, image.length - 1, image.length);
            // Shuffle the image data to make it skitz out

            quantTableLumenData = pixelMash(quantTableLumenData);
            quantTableChromaData = pixelMash(quantTableChromaData);
            imageData = pixelMash(imageData);

            // Concatenate the all the parts of the image back together
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(app0Data);
            outputStream.write(quantTableLumenHeader);
            outputStream.write(quantTableLumenData);
            //Debug Statement
            //System.out.println(byteArrayToHex(quantTableLumenData)); Debug statement
            outputStream.write(quantTableChromaHeader);
            outputStream.write(quantTableChromaData);
            //Debug Statement
            //System.out.println(byteArrayToHex(quantTableChromaData));
            outputStream.write(sofData);
            outputStream.write(imageData);
            //Debug Statement
            //System.out.println(byteArrayToHex(imageData));
            outputStream.write(imageEnd);

            byte[] output = outputStream.toByteArray();
            // Write the bytes back to the file
            try (FileOutputStream stream = new FileOutputStream("C:\\Users\\BigBoii\\IdeaProjects\\ImageFucker\\out\\production\\JPEGMasher\\output.jpg")) {
                stream.write(output);
            }

        } catch(FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch(IndexOutOfBoundsException iobe) {
            iobe.printStackTrace();
        }

    }

    // Really useful for debugging
    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x ", b));
        return sb.toString();
    }

    // This function takes the byte array for any of the sections above
    // and mangles it by randomising the byte order.
    public static byte[] pixelMash(byte[] imageData) {
        Random rand = new Random();
        for (int i = 0; i < imageData.length/16; i++){
            int randomIndex = rand.nextInt(imageData.length);
            byte temp = imageData[randomIndex];
            imageData[randomIndex] = imageData[i];
            imageData[i] = temp;
        }
        return imageData;
    }

}
