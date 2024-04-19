# Blackboard Downloader GUI  
## Description  
A GUI programme that batch downloads files from courses on CUHK's Blackboard platform, so that you don't have to manually click on each file in a browser to download them.  
Only works for CUHK's Blackboard platform.  
Note: This programme is NOT produced by CUHK  

## Setup
Supported platform: Currently only Windows.  
Tested on Windows 10 with JDK 17.0.8  

1. Install a Java Runtime Environment  
   You'll need a JRE equivalent to or newer than Java 17  
   You can find one from Azul Zulu:  
   https://www.azul.com/downloads/?version=java-17-lts&os=windows&package=jre#zulu
     
   Alternatively, you can use JDK 17 from Oracle:  
   https://www.oracle.com/java/technologies/downloads/#java17  


1. Download a compiled jar executable from [Releases](https://github.com/steveglowplunk/Blackboard-Downloader-GUI/releases).  

## Usage  

1. Login to your CUHK Blackboard account through a broswer, such as Firefox or Chrome  

1. Use a browser extension to export a (Netscape format) cookies file, in txt format  
	- If you're using Firefox, use this extension:  
	"Export Cookies" by Rotem Dan  
	https://addons.mozilla.org/en-US/firefox/addon/export-cookies-txt/  
	For example:  
	![firefox_2023_08_08_110851](https://github.com/steveglowplunk/Blackboard-Downloader-GUI/assets/28670916/827e229d-1fb1-4c6b-8f33-3f33b5c21cf8)

	- If you're using Chrome, use this extension:  
	"Get cookies.txt LOCALLY" by kairi003  
	https://chrome.google.com/webstore/detail/get-cookiestxt-locally/cclelndahbckbenkjhflpdbgdldlbecc  
	For example:  
	![chrome_2023_08_08_110954](https://github.com/steveglowplunk/Blackboard-Downloader-GUI/assets/28670916/50ee50cd-c5ee-4183-b4f6-b35cb1cb10d8)

	- But what is a cookie?  
	Think of it as a temporary key that allows login without entering an account and password.  
	The cookies from Blackboard expire every few hours so you'll need to re-export your cookies when they are expired.  
	
1. Simply double click on Blackboard-Downloader-GUI.jar to open the application  

1. 	Click "Load Cookies File" and choose the cookies txt file exported by your browser earlier. You should see a simlar screen:  
![javaw_2023_08_25_171608](https://github.com/steveglowplunk/Blackboard-Downloader-GUI/assets/28670916/dc3b7400-406f-4101-88e9-88515adf790c)
Note: Only available courses (not "locked" on Blackboard's website) can be loaded  

1. Select a course from the list and click "Load Course Content" to load the file list of the selected course  

1. Click on checkboxes to select files you wish to download  

1. Click "Select Download Destination" to choose where the files will be downloaded to  
![javaw_2023_08_25_171814](https://github.com/steveglowplunk/Blackboard-Downloader-GUI/assets/28670916/50aa29cd-f64b-4186-9567-786882954544)
1. Click "Download Selected Files" to start downloading  
![javaw_2023_08_25_171913](https://github.com/steveglowplunk/Blackboard-Downloader-GUI/assets/28670916/2b04f1ff-efe9-4c45-8b86-ce925c0b7e5d)

## Build environment  
The project is built using IntelliJ IDEA, with Amazon Corretto 17 JDK and JavaFX Scene Builder 20.0.0  

## Credits  
The downloader library is modified from here:  
https://github.com/MrMarnic/JavaDownloadLibrary
