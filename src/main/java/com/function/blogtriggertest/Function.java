package com.function.blogtriggertest;

import com.microsoft.azure.functions.annotation.*;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.blob.*;

/**
 * Azure Functions with Azure Blob trigger.
 */


public class Function {
    /**
     * This function will be invoked when a new or updated blob is detected at the specified path. The blob contents are provided as input to this function.
     */

    public static final Pattern userIdPattern = 
    Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    public static final Pattern roleIdPattern =
    Pattern.compile("^R[0-9]+$");
    public static final Pattern roleNamePattern =
    Pattern.compile("^Role[0-9]+$");
    public static final Pattern menuNamePattern =
    Pattern.compile("^M[0-9]+$");
    public static final Pattern screenIdPattern =
    Pattern.compile("^S[0-9]+$");
    public static final Pattern screenNamePattern =
    Pattern.compile("^Screen[0-9]+$");
    public static final Pattern accessLevelPattern =
    Pattern.compile("^(R|W)$");
    
    public static final String storageConnectionString = 
    "DefaultEndpointsProtocol=https;" +
    "AccountName=usercsv;" +
    "AccountKey=OtciLg/q+q+5Kt3gepXmkxftIsTjOYC0CrkWqyh9MARfXcMqwBszfi71Ni5tkWx0+r4Lf4Bf7vPJQMS1jQMqAw==;EndpointSuffix=core.windows.net";
    File downloadedFile = null;
    
    @FunctionName("BlobTriggerJava")
    @StorageAccount("AzureWebJobsStorage")
    public void run(
        @BlobTrigger(name = "content", path = "csvfiles/{name}.csv", dataType = "binary") byte[] content,
        @BindingName("name") String name,
        final ExecutionContext context
    ) {
        context.getLogger().info("Java Blob trigger function processed a blob. Name: " + name + "\n  Size: " + content.length + " Bytes");
        parseCsvFile(name);
    }

    public String parseCsvFile (String name){
        //download file to local storage and parse it (easier)
        //or just read and edit the files (harder)
        CloudStorageAccount storageAccount;
		CloudBlobClient blobClient = null;
		CloudBlobContainer container=null;
       

        System.out.println("this is the file name: " + name);
        try {
            storageAccount = CloudStorageAccount.parse(storageConnectionString);
            blobClient = storageAccount.createCloudBlobClient();
            container = blobClient.getContainerReference("csvfiles");
           

            CloudBlockBlob blob = container.getBlockBlobReference(name+".csv");

            blob.downloadToFile("C:/Users/nickl/azuredownload/"+name+".csv");
            convertToJson(name);
        } catch (Exception e) {
            System.out.println("1st try");
            System.out.println(e);
        }
        return  "sucess";
    }

    public String convertToJson(String name) {
        try(BufferedReader in = new BufferedReader(new FileReader("C:/Users/nickl/azuredownload/"+name+".csv"));){
			CsvToBean<userFile> csvToBean = new CsvToBeanBuilder<userFile>(in)
					.withType(userFile.class)
					.withIgnoreLeadingWhiteSpace(true)
					.build();
                    //System.out.println("in here 1");
			List<userFile> users = csvToBean.parse();
			ObjectWriter ow = new ObjectMapper().writer();//.withDefaultPrettyPrinter();
            List<userFileConfig> errorCheckedList= new ArrayList<userFileConfig>();
            errorCheckedList.add(new userFileConfig("userID", "roleID", "role_name", "menu_name", "screenID", "screen_name", "access_level", "response"));
			//System.out.println("in here 2");
            for(int i = 0; i <users.size(); i++) {
                //System.out.println("Number Processed: " + users.get(i).getRoleID());
				String userJsonCheck = ow.writeValueAsString(users.get(i));
                //System.out.println("userJsonCheck: " + userJsonCheck);
                String errorMessage = checkErrors(users.get(i).getUserID(), users.get(i).getRoleID(), users.get(i).getRole_name(), 
                users.get(i).getMenu_name(), users.get(i).getScreenID(), users.get(i).getScreen_name(), 
                users.get(i).getAccess_level());
                if(errorMessage.isEmpty())
                {
                   
                    String userJson = "{\"userID\": \""+users.get(i).getUserID()+"\","+
                    "\"rolelists\": ["+
                        "{"+
                            "\"roleID\": \""+users.get(i).getRoleID()+"\","+
                            "\"roleName\": \""+users.get(i).getRole_name()+"\""+
                        "}"+
                       
                    "],"+
                    "\"menuslists\": ["+
                        "{"+
                            "\"menuName\": \""+users.get(i).getMenu_name()+"\","+
                            "\"screenlists\": ["+
                                "{"+
                                    "\"screenID\": \""+users.get(i).getScreenID()+"\","+
                                    "\"accessLevel\": \""+users.get(i).getAccess_level()+"\","+
                                    "\"screenName\": \""+users.get(i).getScreen_name()+"\""+
                                "}"+
                            "]"+
                        "}"+
                    "]"+
                "}";

				
                errorCheckedList.add(new userFileConfig(users.get(i).getUserID(), users.get(i).getRoleID(), users.get(i).getRole_name(), 
                users.get(i).getMenu_name(), users.get(i).getScreenID(), users.get(i).getScreen_name(), 
                users.get(i).getAccess_level(), ""));
               int uploadFail = POSTRequest(userJson);
               if (uploadFail != -1){
                errorCheckedList.get(i).setResponse("Entry not uploaded" + '\n');
               }
                //System.out.println("in here 3");
                }else{
                    users.get(i).setResponse(errorMessage);
                    errorCheckedList.add(new userFileConfig(users.get(i).getUserID(), users.get(i).getRoleID(), users.get(i).getRole_name(), 
                    users.get(i).getMenu_name(), users.get(i).getScreenID(), users.get(i).getScreen_name(), 
                    users.get(i).getAccess_level(), users.get(i).getResponse()));
                    String userJsonError = ow.writeValueAsString(users.get(i));
                    //System.out.println("in here 4");   
             /**  
             * edit csv
             * 
            */
           
            try{
                FileWriter writer = new FileWriter("C:/Users/nickl/azuredownload/"+name+"-uploaded.csv");
                ColumnPositionMappingStrategy mappingStrategy=
                new ColumnPositionMappingStrategy();
                mappingStrategy.setType(userFileConfig.class);

                // Arrange column name as provided in below array.
                String[] columns = new String[] 
                        {"userID", "roleID", "role_name", "menu_name", "screenID", "screen_name", "access_level", "response"};
                mappingStrategy.setColumnMapping(columns);

                // Createing StatefulBeanToCsv object
                StatefulBeanToCsvBuilder<userFileConfig> builder=
                            new StatefulBeanToCsvBuilder(writer);
                StatefulBeanToCsv beanWriter = 
                builder.withMappingStrategy(mappingStrategy).build();

                // Write list to StatefulBeanToCsv object
                beanWriter.write(errorCheckedList);

                // closing the writer object
                writer.close();

                CloudStorageAccount storageAccount;
                CloudBlobClient blobClient = null;
                CloudBlobContainer containerUploaded=null;

                storageAccount = CloudStorageAccount.parse(storageConnectionString);
                blobClient = storageAccount.createCloudBlobClient();
                containerUploaded = blobClient.getContainerReference("completedcsvfiles");
                CloudBlockBlob uploadBlob = containerUploaded.getBlockBlobReference(name+"-uploaded.csv");
                uploadBlob.uploadFromFile("C:/Users/nickl/azuredownload/"+name+"-uploaded.csv");

            }catch(Exception e){
                System.out.println("3rd try");
                System.out.println(e);
            }

        }
            
        }
			return "registered";
		}catch (Exception ex) {
            System.out.println("2nd try");
            System.out.println(ex.toString());
			return ex.toString();
	    }	
	}

	public static int POSTRequest(String userJson) throws IOException {

	    final String POST_PARAMS = userJson;
	    //System.out.println(POST_PARAMS);
	    //URL obj = new URL("http://localhost:8051/users/add");
        URL obj = new URL("http://13.70.85.25:8051/users/add");
	    HttpURLConnection postConnection = (HttpURLConnection) obj.openConnection();
	    postConnection.setRequestMethod("POST");
	    postConnection.setRequestProperty("Content-Type", "application/json");

	    postConnection.setDoOutput(true);
	    OutputStream os = postConnection.getOutputStream();
	    os.write(POST_PARAMS.getBytes());
	    os.flush();
	    os.close();

	    int responseCode = postConnection.getResponseCode();
	    //System.out.println("POST Response Code :  " + responseCode);
	    //System.out.println("POST Response Message : " + postConnection.getResponseMessage());

	    if (responseCode == HttpURLConnection.HTTP_OK) { //success
	        BufferedReader in = new BufferedReader(new InputStreamReader(
	            postConnection.getInputStream()));
	        String inputLine;
	        StringBuffer response = new StringBuffer();

	        while ((inputLine = in .readLine()) != null) {
	            response.append(inputLine);
	        } in .close();

	        // print result
	        //System.out.println(response.toString());
            return -1;
	    } else {
	        //System.out.println("POST NOT WORKED");
            return 1;

	    }
	}

    public String checkErrors(String userId, String roleId, String roleName, String menuName, String screenID, String screenName, String accessLevel){
        StringBuilder valid = new StringBuilder();
        if(!userIdPattern.matcher(userId).find() || userId == null) {
           
            valid.append("ID is incorrect" + '\n');
        }  
        if(!roleIdPattern.matcher(roleId).find()){
            
            valid.append("Role ID is incorrect" + '\n'); 
        }
        if(!roleNamePattern.matcher(roleName).find()){
           
            valid.append("Role Name is incorrect" + '\n'); 
        }
        if(!menuNamePattern.matcher(menuName).find()){
           
            valid.append("Menu Name is incorrect" + '\n'); 
        }
        if(!screenIdPattern.matcher(screenID).find()){
           
            valid.append("Screen ID is incorrect" + '\n'); 
        }
        if(!screenNamePattern.matcher(screenName).find()){
           
            valid.append("Screen Name is incorrect" + '\n'); 
        }
        if(!accessLevelPattern.matcher(accessLevel).find()){
           
            valid.append("Access Level is incorrect");
        }
        return valid.toString();
    }

   
    
}


