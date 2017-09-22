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

1. Create an iOS application in Swift using the Bluemix Mobile Project (left menu - look for Mobile - choose Chatbot Project)
2. Add the Watson SDK: [Watson-Developer-Cloud SDK for Swift](https://github.com/watson-developer-cloud/swift-sdk#installation)
3. Instantiate the Bluemix Watson services and get the keys/a token to them
4. Add some code in your iOS app to invoke the cognitive services
5. Quick test of the app.

Alternatively you could use TJ Bot Raspberry Pi robot (you would need a mic, a speaker without the XCode and iOS IDE). Look here for inspiration on how to achieve that with JeanCarl Bisson's' [node-red labs](https://github.com/jeancarl/node-red-labs)

The code looks like this:
```swift
import UIKit
import SwiftSpinner
import ConversationV1
import JSQMessagesViewController
import BMSCore

//parsing json
import SwiftyJSON

//slack
import SlackKit

//Watson TTS
import TextToSpeechV1
//audio!
import AVFoundation

//Watson STT
import SpeechToTextV1



class ViewController: JSQMessagesViewController {
    
    // Configure chat settings for JSQMessages
    let incomingChatBubble = JSQMessagesBubbleImageFactory().incomingMessagesBubbleImage(with: UIColor.jsq_messageBubbleBlue())
    let outgoingChatBubble = JSQMessagesBubbleImageFactory().outgoingMessagesBubbleImage(with: UIColor.jsq_messageBubbleLightGray())
    fileprivate let kCollectionViewCellHeight: CGFloat = 12.5
    
    // Configure Watson Conversation items
    var conversationMessages = [JSQMessage]()
    var conversation : Conversation!
    var context: Context?
    var workspaceID: String!
    
    let group = DispatchGroup()
    
    override func viewDidLoad() {
        
        super.viewDidLoad()
        self.setupTextBubbles()
        // Remove attachment icon from toolbar
        self.inputToolbar.contentView.leftBarButtonItem = nil
        
        NotificationCenter.default.addObserver(self, selector: #selector(didBecomeActive), name: NSNotification.Name.UIApplicationDidBecomeActive, object: nil)
    }
    
    func reloadMessagesView() {
        DispatchQueue.main.async {
            self.collectionView?.reloadData()
        }
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        SwiftSpinner.show("Connecting to Watson", animated: true)
        // Create a configuration path for the BMSCredentials.plist file to read in Watson credentials
        let configurationPath = Bundle.main.path(forResource: "BMSCredentials", ofType: "plist")
        let configuration = NSDictionary(contentsOfFile: configurationPath!)
        // Set the Watson credentials for Conversation service from the BMSCredentials.plist
        let conversationPassword = configuration?["conversationPassword"] as! String
        let conversationUsername = configuration?["conversationUsername"] as! String
        let conversationWorkspaceID = configuration?["conversationWorkspaceID"] as! String  //YOUR_WORKSPACE_ID
        self.workspaceID = conversationWorkspaceID
        // Create date format for Conversation service version
        let version = "2016-12-15"
        // Initialize conversation object
        conversation = Conversation(username: conversationUsername, password: conversationPassword, version: version)
        // Initial conversation message from Watson
        conversation.message(withWorkspace: self.workspaceID, failure: failConversationWithError){
            response in
            for watsonMessage in response.output.text{
                
                let json = self.convertToDictionary(text:watsonMessage);
                let _text = json?["text"] as? String ?? ""
                let _query = json?["expression"] as? String ?? ""
                
                if(!_query.isEmpty){
                    self.searchCloudinary(query: _query)
                }
                print(_text);               
                
                let textToSay = "Watson says: '\(_text)'"
                self.myWatsonTTS(textToSay)
                
                
                // Create message object with Watson response
                let message = JSQMessage(senderId: "Watson", displayName: "Watson", text: _text) //watsonMessage)
                // Add message to conversation message array
                self.conversationMessages.append(message!)
                // Set current context
                self.context = response.context
                DispatchQueue.main.async {
                    self.finishSendingMessage()
                    SwiftSpinner.hide()
                }
                NSLog("watson got from STT: " + self.myWatsonSTT())
            }
        }
    }
    
    func didBecomeActive(_ notification: Notification) {
    }
    
    func myWatsonSTT () -> String {
        NSLog("STT starting to listen")
        let configurationPath = Bundle.main.path(forResource: "BMSCredentials", ofType: "plist")
        let configuration = NSDictionary(contentsOfFile: configurationPath!)
        
        //STT - listen for the *forecast* command
        let usernameSTT = configuration?["sttUsername"] as! String
        let passwordSTT = configuration?["sttPassword"] as! String
        let speechToText = SpeechToText(username: usernameSTT, password: passwordSTT)
        
        var settings = RecognitionSettings(contentType: .opus)
        //settings.continuous = true
        settings.interimResults = true
        var returnText = ""
        let failureSTT = {(error: Error) in print(error)}
        speechToText.recognizeMicrophone(settings: settings, failure: failureSTT) { results in
            NSLog("-----STT: trying speech to text")
            print(results.bestTranscript)
            let textFromStt = results.bestTranscript
            speechToText.stopRecognizeMicrophone()
            NSLog("-----STT: stopping Mic with: " + textFromStt)
            returnText = textFromStt
            self.didPressSend(nil, withMessageText: textFromStt, senderId: self.senderId, senderDisplayName: self.senderDisplayName, date: Date())
            
        }
            
        return returnText
    }
    
    func myWatsonTTS (_ textToSay: String){
        //initiate WATSON TTS
        //TTS - say the data
        // Create a configuration path for the BMSCredentials.plist file to read in Watson credentials
        let configurationPath = Bundle.main.path(forResource: "BMSCredentials", ofType: "plist")
        let configuration = NSDictionary(contentsOfFile: configurationPath!)
        
        let username = configuration?["ttsUsername"] as! String
        let password = configuration?["ttsPassword"] as! String
        let textToSpeech = TextToSpeech(username: username, password: password)
        let failureTTS = { (error: Error) in print(error) }
        textToSpeech.synthesize(textToSay, voice: SynthesisVoice.us_Michael.rawValue, failure: failureTTS) { data in
            var audioPlayer: AVAudioPlayer // see note below
            audioPlayer = try! AVAudioPlayer(data: data)
            audioPlayer.prepareToPlay()
            audioPlayer.play()
            sleep(5)
            NSLog("end of tts")
            
        }
        
        
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    
    // Function handling errors with Tone Analyzer
    func failConversationWithError(_ error: Error) {
        // Print the error to the console
        print(error)
        SwiftSpinner.hide()
        // Present an alert to the user describing what the problem may be
        DispatchQueue.main.async {
            self.showAlert("Conversation Failed", alertMessage: "The Conversation service failed to reply. This could be due to invalid creditials, internet connection or other errors. Please verify your credentials in the WatsonCredentials.plist and rebuild the application. See the README for further assistance.")
        }
    }
    
    // Function to show an alert with an alertTitle String and alertMessage String
    func showAlert(_ alertTitle: String, alertMessage: String){
        // If an alert is not currently being displayed
        if(self.presentedViewController == nil){
            // Set alert properties
            let alert = UIAlertController(title: alertTitle, message: alertMessage, preferredStyle: UIAlertControllerStyle.alert)
            // Add an action to the alert
            alert.addAction(UIAlertAction(title: "Dismiss", style: UIAlertActionStyle.default, handler: nil))
            // Show the alert
            self.present(alert, animated: true, completion: nil)
        }
    }
    
    // Setup text bubbles for conversation
    func setupTextBubbles() {
        // Create sender Id and display name for user
        self.senderId = "TestUser"
        self.senderDisplayName = "TestUser"
        // Set avatars for user and Watson
        collectionView?.collectionViewLayout.incomingAvatarViewSize = CGSize(width: 28, height:32 )
        collectionView?.collectionViewLayout.outgoingAvatarViewSize = CGSize(width: 37, height:37 )
        automaticallyScrollsToMostRecentMessage = true
        
    }
    // Set how many items are in the collection view
    override func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return self.conversationMessages.count
    }
    
    // Set message data for each item in the collection view
    override func collectionView(_ collectionView: JSQMessagesCollectionView!, messageDataForItemAt indexPath: IndexPath!) -> JSQMessageData! {
        return self.conversationMessages[indexPath.row]
        
    }
    
    // Set whih bubble image is used for each message
    override func collectionView(_ collectionView: JSQMessagesCollectionView!, messageBubbleImageDataForItemAt indexPath: IndexPath!) -> JSQMessageBubbleImageDataSource! {
        return conversationMessages[indexPath.item].senderId == self.senderId ? outgoingChatBubble : incomingChatBubble
        
    }
    
    // Set which avatar image is used for each chat bubble
    override func collectionView(_ collectionView: JSQMessagesCollectionView, avatarImageDataForItemAt indexPath: IndexPath) -> JSQMessageAvatarImageDataSource? {
        let message = conversationMessages[(indexPath as NSIndexPath).item]
        var avatar: JSQMessagesAvatarImage
        if (message.senderId == self.senderId){
            avatar  = JSQMessagesAvatarImageFactory.avatarImage(with: UIImage(named:"avatar_small"), diameter: 37)
        }
        else{
            avatar  = JSQMessagesAvatarImageFactory.avatarImage(with: UIImage(named:"watson_avatar"), diameter: 32)
        }
        return avatar
    }
    
    // Create and display timestamp for every third message in the collection view
    override func collectionView(_ collectionView: JSQMessagesCollectionView, attributedTextForCellTopLabelAt indexPath: IndexPath) -> NSAttributedString? {
        if ((indexPath as NSIndexPath).item % 3 == 0) {
            let message = conversationMessages[(indexPath as NSIndexPath).item]
            return JSQMessagesTimestampFormatter.shared().attributedTimestamp(for: message.date)
        }
        return nil
    }
    
    // Set the height for the label that holds the timestamp
    override func collectionView(_ collectionView: JSQMessagesCollectionView, layout collectionViewLayout: JSQMessagesCollectionViewFlowLayout, heightForCellTopLabelAt indexPath: IndexPath) -> CGFloat {
        if (indexPath as NSIndexPath).item % 3 == 0 {
            return kJSQMessagesCollectionViewCellLabelHeightDefault
        }
        return kCollectionViewCellHeight
    }
    
    // Create the cell for each item in collection view
    override func collectionView(_ collectionView: UICollectionView,
                                 cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = super.collectionView(collectionView, cellForItemAt: indexPath)
            as! JSQMessagesCollectionViewCell
        let message = self.conversationMessages[(indexPath as NSIndexPath).item]
        // Set the UI color of each cell based on who the sender is
        if message.senderId == senderId {
            cell.textView!.textColor = UIColor.black
        } else {
            cell.textView!.textColor = UIColor.white
        }
        return cell
    }
    
    // Handle actions when user presses send button
    override func didPressSend(_ button: UIButton!, withMessageText text: String!, senderId: String!, senderDisplayName: String!, date: Date!) {
        // Create message based on user text
        let message = JSQMessage(senderId: senderId, senderDisplayName: senderDisplayName, date: date, text: text)
        // Add message to conversation messages array of JSQMessages
        self.conversationMessages.append(message!)
        DispatchQueue.main.async {
            self.finishSendingMessage()
        }
        
        // Get response from Watson based on user text
        let messageRequest = MessageRequest(text: text, context: self.context)
        conversation.message(withWorkspace: self.workspaceID, request: messageRequest, failure: failConversationWithError) { response in
            // Set current context
            self.context = response.context
            // Handle Watson response
            for watsonMessage in response.output.text{
                if(!watsonMessage.isEmpty){
                    // Create message based on Watson response
                    
                    
                    //---------- start : Text to speech ----------
                    
                    let json = self.convertToDictionary(text:watsonMessage);
                    let _text = json?["text"] as? String ?? ""
                    let _query = json?["expression"] as? String ?? ""
                    
                    if(!_query.isEmpty){
                        self.searchCloudinary(query: _query)
                    }
                    print(_text);
                    
                    //                self.displayCloudinaryImage(_secure_url);
                    
                    
                    
                    
                    let textToSay = _text
                    self.myWatsonTTS(textToSay)
                    
                    //---------- end : Text to speech ----------
                    
                    
                    let message = JSQMessage(senderId: "Watson", displayName: "Watson", text: _text) //watsonMessage)
                    
                    // Add message to conversation message array
                    self.conversationMessages.append(message!)
                    DispatchQueue.main.async {
                        self.finishSendingMessage()
                    }
                    
                }
            }
        }
    }
    
    //cloudinary helpers
    func convertToDictionary(text: String) -> [String: Any]? {
        if let data = text.data(using: .utf8) {
            do {
                return try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any]
            } catch {
                print(error.localizedDescription)
            }
        }
        return nil
    }
    
    func searchCloudinary(query:String){
        print(query)
        
        let config = URLSessionConfiguration.default // Session Configuration
        let session = URLSession(configuration: config) // Load configuration into Session
        
        let webserviceURL = "https://<web-service>.run.webtask.io/cloudinary-search?accessKey=<web-service-key>&expression=" + query.addingPercentEncoding(withAllowedCharacters: .urlHostAllowed)!
        print(webserviceURL)
        
        
        
        
        let url = URL(string: webserviceURL)
        
        let task = session.dataTask(with: url!, completionHandler: {
            (data, response, error) in
            
            if error != nil {
                
                print(error!.localizedDescription)
                
            } else {
                
                do {
                    
                    if let data = data {
                        
                        let results = JSON(data: data)
                        
                        //SLACK starts
                        let bot = SlackKit()
                        bot.addWebAPIAccessWithToken("xoxb-<slack-web-service>")
                        bot.webAPI?.authenticationTest(success: { (success) in
                            print(success)
                            NSLog("bot webAPI: \(success)")
                        }, failure: nil)
                        let helloAction = Action(name: "Cloudinary task", text: "yay")
                        var slackText = ""
                        NSLog("inside bot")
                        var endIndex = "1"
                        
                        // loop through results
                        for (index, photo):(String, JSON) in results {
                            //Do something you want
                            let _index = Int(index)
                            
                            if( _index==0){
                                slackText = "Hello Marek - these are the links to Cloudinary assets: \n"
                            }
                            
                            
                            let _tags = photo["tags"]
                            let _secure_url = photo["secure_url"]
                            let _public_id = photo["public_id"]
                            let _filename = photo["filename"]
                            
                            print("Cloudinary image id: \n", _public_id, _tags, "\nSecure URL:\n" ,_secure_url )
                            print(photo, index)
                            slackText = slackText + "\( _index!+1). filename: \( _filename ), tags: \( _tags) , Secure URL: \n\(_secure_url)\n"
                            
                            endIndex = "\( _index!+1 )"
                            
                        }
                        let attachment = Attachment(fallback: "Hello World", title: "Welcome to SlackKit", callbackID: "hello_world", actions: [helloAction])
                        bot.webAPI?.sendMessage(channel: "C6VHKLV8C", text: slackText, attachments: [], success: nil, failure: nil)
                        
                        self.didPressSend(nil, withMessageText: "I got pictures from 1 to " + endIndex, senderId: "watson", senderDisplayName: "watson", date: Date())
                        
                         //let message = JSQMessage(senderId: self.senderId, displayName: self.senderDisplayName, text: "I got pictures from 1 to " + endIndex) //watsonMessage)
                         
                         // Add message to conversation message array
                         //self.conversationMessages.append(message!)
                           // DispatchQueue.main.async {
                             //   self.finishSendingMessage()
                         //}
                        
                    }
                    
                    
                    
                    if let json = try JSONSerialization.jsonObject(with: data!, options: []) as? [String: Any]
                    {
                        
                        //Implement your logic
                        
                        
                        print(json)
                        
                    }
                    
                } catch {
                    
                    print("error in JSONSerialization")
                    
                }
                
                
            }
            
        })
        task.resume()
    }
}
```




Delivered to you by Cloudinary and IBM 

Subscribe to our Twitter: @Dan and @blumareks
