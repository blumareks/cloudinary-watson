# cloudinary-watson


## LAB 1
Content Assistant: 

A swift mobile app (or TJ Bot) might become a chatbot - a robot content assistant for picture search - 
ask verbally the app (or TJ BOT) for a picture - then the chatbot would search the Cloudinary API for images, 
and post links to pictures - you can choose the picture for your article:   
"Search for San Francisco"  "Add mono filter" "add square transformation"   "Show me a bay bridge in San Francisco."
The app would post the image links to Slack channel to manipulate the links to images.


## Web App
Here comes the details....

## iOS Swift App
In order to start you would need the following:
- [XCode Studio - for MAC only](https://developer.apple.com/xcode/downloads/)
- an iOS simulator, or a device and the Apple Developer License 
- [Sign up to Bluemix](https://bluemix.net/registration/) to create a project with Watson services:
- credentials (API KEY) to the Watons conversation service
- Watson credentials (id and password) to Speech to Text and Text to Speech services

Here comes the steps to create Content Assistant iOS app:

1. Create a typical iOS application in Swift
2. Add the Watson SDK: [Watson-Developer-Cloud SDK for Swift](https://github.com/watson-developer-cloud/swift-sdk#installation)
3. Instantiate the Bluemix Watson services and get the keys/a token to them
4. Add some code in your iOS app to invoke the cognitive services
5. Quick test of the app.

Alternatively you could use TJ Bot Raspberry Pi robot (you would need a mic, a speaker without the XCode and iOS IDE). Look here for inspiration on how to achieve that with JeanCarl Bisson's' [node-red labs](https://github.com/jeancarl/node-red-labs)



Delivered to you by Cloudinary and IBM 

Subscribe to our Twitter: @Dan and @blumareks
