package suita.tarumi.SpringDetectFace.Controller;

import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController

public class ImgController {

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public String transferImg(@RequestBody String base64CapturedImg) throws IOException {
        //Base64のデコードをする前にdataから「data:image/jpg;base64」を削除する必要があるためデータと分ける
        String[] base64CapturedImgForDecode = base64CapturedImg.split(",");
        //デコード・base64CapturedImgForDecode[1]はデータを指す、[0]は「data:image/jpg;base64」
        byte[] decodedCapturedImg = Base64.getDecoder().decode(base64CapturedImgForDecode[1].getBytes(StandardCharsets.UTF_8));

        //撮影画像の置き場をOSに依存しないように作成
        System.out.println("User homeは" + System.getProperty("user.home")+File.separator);
        Path destinationFile = Paths.get(System.getProperty("user.home")+File.separator, "detectedFace.png");

        //撮影画像をpngファイルとして作成
        Files.write(destinationFile, decodedCapturedImg);

        //ここからAWS Rekognition
        String sourceImage = destinationFile.toString();
        Region region = Region.AP_NORTHEAST_1;
        RekognitionClient rekClient = RekognitionClient.builder()
                .region(region)
                .build();

        String result = detectFaceImage(rekClient, sourceImage );
        rekClient.close();

        return result;

    }


    public static String detectFaceImage(RekognitionClient rekClient,String sourceImage ) {
        String result = "";

        try {
            InputStream sourceStream = new FileInputStream(new File(sourceImage));
            SdkBytes sourceBytes = SdkBytes.fromInputStream(sourceStream);

            Image souImage = Image.builder()
                    .bytes(sourceBytes)
                    .build();

            DetectFacesRequest facesRequest = DetectFacesRequest.builder()
                    .attributes(Attribute.ALL)
                    .image(souImage)
                    .build();

            DetectFacesResponse facesResponse = rekClient.detectFaces(facesRequest);
            List<FaceDetail> faceDetails = facesResponse.faceDetails();

            for (FaceDetail face : faceDetails) {
                System.out.println("笑顔判定 : "+ face.smile().value().toString());
                if(face.smile().value().toString() == "true"){
                    result = "いってらっしゃい！";
                } else {
                    result = "笑顔でもう一度！";
                }
            }

        } catch (RekognitionException | FileNotFoundException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        } finally {
            return result;
        }
    }

}
