[![Build Status](https://travis-ci.org/atsid/pst2gmail.svg?branch=master)](https://travis-ci.org/atsid/pst2gmail)

# Import Outlook PST data files into GMail (pst2gmail)

##Overview
The purpose of this application is to remove attachments from emails before importing into GMail.  This is useful when your organization has data that is not allowed to be stored in GMail via policy of Google and/or your organization.  Each email that has had attachments removed will indicate as much in the subject and contain a small snippet in the message that indicates the names of the removed attachments.

##How to Build
Builds are done with maven.

1.  Install maven if not already installed
2.  Run `mvn clean package` in the root directory
 
##How to run

1.  Obtain the application.properties file and update it to suit your needs.  Please make sure that you properly configure the `sender.resolver.map` property for your organization.
2.  Enable GMail API for the account you are using.  Please note that you either need to use the end-users account or a propertly configured service account with domain wide permissions.
  1.  Browse to the google developers console [here] (https://console.developers.google.com/flows/enableapi?apiid=gmail)
  2.  Login with the users gmail account or a domain service account.
  3.  Select `Create a new project` and click `Continue`
  4.  After project is created click `Go to Credentials`
  5.  In the OAuth section, click `Create new Client ID`
  6.  Select `Installed application` and then `Configure consent screen`
  7.  Make sure all information on the page is filled in, then click `Save`.  Make note of the Product name as this is what you will need to put in the properties file for the value of gmail.application.name.
  8.  After ID has been created, click the `Download JSON` button to download the key to your local system.  Keep this safe as it allows access to use the GMail API.
3.  Change into the directory where you build the application
4.  Change into the importer/target folder
5.  run `java -jar pst-clean-gmail-importer-app-1.0-SNAPSHOT.jar -Dconfig.file=<FULL_PATH_TO_APPLICATION_PROPERTIES_FILE>`
  *. At some point the application will start up your default browser and give you a permissions prompt.  This will only happen the first time you run the application.
6.  In the interface, enter the following:
  1.  Browse to find PST file
  2.  Enter output directory where attachments will get placed (or browse)
  3.  Browse to select the JSON file you downloaded above
  4.  Enter the email address of the uesr who's PST you are importing
  5.  Click the start button
  
After the application completes, the status bar will show 100% and the output in the console or log files should indiate it has completed.  All attachments will be in the output folder with an attachments.log indicating what attachments are there.
 
##Known Issues
  * Resolving of exchange email address just does simple string matching based on the map provided in the sender.resolver.map file.  A more robust solution would query the exchange server to fully resolve the address.
  * Encrypted emails are treated as emails with attachments.  The entire contents of the email will be removed and stored in the attachment archive.